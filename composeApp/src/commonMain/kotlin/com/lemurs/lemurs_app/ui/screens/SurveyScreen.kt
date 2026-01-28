package com.lemurs.lemurs_app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.ui.viewmodel.SurveyViewModel
import org.koin.compose.viewmodel.koinViewModel
import com.lemurs.lemurs_app.ui.navigation.AppNavigator

@Composable
fun SurveyScreen(onNavigateTo : (String) -> Unit, navigator: AppNavigator) {
    val logger = Logger.withTag("SurveyScreen")
    val surveyViewModel: SurveyViewModel = koinViewModel()
    val surveyResult by surveyViewModel.surveyResult.collectAsState()
    var surveyAnswers by remember { mutableStateOf(mutableMapOf<String, String>()) }
    var progressPercentage by remember { mutableStateOf(0) }

    // Define your questions
    val questions = listOf(
        "Tell us something interesting",
        "Choose your year of graduation",
        "Rate how your day is going on a scale of 1-5:"
    )
    val labels = listOf(
        "Interesting Fact",
        "Year of Graduation",
        "Day Rating"
    )

    // Define options for the dropdown and bubble selection
    val dropdownOptions = listOf("make a selection", "2024", "2025", "2026", "2027", "2028")
    val bubbleOptions = listOf("1", "2", "3", "4", "5")

    // Questions and Input Fields
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        Spacer(modifier = Modifier.height(16.dp))

        // Submit Button
        Button(
            onClick = {
                logger.d("Survey Answers: $surveyAnswers")
                // surveyViewModel.submitData(surveyAnswers)
            }
        ) {
            Text("Submit")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Submission Result
        surveyResult?.let { result ->
            Text(
                text = when {
                    result.isSuccess -> "Submission Successful"
                    else -> "Submission Failed"
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Back Button
        Button(onClick = { navigator.goBack() }) {
            Text("Back")
        }
    }
}
