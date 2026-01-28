package com.lemurs.lemurs_app.di

import com.lemurs.lemurs_app.health.HealthConnectViewModel
import com.lemurs.lemurs_app.ui.viewmodel.AndroidAudioFileManager
import com.lemurs.lemurs_app.ui.viewmodel.AndroidAudioPlayer
import com.lemurs.lemurs_app.ui.viewmodel.AndroidAudioRecorder
import com.lemurs.lemurs_app.ui.viewmodel.AndroidAudioTimer
import com.lemurs.lemurs_app.ui.viewmodel.AudioFileManager
import com.lemurs.lemurs_app.ui.viewmodel.AudioPlayer
import com.lemurs.lemurs_app.ui.viewmodel.AudioRecorder
import com.lemurs.lemurs_app.ui.viewmodel.AudioTimer
import com.lemurs.lemurs_app.ui.viewmodel.AudioViewModel
import com.lemurs.lemurs_app.util.AndroidUriOpener
import com.lemurs.lemurs_app.util.UriOpener
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    // Register platform implementations under their interface types so Koin can resolve them by interface
    single<AudioRecorder> { AndroidAudioRecorder(get()) }
    single<AudioPlayer> { AndroidAudioPlayer() }
    single<AudioFileManager> { AndroidAudioFileManager(get()) }
    single<AudioTimer> { AndroidAudioTimer() }

    // ADDED to connect UriOpener interface to Android implementation
    // This allows common code to use UriOpener without knowing about Android specifics
    single<UriOpener> { AndroidUriOpener(androidContext()) }

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

    viewModel { 
        HealthConnectViewModel(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
}
