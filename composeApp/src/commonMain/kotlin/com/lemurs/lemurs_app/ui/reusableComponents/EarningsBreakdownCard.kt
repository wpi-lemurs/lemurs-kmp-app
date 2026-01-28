package com.lemurs.lemurs_app.ui.reusableComponents

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lemurs.lemurs_app.ui.theme.LemurBrightGrey
import com.lemurs.lemurs_app.ui.theme.LemurDarkerGrey
import com.lemurs.lemurs_app.ui.theme.LemurLightGrey
import com.lemurs.lemurs_app.ui.theme.LemurWhite
import com.lemurs.lemurs_app.util.formatTwoDecimals


@Composable
fun EarningsBreakdownCard(items: List<Pair<String, String>>) {
    val nonZeroItems = items.filter { it.second.replace("$", "").toDoubleOrNull() != 0.0 }
    val skippedItems = items.filter { it.second.replace("$", "").toDoubleOrNull() == 0.0 }
    Card(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .wrapContentHeight()
            .border(1.dp, color = LemurBrightGrey, shape = MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(containerColor = LemurWhite),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Display the non-zero items (Earnings Breakdown)
            Text(
                text = "Earnings Breakdown",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 16.dp).align(Alignment.CenterHorizontally),
                color = LemurDarkerGrey
            )
            nonZeroItems.forEach { (label, value) ->
                BreakdownRow(label = label, value = value)
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Display the skipped items (0 value)
            if (skippedItems.isNotEmpty()) {
                Text(
                    text = "Skipped Items",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = LemurDarkerGrey
                )
                skippedItems.forEach { (label, value) ->
                    BreakdownRow(label = label, value = value, isBold = false)
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }


            val totalValue = nonZeroItems.sumOf { it.second.replace("$", "").toDouble() }
            BreakdownRow(label = "Total", value = formatTwoDecimals(totalValue), isBold = true)
        }
    }
}

@Composable
fun BreakdownRow(label: String, value: String, isBold: Boolean = false) {
    val valueDouble = value.replace("$", "").toDoubleOrNull() ?: 0.0
    val textColor = if (valueDouble == 0.0) LemurLightGrey else LemurDarkerGrey

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            else MaterialTheme.typography.bodyLarge,
            color = textColor
        )
        Text(
            text = "$$value",
            style = if (isBold) MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            else MaterialTheme.typography.bodyLarge,
            color = textColor
        )
    }
}

