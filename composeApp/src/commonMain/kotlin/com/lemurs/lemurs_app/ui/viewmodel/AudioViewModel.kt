package com.lemurs.lemurs_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.data.api.LemursApiServiceImpl.AudioDataRequest
import com.lemurs.lemurs_app.data.local.activeData.Audio
import com.lemurs.lemurs_app.data.repositories.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Platform-specific interface for audio operations
expect interface AudioRecorder {
    fun startRecording(fileName: String)
    fun stopRecording()
    fun clearRecording()
    fun isRecording(): Boolean
    fun getRecordingDuration(): Long
}

expect interface AudioPlayer {
    fun playAudio(fileName: String, onCompletionListener: () -> Unit)
    fun stopAudio()
    fun isPlaying(): Boolean
}

expect interface AudioFileManager {
    fun getAudioFilePath(): String
    fun doesAudioFileExist(): Boolean
    fun convertAudioToBase64(): String
}

expect interface AudioTimer {
    fun startTimer(onTick: (Long) -> Unit)
    fun stopTimer()
}

// Common AudioViewModel that uses platform-specific implementations
class AudioViewModel(
    private val appRepository: AppRepository,
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer,
    private val audioFileManager: AudioFileManager,
    private val audioTimer: AudioTimer
) : ViewModel() {

    private val logger: Logger = Logger.withTag("AudioViewModel")

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _status = MutableStateFlow("Idle")
    val status: StateFlow<String> = _status

    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: StateFlow<Long> = _recordingDuration

    private val _audioExists = MutableStateFlow(false)
    val audioExists: StateFlow<Boolean> = _audioExists
    private var currentQuestionIndex: Int = 1

    fun setQuestionIndex(questionIndex: Int) {
        // Convert from 0-based index to 1-based for backend
        currentQuestionIndex = questionIndex + 1
        logger.d { "Question index set to: $currentQuestionIndex (from 0-based index: $questionIndex)" }
    }

    fun startRecording() {
        _recordingDuration.value = 0L

        try {
            audioRecorder.startRecording(audioFileManager.getAudioFilePath())
            _isRecording.value = true
            _status.value = "Recording Started"

            audioTimer.startTimer { duration ->
                _recordingDuration.value = duration
            }

            logger.d { "Recording started, file path: ${audioFileManager.getAudioFilePath()}" }
        } catch (e: Exception) {
            logger.e(e) { "Failed to start recording" }
            _status.value = "Recording Failed"
        }
    }

    fun stopRecording() {
        try {
            logger.d { "Trying to stop recording" }
            audioRecorder.stopRecording()
            _isRecording.value = false
            _status.value = "Recording Stopped"

            // Audio exists only if user records for at least 15 seconds
            _audioExists.value = _recordingDuration.value >= 15
            audioTimer.stopTimer()

            if (audioFileManager.doesAudioFileExist()) {
                logger.d { "Recording saved at: ${audioFileManager.getAudioFilePath()}" }
            } else {
                logger.e { "Recording file not found at: ${audioFileManager.getAudioFilePath()}" }
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to stop recording" }
        }
    }

    fun clearRecording() {
        _isRecording.value = false
        _status.value = "Recording Cleared"
        _audioExists.value = false

        try {
            audioRecorder.clearRecording()
            audioTimer.stopTimer()
            _recordingDuration.value = 0L
            logger.d { "Recording cleared" }
        } catch (e: Exception) {
            logger.e(e) { "Failed to clear recording" }
        }
    }

    fun doesAudioFileExist(): Boolean {
        return audioFileManager.doesAudioFileExist()
    }

    fun playAudio() {
        try {
            logger.d { "Audio file path: ${audioFileManager.getAudioFilePath()}" }

            if (!audioFileManager.doesAudioFileExist()) {
                logger.e { "Audio file does not exist: ${audioFileManager.getAudioFilePath()}" }
                _status.value = "No recording present"
                return
            }

            _isPlaying.value = true
            _status.value = "Playing Recording"

            audioPlayer.playAudio(audioFileManager.getAudioFilePath()) {
                _isPlaying.value = false
                _status.value = "Playback Completed"
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to play audio" }
            _isPlaying.value = false
            _status.value = "Playback Failed"
        }
    }

    fun stopAudio() {
        _isPlaying.value = false
        _status.value = "Playback Stopped"
        audioPlayer.stopAudio()
    }

    private fun convertAudioToBase64(): String {
        return try {
            audioFileManager.convertAudioToBase64()
        } catch (e: Exception) {
            logger.e(e) { "Failed to convert audio to base64" }
            ""
        }
    }

    fun submitAudioData(surveyResponseId: Int) {
        logger.d { "Starting audio submission with provided surveyResponseId: $surveyResponseId" }

        // Format timestamp to match backend expected format: yyyy-MM-dd'T'HH:mm:ss.SSS (ISO 8601)
        val now = Clock.System.now()
        val localDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val formattedTimestamp = "${localDateTime.date}T${localDateTime.time.toString().take(12)}" // ISO 8601 format with T

        val audioRequest = AudioDataRequest(
            timestamp = formattedTimestamp,
            audioQuestionId = currentQuestionIndex,
            audioByte64 = convertAudioToBase64(),
            surveyResponseId = surveyResponseId
        )

        logger.d { "Creating AudioDataRequest with surveyResponseId: $surveyResponseId" }
        logger.d { "AudioDataRequest created: timestamp=${audioRequest.timestamp}, audioQuestionId=${audioRequest.audioQuestionId}, surveyResponseId=${audioRequest.surveyResponseId}, audioLength=${audioRequest.audioByte64.length}" }

        viewModelScope.launch(Dispatchers.Default) {
            val success = appRepository.submitAudioData(audioRequest)
            if (success) {
                logger.d { "Audio data submitted successfully" }
            } else {
                logger.e { "Failed to submit audio data" }
            }
        }
    }
    fun saveAudioData(surveyResponseId: Int) {
        logger.d { "Starting audio submission with provided surveyResponseId: $surveyResponseId" }

        // Format timestamp to match backend expected format: yyyy-MM-dd'T'HH:mm:ss.SSS (ISO 8601)
        val now = Clock.System.now()
        val localDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val formattedTimestamp = "${localDateTime.date}T${localDateTime.time.toString().take(12)}" // ISO 8601 format with T

        val audioRequest = Audio(
            surveyResponseId = surveyResponseId,
            date = formattedTimestamp,
            audioByte64 = convertAudioToBase64(),
            questionId = currentQuestionIndex
        )

        logger.d { "Creating AudioDataRequest with surveyResponseId: $surveyResponseId" }
        logger.d { "AudioDataRequest created: timestamp=${audioRequest.date}, audioQuestionId=${audioRequest.questionId}, surveyResponseId=${audioRequest.surveyResponseId}, audioLength=${audioRequest.audioByte64.length}" }

        viewModelScope.launch(Dispatchers.Default) {
            appRepository.saveAudioResponseLocally(audioRequest)
        }
    }
}
