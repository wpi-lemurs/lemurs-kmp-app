package com.lemurs.lemurs_app.data.local

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.data.AndroidContextProvider
import java.util.concurrent.TimeUnit

/**
 * Class to schedule work every two hours for syncing passive data to API
 */
actual class SendDataScheduler{
    private val context = requireNotNull(AndroidContextProvider.context)
    private val workManager = WorkManager.getInstance(context)
    /**
     * creates periodic work request to call passive data worker every two hours
     */
    val logger = Logger.withTag("Work Manager")
    fun getConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .build()
    }

    actual fun scheduleBluetooth() {
        //repeats every 2 hours
        val bluetoothRequest =
            PeriodicWorkRequestBuilder<SendBluetoothDataWorker>(2, TimeUnit.HOURS)
                .setConstraints(getConstraints())
                .build()
        workManager.enqueueUniquePeriodicWork("send bluetooth schedule", ExistingPeriodicWorkPolicy.KEEP,bluetoothRequest)
    }

    //
    actual fun scheduleScreentime() {
        val screentimeRequest =
            PeriodicWorkRequestBuilder<SendScreentimeDataWorker>(15, TimeUnit.MINUTES)
                .setConstraints(getConstraints())
                .build()
        workManager.enqueueUniquePeriodicWork("send screentime schedule", ExistingPeriodicWorkPolicy.KEEP, screentimeRequest)
    }

    actual fun scheduleSurveyResponse() {
        val surveyResponseRequest =
            PeriodicWorkRequestBuilder<SendSurveyResponseWorker>(1, TimeUnit.HOURS)
                .setConstraints(getConstraints())
                .build()
        workManager.enqueueUniquePeriodicWork(
            "send survey response schedule",
            ExistingPeriodicWorkPolicy.KEEP,
            surveyResponseRequest
        )
        logger.i { "Scheduled survey response worker every 1 hour" }
    }
}