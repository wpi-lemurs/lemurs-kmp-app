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
import com.lemurs.lemurs_app.ui.theme.LemurBlack
import com.lemurs.lemurs_app.ui.theme.LemurDarkGrey

@Composable
fun DailyInformationScreen(
    onNavigateTo: (String) -> Unit, // abstracted: KMM compatible. For navigating to other screens
    onBack: () -> Unit,             // abstracted: back navigation
) {
    val onNextButtonClicked = { onNavigateTo(LemurScreen.Daily.name) }
    val scrollState = rememberLazyListState()

    Column(
        modifier = Modifier.fillMaxSize(),
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
                                append("\n\nProcedure: ")
                            }
                            append("This survey will only take around 5 minutes to complete. You will be asked to answer questions and record samples of your voice. Try to complete the survey in one sitting without closing the app, if the app is reopened while taking the survey your answers will not save. You will be asked to answer questions about yourself and your mental health.")

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
                                    fontWeight = FontWeight.Bold,
                                    color = LemurBlack
                                )
                            ) {
                                append("\n\nUMASS: ")
                            }
                            append("If you are having a mental health crisis, please call CCPH at 413-545-2337, or call or text 988.")

                            withStyle(
                                style = SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = LemurBlack
                                )
                            ) {
                                append("\n\nWPI: ")
                            }
                            append("If you are having a mental health crisis, please contact 508-831-5540 (or after 5pm 508-831-5555), or call or text 988.")
                        }
                    )
                }
            }
            item{ Spacer(modifier = Modifier.height(32.dp)) }
        }


            BottomBar(
                onBottomBarClick = onNextButtonClicked,
                isClickable = true
            )
    }
}
