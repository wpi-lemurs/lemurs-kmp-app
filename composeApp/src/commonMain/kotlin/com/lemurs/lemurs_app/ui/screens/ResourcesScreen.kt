package com.lemurs.lemurs_app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lemurs.lemurs_app.ui.theme.LemurBlack
import com.lemurs.lemurs_app.ui.theme.LemurBlue
import com.lemurs.lemurs_app.ui.theme.LemurButtonBlue
import com.lemurs.lemurs_app.ui.theme.LemurDarkGrey
import com.lemurs.lemurs_app.util.UriOpener


@Composable
fun ResourcesScreen(
    onNavigateTo: (String) -> Unit,
    uriOpener: UriOpener
){
    val uriHandler = LocalUriHandler.current
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.wrapContentHeight().fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Resources",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 40.sp,
                    lineHeight = 50.sp
                ),
                modifier = Modifier.padding(bottom = 16.dp, top = 32.dp),
                color = LemurDarkGrey,
                textAlign = TextAlign.Center,
            )
        }
        Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "WPI SDCC: 508-831-5540",
                //            fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            ClickableText(
                text = buildAnnotatedString {
                    pushStringAnnotation(tag = "URL", annotation = "https://emutivo.wpi.edu/")
                    withStyle(
                        style = SpanStyle(
                            color = LemurButtonBlue,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append("Click here to go to the EMUTIVO Website")
                    }
                    pop()
                },
                onClick = { offset ->
                    uriOpener.openUri("https://emutivo.wpi.edu/")
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                buildAnnotatedString {
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
}