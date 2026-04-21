package com.lemurs.lemurs_app.data.screentime

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lemurs.lemurs_app.data.local.UseCaseResult
import com.lemurs.lemurs_app.data.local.passiveData.ScreentimeDAO
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Worker class for collecting and storing screen time data
 */
class ScreentimeWorker(appContext: Context, workerParams: WorkerParameters):
    CoroutineWorker(appContext, workerParams), KoinComponent {
    val screentimeDAO: ScreentimeDAO by inject()

    val screentimeStore = ScreentimeTimeStore(appContext)
    val screentimeUseCase = ScreentimeUseCase(screentimeDAO, screentimeStore)

    /**
     * calls passive data use case to do work
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    override suspend fun doWork(): Result {
        return when(screentimeUseCase.getScreenTime()) {
            is UseCaseResult.Success<*> -> Result.success()
            is UseCaseResult.Failure<*> -> Result.retry()
            else -> Result.retry()
        }
    }
}