package com.lemurs.lemurs_app.health

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lemurs.lemurs_app.data.local.UseCaseResult
import com.lemurs.lemurs_app.data.repositories.AppRepository
import com.lemurs.lemurs_app.data.AndroidActivityProvider
import com.lemurs.lemurs_app.data.local.passiveData.BluetoothDAO
import com.lemurs.lemurs_app.data.local.passiveData.CalorieDAO
import com.lemurs.lemurs_app.data.local.passiveData.DistanceDAO
import com.lemurs.lemurs_app.data.local.passiveData.HealthDataUseCase
import com.lemurs.lemurs_app.data.local.passiveData.SleepDAO
import com.lemurs.lemurs_app.data.local.passiveData.SpeedDAO
import com.lemurs.lemurs_app.data.local.passiveData.StepDAO
import com.lemurs.lemurs_app.data.local.passiveData.WeightDAO
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Worker class for collecting and storing health data
 */
class HealthDataWorker(appContext: Context, workerParams: WorkerParameters):
    CoroutineWorker(appContext, workerParams), KoinComponent {

    val appRepository : AppRepository by inject()
    val healthConnectTokensImpl: com.lemurs.lemurs_app.data.datastore.HealthConnectTokensImpl by inject()
    private val stepDAO: StepDAO by inject()
    private val calorieDAO: CalorieDAO by inject()
    private val distanceDAO: DistanceDAO by inject()
    private val speedDAO: SpeedDAO by inject()
    private val weightDAO: WeightDAO by inject()
    private val sleepDAO: SleepDAO by inject()

    private val activity = requireNotNull(AndroidActivityProvider.activity)
    val application = activity.application
        val health = HealthConnectViewModel(
            application,
            appRepository,
            stepDAO,
            calorieDAO,
            distanceDAO,
            speedDAO,
            weightDAO,
            sleepDAO,
            healthConnectTokensImpl
        )
    val healthDataUseCase = HealthDataUseCase(
        stepDAO,
        calorieDAO,
        distanceDAO,
        speedDAO,
        weightDAO,
        sleepDAO
    )

    /**
     * Calls passive data use case to do work
     * 1. Attempts to submit any previously stored data from local database
     * 2. If successful, pulls new health data changes from HealthConnect
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    override suspend fun doWork(): Result {
        // First, try to submit any previously stored data from local database
        val submitResult = healthDataUseCase.call()

        // Pull changes from HealthConnect using changes tokens
        // This gets only new/updated data since the last sync
        return try {
            if (submitResult is UseCaseResult.Success<*>) {
                // Pull all changes from Health Connect
                health.pullAllChanges()
                Result.success()
            } else {
                // If we couldn't submit local data, still try to pull changes
                // but the new data will be stored locally
                health.pullAllChanges()
                Result.retry()
            }
        } catch (e: Exception) {
            // If pulling changes fails, retry later
            Result.retry()
        }
    }
}