package com.lemurs.lemurs_app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.ui.theme.LemurBrightGrey
import com.lemurs.lemurs_app.ui.theme.LemurButtonBlue
import com.lemurs.lemurs_app.ui.theme.LemurDarkBlue
import com.lemurs.lemurs_app.ui.theme.LemurDarkGrey
import com.lemurs.lemurs_app.ui.theme.LemurDarkerGrey
import com.lemurs.lemurs_app.ui.theme.LemurGrey
import com.lemurs.lemurs_app.ui.theme.LemurWhite
import com.lemurs.lemurs_app.ui.theme.LemurWhiteGrey
import com.lemurs.lemurs_app.ui.viewmodel.AudioViewModel
import com.lemurs.lemurs_app.ui.viewmodel.ProgressViewModel
import com.lemurs.lemurs_app.ui.viewmodel.SubmissionViewModel
import com.lemurs.lemurs_app.ui.viewmodel.SurveyAvailabilityViewModel
import com.lemurs.lemurs_app.ui.viewmodel.WeeklyQuestionsViewModel
import com.lemurs.lemurs_app.ui.viewmodel.WritingViewModel
import kotlinx.coroutines.launch
import lemurs_app.composeapp.generated.resources.Res
import lemurs_app.composeapp.generated.resources.delete_icon
import lemurs_app.composeapp.generated.resources.mic_24dp
import lemurs_app.composeapp.generated.resources.pause_icon
import lemurs_app.composeapp.generated.resources.play_arrow
import lemurs_app.composeapp.generated.resources.stop_record
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
// Import Calendar
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.daysUntil

