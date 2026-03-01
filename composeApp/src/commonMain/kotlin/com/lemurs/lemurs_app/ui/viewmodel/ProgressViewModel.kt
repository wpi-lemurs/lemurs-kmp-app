package com.lemurs.lemurs_app.ui.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.survey.Progress
import com.lemurs.lemurs_app.survey.fetchAndParseProgress
import com.lemurs.lemurs_app.util.DemoMode
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ProgressViewModel : ViewModel() {
    private var progress = mutableStateOf<Progress?>(null)
    private val _progress = mutableStateOf<Progress?>(null)
    val newProgress: MutableState<Progress?> = _progress
    val logger = Logger.withTag("Progress")

    // Mock progress data for demo/review mode
    private val demoProgress = Progress(
        earned = 125.50,
        dailySurveysTotalCompleted = 42,
        dailySurveysTotalGoal = 90,
        dailySurveysTotalBonus = 15.00,
        dailySurveysWeeklyCompleted = 6,
        dailySurveysWeeklyGoal = 14,
        dailySurveysWeeklyBonus = 5.00,
        nextWeeklySurvey = null
    )

    fun getProgress(): Progress? {
        // Return mock data in demo mode
        if (DemoMode.isActive) {
            logger.d("Demo mode: returning mock progress data")
            progress.value = demoProgress
            return demoProgress
        }

        if (progress.value == null) {
            runBlocking {
                refreshProgress()
            }
        }

        return progress.value;
    }

    suspend fun refreshProgress() {
        // Return mock data in demo mode
        if (DemoMode.isActive) {
            logger.d("Demo mode: returning mock progress data")
            progress.value = demoProgress
            return
        }

        try {
            progress.value = fetchAndParseProgress()
        } catch (e: Exception) {
            logger.w("Couldn't fetch progress data: ${e.message}")
        }
    }

    fun newRefreshProgress() {
        viewModelScope.launch {
            // Return mock data in demo mode
            if (DemoMode.isActive) {
                logger.d("Demo mode: returning mock progress data")
                _progress.value = demoProgress
                return@launch
            }

            try {
                logger.d { "Fetching with new progress data" }
                _progress.value = fetchAndParseProgress()
                logger.w("The new progress is: ${_progress.value}")
            } catch (e: Exception) {
                logger.w("Couldn't fetch progress data: ${e.message}")
            }
        }
    }
}