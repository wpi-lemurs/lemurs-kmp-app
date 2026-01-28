package com.lemurs.lemurs_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.data.api.LemursApiServiceImpl.WrittenResponseRequest
import com.lemurs.lemurs_app.data.local.activeData.Written
import com.lemurs.lemurs_app.data.repositories.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


class WritingViewModel(
//    private val WritingViewModel?,
    private val appRepository: AppRepository,

    ) : ViewModel() {
    private var currentQuestionIndex: Int = 1

    private val logger: Logger = Logger.withTag("WritingViewModel")

    private val _text = MutableStateFlow<String>("")
    val text: StateFlow<String> get() = _text

//    init {
//        logger.d { "WritingViewModel initialized" }
//        loadInitialText()
//    }

    /**
     * PARAM: String
     * Decides whether to updateWriting or loadInitialText.
     * return NOTHING
     */

    fun submitWritingData(newText: String, surveyResponseId: Int) {
        logger.d { "Starting audio submission with provided surveyResponseId: $surveyResponseId" }

        val now = Clock.System.now()
        val localDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val formattedTimestamp = "${localDateTime.date}T${localDateTime.time.toString().take(12)}"

        val writtenResponseReq = WrittenResponseRequest(
            surveyResponseId = surveyResponseId,
            writtenQuestionId = currentQuestionIndex,
            writtenData = newText,
            timestamp = formattedTimestamp
        )

        logger.d { "Creating WrittenRequest with surveyResponseId: $surveyResponseId" }
        logger.d { "WrittenRequest created: timestamp=${writtenResponseReq.timestamp}, surveyResponseId=${writtenResponseReq.surveyResponseId}, writtenRequestId=${writtenResponseReq.writtenQuestionId}, writtenData=${writtenResponseReq.writtenData}" }

        viewModelScope.launch(Dispatchers.Default) {
            val success = appRepository.saveWriting(writtenResponseReq)
            if (success) {
                logger.d { "Written data submitted successfully" }
            } else {
                logger.e { "Failed to submit written data" }
            }
        }
    }
    fun saveWritingData(newText: String, surveyResponseId: Int) {
        logger.d { "Starting audio submission with provided surveyResponseId: $surveyResponseId" }

        val now = Clock.System.now()
        val localDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val formattedTimestamp = "${localDateTime.date}T${localDateTime.time.toString().take(12)}"

        val writtenResponseReq = Written(
            surveyResponseId = surveyResponseId,
            questionNumber = currentQuestionIndex,
            response = newText,
            date = formattedTimestamp
        )

        logger.d { "Creating WrittenRequest with surveyResponseId: $surveyResponseId" }
        logger.d { "WrittenRequest created: timestamp=${writtenResponseReq.date}, surveyResponseId=${writtenResponseReq.surveyResponseId}, writtenRequestId=${writtenResponseReq.questionNumber}, writtenData=${writtenResponseReq.response}" }

        viewModelScope.launch(Dispatchers.Default) {
            appRepository.saveWrittenResponseLocally(writtenResponseReq)
        }
    }
}