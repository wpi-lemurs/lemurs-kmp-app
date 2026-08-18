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

/**
 * Displays a ticking countdown.
 *
 * Purely a display: it no longer reports whether the survey is open. Openness is decided from the
 * survey windows in the participant's timezone, so a countdown that merely reached zero was never
 * the right signal — it drifted from the real state whenever the two disagreed.
 *
 * Keyed on [totalSeconds] so a refreshed value replaces the running count rather than being
 * ignored by a stale `remember`.
 */
@Composable
fun CountdownTimer(totalSeconds: Long) {
    var timeLeft by remember(totalSeconds) { mutableStateOf(totalSeconds.coerceAtLeast(0L)) }

    LaunchedEffect(totalSeconds) {
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
        text = when {
            days == 0L ->
                "${hours.toString().padStart(2, '0')}" +
                    ":${minutes.toString().padStart(2, '0')}" +
                    ":${seconds.toString().padStart(2, '0')}"

            days == 1L -> "$hours Hours"
            else -> "$days Days"
        },
        style = MaterialTheme.typography.titleSmall,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        color = LemurDarkerGrey
    )
}
