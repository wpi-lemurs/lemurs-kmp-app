package com.lemurs.lemurs_app.health

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.lemurs.lemurs_app.data.AndroidActivityLauncherProvider.logger
import com.lemurs.lemurs_app.data.AndroidContextProvider
import java.util.concurrent.TimeUnit

class HealthDataScheduler {

    private fun getConstraints(): Constraints {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
        return constraints
    }

    private val context = requireNotNull(AndroidContextProvider.context)
    private val workManager = WorkManager.getInstance(context)


    fun scheduleHealth() {
        logger.w("scheduling health data in worker")
        val request =
            // Minimum interval for PeriodicWorkRequest is 15 minutes
            // For more frequent testing, you can use OneTimeWorkRequest with delays
            PeriodicWorkRequestBuilder<HealthDataWorker>(15, TimeUnit.MINUTES)
                .setConstraints(getConstraints())
                .build()

        workManager.enqueueUniquePeriodicWork("health data schedule", ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /**
     * For debugging/testing: Run the health worker immediately once
     * This is useful for testing without waiting 15 minutes
     */
    fun scheduleOneTime() {
        logger.w("scheduling one-time health data worker for immediate execution")
        val request = OneTimeWorkRequestBuilder<HealthDataWorker>()
            .setConstraints(getConstraints())
            .build()

        workManager.enqueue(request)
    }
}