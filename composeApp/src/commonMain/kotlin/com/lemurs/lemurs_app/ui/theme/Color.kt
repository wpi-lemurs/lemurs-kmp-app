package com.lemurs.lemurs_app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val LemurBlack = Color(0xFF000000)      // it's black
val LemurDarkerGrey = Color(0xFF53595A)
val LemurDarkGrey = Color(0xFF636363)   //skip button / hard to see
val LemurGrey = Color(0xFF656565)       //subtle title text
val LemurLightGrey = Color(0xFF8C8C8C)  //subtle text
val LemurBrightGrey = Color(0xFFBBBBBB) //unavailable buttons
val LemurWhiteGrey = Color(0xFFEAEAEA)  //unavailable text fields
val LemurWhite = Color(0xFFFFFFFF)      // it's white
val LemurContainerLight = Color(0xFFD9D9D9)
val LemurDarkestGrey = Color(0xFF444444)
val LemurDarkBlue = Color(0xFF1C5F6E)   // top card / outline
val LemurBlue = Color(0xFF1C5F6E)       // button
val LemurLightBlue = Color(0xFF78D3E8)  // half-completion / profile / progress bar
val LemurBrightBlue = Color(0xFFC3EDFF) // background
val LemurButtonBlue = Color(0xFF398A9D)
val LemurButtonGrey = Color(0xFF9A9A9A)


val CustomLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF398A9D),           // Soft teal-blue
    onPrimary = Color.White,               // White text on primary
    primaryContainer = Color(0xFF5A9CA8),  // Slightly lighter teal-blue
    onPrimaryContainer = Color.White,      // White text on primary container
    secondary = Color(0xFFB7E0EB),         // Light blue for secondary elements
    onSecondary = Color.Black,             // Black text on secondary
    secondaryContainer = Color(0xFFD2F2FA),// Even lighter blue for containers
    onSecondaryContainer = Color.Black,    // Black text on secondary container
    tertiary = Color.White,          // Grey for tertiary elements
    onTertiary = Color.Black,              // Black text on tertiary
    tertiaryContainer = Color.White, // White for containers
    onTertiaryContainer = Color.Black,     // Black text on tertiary container
    background = Color.White,              // White background
    onBackground = Color(0xFF1C5F6E),   // Dark teal text on background
    surface = Color.White,                 // White surface for cards
    onSurface = Color(0xFF1C5F6E),               // Black text on surface
    surfaceVariant = Color(0xFFE0E0E0),    // Light grey for surface variant
    onSurfaceVariant = Color.Black,        // Black text on surface variant
    surfaceTint = Color(0xFF4A7C8A),       // Tint matches primary color
    inverseSurface = Color(0xFF2D2D2D),    // Darker surface for contrast
    inverseOnSurface = Color.White,        // White text on inverse surface
    error = Color(0xFFB00020),             // Standard error red
    onError = Color.White,                 // White text on error
    errorContainer = Color(0xFFF2B8B5),    // Light error container red
    onErrorContainer = Color.Black,        // Black text on error container
    outline = Color(0xFFB0B0B0),           // Light grey for outlines
    outlineVariant = Color(0xFF8A8A8A),    // Darker grey for outlines
    scrim = Color.Black.copy(alpha = 0.32f), // Semi-transparent black scrim
)
