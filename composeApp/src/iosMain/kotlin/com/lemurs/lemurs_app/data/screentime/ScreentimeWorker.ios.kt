package com.lemurs.lemurs_app.data.screentime

import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.data.local.UseCaseResult
import com.lemurs.lemurs_app.data.local.passiveData.ScreentimeDAO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * iOS implementation of ScreentimeWorker.
 * Unlike Android's WorkManager-based worker, iOS uses BGTaskScheduler
 * for background tasks. This class provides the business logic that
 * gets called when the background task executes.
 */
class ScreentimeWorker : KoinComponent {
    private val logger = Logger.withTag("ScreentimeWorker-iOS")
    private val screentimeDAO: ScreentimeDAO by inject()
    private val screentimeUseCase = ScreentimeUseCase(screentimeDAO)

    /**
     * Execute the screen time collection work.
     * This is called by the BGTaskScheduler when the background task runs.
     */
    suspend fun doWork(): Boolean {
        logger.w("ScreentimeWorker: Starting work")

        return when (screentimeUseCase.getScreenTime()) {
            is UseCaseResult.Success<*> -> {
                logger.w("ScreentimeWorker: Work completed successfully")
                true
            }
            is UseCaseResult.Failure<*> -> {
                logger.w("ScreentimeWorker: Work failed, should retry")
                false
            }
            else -> {
                logger.w("ScreentimeWorker: Unknown result, should retry")
                false
            }
        }
    }

    /**
     * Execute work in a coroutine scope.
     * This allows the Swift bridge to call this method easily.
     */
    fun executeWork(completion: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = doWork()
            completion(result)
        }
    }
}