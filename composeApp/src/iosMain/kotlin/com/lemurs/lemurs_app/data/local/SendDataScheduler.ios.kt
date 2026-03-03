package com.lemurs.lemurs_app.data.local

import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.data.local.passiveData.ScreentimeDAO
import com.lemurs.lemurs_app.data.local.passiveData.ScreentimeDataUseCase
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import platform.BackgroundTasks.BGTaskScheduler
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSinceNow

/**
 * Class to schedule work for syncing passive data to API on iOS
 */
actual class SendDataScheduler : KoinComponent {

    private val logger = Logger.withTag("SendDataScheduler-iOS")
    private val screentimeDAO: ScreentimeDAO by inject()

    private val screentimeTaskIdentifier = "com.lemurs.lemurs_app.sendScreentimeData"

    /**
     * Register background tasks - call this during app initialization
     */
    fun registerBackgroundTasks() {
        BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(
            screentimeTaskIdentifier,
            null
        ) { task ->
            task?.let { handleScreentimeSyncTask(it) }
        }
        logger.i("Registered background task: $screentimeTaskIdentifier")
    }

    /**
     * Handle screen time data sync task
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun handleScreentimeSyncTask(task: platform.BackgroundTasks.BGTask) {
        logger.i("Screen time sync task started")

        var taskCompleted = false

        // Set expiration handler
        task.expirationHandler = {
            logger.w("Screen time sync task expired")
            task.setTaskCompletedWithSuccess(false)
            taskCompleted = true
        }

        // Perform sync
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val screentimeDataUseCase = ScreentimeDataUseCase(screentimeDAO)
                val result = screentimeDataUseCase.call()

                if (!taskCompleted) {
                    when (result) {
                        is UseCaseResult.Success -> {
                            logger.i("Screen time sync completed successfully")
                            task.setTaskCompletedWithSuccess(true)
                        }
                        is UseCaseResult.Failure -> {
                            logger.w("Screen time sync failed")
                            task.setTaskCompletedWithSuccess(false)
                        }
                    }
                }
            } catch (e: Exception) {
                logger.e("Screen time sync error: ${e.message}")
                if (!taskCompleted) {
                    task.setTaskCompletedWithSuccess(false)
                }
            } finally {
                // Schedule next sync
                scheduleScreentime()
            }
        }
    }

    actual fun scheduleBluetooth() {
        // iOS bluetooth scheduling handled separately by BluetoothSchedulerRegistration
        logger.i("Bluetooth scheduling handled by BluetoothSchedulerRegistration")
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun scheduleScreentime() {
        val request = BGAppRefreshTaskRequest(screentimeTaskIdentifier)

        // Run after 15 minutes
        request.earliestBeginDate = NSDate.dateWithTimeIntervalSinceNow(15.0 * 60.0)

        try {
            BGTaskScheduler.sharedScheduler.submitTaskRequest(request, null)
            logger.i("Scheduled screen time sync for ~15 minutes from now")
        } catch (e: Exception) {
            logger.e("Failed to schedule screen time sync: ${e.message}")
        }
    }

    actual fun scheduleSurveyResponse() {
        // iOS survey response scheduling - not yet implemented for iOS
        logger.i("Survey response scheduling - not yet implemented for iOS")
    }

    /**
     * FOR TESTING: Perform immediate sync without waiting for background task
     */
    suspend fun performImmediateSync(): Boolean {
        logger.i("Performing immediate screen time sync (TESTING)")
        return try {
            val screentimeDataUseCase = ScreentimeDataUseCase(screentimeDAO)
            val result = screentimeDataUseCase.call()

            when (result) {
                is UseCaseResult.Success -> {
                    logger.i("Immediate screen time sync completed successfully")
                    // Schedule next background sync
                    scheduleScreentime()
                    true
                }
                is UseCaseResult.Failure -> {
                    logger.w("Immediate screen time sync failed")
                    false
                }
            }
        } catch (e: Exception) {
            logger.e("Immediate screen time sync error: ${e.message}")
            false
        }
    }
}



