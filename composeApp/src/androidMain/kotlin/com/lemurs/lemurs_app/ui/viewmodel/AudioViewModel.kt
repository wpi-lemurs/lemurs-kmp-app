package com.lemurs.lemurs_app.ui.viewmodel

import android.app.Application
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.CountDownTimer
import co.touchlab.kermit.Logger
import java.io.File
import kotlinx.io.IOException

actual interface AudioRecorder {
    actual fun startRecording(fileName: String)
    actual fun stopRecording()
    actual fun clearRecording()
    actual fun isRecording(): Boolean
    actual fun getRecordingDuration(): Long
}

actual interface AudioPlayer {
    actual fun playAudio(fileName: String, onCompletionListener: () -> Unit)
    actual fun stopAudio()
    actual fun isPlaying(): Boolean
}

actual interface AudioFileManager {
    actual fun getAudioFilePath(): String
    actual fun doesAudioFileExist(): Boolean
    actual fun convertAudioToBase64(): String
}

actual interface AudioTimer {
    actual fun startTimer(onTick: (Long) -> Unit)
    actual fun stopTimer()
}

// Concrete Android implementations
class AndroidAudioRecorderImpl(private val context: Application) : AudioRecorder {
    private val logger: Logger = Logger.withTag("AndroidAudioRecorder")
    private var mRecorder: MediaRecorder? = null
    private var recordingDuration = 0L
    private var isCurrentlyRecording = false

    override fun startRecording(fileName: String) {
        mRecorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            MediaRecorder(context).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(fileName)
                try {
                    logger.d { "Trying to record" }
                    prepare()
                    start()
                    isCurrentlyRecording = true
                } catch (e: IOException) {
                    logger.e(e) { "prepare() failed" }
                    throw e
                }
            }
        } else {
            MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(fileName)
                try {
                    prepare()
                    start()
                    isCurrentlyRecording = true
                } catch (e: IOException) {
                    logger.e(e) { "prepare() failed" }
                    throw e
                }
            }
        }
    }

    override fun stopRecording() {
        mRecorder?.apply {
            try {
                stop()
                isCurrentlyRecording = false
            } catch (e: RuntimeException) {
                logger.e(e) { "stop() failed" }
            }
            release()
        }
        mRecorder = null
    }

    override fun clearRecording() {
        mRecorder?.apply {
            try {
                if (isCurrentlyRecording) {
                    stop()
                }
                release()
            } catch (e: RuntimeException) {
                logger.e(e) { "stop() failed" }
            }
        }
        mRecorder = null
        isCurrentlyRecording = false
        recordingDuration = 0L
    }

    override fun isRecording(): Boolean = isCurrentlyRecording

    override fun getRecordingDuration(): Long = recordingDuration
}

class AndroidAudioPlayerImpl : AudioPlayer {
    private val logger: Logger = Logger.withTag("AndroidAudioPlayer")
    private var mPlayer: MediaPlayer? = null
    private var isCurrentlyPlaying = false

    override fun playAudio(fileName: String, onCompletionListener: () -> Unit) {
        mPlayer = MediaPlayer().apply {
            try {
                setDataSource(fileName)
                prepare()
                start()
                isCurrentlyPlaying = true
                setOnCompletionListener {
                    isCurrentlyPlaying = false
                    onCompletionListener()
                }
            } catch (e: Exception) {
                logger.e(e) { "Unknown exception" }
                throw e
            }
        }
    }

    override fun stopAudio() {
        mPlayer?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                logger.e(e) { "Failed to stop audio" }
            }
        }
        mPlayer = null
        isCurrentlyPlaying = false
    }

    override fun isPlaying(): Boolean = isCurrentlyPlaying
}

class AndroidAudioFileManagerImpl(private val context: Application) : AudioFileManager {
    private val logger: Logger = Logger.withTag("AndroidAudioFileManager")
    private val fileName: String = context.filesDir.absolutePath + "AudioRecording.3gp"

    override fun getAudioFilePath(): String = fileName

    override fun doesAudioFileExist(): Boolean {
        val file = File(fileName)
        return file.exists()
    }

    override fun convertAudioToBase64(): String {
        val file = File(fileName)
        return if (file.exists()) {
            val audioBytes = file.readBytes()
            android.util.Base64.encodeToString(audioBytes, android.util.Base64.DEFAULT)
        } else {
            logger.e { "Audio file does not exist: $fileName" }
            ""
        }
    }
}

class AndroidAudioTimerImpl : AudioTimer {
    private val logger: Logger = Logger.withTag("AndroidAudioTimer")
    private var timer: CountDownTimer? = null

    override fun startTimer(onTick: (Long) -> Unit) {
        var duration = 0L
        timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                duration += 1
                onTick(duration)
            }

            override fun onFinish() {
                logger.d { "Timer finished" }
            }
        }.start()
    }

    override fun stopTimer() {
        timer?.cancel()
        timer = null
    }
}
