package com.lemurs.lemurs_app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import com.lemurs.lemurs_app.App
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
//import androidx.navigation.NavHostController
//import androidx.navigation.compose.rememberNavController
import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.ui.reusableComponents.GoalsModule
import com.lemurs.lemurs_app.ui.reusableComponents.SurveyOpenButton
import com.lemurs.lemurs_app.ui.theme.LemurButtonBlue
import com.lemurs.lemurs_app.ui.theme.LemurDarkerGrey
import com.lemurs.lemurs_app.ui.theme.LemurDarkestGrey
import com.lemurs.lemurs_app.ui.theme.LemurWhite
import com.lemurs.lemurs_app.ui.theme.LemursAppTheme
import com.lemurs.lemurs_app.ui.viewmodel.ProgressViewModel
import com.lemurs.lemurs_app.ui.viewmodel.SurveyAvailabilityViewModel
import com.lemurs.lemurs_app.getPlatform
import org.koin.compose.viewmodel.koinViewModel

// Platform-specific expect declaration for iOS notification scheduling
expect fun scheduleWeeklySurveyNotificationIos(nextWeeklySurvey: String)

@Composable
fun MainScreen(onNavigateTo: (String) -> Unit) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val progressViewModel: ProgressViewModel = koinViewModel()
    val surveyAvailabilityViewModel: SurveyAvailabilityViewModel = koinViewModel()
    val logger = Logger.withTag("MainScreen")


    LaunchedEffect(Unit) {
        progressViewModel.newRefreshProgress()
        surveyAvailabilityViewModel.clearAvailabilityCache()
        surveyAvailabilityViewModel.refreshAvailability()
        logger.w("Launched Effect called ${surveyAvailabilityViewModel.getAvailability()}" )

        // Enable debug mode for testing - allows taking surveys multiple times
        // Comment out this line when not testing
//        surveyAvailabilityViewModel.enableDebugMode()
    }
    val currentProgress = progressViewModel.newProgress.value

    LaunchedEffect(currentProgress?.nextWeeklySurvey) {
        // Only run on iOS
        if (getPlatform().name.lowercase().contains("ios")) {
            val nextWeeklySurvey = currentProgress?.nextWeeklySurvey
            val timeUntilWeekly = surveyAvailabilityViewModel.secondsUntilAvailable("weekly")
            // Only schedule if availability is loaded AND the survey is closed (future date).
            // When timeUntilWeekly is null (cache not yet loaded) or negative (currently open),
            // skip — a subsequent recompose with fresh data will schedule correctly.
            if (nextWeeklySurvey != null && timeUntilWeekly != null && timeUntilWeekly > 0) {
                scheduleWeeklySurveyNotificationIos(nextWeeklySurvey)
                logger.w { "Scheduled weekly survey notification for $nextWeeklySurvey" }
            } else {
                logger.w { "Skipping notification scheduling (timeUntilWeekly=$timeUntilWeekly)" }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.padding(16.dp))
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val timeUntilDaily = surveyAvailabilityViewModel.secondsUntilAvailable("daily")
            Text(
                text = "Daily Survey",
                style = MaterialTheme.typography.titleLarge,
                color = LemurDarkestGrey,
            )
            if (timeUntilDaily == null) {
                CircularProgressIndicator()
            } else {
                SurveyOpenButton(onNavigate = {onNavigateTo(LemurScreen.DailyInformation.name)},  timeUntil = timeUntilDaily)
                Spacer(modifier = Modifier.height(8.dp))
            }

            val timeUntilWeekly = surveyAvailabilityViewModel.secondsUntilAvailable("weekly")
            logger.w { "(MainScreen) Weekly timeUntil: $timeUntilWeekly from availability=${surveyAvailabilityViewModel.getAvailability()}" }
            Text(
                text = "Weekly Survey",
                style = MaterialTheme.typography.titleLarge,
                color = LemurDarkestGrey,
            )
            if (timeUntilWeekly == null) {
                CircularProgressIndicator()
            } else {
                SurveyOpenButton(onNavigate = {onNavigateTo(LemurScreen.WeeklyInformation.name)}, timeUntil = timeUntilWeekly)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (progressViewModel.getProgress() == null) {
                CircularProgressIndicator()
            } else {
                logger.w { "Current Progress: $currentProgress" }

                if (currentProgress != null) {
                    GoalsModule(
                        title = "Weeks 1-2 Bonus Goal",
                        reward = 10.00,
                        progressLabel = "Complete at least 23 Daily Surveys",
                        completed = currentProgress.dailySurveysTotalCompleted,
                        total = 23 // significant changes need to made to API to avoid hardcoded values
                    )
                    GoalsModule(
                        title = "Weeks 1-3 Bonus Goal",
                        reward = 15.00,
                        progressLabel = "Complete at least 34 Daily Surveys",
                        completed = currentProgress.dailySurveysTotalCompleted,
                        total = 34
                    )
                    GoalsModule(
                        title = "Weeks 1-4 Bonus Goal",
                        reward = currentProgress.dailySurveysTotalBonus,
                        progressLabel = "Complete at least 45 Daily Surveys",
                        completed = currentProgress.dailySurveysTotalCompleted,
                        total = currentProgress.dailySurveysTotalGoal
                    )
                }
            }
        }
    }
}

//@Preview
@Composable
fun PreviewMainScreen(){
    LemursAppTheme {
        // Since i'm using a simple navigator callback
        // I create a navigator class called AppNavigator
        // This is just a preview, no actual navigation needed here.
        MainScreen(onNavigateTo = {})
    }
}
