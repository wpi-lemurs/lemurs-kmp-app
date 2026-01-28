package com.lemurs.lemurs_app

import android.app.Application
import com.lemurs.lemurs_app.di.appModule
import com.lemurs.lemurs_app.di.commonModule
import com.lemurs.lemurs_app.di.platformModule
import com.lemurs.lemurs_app.data.AndroidContextProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class LemursApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // for the Android app we call the modules from the shared app here.
        val application = this@LemursApp
        startKoin {
            androidContext(this@LemursApp)
            modules(commonModule(), platformModule(), appModule, module { single<Application> { application } })
        }

        // Set the context for AndroidContextProvider
        AndroidContextProvider.setContext(this)
    }
}
