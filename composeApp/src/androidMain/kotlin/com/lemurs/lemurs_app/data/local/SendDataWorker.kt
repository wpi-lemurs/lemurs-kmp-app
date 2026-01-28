package com.lemurs.lemurs_app.data.local

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lemurs.lemurs_app.data.local.passiveData.BluetoothDAO
import com.lemurs.lemurs_app.data.local.passiveData.BluetoothDataUseCase
import com.lemurs.lemurs_app.data.local.passiveData.GPSDAO
import com.lemurs.lemurs_app.data.local.passiveData.GPSDataUseCase
import com.lemurs.lemurs_app.data.local.passiveData.ScreentimeDAO
import com.lemurs.lemurs_app.data.local.passiveData.ScreentimeDataUseCase
import com.lemurs.lemurs_app.data.local.activeData.SurveyResponseDAO
import com.lemurs.lemurs_app.data.local.activeData.AudioDAO
import com.lemurs.lemurs_app.data.local.activeData.WrittenDAO
import com.lemurs.lemurs_app.data.repositories.AppRepository
import com.lemurs.lemurs_app.data.datasource.health.HealthRemoteDataSource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Worker class for sending passive data
 */
class SendBluetoothDataWorker (appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams), KoinComponent {
    private val bluetoothDAO: BluetoothDAO by inject()
    val bluetoothDataUseCase = BluetoothDataUseCase(bluetoothDAO)



    /**
     * calls passive data use case to do work
     */
    override suspend fun doWork(): Result {
        return when (bluetoothDataUseCase.call()) {
            is UseCaseResult.Success -> Result.success()
            is UseCaseResult.Failure -> Result.retry()
        }
    }
}

class SendGpsDataWorker (appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams), KoinComponent {
    private val gpsDAO: GPSDAO by inject()
    val gpsDataUseCase = GPSDataUseCase(gpsDAO)

    /**
     * calls passive data use case to do work
     */
    override suspend fun doWork(): Result {
        return when (gpsDataUseCase.call()) {
            is UseCaseResult.Success -> Result.success()
            is UseCaseResult.Failure -> Result.retry()
        }
    }
}

class SendScreentimeDataWorker (appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams), KoinComponent {

    private val screentimeDAO: ScreentimeDAO by inject()
    val screentimeDataUseCase = ScreentimeDataUseCase(screentimeDAO)

    /**
     * calls passive data use case to do work
     */
    override suspend fun doWork(): Result {
        return when (screentimeDataUseCase.call()) {
            is UseCaseResult.Success -> Result.success()
            is UseCaseResult.Failure -> Result.retry()
        }
    }
}

class SendSurveyResponseWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams), KoinComponent {
    private val surveyResponseDAO: SurveyResponseDAO by inject()
    val healthRemoteDataSource: HealthRemoteDataSource by inject()
    val audioDAO: AudioDAO by inject()
    val writingDAO: WrittenDAO by inject()
    val appRepository = AppRepository(healthRemoteDataSource, surveyResponseDAO, audioDAO, writingDAO)
    override suspend fun doWork(): Result {
        return try {
            val allSent = appRepository.handleSurveyResponse()
            if (allSent) Result.success() else Result.retry()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
