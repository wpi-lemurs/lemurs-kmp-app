package com.lemurs.lemurs_app

import androidx.compose.ui.window.ComposeUIViewController
import com.lemurs.lemurs_app.di.appModule
import com.lemurs.lemurs_app.di.commonModule
import com.lemurs.lemurs_app.di.platformModule
import org.koin.core.context.startKoin

fun initKoin() {
    startKoin {
        modules(
            appModule,
            commonModule(),
            platformModule()
        )
    }
}

fun MainViewController() = ComposeUIViewController {
    App()
}
