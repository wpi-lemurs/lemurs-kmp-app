package com.lemurs.lemurs_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.ui.reusableComponents.BreakdownRow
import com.lemurs.lemurs_app.ui.reusableComponents.ValueDisplay
import com.lemurs.lemurs_app.ui.theme.LemurBlack
import com.lemurs.lemurs_app.ui.theme.LemurBlue
import com.lemurs.lemurs_app.ui.theme.LemurBrightBlue
import com.lemurs.lemurs_app.ui.theme.LemurButtonBlue
import com.lemurs.lemurs_app.ui.theme.LemurDarkBlue
import com.lemurs.lemurs_app.ui.theme.LemurDarkGrey
import com.lemurs.lemurs_app.ui.theme.LemurDarkerGrey
import com.lemurs.lemurs_app.ui.theme.LemurGrey
import com.lemurs.lemurs_app.ui.theme.LemurLightBlue
import com.lemurs.lemurs_app.ui.theme.LemurLightGrey
import com.lemurs.lemurs_app.ui.theme.LemurWhite
import com.lemurs.lemurs_app.ui.theme.LemursAppTheme
import com.lemurs.lemurs_app.ui.viewmodel.ProgressViewModel
import com.lemurs.lemurs_app.ui.viewmodel.SubmissionViewModel
import com.lemurs.lemurs_app.ui.viewmodel.SurveyAvailabilityViewModel
import com.lemurs.lemurs_app.ui.viewmodel.WeeklyQuestionsViewModel
import com.lemurs.lemurs_app.util.formatTwoDecimals
//import lemurs_app.shared.generated.resources.Res
//import lemurs_app.shared.generated.resources.lemuricon
//import org.jetbrains.compose.resources.ExperimentalResourceApi
//import org.jetbrains.compose.resources.painterResource
//import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
//import org.koin.compose.viewmodel.koinViewModel

enum class LemurScreen(val title: String) {
    Main(title = "main_screen"),
    Survey(title = "survey_screen"),
    Profile(title = "profile"),
    Progress(title = "progress"),
    History(title = "history"),
    Information(title = "information"),
    Demographics(title = "demographics"),
    Permissions(title = "permissions"),
    Daily(title = "daily"),
    Weekly(title = "weekly"),
    Writing(title = "writing"),
    Audio(title = "audio"),
    Submission(title = "submission"),
    Login(title = "login"),
    HealthConnect(title = "health_connect"),
    DailyInformation(title = "dailyinformation"),
    WeeklyInformation(title = "weeklyinformation"),
    Resources(title = "resources")
}

