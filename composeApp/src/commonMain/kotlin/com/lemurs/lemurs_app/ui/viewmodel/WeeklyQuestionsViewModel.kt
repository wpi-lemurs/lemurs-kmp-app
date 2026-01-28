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
import com.lemurs.lemurs_app.survey.Surveys
import com.lemurs.lemurs_app.survey.fetchAndParseWeeklySurvey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class WeeklySurveyResponse(
    val surveyResponseId: Int,
    val message: String
)

class WeeklyQuestionsViewModel : ViewModel(), KoinComponent {
    var surveys = mutableStateOf<List<Surveys>?>(null)
    var surveyAnswers = mutableStateOf<HashMap<Int, HashMap<Int, String>>>(hashMapOf())
    // Change the request stack to accept surveyResponseId parameter
    private val _requestStack1 = MutableStateFlow<MutableList<(Int) -> Unit>>(mutableListOf())

    // Add survey response ID management
    private val _currentSurveyResponseId = MutableStateFlow<Int>(-1)

    val logger = Logger.withTag("WeeklyQuestions")
    private val appRepository: AppRepository by inject()

    // Update addRequest to accept functions that take surveyResponseId
    fun addRequest(request: (Int) -> Unit) {
        logger.d { "Adding request to stack" }
        _requestStack1.value.add(request)
        logger.d { "Request added to stack" }

    }

    init {
        viewModelScope.launch {
            try {
                surveys.value = fetchAndParseWeeklySurvey()
                if (surveys.value != null) {
                    surveyAnswers.value = hashMapOf()
                    for (survey in surveys.value!!) {
                        surveyAnswers.value[survey.id] = hashMapOf()
                    }
                }
            } catch (e: Exception) {
                logger.e("Failed to fetch surveys: ${e.message}", e)
            }
        }
    }

    suspend fun submitSurvey(onSurveySuccess: ((Int) -> Unit)? = null) {
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

        try {
            for (completedSurvey in completedSurveys) {
                val answersMap = completedSurvey.answers.associate { it.id.toString() to it.answer }
                val answersJson = Json.encodeToString(answersMap)
                logger.w("Survey id submission saving into local database with id = ${completedSurvey.id}")
                val surveyResponse = SurveyResponse(
                    id = completedSurvey.id,
                    answers = answersJson,
                    timestamp = now.toString(),
                    notificationTime = getNotificationTime().toString(),
                    type = 2
                )
                // Save locally for retry
                appRepository.saveSurveyResponseLocally(surveyResponse)
                // Get provisionally a survey response ID
                // Negative ID indicates a failed submission
                setCurrentSurveyResponseId(-1 * completedSurvey.id)
                // Schedule background worker to retry
                SendDataScheduler().scheduleSurveyResponse()
                onSurveySuccess?.invoke(-1 * completedSurvey.id)
            }
        } catch (e: Exception) {
            logger.e("Failed to submit survey: ${e.message}", e)
        }
    }

    fun getNotificationTime(): Instant {
        logger.w("getting notification time...")
        val notificationTimesImpl: NotificationTimesImpl by inject()

        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time

        val morningStart = LocalTime(8, 0)
        val morningEnd = LocalTime(13, 0)
        val afternoonStart = LocalTime(15, 0)
        val afternoonEnd = LocalTime(20, 0)
        
        // Use runBlocking to ensure we get the result synchronously
        val notificationsStart = runBlocking {
            try {
                if (now >= morningStart && now < morningEnd) {
                    val morningTime = notificationTimesImpl.getMorningTime().first()
                    if (morningTime.isNotEmpty()) {
                        Instant.parse(morningTime)
                    } else {
                        Clock.System.now()
                    }
                } else if (now >= afternoonStart && now < afternoonEnd) {
                    val afternoonTime = notificationTimesImpl.getAfternoonTime().first()
                    if (afternoonTime.isNotEmpty()) {
                        Instant.parse(afternoonTime)
                    } else {
                        Clock.System.now()
                    }
                } else {
                    Clock.System.now()
                }
            } catch (e: Exception) {
                logger.e("Error parsing notification time: ${e.message}")
                Clock.System.now()
            }
        }
        
        logger.w("got notification time: $notificationsStart")
        return notificationsStart
    }