@Composable
actual fun AudioScreen(onNavigateTo: (String) -> Unit) {
  val audioViewModel: AudioViewModel = koinViewModel()
  val viewModel: WeeklyQuestionsViewModel = koinViewModel()
  val progressViewModel: ProgressViewModel = koinViewModel()
  val surveyAvailabilityViewModel: SurveyAvailabilityViewModel = koinViewModel()
  val coroutineScope = rememberCoroutineScope()
  val submissionViewModel: SubmissionViewModel = koinInject()

  // Track submission state to prevent multiple submissions
  var isSubmitting by remember { mutableStateOf(false) }

  val handleSubmission: () -> Unit = {
    coroutineScope.launch {
      if (!isSubmitting) {
        isSubmitting = true
        try {
          viewModel.addAudioRequest(audioViewModel)
          viewModel.executeRequests()
          // Submit all weekly data FIRST before refreshing (like daily screen pattern)
          viewModel.submitAllWeeklyData()
          submissionViewModel.markItemCompleted("Audio Prompt", "1.50")
          // Refresh progress data AFTER submission is complete
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

  var isRecording by remember { mutableStateOf(false) }
  val buttonColor by remember { mutableStateOf(Color.Transparent) }
  val isPlaying by audioViewModel.isPlaying.collectAsState()
  val isCompleted = audioViewModel.audioExists.collectAsState()
  val recordingDuration by audioViewModel.recordingDuration.collectAsState()
  val logger: Logger = Logger.withTag("AudioScreen")

  val context = LocalContext.current
  val permissionLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
      if (!isGranted) {
        return@rememberLauncherForActivityResult
      }
    }

  val hasAudioPermission =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED


  LaunchedEffect(Unit) {
    if (!hasAudioPermission) {
      permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
  }

  // --- Start of Weekly Question Logic ---
  val weeklyQuestions = listOf(
    "Tell us about a place you love to visit.",
    "Describe your favorite happy memory.",
    "Describe what makes a good morning.",
    "Describe what type of music makes you happy."
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

    if (questionIndex != -1) {
      audioViewModel.setQuestionIndex(questionIndex)
    }
  }
  // --- End of Weekly Question Logic ---

  Box(modifier=Modifier.fillMaxSize()){
    Column(modifier = Modifier.fillMaxSize()) {
      Box(modifier = Modifier.fillMaxWidth().weight(0.8f)) {
        Column(modifier = Modifier.fillMaxSize()) {
          Text(
            text = "Audio Prompt",
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp),
            color = LemurDarkGrey,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
          )
          Text(
            text = "Press the microphone button, then answer the following prompt into the microphone.",
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 32.dp, bottom = 32.dp, end = 32.dp),
            textAlign = TextAlign.Start,
            color = LemurGrey
          )
          Text(
            modifier = Modifier.padding(start = 32.dp, bottom = 4.dp),
            text = currentSurveyQuestion,
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.SemiBold,
              fontSize = 20.sp
            ),
            color = LemurDarkerGrey,
            textAlign = TextAlign.Start

          )
          Text(
            text = "Response must be at least 15 seconds long.",
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 32.dp, end = 32.dp),
            textAlign = TextAlign.Start,
            color = LemurGrey
          )
        }
      }
      Box(
        modifier = Modifier.fillMaxWidth().weight(1f)
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Left Button (Play Audio)
            IconButton(
              onClick = {
                if (!isPlaying) {

                  audioViewModel.playAudio()
                } else {
                  audioViewModel.stopAudio()
                }
              },
              enabled = true,
              modifier =
              Modifier.size(64.dp)
                .background(buttonColor, CircleShape)
                .padding(0.dp)
                .border(width = 2.dp, color = if(isCompleted.value)LemurDarkerGrey else LemurBrightGrey, shape = CircleShape),
            ) {
              Icon(
                painter =
                painterResource(if (isPlaying) Res.drawable.pause_icon else Res.drawable.play_arrow),
                contentDescription = "Play/Stop Recording",
                tint = if(isCompleted.value)LemurGrey else LemurWhiteGrey,
                modifier = Modifier.size(48.dp)
              )
            }

            // Main Audio Button (Mic / Stop Recording)
            IconButton(
              onClick = {
                logger.d { "Button clicked" }
                logger.d { "Is recording: $isRecording" }
                isRecording = !isRecording
                if (isRecording) {

                  logger.d { "Starting recording" }
                  audioViewModel.startRecording()

                } else {
                  logger.d { "Stopping recording" }
                  audioViewModel.stopRecording()
                }
              },
              enabled = true,
              modifier =
              Modifier.size(90.dp)
                .background(LemurButtonBlue, CircleShape)
                .padding(0.dp)
                .border(width = 8.dp, color = if(isRecording) LemurDarkBlue else LemurButtonBlue, shape = CircleShape),
            ) {
              Icon(
                painter =
                painterResource(if (isRecording) Res.drawable.stop_record else Res.drawable.mic_24dp),
                contentDescription = "Mic/Stop Recording",
                tint = LemurWhite,
                modifier = Modifier.size(60.dp)
              )
            }

            // Right Button (Delete Audio)
            IconButton(
              onClick = {
                audioViewModel.clearRecording()
              },
              enabled = true,
              modifier =
              Modifier.size(64.dp)
                .background(buttonColor, CircleShape)
                .padding(0.dp)
                .border(width = 2.dp, color = if(isCompleted.value)LemurDarkerGrey else LemurBrightGrey, shape = CircleShape),
            ) {
              Icon(
                painter = painterResource(Res.drawable.delete_icon),
                contentDescription = "Clear Recording",
                tint = if(isCompleted.value)LemurGrey else LemurWhiteGrey,
                modifier = Modifier.size(48.dp)
              )
            }
          }
          Text(
            text = if (isRecording) "Tap to Stop Recording\nRecording Duration: ${recordingDuration}s" else "Tap to Start Recording",
            style = MaterialTheme.typography.bodyLarge.copy(
              fontWeight = FontWeight.Bold,
              color = LemurDarkerGrey
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
          )
        }

      }
    }
    Column(modifier = Modifier.fillMaxWidth().align(alignment = Alignment.BottomCenter)) {
      BottomBar(onBottomBarClick = handleSubmission, isClickable = isCompleted.value && !isSubmitting, text = if (isSubmitting) "Submitting..." else "Complete Survey")
    }
  }
}