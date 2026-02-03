package com.lemurs.lemurs_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lemurs.lemurs_app.ui.reusableComponents.BottomLinearProgressBar
import com.lemurs.lemurs_app.ui.theme.LemurBrightGrey
import com.lemurs.lemurs_app.ui.theme.LemurDarkBlue
import com.lemurs.lemurs_app.ui.theme.LemurDarkGrey
import com.lemurs.lemurs_app.ui.theme.LemurDarkerGrey
import com.lemurs.lemurs_app.ui.theme.LemurGrey
import com.lemurs.lemurs_app.ui.theme.LemurLightGrey
import com.lemurs.lemurs_app.ui.theme.inputFieldColors
import com.lemurs.lemurs_app.ui.viewmodel.SubmissionViewModel
import com.lemurs.lemurs_app.ui.viewmodel.WeeklyQuestionsViewModel
import com.lemurs.lemurs_app.ui.viewmodel.WritingViewModel
import androidx.compose.runtime.rememberCoroutineScope
import com.lemurs.lemurs_app.ui.viewmodel.ProgressViewModel
import com.lemurs.lemurs_app.ui.viewmodel.SurveyAvailabilityViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
// Import Calendar
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.daysUntil
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WritingScreen(onNavigateTo: (String) -> Unit) {

    // Added fields from WritingViewModel
    val writingViewModel: WritingViewModel = koinViewModel()
    val viewModel: WeeklyQuestionsViewModel = koinViewModel()
    var text by rememberSaveable { mutableStateOf("") }
    val writingCompleted = rememberSaveable { mutableStateOf(text.length > 10) }
    val submissionViewModel: SubmissionViewModel = koinInject()
    // added this ----
    val surveyAvailabilityViewModel: SurveyAvailabilityViewModel = koinViewModel()
    val progressViewModel: ProgressViewModel = koinViewModel()
    val coroutineScope = rememberCoroutineScope()

    // Track submission state to prevent multiple submissions
    var isSubmitting by remember { mutableStateOf(false) }

    val handleSubmission: () -> Unit = {
        coroutineScope.launch {
            if (!isSubmitting) {
                isSubmitting = true
                try {
                    viewModel.addTextPromptRequest(text, writingViewModel)
                    viewModel.executeRequests()
                    // Refresh progress data like daily screen does
                    progressViewModel.refreshProgress()
                    progressViewModel.newRefreshProgress()
                    surveyAvailabilityViewModel.refreshAvailability()
                    submissionViewModel.markItemCompleted("Writing Prompt", "1.50")
                    onNavigateTo(LemurScreen.Audio.name)
                } finally {
                    isSubmitting = false
                }
            }
        }
    }

    // --- Start of Weekly Question Logic ---
    val weeklyQuestions = listOf(
        "Describe your favorite place.",
        "Describe a good friend.",
        "Describe an interesting person you have met.",
        "Describe an inspirational friend or family member."
    )

    var currentSurveyQuestion by remember { mutableStateOf("Loading question...") }

    fun getWeekOfYear(date: kotlinx.datetime.LocalDate): Int {
        // Simplified ISO week calculation (Monday as first day)
        val firstDayOfYear = kotlinx.datetime.LocalDate(date.year, 1, 1)
        val daysBetween = firstDayOfYear.daysUntil(date)
        return (daysBetween / 7) + 1
    }

    LaunchedEffect(Unit) { // Runs once on initial composition
        val now = Clock.System.now()
        val localDate = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val weekOfYear = getWeekOfYear(localDate)
        // Ensure questionIndex is always valid and cycles through the list
        val questionIndex = if (weeklyQuestions.isNotEmpty()) {
            (weekOfYear - 1).coerceAtLeast(0) % weeklyQuestions.size
        } else {
            -1 // Or handle empty list case appropriately
        }

        currentSurveyQuestion = if (questionIndex != -1) {
            weeklyQuestions[questionIndex]
        } else {
            "No survey questions available."
        }
    }
    // --- End of Weekly Question Logic ---

    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Centered Writing Prompt Title
            Text(
                text = "Writing Prompt",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp),
                color = LemurDarkGrey,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            )

            Text(
                text = "Type your response to the prompt in the text box below.",
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                textAlign = TextAlign.Start,
                color = LemurGrey
            )

            Text(
                text = currentSurveyQuestion,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                textAlign = TextAlign.Start,
                color = LemurDarkerGrey
            )

            Text(
                text = "Response must be at least 25 characters long.",
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                textAlign = TextAlign.Start,
                color = LemurGrey
            )

            // Text Field with 5-line height and Character Counter
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it.take(1000)
                    writingCompleted.value = text.length > 25
                },
                label = { Text("Enter Response") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 250.sp.value.dp), // Approx. 5 lines
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = inputFieldColors(),
                maxLines = 5 // TextField will expand if necessary but starts with 5 lines
            )

            // Character Counter
            Text(
                text = "${text.length}/1000",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .wrapContentWidth(Alignment.End),
                color = LemurGrey
            )
        }
        // Bottom Bar and Progress Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(alignment = Alignment.BottomCenter)
        ) {

            BottomBar(

                onBottomBarClick = handleSubmission,
                isClickable = writingCompleted.value && !isSubmitting,
                text = if (isSubmitting) "Submitting..." else ""
            )

        }

    }


}
