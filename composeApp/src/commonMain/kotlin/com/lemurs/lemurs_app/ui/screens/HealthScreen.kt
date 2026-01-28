package com.lemurs.lemurs_app.ui.screens

import androidx.compose.runtime.Composable
//import androidx.navigation.NavController

// CHANGED: navController is not used here because it is not Kotlin Multiplatform compatible
// Since its an expect function, it must be implemented in androidMain and iosMain separately.
@Composable
expect fun HealthScreen(onNavigateTo: (String) -> Unit)