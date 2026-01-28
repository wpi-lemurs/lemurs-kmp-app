package com.lemurs.lemurs_app.health

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.lemurs.lemurs_app.data.AndroidContextProvider
import java.util.concurrent.TimeUnit

class HealthScheduler {

    // Use HealthDataScheduler instead. This is from the previous team and is not called
    private val context = requireNotNull(AndroidContextProvider.context)
    private val workManager = WorkManager.getInstance(context)

    fun schedule() {
        // Repeats every 15 MINUTES
        val dataRequest =
            PeriodicWorkRequestBuilder<HealthDataWorker>(15, TimeUnit.MINUTES)
                .setConstraints(getConstraints())
                .build()

        workManager.enqueueUniquePeriodicWork(
            "collect health schedule",
            ExistingPeriodicWorkPolicy.KEEP,
            dataRequest
        )
    }

    private fun getConstraints(): Constraints {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
        return constraints
    }
}

