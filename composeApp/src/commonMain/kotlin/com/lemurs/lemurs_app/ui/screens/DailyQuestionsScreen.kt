package com.lemurs.lemurs_app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
//import androidx.navigation.NavController
import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.survey.Questions
import com.lemurs.lemurs_app.ui.reusableComponents.QuestionFactory
import com.lemurs.lemurs_app.ui.theme.LemurButtonBlue
import com.lemurs.lemurs_app.ui.viewmodel.DailyQuestionsViewModel
import com.lemurs.lemurs_app.ui.viewmodel.ProgressViewModel
import com.lemurs.lemurs_app.ui.viewmodel.SubmissionViewModel
import com.lemurs.lemurs_app.ui.viewmodel.SurveyAvailabilityViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DailyQuestionsScreen(onNavigateTo: (String) -> Unit = {}) {
    val viewModel: DailyQuestionsViewModel = koinViewModel()
    val progressViewModel: ProgressViewModel = koinViewModel()
    val surveyAvailabilityViewModel: SurveyAvailabilityViewModel = koinViewModel()
    val coroutineScope = rememberCoroutineScope()
    val logger = Logger.withTag("DailyQuestionsScreen")
    var categorizedQuestionsLength = 1
    val scrollState = rememberScrollState()
    val submissionViewModel: SubmissionViewModel = koinInject()
    val showDangerAlert = remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    // Set survey type and clear items at start of daily survey
    LaunchedEffect(Unit) {
        submissionViewModel.clearSurveyItems()
        submissionViewModel.setSurveyType("daily")
    }

    // Track submission state to prevent multiple submissions
    var isSubmitting by remember { mutableStateOf(false) }

    // Add state to track submission
    var isFinalSubmission by remember { mutableStateOf(false) }

    // Function to handle submission after alert
    val handlePostAlertSubmission: () -> Unit = {
        showDangerAlert.value = false
        coroutineScope.launch {
            if (!isSubmitting) {
                isSubmitting = true
                try {
                    viewModel.submitSurvey()
                    progressViewModel.refreshProgress()
                    progressViewModel.newRefreshProgress()
                    surveyAvailabilityViewModel.refreshAvailability()
                    onNavigateTo(LemurScreen.Submission.name)
                } finally {
                    isSubmitting = false
                }
            }
        }
    }

    if (showDangerAlert.value && isFinalSubmission) {
        AlertDialog(
            onDismissRequest = handlePostAlertSubmission,
            title = { Text("Immediate Support Available") },
            text = {
                val annotatedString = buildAnnotatedString {
                    append("For immediate support, please use the following resources:\n\n")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("UMASS: ")
                    }
                    append("If you are having a mental health crisis, please call CCPH at ")

                    pushStringAnnotation(tag = "tel", annotation = "4135452337")
                    withStyle(
                        style = SpanStyle(
                            color = LemurButtonBlue,
                            textDecoration = TextDecoration.Underline
                        )
                    ) { append("413-545-2337") }
                    pop()

                    append(", or call or text ")

                    pushStringAnnotation(tag = "tel", annotation = "988")
                    withStyle(
                        style = SpanStyle(
                            color = LemurButtonBlue,
                            textDecoration = TextDecoration.Underline
                        )
                    ) { append("988") }
                    pop()

                    append(".\n\n")

                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("WPI: ")
                    }
                    append("If you are having a mental health crisis, please contact SDCC at ")

                    pushStringAnnotation(tag = "tel", annotation = "5088315540")
                    withStyle(
                        style = SpanStyle(
                            color = LemurButtonBlue,
                            textDecoration = TextDecoration.Underline
                        )
                    ) { append("508-831-5540") }
                    pop()

                    append(", or call or text ")

                    pushStringAnnotation(tag = "tel", annotation = "988")
                    withStyle(
                        style = SpanStyle(
                            color = LemurButtonBlue,
                            textDecoration = TextDecoration.Underline
                        )
                    ) { append("988") }
                    pop()

                    append(".")
                }
                ClickableText(
                    text = annotatedString,
                    onClick = { offset ->
                        annotatedString
                            .getStringAnnotations(
                                tag = "tel",
                                start = offset,
                                end = offset
                            )
                            .firstOrNull()
                            ?.let { annotation ->
                                uriHandler.openUri("tel:${annotation.item}")
                            }
                    }
                )
            },
            confirmButton = {
                Button(onClick = handlePostAlertSubmission) { Text("OK") }
            }
        )
    }

    // Show a loading indicator while the survey data is being fetched
    val surveys = viewModel.surveys.value.orEmpty()

    if (surveys.isEmpty()) {
        // Either still loading or no surveys available - show a loading indicator for now.
        CircularProgressIndicator()
    } else {
        val survey = surveys[0]
        val userAnswersForThisSurvey = viewModel.surveyAnswers.value[survey.id] ?: hashMapOf()

        val categorizedQuestions = mutableListOf<List<Questions>>()
        var currentSublist = mutableListOf<Questions>()
        for (question in survey.questions) {
            if (question.style == "category" && currentSublist.isNotEmpty()) {
                categorizedQuestions.add(currentSublist)
                currentSublist = mutableListOf()
            }
            currentSublist.add(question)
        }
        if (currentSublist.isNotEmpty()) {
            categorizedQuestions.add(currentSublist)
        }
        categorizedQuestionsLength = categorizedQuestions.size

        var currentQuestions =
            if (viewModel.currentQuestionIndex.value < categorizedQuestions.size)
                categorizedQuestions[viewModel.currentQuestionIndex.value]
            else emptyList()
        currentQuestions =
            currentQuestions.filter { question: Questions ->
                if (question.prerequisiteQuestionId == null) {
                    true
                } else {
                    val userAnswer = userAnswersForThisSurvey[question.prerequisiteQuestionId]
                    val requiredAnswer = question.prerequisiteAnswer?.split(",") ?: emptyList()
                    userAnswer != null &&
                        requiredAnswer.any {
                            it.equals(userAnswer.toString(), ignoreCase = true)
                        }
                }
            }

        val interactiveQuestions =
            currentQuestions.filter { it.style != "category" && it.style != "parent-question" }

        val isCompleted by
            remember(currentQuestions, viewModel.surveyAnswers.value[survey.id]) {
                derivedStateOf {
                    interactiveQuestions.all { question ->
                        viewModel.surveyAnswers.value[survey.id]?.containsKey(question.id) == true
                    }
                }
            }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxHeight().fillMaxWidth(0.8F),
            ) {
                currentQuestions.forEach { question ->
                    key(question.id) {
                        QuestionFactory(
                            question = question,
                            onAnswerSelected = { answer ->
                                // create a new map of answered questions for Compose reactivity
                                val oldOuter = viewModel.surveyAnswers.value
                                val newOuter = HashMap(oldOuter)

                                val oldInner = newOuter[survey.id] ?: HashMap()
                                val newInner = HashMap(oldInner)
                                // Normalize yes-no answers
                                var normalizedAnswer = answer
                                if (question.style == "yes-no") {
                                    normalizedAnswer = when (answer) {
                                        "0" -> "yes"
                                        "1" -> "no"
                                        else -> answer
                                    }
                                } else {
                                    normalizedAnswer = when (answer) {
                                        "0" -> "1"
                                        "1" -> "2"
                                        "2" -> "3"
                                        "3" -> "4"
                                        "4" -> "5"
                                        else -> answer
                                    }
                                }

                                newInner[question.id] = normalizedAnswer
                                newOuter[survey.id] = newInner

                                if (viewModel.shouldTriggerDangerAlert(question, normalizedAnswer)) {
                                    showDangerAlert.value = true
                                }

                                viewModel.surveyAnswers.value = newOuter
                                logger.w("show danger alert value: ${showDangerAlert.value}")
                            },
                        )
                    }
                }
            }

            val onNextButtonClicked: () -> Unit = {
                if (viewModel.currentQuestionIndex.value < categorizedQuestionsLength - 1) {
                    logger.w { "The current question index is: ${viewModel.currentQuestionIndex.value}" }
                    logger.w { "The length of the questions is: ${survey.questions.size}" }
                    viewModel.currentQuestionIndex.value++
                } else {
                    // Mark items completed FIRST, before any async operations
                    submissionViewModel.markItemCompleted("Self-Harm Questions", "0.50")
                    submissionViewModel.markItemCompleted("Emotion Questions", "0.50")
                    submissionViewModel.markItemCompleted("Context Questions", "0.50")
                    submissionViewModel.markItemCompleted("Stress Questions", "0.50")
                    submissionViewModel.markItemCompleted("Regulation Questions", "0.50")
                    submissionViewModel.markItemCompleted("Sleep Questions", "0.50")

                    isFinalSubmission = true

                    // Check if alert should be shown
                    if (!showDangerAlert.value) {
                        // If no alert needed, submit
                        coroutineScope.launch {
                            isSubmitting = true
                            try {
                                viewModel.submitSurvey()
                                progressViewModel.refreshProgress()
                                progressViewModel.newRefreshProgress()
                                surveyAvailabilityViewModel.refreshAvailability()
                                onNavigateTo(LemurScreen.Submission.name)
                            } finally {
                                isSubmitting = false
                            }
                        }
                    }
                    // If alert should be shown, it will handle submission on dismissal
                }
            }

            BottomBar(
                onBottomBarClick = onNextButtonClicked,
                isClickable = isCompleted && !isSubmitting,
                text = if (isSubmitting && viewModel.currentQuestionIndex.value >= categorizedQuestionsLength - 1) "Submitting..." else ""
            )
        }
    }

    LaunchedEffect(viewModel.currentQuestionIndex.value) { scrollState.scrollTo(0) }
}
