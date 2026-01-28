package com.lemurs.lemurs_app.data.screentime

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.lemurs.lemurs_app.data.AndroidContextProvider
import java.util.concurrent.TimeUnit

actual class ScreentimeScheduler {
    private val context =  requireNotNull(AndroidContextProvider.context)
    private val workManager = WorkManager.getInstance(context)
    actual fun schedule() {
        //repeats every 15 MINUTES
        val dataRequest =
            PeriodicWorkRequestBuilder<ScreentimeWorker>(15, TimeUnit.MINUTES)
                .setConstraints(getConstraints())
                .build()

        workManager.enqueueUniquePeriodicWork("collect screentime schedule", ExistingPeriodicWorkPolicy.KEEP, dataRequest)
    }

    actual fun scheduleQuick() {
        //repeats every 30 seconds
//        val dataRequest =
//            PeriodicWorkRequestBuilder<ScreentimeWorker>(30, TimeUnit.SECONDS)
//                .setConstraints(constraints)
//                .build()

        //repeats every 1 minute
        val dataRequest =
            PeriodicWorkRequestBuilder<ScreentimeWorker>(1, TimeUnit.MINUTES)
                .setConstraints(getConstraints())
                .build()

        workManager.enqueue(dataRequest)
    }

    private fun getConstraints(): Constraints {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
        return constraints
    }
}