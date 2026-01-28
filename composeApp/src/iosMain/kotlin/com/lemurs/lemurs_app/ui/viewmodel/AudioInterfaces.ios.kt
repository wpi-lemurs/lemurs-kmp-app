package com.lemurs.lemurs_app.ui.viewmodel

// iOS implementation of AudioRecorder
actual interface AudioRecorder {
    actual fun startRecording(fileName: String)
    actual fun stopRecording()
    actual fun clearRecording()
    actual fun isRecording(): Boolean
    actual fun getRecordingDuration(): Long
}

// iOS implementation of AudioPlayer
actual interface AudioPlayer {
    actual fun playAudio(fileName: String, onCompletionListener: () -> Unit)
    actual fun stopAudio()
    actual fun isPlaying(): Boolean
}

// iOS implementation of AudioFileManager
actual interface AudioFileManager {
    actual fun getAudioFilePath(): String
    actual fun doesAudioFileExist(): Boolean
    actual fun convertAudioToBase64(): String
}

// iOS implementation of AudioTimer
actual interface AudioTimer {
    actual fun startTimer(onTick: (Long) -> Unit)
    actual fun stopTimer()
}

// Stub implementations for iOS
class IosAudioRecorder : AudioRecorder {
    override fun startRecording(fileName: String) {
        // TODO: Implement iOS audio recording
    }

    override fun stopRecording() {
        // TODO: Implement iOS audio recording
    }

    override fun clearRecording() {
        // TODO: Implement iOS audio recording
    }

    override fun isRecording(): Boolean = false

    override fun getRecordingDuration(): Long = 0L
}

class IosAudioPlayer : AudioPlayer {
    override fun playAudio(fileName: String, onCompletionListener: () -> Unit) {
        // TODO: Implement iOS audio playback
        onCompletionListener()
    }

    override fun stopAudio() {
        // TODO: Implement iOS audio playback
    }

    override fun isPlaying(): Boolean = false
}

class IosAudioFileManager : AudioFileManager {
    override fun getAudioFilePath(): String = ""

    override fun doesAudioFileExist(): Boolean = false

    override fun convertAudioToBase64(): String = ""
}

class IosAudioTimer : AudioTimer {
    override fun startTimer(onTick: (Long) -> Unit) {
        // TODO: Implement iOS timer
    }

    override fun stopTimer() {
        // TODO: Implement iOS timer
    }
}
