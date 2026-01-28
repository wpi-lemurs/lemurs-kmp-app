package com.lemurs.lemurs_app.di

import com.lemurs.lemurs_app.util.IosUriOpener
import com.lemurs.lemurs_app.util.UriOpener
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    // iOS-specific dependencies
    single<UriOpener> { IosUriOpener() }
    
    // Audio recording, playback, etc. would go here when implemented
}