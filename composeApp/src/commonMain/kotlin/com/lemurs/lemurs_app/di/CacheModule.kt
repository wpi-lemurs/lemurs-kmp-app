package com.lemurs.lemurs_app.di

import com.lemurs.lemurs_app.data.local.activeData.AudioDAO
import com.lemurs.lemurs_app.data.local.activeData.AudioDAOImpl
import com.lemurs.lemurs_app.data.local.activeData.SurveyResponseDAO
import com.lemurs.lemurs_app.data.local.activeData.SurveyResponseDAOImpl
import com.lemurs.lemurs_app.data.local.activeData.WrittenDAO
import com.lemurs.lemurs_app.data.local.activeData.WrittenDAOImpl
import com.lemurs.lemurs_app.data.local.passiveData.BluetoothDAO
import com.lemurs.lemurs_app.data.local.passiveData.BluetoothDAOImpl
import com.lemurs.lemurs_app.data.local.passiveData.BluetoothDataUseCase
import com.lemurs.lemurs_app.data.local.passiveData.CalorieDAO
import com.lemurs.lemurs_app.data.local.passiveData.CalorieDAOImpl
import com.lemurs.lemurs_app.data.local.passiveData.DistanceDAO
import com.lemurs.lemurs_app.data.local.passiveData.DistanceDAOImpl
import com.lemurs.lemurs_app.data.local.passiveData.GPSDAO
import com.lemurs.lemurs_app.data.local.passiveData.GPSDAOImpl
import com.lemurs.lemurs_app.data.local.passiveData.GPSDataUseCase
import com.lemurs.lemurs_app.data.local.passiveData.HealthDAO
import com.lemurs.lemurs_app.data.local.passiveData.HealthDAOImpl
import com.lemurs.lemurs_app.data.local.passiveData.ScreentimeDAO
import com.lemurs.lemurs_app.data.local.passiveData.ScreentimeDAOImpl
import com.lemurs.lemurs_app.data.local.passiveData.ScreentimeDataUseCase
import com.lemurs.lemurs_app.data.local.passiveData.SleepDAO
import com.lemurs.lemurs_app.data.local.passiveData.SleepDAOImpl
import com.lemurs.lemurs_app.data.local.passiveData.SpeedDAO
import com.lemurs.lemurs_app.data.local.passiveData.SpeedDAOImpl
import com.lemurs.lemurs_app.data.local.passiveData.StepDAO
import com.lemurs.lemurs_app.data.local.passiveData.StepDAOImpl
import com.lemurs.lemurs_app.data.local.passiveData.TikTokDAO
import com.lemurs.lemurs_app.data.local.passiveData.TikTokDAOImpl
import com.lemurs.lemurs_app.data.local.passiveData.WeightDAO
import com.lemurs.lemurs_app.data.local.passiveData.WeightDAOImpl
import com.lemurs.lemurs_app.data.local.userData.UserDAO
import com.lemurs.lemurs_app.data.local.userData.UserDAOImpl
import com.lemurs.lemurs_app.data.repositories.AppRepository
import org.koin.core.module.Module
import org.koin.dsl.module

// This module contains all the shared dependencies for your application.
fun commonModule() = module {
    single {
        AppRepository(get(), get(), get(), get())
    }

    // DAOs
    single<AudioDAO> { AudioDAOImpl() }
    single<SurveyResponseDAO> { SurveyResponseDAOImpl() }
    single<WrittenDAO> { WrittenDAOImpl() }
    single<BluetoothDAO> { BluetoothDAOImpl() }
    single<CalorieDAO> { CalorieDAOImpl() }
    single<DistanceDAO> { DistanceDAOImpl() }
    single<GPSDAO> { GPSDAOImpl() }
    single<HealthDAO> { HealthDAOImpl() }
    single<ScreentimeDAO> { ScreentimeDAOImpl() }
    single<SleepDAO> { SleepDAOImpl() }
    single<SpeedDAO> { SpeedDAOImpl() }
    single<StepDAO> { StepDAOImpl() }
    single<TikTokDAO> { TikTokDAOImpl() }
    single<UserDAO> { UserDAOImpl() }
    single<WeightDAO> { WeightDAOImpl() }

    // UseCases
    factory { BluetoothDataUseCase(get()) }
    factory { GPSDataUseCase(get()) }
    factory { ScreentimeDataUseCase(get()) }
}

// This expects each platform (Android, iOS, etc.) to provide its own specific modules.
expect fun platformModule(): Module
