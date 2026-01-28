package com.lemurs.lemurs_app.ui.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.survey.Progress
import com.lemurs.lemurs_app.survey.fetchAndParseProgress
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ProgressViewModel : ViewModel() {
    private var progress = mutableStateOf<Progress?>(null)
    private val _progress = mutableStateOf<Progress?>(null)
    val newProgress: MutableState<Progress?> = _progress
    val logger = Logger.withTag("Progress")



    fun getProgress(): Progress? {
        if (progress.value == null) {
            runBlocking {
                refreshProgress()
            }
        }

        return progress.value;
    }

    suspend fun refreshProgress() {
        try {
            progress.value = fetchAndParseProgress()
        } catch (e: Exception) {
            logger.w("Couldn't fetch progress data: ${e.message}")
        }
    }

    fun newRefreshProgress() {
        viewModelScope.launch {
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