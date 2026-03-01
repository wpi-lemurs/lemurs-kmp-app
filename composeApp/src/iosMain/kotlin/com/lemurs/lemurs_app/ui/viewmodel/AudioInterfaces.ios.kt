package com.lemurs.lemurs_app.ui.viewmodel

import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.*
import platform.Foundation.*
import platform.darwin.NSObject
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time

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

// Full iOS implementation using AVFoundation
@OptIn(ExperimentalForeignApi::class)
class IosAudioRecorder : AudioRecorder {
    private val logger: Logger = Logger.withTag("IosAudioRecorder")
    private var audioRecorder: AVAudioRecorder? = null
    private var currentFilePath: String = ""
    private var startTime: Long = 0L

    override fun startRecording(fileName: String) {
        logger.d { "Starting recording to: $fileName" }
        currentFilePath = fileName

        try {
            // Configure audio session for recording
            val audioSession = AVAudioSession.sharedInstance()
            audioSession.setCategory(AVAudioSessionCategoryPlayAndRecord, error = null)
            audioSession.setActive(true, error = null)

            // Create URL for the file
            val fileUrl = NSURL.fileURLWithPath(fileName)

            // Recording settings - using numeric constants for audio format
            // kAudioFormatMPEG4AAC = 1633772320 ('aac ')
            val settings = mapOf<Any?, Any?>(
                AVFormatIDKey to 1633772320L,
                AVSampleRateKey to 44100.0,
                AVNumberOfChannelsKey to 1L,
                AVEncoderAudioQualityKey to 127L // AVAudioQualityMax
            )

            // Create recorder
            audioRecorder = AVAudioRecorder(fileUrl, settings, null)
            audioRecorder?.prepareToRecord()
            audioRecorder?.record()
            startTime = NSDate.date().timeIntervalSince1970.toLong()

            logger.d { "Recording started successfully" }
        } catch (e: Exception) {
            logger.e(e) { "Failed to start recording" }
        }
    }

    override fun stopRecording() {
        logger.d { "Stopping recording" }
        audioRecorder?.stop()

        // Deactivate audio session
        val audioSession = AVAudioSession.sharedInstance()
        audioSession.setActive(false, error = null)

        logger.d { "Recording stopped" }
    }

    override fun clearRecording() {
        logger.d { "Clearing recording" }
        audioRecorder?.stop()
        audioRecorder = null

        // Delete the file if it exists
        if (currentFilePath.isNotEmpty()) {
            val fileManager = NSFileManager.defaultManager
            if (fileManager.fileExistsAtPath(currentFilePath)) {
                fileManager.removeItemAtPath(currentFilePath, null)
                logger.d { "Recording file deleted" }
            }
        }
    }

    override fun isRecording(): Boolean = audioRecorder?.isRecording() ?: false

    override fun getRecordingDuration(): Long {
        return if (audioRecorder?.isRecording() == true) {
            (NSDate.date().timeIntervalSince1970.toLong() - startTime)
        } else {
            0L
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
class IosAudioPlayer : AudioPlayer {
    private val logger: Logger = Logger.withTag("IosAudioPlayer")
    private var audioPlayer: AVAudioPlayer? = null
    private var completionListener: (() -> Unit)? = null
    private var playbackDelegate: AudioPlayerDelegate? = null

    override fun playAudio(fileName: String, onCompletionListener: () -> Unit) {
        logger.d { "Playing audio from: $fileName" }
        completionListener = onCompletionListener

        try {
            val fileUrl = NSURL.fileURLWithPath(fileName)

            // Configure audio session for playback
            val audioSession = AVAudioSession.sharedInstance()
            audioSession.setCategory(AVAudioSessionCategoryPlayback, error = null)
            audioSession.setActive(true, error = null)

            audioPlayer = AVAudioPlayer(fileUrl, null)
            playbackDelegate = AudioPlayerDelegate {
                logger.d { "Playback completed" }
                completionListener?.invoke()
            }
            audioPlayer?.delegate = playbackDelegate
            audioPlayer?.prepareToPlay()
            audioPlayer?.play()

            logger.d { "Playback started" }
        } catch (e: Exception) {
            logger.e(e) { "Failed to play audio" }
            onCompletionListener()
        }
    }

    override fun stopAudio() {
        logger.d { "Stopping audio playback" }
        audioPlayer?.stop()
        audioPlayer = null
    }

    override fun isPlaying(): Boolean = audioPlayer?.isPlaying() ?: false
}

// Delegate class for AVAudioPlayer completion
class AudioPlayerDelegate(private val onCompletion: () -> Unit) : NSObject(), AVAudioPlayerDelegateProtocol {
    override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) {
        onCompletion()
    }
}

@OptIn(ExperimentalForeignApi::class)
class IosAudioFileManager : AudioFileManager {
    private val logger: Logger = Logger.withTag("IosAudioFileManager")
    private val audioFilePath: String by lazy {
        val documentsDir = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).firstOrNull() as? String ?: ""
        "$documentsDir/audio_recording.m4a"
    }

    override fun getAudioFilePath(): String = audioFilePath

    override fun doesAudioFileExist(): Boolean {
        val exists = NSFileManager.defaultManager.fileExistsAtPath(audioFilePath)
        logger.d { "Audio file exists at $audioFilePath: $exists" }
        return exists
    }

    override fun convertAudioToBase64(): String {
        logger.d { "Converting audio to Base64" }
        return try {
            val fileData = NSData.dataWithContentsOfFile(audioFilePath)
            if (fileData != null) {
                val base64String = fileData.base64EncodedStringWithOptions(0u)
                logger.d { "Converted audio to Base64, length: ${base64String.length}" }
                base64String
            } else {
                logger.e { "Failed to read audio file data" }
                ""
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to convert audio to Base64" }
            ""
        }
    }
}

class IosAudioTimer : AudioTimer {
    private val logger: Logger = Logger.withTag("IosAudioTimer")
    private var elapsedSeconds: Long = 0L
    private var isRunning: Boolean = false
    private var timerBlock: (() -> Unit)? = null

    override fun startTimer(onTick: (Long) -> Unit) {
        logger.d { "Starting timer" }
        elapsedSeconds = 0L
        isRunning = true

        // Use recursive dispatch with delay for timer
        timerBlock = {
            if (isRunning) {
                elapsedSeconds++
                onTick(elapsedSeconds)
                // Schedule next tick after 1 second
                scheduleNextTick()
            }
        }
        scheduleNextTick()
    }

    private fun scheduleNextTick() {
        if (!isRunning) return

        val dispatchTime = dispatch_time(DISPATCH_TIME_NOW, 1_000_000_000L) // 1 second in nanoseconds
        dispatch_after(dispatchTime, dispatch_get_main_queue()) {
            timerBlock?.invoke()
        }
    }

    override fun stopTimer() {
        logger.d { "Stopping timer" }
        isRunning = false
        timerBlock = null
    }
}

// iOS Audio Permission Manager
@OptIn(ExperimentalForeignApi::class)
class IosAudioPermissionManager {
    private val logger: Logger = Logger.withTag("IosAudioPermissionManager")

    fun requestMicrophonePermission(onResult: (Boolean) -> Unit) {
        val audioSession = AVAudioSession.sharedInstance()
        audioSession.requestRecordPermission { granted ->
            logger.d { "Microphone permission ${if (granted) "granted" else "denied"}" }
            onResult(granted)
        }
    }

    fun hasMicrophonePermission(): Boolean {
        val audioSession = AVAudioSession.sharedInstance()
        return audioSession.recordPermission == AVAudioSessionRecordPermissionGranted
    }
}
