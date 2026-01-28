package com.lemurs.lemurs_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WeeklySurveyViewModel: ViewModel() {
    private val logger = Logger.withTag("WeeklySurveyViewModel")

    private val _requestStack1 = MutableStateFlow<MutableList<() -> Unit>>(mutableListOf())

    fun addRequest(request: () -> Unit) {
        logger.d { "Adding request to stack" }
        _requestStack1.value.add { request() }
        logger.d { "Request added to stack" }

    }

    fun executeRequests() {

        if (_requestStack1.value.isEmpty()) {
            logger.d { "No requests really to execute" }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {

            for (it in _requestStack1.value) {
                logger.d { "Invoking request" }
                it.invoke()
            }
            clearRequests()
        }
    }

    private fun clearRequests() {
        _requestStack1.value.clear()
    }


}
