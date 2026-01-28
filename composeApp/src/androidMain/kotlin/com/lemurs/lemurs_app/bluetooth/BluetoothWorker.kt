package com.lemurs.lemurs_app.bluetooth

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lemurs.lemurs_app.data.AndroidActivityProvider
import com.lemurs.lemurs_app.data.local.UseCaseResult
import com.lemurs.lemurs_app.data.local.passiveData.BluetoothDAO
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

/**
 * Worker class for collecting and storing screen time data
 */
class BluetoothWorker(appContext: Context, workerParams: WorkerParameters):
    CoroutineWorker(appContext, workerParams), KoinComponent {


    private val activity = requireNotNull(AndroidActivityProvider.activity)
    private val bluetoothDAO: BluetoothDAO by inject()
    val bluetoothUseCase = BluetoothFunction(activity, bluetoothDAO)

    /**
     * calls passive data use case to do work
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    override suspend fun doWork(): Result {
        return when(bluetoothUseCase.scan()) {
            is UseCaseResult.Success<*> -> Result.success()
            is UseCaseResult.Failure<*> -> Result.retry()
            else -> Result.retry()
        }
    }
}