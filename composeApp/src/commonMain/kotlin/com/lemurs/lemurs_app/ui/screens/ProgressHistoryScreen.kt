package com.lemurs.lemurs_app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
//import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController

@Composable
fun ProgressHistoryScreen(
    onNavigateTo: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Progress History Screen", fontSize = 30.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                onNavigateTo(LemurScreen.Main.name)
            }
        ) {
            Text(text = "Home Screen")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                onNavigateTo(LemurScreen.Progress.name)
            }
        ) {
            Text(text = "Progress Screen")
        }
    }
}


//@Preview
//@Composable
//fun ProgressHistoryScreen() {
//    Lemurs_AppTheme {
//        ProgressHistoryScreen(
//            onProgressButtonClicked = {},
//            onHomeButtonClicked = {},
//            modifier = Modifier.fillMaxSize()
//        )
//    }
//}