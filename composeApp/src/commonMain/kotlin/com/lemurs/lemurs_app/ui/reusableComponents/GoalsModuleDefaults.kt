package com.lemurs.lemurs_app.ui.reusableComponents

import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.lemurs.lemurs_app.ui.theme.CustomColorScheme
import com.lemurs.lemurs_app.ui.theme.LemurContainerLight
import com.lemurs.lemurs_app.ui.theme.LemurDarkerGrey
import com.lemurs.lemurs_app.ui.theme.LemurGrey

object GoalsModuleDefaults {
    @Composable
    fun defaultColors() = Colors(
        titleColor = LemurDarkerGrey,                // White for title text
        rewardColor = LemurDarkerGrey,        // Teal for reward text
        progressLabelColor = LemurGrey,        // Medium Gray for progress label
        progressTrackColor = CustomColorScheme.outlineVariant, // Dark Gray for progress bar track
        progressBarColor = CustomColorScheme.primaryContainer,   // Teal for progress bar fill
        progressPercentageColor = CustomColorScheme.primary, // Light Teal for percentage text
        cardColors = CardDefaults.cardColors(containerColor = LemurContainerLight),
    )

    data class Colors(
        val titleColor: Color,
        val rewardColor: Color,
        val progressLabelColor: Color,
        val progressTrackColor: Color,
        val progressBarColor: Color,
        val progressPercentageColor: Color,
        val cardColors: CardColors
    )
}
