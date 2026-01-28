package com.lemurs.lemurs_app.di

import androidx.datastore.core.DataStore
import com.lemurs.JwtTokenResponse
import com.lemurs.NotificationTimes
import com.lemurs.lemurs_app.data.api.LemursApiServiceImpl
import com.lemurs.lemurs_app.data.datasource.health.HealthRemoteDataSource
import com.lemurs.lemurs_app.data.datastore.JwtTokenResponseImpl
import com.lemurs.lemurs_app.data.datastore.NotificationTimesImpl
import com.lemurs.lemurs_app.data.datastore.getDataStore
import com.lemurs.lemurs_app.data.datastore.getTimesDataStore
import com.lemurs.lemurs_app.data.datastore.getHealthConnectTokensDataStore
import com.lemurs.lemurs_app.data.repositories.AppRepository
import com.lemurs.lemurs_app.ui.viewmodel.DailyQuestionsViewModel
import com.lemurs.lemurs_app.ui.viewmodel.DemographicsViewModel
import com.lemurs.lemurs_app.ui.viewmodel.ProfileViewModel
import com.lemurs.lemurs_app.ui.viewmodel.ProgressHistoryViewModel
import com.lemurs.lemurs_app.ui.viewmodel.ProgressViewModel
import com.lemurs.lemurs_app.ui.viewmodel.ResourcesViewModel
import com.lemurs.lemurs_app.ui.viewmodel.SubmissionViewModel
import com.lemurs.lemurs_app.ui.viewmodel.SurveyAvailabilityViewModel
import com.lemurs.lemurs_app.ui.viewmodel.SurveyInformationViewModel
import com.lemurs.lemurs_app.ui.viewmodel.SurveyViewModel
import com.lemurs.lemurs_app.ui.viewmodel.WeeklyQuestionsViewModel
import com.lemurs.lemurs_app.ui.viewmodel.WeeklySurveyViewModel
import com.lemurs.lemurs_app.ui.viewmodel.WritingViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.serialization.kotlinx.json.json
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule: Module = module {
    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.ALL
            }
        }
    }

    single { LemursApiServiceImpl(get()) }
    single { HealthRemoteDataSource(get()) }
    single<AppRepository> { AppRepository(get(), get(),get(), get()) }
    single { WeeklyQuestionsViewModel() }
    single { SubmissionViewModel() }

    viewModel { SurveyViewModel(get()) }
    viewModel { DailyQuestionsViewModel() }
    viewModel { ProfileViewModel(get()) }
    viewModel { DemographicsViewModel(get()) }
    viewModel { ProgressHistoryViewModel(get()) }
    viewModel { ProgressViewModel() }
    viewModel { SurveyInformationViewModel(get()) }
    viewModel { SurveyAvailabilityViewModel() }
    viewModel { WeeklySurveyViewModel() }
    viewModel { WritingViewModel(get()) }
    viewModel { ResourcesViewModel(get()) }

    single<DataStore<JwtTokenResponse>>(qualifier = named("jwtTokenResponse")) { getDataStore() }
    single<JwtTokenResponseImpl> {
        JwtTokenResponseImpl(
            get<DataStore<JwtTokenResponse>>(
                qualifier = named("jwtTokenResponse")
            )
        )
    }

    single<DataStore<NotificationTimes>>(qualifier = named("notificationTimes")) { getTimesDataStore() }
    single<NotificationTimesImpl> {
        NotificationTimesImpl(
            get<DataStore<NotificationTimes>>(
                qualifier = named("notificationTimes")
            )
        )
    }

    single<DataStore<com.lemurs.HealthConnectTokens>>(qualifier = named("healthConnectTokens")) { getHealthConnectTokensDataStore() }
    single<com.lemurs.lemurs_app.data.datastore.HealthConnectTokensImpl> {
        com.lemurs.lemurs_app.data.datastore.HealthConnectTokensImpl(
            get<DataStore<com.lemurs.HealthConnectTokens>>(
                qualifier = named("healthConnectTokens")
            )
        )
    }
}
