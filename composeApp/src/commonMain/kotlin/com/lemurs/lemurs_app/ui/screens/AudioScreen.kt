package com.lemurs.lemurs_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lemurs.lemurs_app.ui.theme.LemursAppTheme
//import org.jetbrains.compose.ui.tooling.preview.Preview

// CHANGED: navController is not used here because it is not Kotlin Multiplatform compatible
// Since its an expect function, it must be implemented in androidMain and iosMain separately.
@Composable
expect fun AudioScreen(onNavigateTo: (String) -> Unit)