    fun executeRequests() {
        if (_requestStack1.value.isEmpty()) {
            logger.d { "No requests to execute" }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                logger.d { "Submitting survey first to get surveyResponseId" }
                // Submit survey and wait for the response
                submitSurvey { surveyResponseId ->
                    logger.d { "Survey submitted successfully, now executing ${_requestStack1.value.size} queued requests with surveyResponseId: $surveyResponseId" }
                    // Execute all queued requests with the surveyResponseId
                    for (request in _requestStack1.value) {
                        try {
                            logger.d { "Invoking request with surveyResponseId: $surveyResponseId" }
                            request.invoke(surveyResponseId)
                        } catch (e: Exception) {
                            logger.e("Error executing request: ${e.message}", e)
                        }
                    }
                    clearRequests()
                }
            } catch (e: Exception) {
                logger.e("Error in executeRequests: ${e.message}", e)
                clearRequests()
            }
        }
    }

    /**
     * Gets the current survey response ID that was returned from the backend.
     * Returns -1 if no survey has been submitted yet or no ID is available.
     */
    fun getCurrentSurveyResponseId(): Int {
        return _currentSurveyResponseId.value
    }

    /**
     * Sets the current survey response ID. This can be used when resuming
     * an existing survey session.
     */
    fun setCurrentSurveyResponseId(surveyResponseId: Int) {
        _currentSurveyResponseId.value = surveyResponseId
        logger.d { "Set current survey response ID to: $surveyResponseId" }
    }

    /**
     * Clears the current survey response ID. This should be called when
     * a survey session is completed or cancelled.
     */
    fun clearCurrentSurveyResponseId() {
        val clearedId = _currentSurveyResponseId.value
        _currentSurveyResponseId.value = -1
        logger.d { "Cleared survey response ID: $clearedId" }
    }

    fun clearRequests() {
        _requestStack1.value.clear()
    }

    // Helper functions for adding specific types of requests to the stack

    /**
     * Adds an audio submission request to the stack.
     * The audio will be submitted after the survey is completed and the surveyResponseId is available.
     */
    fun addAudioRequest(audioViewModel: AudioViewModel) {
        addRequest { surveyResponseId ->
            logger.d { "Executing audio request with surveyResponseId: $surveyResponseId" }
            //if (surveyResponseId < 0) {
                //logger.e { "Invalid surveyResponseId: $surveyResponseId" }
                audioViewModel.saveAudioData(surveyResponseId)
            //} else {
                logger.d { "Submitting audio data with surveyResponseId: $surveyResponseId" }
                //audioViewModel.submitAudioData(surveyResponseId)
            //}
        }
    }

    /**
     * Adds a text prompt submission request to the stack.
     * This is a placeholder for future text prompt functionality.
     */
    fun addTextPromptRequest(text: String, writingViewModel: WritingViewModel) {
        addRequest { surveyResponseId ->
            logger.d { "Executing text/writing prompt request with surveyResponseId: $surveyResponseId, text: $text" }
            //if (surveyResponseId < 0) {
                //logger.e { "Invalid surveyResponseId: $surveyResponseId" }
                writingViewModel.saveWritingData(text, surveyResponseId)
            //} else {
                logger.d { "Submitting text/writing prompt data with surveyResponseId: $surveyResponseId, text: $text" }
                //writingViewModel.submitWritingData( text, surveyResponseId)
            //}
        }
    }
    suspend fun submitAllWeeklyData() {
        viewModelScope.launch {
            if(appRepository.handleSurveyResponse()){
                logger.d { "All weekly data submitted successfully" }
            }
            else {
                logger.e { "Failed to submit all weekly data" }
                SendDataScheduler().scheduleSurveyResponse()
            }
        }
    }
}
