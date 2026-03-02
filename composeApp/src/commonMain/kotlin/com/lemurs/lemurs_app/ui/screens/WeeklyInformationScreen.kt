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
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lemurs.lemurs_app.ui.theme.LemurBlack
import com.lemurs.lemurs_app.ui.theme.LemurButtonBlue
import com.lemurs.lemurs_app.ui.theme.LemurDarkGrey
import com.lemurs.lemurs_app.util.UriOpener

@Composable
fun WeeklyInformationScreen(
    onNavigateTo: (String) -> Unit,
    onBack: () -> Unit,
    uriOpener: UriOpener
) {
    val onNextButtonClicked = { onNavigateTo(LemurScreen.Weekly.name) }
    val scrollState = rememberLazyListState()
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
                                append("\n\nProcedure: ")
                            }
                            append("This survey will only take around 5 minutes to complete. Try to complete the survey in one sitting without closing the app, if the app is reopened while taking the survey your answers will not save. You will be asked to answer questions about yourself and your mental health.")
                            withStyle(
                                style = SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = LemurBlack
                                )
                            ) {
                                append("\n\nVoluntary/Risk: ")
                            }
                            append("You can share as much or little data as you would like and you may stop the study at any time.")
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Crisis resources section - left aligned
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        // UMASS Section with clickable phone numbers
                        Text(
                            text = "UMASS:",
                            fontWeight = FontWeight.Bold,
                            color = LemurBlack
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ClickableText(
                            text = buildAnnotatedString {
                                append("If you are having a mental health crisis, please call CCPH at ")
                                withStyle(
                                    style = SpanStyle(
                                        color = LemurButtonBlue,
                                        textDecoration = TextDecoration.Underline
                                    )
                                ) {
                                    append("413-545-2337")
                                }
                                append(", or call or text ")
                                withStyle(
                                    style = SpanStyle(
                                        color = LemurButtonBlue,
                                        textDecoration = TextDecoration.Underline
                                    )
                                ) {
                                    append("988")
                                }
                                append(".")
                            },
                            onClick = { offset ->
                                when {
                                    offset in 62..73 -> uriOpener.openUri("tel:413-545-2337")
                                    offset in 90..92 -> uriOpener.openUri("tel:988")
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // WPI Section with clickable phone numbers
                        Text(
                            text = "WPI:",
                            fontWeight = FontWeight.Bold,
                            color = LemurBlack
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ClickableText(
                            text = buildAnnotatedString {
                                append("If you are having a mental health crisis, please contact ")
                                withStyle(
                                    style = SpanStyle(
                                        color = LemurButtonBlue,
                                        textDecoration = TextDecoration.Underline
                                    )
                                ) {
                                    append("508-831-5540")
                                }
                                append(" (or after 5pm ")
                                withStyle(
                                    style = SpanStyle(
                                        color = LemurButtonBlue,
                                        textDecoration = TextDecoration.Underline
                                    )
                                ) {
                                    append("508-831-5555")
                                }
                                append("), or call or text ")
                                withStyle(
                                    style = SpanStyle(
                                        color = LemurButtonBlue,
                                        textDecoration = TextDecoration.Underline
                                    )
                                ) {
                                    append("988")
                                }
                                append(".")
                            },
                            onClick = { offset ->
                                when {
                                    offset in 57..68 -> uriOpener.openUri("tel:508-831-5540")
                                    offset in 84..95 -> uriOpener.openUri("tel:508-831-5555")
                                    offset in 115..117 -> uriOpener.openUri("tel:988")
                                }
                            }
                        )
                    }
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