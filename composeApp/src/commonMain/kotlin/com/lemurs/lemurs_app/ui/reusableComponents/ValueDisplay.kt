package com.lemurs.lemurs_app.ui.reusableComponents

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lemurs.lemurs_app.ui.theme.LemurBlue
import com.lemurs.lemurs_app.ui.theme.LemurButtonBlue
import com.lemurs.lemurs_app.ui.theme.LemurLightBlue
import com.lemurs.lemurs_app.ui.theme.LemurWhite

@Composable
fun ValueDisplay(valueName: String, value: Double, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = LemurButtonBlue)
    ) {
        Text(
            text = "$valueName$$value",
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.titleMedium,
            color = LemurWhite
        )
    }
}