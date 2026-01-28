package com.lemurs.lemurs_app.bluetooth

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.lemurs.lemurs_app.data.AndroidContextProvider
import java.util.concurrent.TimeUnit

class BluetoothScheduler {
    private val context =  requireNotNull(AndroidContextProvider.context)
    private val workManager = WorkManager.getInstance(context)
    fun schedule() {
        //repeats every 15 MINUTES
        val dataRequest =
            PeriodicWorkRequestBuilder<BluetoothWorker>(15, TimeUnit.MINUTES)
                .setConstraints(getConstraints())
                .build()

        workManager.enqueueUniquePeriodicWork("collect bluetooth schedule", ExistingPeriodicWorkPolicy.KEEP, dataRequest)
    }

    private fun getConstraints(): Constraints {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
        return constraints
    }
}