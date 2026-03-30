package com.lemurs.lemurs_app.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.data.datastore.NotificationTimesImpl
import com.lemurs.lemurs_app.data.local.SendDataScheduler
import com.lemurs.lemurs_app.data.local.activeData.SurveyResponse
import com.lemurs.lemurs_app.data.repositories.AppRepository
import com.lemurs.lemurs_app.survey.Answers
import com.lemurs.lemurs_app.survey.CompletedSurveys
import com.lemurs.lemurs_app.survey.DangerAlertTrigger
import com.lemurs.lemurs_app.survey.Questions
import com.lemurs.lemurs_app.survey.SurveySubmission
import com.lemurs.lemurs_app.survey.Surveys
import com.lemurs.lemurs_app.survey.fetchAndParseDailySurvey
import com.lemurs.lemurs_app.survey.fetchDangerAlertTriggers
import com.lemurs.lemurs_app.survey.postDailySurvey
import com.lemurs.lemurs_app.util.DemoMode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DailyQuestionsViewModel : ViewModel(), KoinComponent {
    var surveys = mutableStateOf<List<Surveys>?>(null)

    var surveyAnswers = mutableStateOf<HashMap<Int, HashMap<Int, String>>>(hashMapOf())
    var dangerAlertTriggerQuestionIds = mutableStateOf<Set<Int>>(emptySet())
    var dangerAlertTriggerThresholds = mutableStateOf<Map<Int, Int>>(emptyMap())
    val logger = Logger.withTag("DailyQuestions")
    var currentQuestionIndex = mutableStateOf(0)

    private val _requestStack1 = MutableStateFlow<MutableList<() -> Unit>>(mutableListOf())
    private val appRepository: AppRepository by inject()
    val notificationTimesImpl : NotificationTimesImpl by inject()

    fun addRequest(request: () -> Unit) {
        logger.d { "Adding request to stack" }
        _requestStack1.value.add { request() }
        logger.d { "Request added to stack" }

    }


    init {
        viewModelScope.launch {
            surveys.value = fetchAndParseDailySurvey()
            if (surveys.value != null) {
                surveyAnswers.value = hashMapOf()
                for (survey in surveys.value!!) {
                    surveyAnswers.value[survey.id] = hashMapOf()
                }
            }

            loadDangerAlertTriggers()
        }
    }

    suspend fun refreshDailySurvey() {
        surveys.value = fetchAndParseDailySurvey()
    }

    suspend fun refreshDangerAlertTriggers() {
        loadDangerAlertTriggers()
    }

    private suspend fun loadDangerAlertTriggers() {
        try {
            val triggers = fetchDangerAlertTriggers().filter { it.resolvedIsActive }
            dangerAlertTriggerQuestionIds.value =
                triggers.mapNotNull(DangerAlertTrigger::resolvedQuestionId).toSet()
            dangerAlertTriggerThresholds.value =
                triggers.mapNotNull { trigger ->
                    val questionId = trigger.resolvedQuestionId
                    val threshold = trigger.resolvedThreshold
                    if (questionId != null && threshold != null) questionId to threshold else null
                }.toMap()
            logger.d {
                val previewIds = dangerAlertTriggerQuestionIds.value.sorted().take(5)
                "Loaded danger alert triggers: active=${triggers.size}, " +
                    "questionIds=${dangerAlertTriggerQuestionIds.value.size}, " +
                    "thresholds=${dangerAlertTriggerThresholds.value.size}, " +
                    "previewIds=$previewIds"
            }
        } catch (e: Exception) {
            logger.w("Failed to fetch danger alert triggers: ${e.message}. Falling back to survey metadata.")
            dangerAlertTriggerQuestionIds.value = emptySet()
            dangerAlertTriggerThresholds.value = emptyMap()
        }
    }

    fun shouldTriggerDangerAlert(question: Questions, normalizedAnswer: String): Boolean {
        val serverTriggeredQuestion = dangerAlertTriggerQuestionIds.value.contains(question.id)
        val localTriggeredQuestion = question.resolvedIsTriggerQuestion
        if (!serverTriggeredQuestion && !localTriggeredQuestion) {
            return false
        }

        val isYesAnswer = normalizedAnswer.equals("yes", ignoreCase = true)
        if (isYesAnswer) {
            return true
        }

        val answerValue = normalizedAnswer.toIntOrNull() ?: return false
        val threshold =
            dangerAlertTriggerThresholds.value[question.id] ?: question.resolvedTriggerThreshold
        return threshold != null && answerValue >= threshold
    }


    fun getSurveyType(): Int {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
        return when {
            now >= LocalTime(8, 0) && now < LocalTime(13, 0) -> 0 // morning
            now >= LocalTime(15, 0) && now < LocalTime(20, 0) -> 1 // afternoon
            else -> 2 // weekly (phq-9) or other
        }
    }

    suspend fun submitSurvey() {
        // In demo mode, simulate successful survey submission
        if (DemoMode.isActive) {
            logger.d("Demo mode: simulating successful daily survey submission")
            return
        }

        val now = Clock.System.now()
        val completedSurveys = ArrayList<CompletedSurveys>()
        for (surveyId in surveyAnswers.value.keys) {
            val answers = ArrayList<Answers>()
            for (answerId in surveyAnswers.value[surveyId]?.keys!!) {
                val answer = Answers(answerId, surveyAnswers.value[surveyId]!![answerId]!!)
                answers.add(answer)
            }
            val completedSurvey = CompletedSurveys(surveyId, answers)
            completedSurveys.add(completedSurvey)
        }
        val submission = SurveySubmission(now, completedSurveys, getNotificationTime())
        try {
            // Try to submit survey
            val result = postDailySurvey(submission) // Should return Boolean or Result
            if (!result.status.isSuccess()) {
                throw Exception("Network is unreachable or server error")
            }
        } catch (e: Exception) {
            // Save locally and schedule worker
            val surveyType = getSurveyType()
            for (completedSurvey in completedSurveys) {
                val answersMap = completedSurvey.answers.associate { it.id.toString() to it.answer }
                val answersJson = Json.encodeToString(answersMap)
                val surveyResponse = SurveyResponse(
                    id = completedSurvey.id,
                    answers = answersJson,
                    timestamp = now.toString(),
                    notificationTime = getNotificationTime().toString(),
                    type = surveyType
                )
                // Save locally for retry
                appRepository.saveSurveyResponseLocally(surveyResponse)
                // Schedule background worker to retry
                SendDataScheduler().scheduleSurveyResponse()
            logger.w { "Survey submission failed, saved locally and scheduled background sync." }
            }
            // Notify user about network issue
            logger.e { "Survey not sent: ${e.message}. Saved locally and will retry when online." }
            // Optionally, trigger UI notification here (Toast/Snackbar)
            // TODO: Implement network sync scheduling if needed
        }
    }

    fun getNotificationTime() : Instant{
        logger.w("getting notification time...")
        var notificationsStart = Clock.System.now()

        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time

        val morningStart = LocalTime(8, 0)
        val morningEnd = LocalTime(13, 0)
        val afternoonStart = LocalTime(15, 0)
        val afternoonEnd = LocalTime(20, 0)
        
        // Use runBlocking to wait for the result and handle empty notification times
        notificationsStart = runBlocking {
            try {
                if (now >= morningStart && now < morningEnd) {
                    val morningTime = notificationTimesImpl.getMorningTime().first()
                    if (!morningTime.isNullOrEmpty()) {
                        Instant.parse(morningTime)
                    } else {
                        logger.w("No morning notification was fired, user opened app directly - using current time")
                        Clock.System.now()
                    }
                } else if (now >= afternoonStart && now < afternoonEnd) {
                    val afternoonTime = notificationTimesImpl.getAfternoonTime().first()
                    if (!afternoonTime.isNullOrEmpty()) {
                        Instant.parse(afternoonTime)
                    } else {
                        logger.w("No afternoon notification was fired, user opened app directly - using current time")
                        Clock.System.now()
                    }
                } else {
                    logger.w("Survey completed outside normal hours - using current time")
                    Clock.System.now()
                }
            } catch (e: Exception) {
                logger.e("Error getting notification time: ${e.message}, using current time as fallback")
                Clock.System.now()
            }
        }
        
        logger.w("got notification time: $notificationsStart")
        return notificationsStart
    }

    fun preparingSurveyRequest(){
        viewModelScope.launch(Dispatchers.IO) {
            submitSurvey()
        }
    }

    fun addSubmitSurveyToRequestStack(){
        addRequest(::preparingSurveyRequest)
    }
}