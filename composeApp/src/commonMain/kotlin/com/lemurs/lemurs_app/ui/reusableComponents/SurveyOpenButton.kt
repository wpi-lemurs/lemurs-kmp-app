package com.lemurs.lemurs_app.ui.reusableComponents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lemurs.lemurs_app.survey.SurveyWindowState
import com.lemurs.lemurs_app.ui.theme.LemurButtonBlue
import com.lemurs.lemurs_app.ui.theme.LemurButtonGrey
import com.lemurs.lemurs_app.ui.theme.LemurDarkerGrey
import com.lemurs.lemurs_app.ui.theme.LemurWhite

/**
 * The daily survey button.
 *
 * Openness comes from [state], which was decided in the participant's own timezone. The button no
 * longer infers it from a countdown reaching zero — that made the label and the enabled state two
 * separate sources of truth that could disagree.
 */
@Composable
fun SurveyOpenButton(onNavigate: () -> Unit, state: SurveyWindowState) {
    when (state) {
        is SurveyWindowState.Open -> SurveyButtonLayout(
            label = "Open now",
            countdownSeconds = state.secondsUntilClose,
            buttonText = "Start Survey",
            enabled = true,
            onClick = onNavigate
        )

        is SurveyWindowState.AlreadyCompleted -> SurveyButtonLayout(
            label = "Next opens in",
            countdownSeconds = state.secondsUntilNextOpen,
            buttonText = "Completed",
            enabled = false,
            onClick = {}
        )

        is SurveyWindowState.Closed -> SurveyButtonLayout(
            label = "Opens in",
            countdownSeconds = state.secondsUntilNextOpen,
            buttonText = "Start Survey",
            enabled = false,
            onClick = {}
        )

        is SurveyWindowState.StudyConcluded -> SurveyButtonLayout(
            label = "Concluded",
            countdownSeconds = null,
            buttonText = "Study Concluded",
            enabled = false,
            onClick = {}
        )
    }
}

/** The weekly survey, which is gated on an absolute instant rather than a daily window. */
@Composable
fun WeeklySurveyOpenButton(onNavigate: () -> Unit, secondsUntilOpen: Long?) {
    val isOpen = secondsUntilOpen != null && secondsUntilOpen <= 0L
    SurveyButtonLayout(
        label = if (isOpen) "Open now" else "Opens in",
        countdownSeconds = if (isOpen) null else secondsUntilOpen,
        buttonText = "Start Survey",
        enabled = isOpen,
        onClick = onNavigate
    )
}

@Composable
private fun SurveyButtonLayout(
    label: String,
    countdownSeconds: Long?,
    buttonText: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .padding(top = 24.dp)
                .align(Alignment.TopCenter)
                .fillMaxWidth(0.55f)
                .height(40.dp)
        ) {
            Spacer(modifier = Modifier.height(15.dp))
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = LemurDarkerGrey,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (countdownSeconds != null && countdownSeconds > 0L) {
                    CountdownTimer(countdownSeconds)
                }
            }
        }

        Button(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (enabled) LemurButtonBlue else LemurButtonGrey,
                disabledContainerColor = LemurButtonGrey,
                disabledContentColor = LemurDarkerGrey
            ),
            enabled = enabled,
            modifier = Modifier
                .shadow(if (enabled) 4.dp else 0.dp, RoundedCornerShape(8.dp))
                .height(36.dp)
                .fillMaxWidth(0.68f)
                .align(Alignment.TopCenter)
        ) {
            Text(
                text = buttonText,
                color = LemurWhite,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                textAlign = TextAlign.Center
            )
        }
    }
}
