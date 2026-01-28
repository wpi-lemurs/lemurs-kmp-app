package com.lemurs.lemurs_app.ui.reusableComponents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lemurs.lemurs_app.ui.theme.LemurDarkerGrey
import com.lemurs.lemurs_app.ui.theme.LemurGrey
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun GoalsModule(
    title: String,
    reward: Double,
    progressLabel: String,
    completed: Int,
    total: Int,
    modifier: Modifier = Modifier,
    colors: GoalsModuleDefaults.Colors = GoalsModuleDefaults.defaultColors()
) {
    val cents = (reward * 100).roundToInt()
    val whole = cents / 100
    val fraction = (cents % 100).absoluteValue
    val rewardString = "$$whole.${fraction.toString().padStart(2, '0')}"
    val progress = completed.toDouble() / total.toDouble()
    val progressPercentage = (progress * 100).toInt().coerceAtMost(100) // cap at 100

    Column(modifier = modifier.padding(4.dp)) {
        // Card
        Card(
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = colors.cardColors
            //elevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 2.dp)
            ) {
                // Title and Reward Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.titleColor
                    )
                    Text(
                        text = rewardString,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.rewardColor
                    )
                }

                // Progress Label
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = progressLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = LemurGrey
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))

                // Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .background(
                            colors.progressTrackColor,
                            RoundedCornerShape(5.dp)
                        ) // Gray track
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress.toFloat())
                            .background(
                                colors.progressBarColor,
                                RoundedCornerShape(5.dp)
                            ) // Green progress
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Progress Percentage
                Text(
                    text = "$progressPercentage%",
                    style = MaterialTheme.typography.labelSmall,
                    color = LemurDarkerGrey,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
