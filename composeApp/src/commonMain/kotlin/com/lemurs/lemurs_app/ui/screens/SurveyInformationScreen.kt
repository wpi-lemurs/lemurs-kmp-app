package com.lemurs.lemurs_app.ui.screens


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
import com.lemurs.lemurs_app.ui.theme.LemurBlack
import com.lemurs.lemurs_app.ui.theme.LemurDarkGrey


@Composable
fun SurveyInformationScreen(
    onNavigateTo:  (String) -> Unit  // KMM compatible: For navigating to other screens
) {
    val isCompleted = rememberSaveable { mutableStateOf(false) }
    val onNextButtonClicked = { onNavigateTo(LemurScreen.Demographics.name) }
    val scrollState = rememberLazyListState()

    // Update completion state based on scroll position
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.isScrollInProgress }
            .collect {
                val lastItemVisible = scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                val totalItems = scrollState.layoutInfo.totalItemsCount

                if (lastItemVisible == totalItems - 1) {
                    isCompleted.value = true
                }
            }
    }
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(0.9f).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Mental Health\nDetection Study",
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp, lineHeight =  50.sp),
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = LemurDarkGrey,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = LemurBlack
                                )
                            ) {
                                append("Instructions for Participants:\n")
                            }
                            append("Welcome to your survey! Please remember we do not review responses in real-time.")

                        })
                    Text(
                        buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = LemurBlack
                                )
                            ) {
                                append("\nGoal: ")
                            }
                            append("The goal of this project is to build an AI tool to screen for mental health. The more information you share, the more effectively we can detect mental health conditions which could save lives!")

                            withStyle(
                                style = SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = LemurBlack
                                )
                            ) {
                                append("\n\nProcedure: ")
                            }
                            append("This survey will only take around 5 minutes to complete. You will be asked to answer questions, record samples of your voice, and share phone/social media data. No private messages will be collected.")
                            withStyle(
                                style = SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = LemurBlack
                                )
                            ) {
                                append("\n\nPrivacy: ")
                            }
                            append("All information you give will be stored anonymously on a secure server and encrypted.")
                            withStyle(
                                style = SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = LemurBlack
                                )
                            ) {
                                append("\n\nVoluntary/Risk: ")
                            }
                            append("You can share as much or little data as you would like and you may stop the study at any time.")
                            withStyle(
                                style = SpanStyle(
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.Bold,
                                    color = LemurBlack
                                )
                            ) {
                                append("\n\nUMASS: ")
                            }
                            append("If you are having a mental health crisis, please call CCPH at 413-545-2337, or call or text 988.")

                            withStyle(
                                style = SpanStyle(
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.Bold,
                                    color = LemurBlack
                                )
                            ) {
                                append("\n\nWPI: ")
                            }
                            append("If you are having a mental health crisis, please contact SDCC at 508-831-5540, or call or text 988.")

                        }
                    )
                }
            }
            item{Spacer(modifier = Modifier.height(32.dp))}
        }

        if (isCompleted.value) {
            BottomBar(
                onBottomBarClick = onNextButtonClicked,
                isClickable = true
            )
        } else {
            BottomBar(
                onBottomBarClick = {},
                isClickable = false
            )
        }

    }

}
/*
//@Preview
@Composable
fun SurveyInformationPreview() {
    Lemurs_AppTheme {
        SurveyInformationScreen(
            onNextButtonClicked = {},
            onHomeButtonClicked = {},
            modifier = Modifier.fillMaxSize(),
            isCompleted =
        )
    }
}*/