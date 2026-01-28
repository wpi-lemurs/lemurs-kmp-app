package com.lemurs.lemurs_app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Custom color scheme for input fields
val CustomColorScheme = CustomLightColorScheme

@Composable
fun LemursAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CustomColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun inputFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = LemurDarkerGrey,                              // 100% opacity when focused
    unfocusedTextColor = LemurDarkerGrey.copy(alpha = 0.5f),         // 50% opacity when unfocused
    focusedContainerColor = Color.White,                         // White background when focused
    unfocusedContainerColor = Color.White,                       // White background when unfocused
    cursorColor = CustomColorScheme.primary,                     // Primary color for cursor
    focusedIndicatorColor = CustomColorScheme.primary,           // Border color when focused
    unfocusedIndicatorColor = LemurDarkerGrey,                        // Border color when unfocused
    focusedLabelColor = LemurDarkerGrey,                             // Label color when focused
    unfocusedLabelColor = LemurDarkerGrey.copy(alpha = 0.5f)         // Label color when unfocused
)

@Composable
fun filledButtonColors() = ButtonDefaults.buttonColors(
    containerColor = LemurButtonBlue,           // Background color for filled buttons
    contentColor = LemurWhite,          // Text/Icon color for filled buttons
    disabledContainerColor = CustomColorScheme.surfaceVariant,  // Background for disabled filled buttons
    disabledContentColor = CustomColorScheme.onSurfaceVariant.copy(alpha = 0.38f) // Text/Icon for disabled buttons
)

@Composable
fun outlinedButtonColors() = ButtonDefaults.outlinedButtonColors(
    containerColor = Color.Transparent,            // Transparent background for outlined buttons
    contentColor = CustomColorScheme.primary,            // Text/Icon color for outlined buttons
    disabledContainerColor = CustomColorScheme.surfaceVariant,    // Background for disabled outlined buttons
    disabledContentColor = CustomColorScheme.onSurfaceVariant.copy(alpha = 0.38f) // Text/Icon for disabled buttons
)

