package com.lemurs.lemurs_app.ui.reusableComponents

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import com.lemurs.lemurs_app.ui.theme.LemurDarkerGrey
import kotlinx.coroutines.delay

@Composable
fun CountdownTimer(totalSeconds: Long): Boolean {
    var timeLeft by remember { mutableStateOf(totalSeconds) }

    // Handle cases where totalSeconds is zero or negative (already available / debug mode)
    if (totalSeconds <= 0L) {
        return true
    }

    var timeUp = false
    if (timeLeft <= 0L) {
        timeUp = true
    }
    LaunchedEffect(totalSeconds) {
        if (timeLeft < 0L) {
            timeLeft = 0L
        }
        while (timeLeft > 0L) {
            delay(1000L)
            timeLeft--
        }
    }

    val days = timeLeft / (24 * 60 * 60)
    val hours = (timeLeft % (24 * 60 * 60)) / (60 * 60)
    val minutes = (timeLeft % (60 * 60)) / 60
    val seconds = timeLeft % 60

    Text(
        text = if (days == 0L)
            "${hours.toString().padStart(2, '0')}" +
                    ":${minutes.toString().padStart(2, '0')}" +
                    ":${seconds.toString().padStart(2, '0')}"
        else if (days == 1L)
            "${hours.toString()} Hours"
        else
            "${days.toString()} Days",
        style = MaterialTheme.typography.titleSmall,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        color = LemurDarkerGrey
    )
    return timeUp
}