/*
* This is the configuration for everything in the top bar of the app
* */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LemursTopBar(
    currentScreen: LemurScreen,
    canNavigateBack: Boolean,
    onNavigateBack: () -> Unit = {},
    onNavigateTo: (String) -> Unit = {},
    onMenuClick: () -> Unit = {},
    brandPainter: Painter? = null,
    modifier: Modifier = Modifier,
) {
    if (currentScreen == LemurScreen.Login) return //doesn't render topbar if login screen

    val progressViewModel: ProgressViewModel = koinInject()
    var showSkipDialog by remember { mutableStateOf(false) }
    val logger: Logger = Logger.withTag("LemursTopBar")
    val currentProgress = progressViewModel.newProgress.value
    val earnings = currentProgress?.earned as? Double ?: 0.0
    val earningsFormatted = formatTwoDecimals(earnings)
    val submissionViewModel: SubmissionViewModel = koinInject()
    val weeklyQuestionsViewModel: WeeklyQuestionsViewModel = koinInject()
    val surveyAvailabilityViewModel: SurveyAvailabilityViewModel = koinInject()
    val scope = rememberCoroutineScope()

    if (currentScreen == LemurScreen.Main) {
        LaunchedEffect(Unit) {
            progressViewModel.newRefreshProgress()
        }
    }

    TopAppBar(
        title = {
            Box(
                modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LemurBlue)
                    .wrapContentSize(),
                contentAlignment = Alignment.Center
            ) {
                // show the earnings if the current screen is the main screen
                if (currentScreen == LemurScreen.Main) {
                    if (progressViewModel.getProgress() == null) {
                        CircularProgressIndicator()
                    } else {
//                        logger.d { "Progress: ${progressViewModel.getProgress()}" }
//                        logger.d { "Earnings: ${progressViewModel.getProgress()!!.earned}" }
                        logger.d { "The value display value is here" }
                        logger.d {" The other way for getting progress: $currentProgress"}
                        if (currentProgress == null) {
                            CircularProgressIndicator()
                        } else {
                            Card(
                                modifier = modifier,
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = LemurButtonBlue)
                            ) {
                                Text(
                                    text = "Earnings: $${earningsFormatted}",
                                    modifier = Modifier.padding(8.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = LemurWhite
                                )
                            }
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(containerColor = LemurDarkBlue),
        modifier = Modifier,

        // Show back button if navigation is possible
        navigationIcon = {
            when {
                currentScreen == LemurScreen.Profile || currentScreen == LemurScreen.Resources-> {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = LemurWhite
                        )
                    }
                }
                currentScreen == LemurScreen.Main -> {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = "Menu",
                            tint = Color.White
                        )
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight().aspectRatio(1f)
                            .padding(start = 8.dp, bottom = 6.dp, top = 6.dp)
                    ) {
                        brandPainter?.let { painter ->
                            androidx.compose.foundation.Image(
                                painter = painter,
                                contentDescription = "Lemur Icon",
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .aspectRatio(1f),
                            )
                        }
                    }
                }
            }
        },
        // Skip button for specific screens, navigates to the next screen
        actions = {
            val skipableScreens = mutableListOf(
                LemurScreen.Writing, LemurScreen.Audio
            )

            if (skipableScreens.contains(currentScreen)) {
                TextButton(
                    onClick = { showSkipDialog = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "Skip",
                            fontSize = 16.sp,
                            color = Color(0xFF999999),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Arrow Forward",
                            tint = Color(0xFF999999),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    )
    // Show skip dialog if active
    if (showSkipDialog) {
        AlertDialog(
            onDismissRequest = { showSkipDialog = false },
            title = {
                Text(
                    text = "Skip ${currentScreen.name} Prompt?",
                    style = MaterialTheme.typography.headlineLarge,
                    color = LemurBlack
                )
            },
            text = {
                Text(
                    text = buildAnnotatedString {
                        append("You will lose ")
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append("$1.50")
                        }
                        append(" if you skip the ${currentScreen.name} prompt. Do you want to continue?")
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
                    color = LemurDarkerGrey
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSkipDialog = false
                        val skipableScreens = listOf(
                            LemurScreen.Writing, LemurScreen.Audio, LemurScreen.Submission
                        )
                        val nextScreen =
                            skipableScreens[(skipableScreens.indexOf(currentScreen) + 1) % skipableScreens.size].name
                        submissionViewModel.markItemSkipped("${currentScreen.name} Prompt")
                        if (currentScreen == LemurScreen.Audio) {
                            scope.launch {
                                weeklyQuestionsViewModel.submitAllWeeklyData()
                                // Invalidate cached availability so MainScreen reads fresh state
                                surveyAvailabilityViewModel.clearAvailabilityCache()
                                progressViewModel.refreshProgress()
                                progressViewModel.newRefreshProgress()
                                surveyAvailabilityViewModel.refreshAvailability()
                                onNavigateTo(nextScreen)
                            }
                        } else {
                            onNavigateTo(nextScreen)
                        }
                    }
                ) {
                    Text(text = "Skip",
                        style = MaterialTheme.typography.headlineLarge,
                        color = LemurLightGrey)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSkipDialog = false }) {
                    Text(text = "Stay",
                        style = MaterialTheme.typography.headlineLarge,
                        color = LemurBlack)
                }
            }
        )
    }
}

@Composable
fun BottomBar(onBottomBarClick: () -> Unit, isClickable: Boolean, modifier: Modifier = Modifier, text: String = "") {

    // Set background color of Bottom Bar and change if clicked
    val backgroundColor = if (isClickable) LemurDarkBlue else LemurGrey

    // Screens where bottom bar is present: Information, Demographics, Permissions, Daily, PHQ,
    // Writing, Audio

    Row(
        modifier =
        Modifier.fillMaxWidth().height(50.dp)
            .then(
                // Enables click if clickable (set within each screen file)
                if (isClickable) Modifier.clickable { onBottomBarClick() } else Modifier)
            .background(backgroundColor),
        verticalAlignment = Alignment.CenterVertically) {
        Text(
            // Label changes based on clickability (Complete Section -> Submit)
            text = text.ifEmpty { "Next section" },
            modifier = Modifier.background(color = Color.Transparent).fillMaxWidth(),
            // Change color based on clickability
            color = if (backgroundColor == LemurGrey) LemurBlack else LemurWhite,
            style = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
        )
    }
}
