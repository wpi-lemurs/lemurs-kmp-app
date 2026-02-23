package com.lemurs.lemurs_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.lemurs.lemurs_app.data.repositories.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SubmissionViewModel : ViewModel() {
    private val _surveyItems = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val surveyItems: StateFlow<List<Pair<String, String>>> = _surveyItems

    private val _surveyType = MutableStateFlow<String>("")
    val surveyType: StateFlow<String> = _surveyType

    // Items that should be shown for weekly survey submissions
    private val weeklyAllowedItems = setOf(
        "PHQ-9",
        "Writing Prompt",
        "Audio Prompt"
    )

    fun setSurveyType(type: String) {
        _surveyType.value = type
    }

    fun markItemCompleted(itemName: String, amount: String) {
        val updatedList = _surveyItems.value.toMutableList()
        updatedList.removeAll { it.first == itemName } // Remove existing entry if any
        updatedList.add(itemName to amount) // Add completed section
        _surveyItems.value = updatedList
    }

    fun markItemSkipped(itemName: String) {
        val updatedList = _surveyItems.value.toMutableList()
        updatedList.removeAll { it.first == itemName } // Remove existing entry if any
        updatedList.add(itemName to "0.00") // Add skipped section
        _surveyItems.value = updatedList
    }

    fun clearSurveyItems() {
        _surveyItems.value = emptyList()
    }

    /**
     * Returns filtered survey items based on survey type.
     * For weekly surveys, only PHQ-9, Writing Prompt, and Audio Prompt are shown.
     * For daily surveys, all items are shown.
     */
    fun getFilteredSurveyItems(): List<Pair<String, String>> {
        val items = _surveyItems.value
        return if (_surveyType.value == "weekly") {
            items.filter { it.first in weeklyAllowedItems }
        } else {
            items
        }
    }
}