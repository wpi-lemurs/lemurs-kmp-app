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

}