package com.lemurs.lemurs_app.ui.screens

//import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lemurs.lemurs_app.ui.theme.LemurBrightGrey
import com.lemurs.lemurs_app.ui.theme.LemurButtonBlue
import com.lemurs.lemurs_app.ui.theme.LemurDarkerGrey
import com.lemurs.lemurs_app.ui.theme.LemurGrey
import com.lemurs.lemurs_app.ui.theme.LemurLightGrey
import com.lemurs.lemurs_app.ui.theme.LemurWhite
import com.lemurs.lemurs_app.ui.theme.LemursAppTheme


@Composable
fun ProgressScreen(
    onNavigateTo: (String) -> Unit,
    totalEarnings: String,
    dailyProgress: Int,
    weeklyProgress: Int
) {
    val earningsItems = listOf(
        "Daily Surveys" to "$7.50",
        "Weekly Surveys" to "$10.00",
        "Permissions" to "$20.00",
        "Milestones" to "$12.50"
    )
    val totalEarnings = earningsItems.sumOf { it.second.replace("$", "").toDouble() }
    val totalEarningsFormatted = "$${totalEarnings.toString()}"


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "My Progress",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Normal),
                modifier = Modifier.padding(bottom = 8.dp),
                color = LemurLightGrey
            )
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Total Earnings: $totalEarningsFormatted",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = LemurDarkerGrey,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Daily Surveys Progress
        item {
            ProgressBarWithLabel(
                label = "Daily Surveys Completed",
                progress = dailyProgress,
                total = 28
            )
        }


        // Weekly Progress Bar
        item {
            ProgressBarWithLabel(
                label = "Weekly Surveys Completed",
                progress = weeklyProgress,
                total = 5
            )
        }

        // Earnings Breakdown Card
        item {
            Spacer(modifier = Modifier.height(32.dp))
            ProgressBreakdownCard(
                items = earningsItems
            )
        }

    }

}

@Composable
fun ProgressBarWithLabel(label: String, progress: Int, total: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(0.8f)
    ) {
        // Progress label
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = LemurGrey
            )
            Text(
                text = "$progress/$total",
                style = MaterialTheme.typography.bodyMedium,
                color = LemurGrey
            )
        }

        // Progress bar
        LinearProgressIndicator(
            progress = progress.toFloat() / total,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RectangleShape),
            color = LemurButtonBlue, // Custom teal-like color
            trackColor = LemurGrey
        )

        // Percentage Text
        Text(
            text = "${(progress * 100 / total)}%",
            style = MaterialTheme.typography.bodySmall,
            color = LemurGrey,
            modifier = Modifier.align(Alignment.Start)
        )
    }
}

@Composable
fun ProgressBreakdownCard(items: List<Pair<String, String>>) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .wrapContentHeight()
            .border(1.dp, color = LemurBrightGrey, shape = RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = LemurWhite),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Title
            Text(
                text = "Progress Breakdown",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .align(Alignment.CenterHorizontally),
                color = LemurDarkerGrey
            )

            // List each progress item
            items.forEach { (label, value) ->
                ProgressBreakdownRow(label = label, value = value)
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Total
            val totalValue: Double = items.sumOf { it.second.replace("$", "").toDouble() }
            ProgressBreakdownRow(
                label = "Total",
                value = "$${totalValue}",
                isBold = true
            )
        }
    }
}

@Composable
fun ProgressBreakdownRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            else MaterialTheme.typography.bodyMedium,
            color = LemurDarkerGrey
        )
        Text(
            text = value,
            style = if (isBold) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            else MaterialTheme.typography.bodyMedium,
            color = LemurDarkerGrey
        )
    }
}

//@Preview
@Composable
fun ProgressPreview() {
    LemursAppTheme {
        ProgressScreen(
            onNavigateTo = {},
            totalEarnings = "100",
            dailyProgress = 3,
            weeklyProgress = 1
        )
    }
}