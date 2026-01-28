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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.lemurs.lemurs_app.ui.theme.LemurButtonBlue
import com.lemurs.lemurs_app.ui.theme.LemurButtonGrey
import com.lemurs.lemurs_app.ui.theme.LemurDarkerGrey
import com.lemurs.lemurs_app.ui.theme.LemurWhite

@Composable
fun OpenOrCloseRow(totalSeconds: Long): Boolean {
    var isOpenNow by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (isOpenNow) "Open now" else "Opens in",
            style = MaterialTheme.typography.titleSmall,
            color = LemurDarkerGrey,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        isOpenNow = CountdownTimer(totalSeconds) // Adjust your countdown value
    }
    return isOpenNow
}

@Composable
fun SurveyOpenButton(onNavigate: () -> Unit, timeUntil: Long) {
    var isOpenNow by remember { mutableStateOf(false) }

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
            isOpenNow = OpenOrCloseRow(timeUntil)
        }

        Button(
            onClick = onNavigate,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isOpenNow) LemurButtonBlue else LemurButtonGrey,
                disabledContainerColor = LemurButtonGrey,
                disabledContentColor = LemurDarkerGrey
            ),
            enabled = isOpenNow,
            modifier = Modifier
                .shadow(if (isOpenNow) 4.dp else 0.dp, RoundedCornerShape(8.dp))
                .height(36.dp)
                .fillMaxWidth(0.68f)
                .align(Alignment.TopCenter)
        ) {
            Text(
                text = "Start Survey",
                color = LemurWhite,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                textAlign = TextAlign.Center
            )
        }
    }
}