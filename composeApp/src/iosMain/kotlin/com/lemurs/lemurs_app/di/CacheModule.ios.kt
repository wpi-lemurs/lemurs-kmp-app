package com.lemurs.lemurs_app.di

import com.lemurs.lemurs_app.ui.viewmodel.AudioFileManager
import com.lemurs.lemurs_app.ui.viewmodel.AudioPlayer
import com.lemurs.lemurs_app.ui.viewmodel.AudioRecorder
import com.lemurs.lemurs_app.ui.viewmodel.AudioTimer
import com.lemurs.lemurs_app.ui.viewmodel.AudioViewModel
import com.lemurs.lemurs_app.ui.viewmodel.IosAudioFileManager
import com.lemurs.lemurs_app.ui.viewmodel.IosAudioPlayer
import com.lemurs.lemurs_app.ui.viewmodel.IosAudioRecorder
import com.lemurs.lemurs_app.ui.viewmodel.IosAudioTimer
import com.lemurs.lemurs_app.ui.viewmodel.IosAudioPermissionManager
import com.lemurs.lemurs_app.util.IosUriOpener
import com.lemurs.lemurs_app.util.UriOpener
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    // iOS-specific dependencies
    single<UriOpener> { IosUriOpener() }
    
    // Audio recording, playback, etc.
    single<AudioRecorder> { IosAudioRecorder() }
    single<AudioPlayer> { IosAudioPlayer() }
    single<AudioFileManager> { IosAudioFileManager() }
    single<AudioTimer> { IosAudioTimer() }
    single { IosAudioPermissionManager() }

    // ViewModels
    viewModel {
        AudioViewModel(
            appRepository = get(),
            audioRecorder = get(),
            audioPlayer = get(),
            audioFileManager = get(),
            audioTimer = get()
        )
    }
}