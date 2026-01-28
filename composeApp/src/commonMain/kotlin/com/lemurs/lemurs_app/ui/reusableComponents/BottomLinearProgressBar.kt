package com.lemurs.lemurs_app.ui.reusableComponents

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.lemurs.lemurs_app.ui.theme.CustomColorScheme


@Composable
fun BottomLinearProgressBar(progressPercentage: Int) {
    val progressDecimal = progressPercentage / 100f

    LinearProgressIndicator(
        progress = { progressDecimal },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RectangleShape),
        color = CustomColorScheme.surfaceTint,
        trackColor = CustomColorScheme.inverseSurface,
        gapSize = 0.dp
    )

}