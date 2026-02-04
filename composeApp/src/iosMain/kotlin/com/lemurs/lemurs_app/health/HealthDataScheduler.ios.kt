package com.lemurs.lemurs_app.health

import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.data.dtos.CaloriesDataDto
import com.lemurs.lemurs_app.data.dtos.DistanceDataDto
import com.lemurs.lemurs_app.data.dtos.StepsDataDto
import com.lemurs.lemurs_app.data.health.IOSHealthKitBridgeProvider
import com.lemurs.lemurs_app.data.repositories.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * iOS implementation of HealthDataScheduler.
 *
 * This uses Swift's BGTaskScheduler for background health data sync.
 * The actual background task registration and execution is handled in Swift
 * (HealthDataTaskScheduler.swift) because BGTaskScheduler requires native iOS APIs.
 *
 * This Kotlin class provides the interface that can be called from common code,
 * and delegates to the Swift implementation via the bridge.
 */
actual class HealthDataScheduler {

    private val logger = Logger.withTag("HealthDataScheduler-iOS")

    /**
     * Schedule periodic health data sync.
     * On iOS, this registers a BGAppRefreshTask that the system will execute
     * at opportune times (typically every 15+ minutes, but system-determined).
     */
    actual fun scheduleHealth() {
        logger.i("Scheduling periodic health data sync via Swift bridge")

        val bridge = IOSHealthDataSchedulerProvider.bridge
        if (bridge == null) {
            logger.w("Health data scheduler bridge not available - ensure it's registered in Swift")
            return
        }

        bridge.scheduleBackgroundHealthSync()
    }

    /**
     * Schedule an immediate one-time health data sync for testing.
     * This triggers an immediate sync rather than waiting for the background scheduler.
     */
    actual fun scheduleOneTime() {
        logger.i("Scheduling one-time health data sync via Swift bridge")

        val bridge = IOSHealthDataSchedulerProvider.bridge
        if (bridge == null) {
            logger.w("Health data scheduler bridge not available - ensure it's registered in Swift")
            // Fall back to direct HealthKit call if bridge not available
            performImmediateSync()
            return
        }

        bridge.performImmediateSync()
    }

    /**
     * Fallback: perform immediate sync directly via HealthKit bridge
     */
    private fun performImmediateSync() {
        logger.i("Performing immediate health sync via HealthKit bridge")

        val healthBridge = IOSHealthKitBridgeProvider.bridge
        if (healthBridge == null) {
            logger.e("HealthKit bridge not available")
            return
        }

        // Get current time range (last 24 hours)
        val now = kotlinx.datetime.Clock.System.now()
        val oneDayInMillis = 24 * 60 * 60 * 1000L
        val startTime = now.toEpochMilliseconds() - oneDayInMillis
        val endTime = now.toEpochMilliseconds()

        // Fetch step count as an example
        healthBridge.getStepCount(
            startTimeMillis = startTime,
            endTimeMillis = endTime,
            onSuccess = { steps ->
                logger.i("Immediate sync: fetched $steps steps")
            },
            onError = { error ->
                logger.e("Immediate sync failed: $error")
            }
        )
    }
}

/**
 * Interface for iOS health data scheduler bridge.
 * Implemented in Swift (HealthDataTaskScheduler.swift).
 */
interface IOSHealthDataSchedulerBridge {
    /**
     * Schedule background health data sync using BGTaskScheduler
     */
    fun scheduleBackgroundHealthSync()

    /**
     * Perform immediate health data sync (for testing)
     */
    fun performImmediateSync()

    /**
     * Cancel all scheduled background health tasks
     */
    fun cancelScheduledTasks()
}

/**
 * Provider for accessing the Swift health data scheduler bridge.
 * Set from Swift side during app initialization.
 */
object IOSHealthDataSchedulerProvider {
    var bridge: IOSHealthDataSchedulerBridge? = null
}

/**
 * Interface that Swift calls when health data is collected.
 * Kotlin implements this to receive data and send it to the API.
 */
interface IOSHealthDataCallback {
    /**
     * Called when steps data is collected from HealthKit.
     * @param steps The step count
     * @param startTimeMillis Start of the measurement period (Unix epoch ms)
     * @param endTimeMillis End of the measurement period (Unix epoch ms)
     */
    fun onStepsCollected(steps: Long, startTimeMillis: Long, endTimeMillis: Long)

    /**
     * Called when calories data is collected from HealthKit.
     * @param calories The active calories burned
     * @param startTimeMillis Start of the measurement period (Unix epoch ms)
     * @param endTimeMillis End of the measurement period (Unix epoch ms)
     */
    fun onCaloriesCollected(calories: Double, startTimeMillis: Long, endTimeMillis: Long)

    /**
     * Called when distance data is collected from HealthKit.
     * @param distanceMeters The distance walked/run in meters
     * @param startTimeMillis Start of the measurement period (Unix epoch ms)
     * @param endTimeMillis End of the measurement period (Unix epoch ms)
     */
    fun onDistanceCollected(distanceMeters: Double, startTimeMillis: Long, endTimeMillis: Long)

    /**
     * Called when all health data sync is complete
     * @param success Whether the sync was successful
     */
    fun onSyncComplete(success: Boolean)
}

/**
 * Provider for the health data callback.
 * Swift calls these methods when data is collected.
 */
object IOSHealthDataCallbackProvider {
    var callback: IOSHealthDataCallback? = null

    /**
     * Register the default callback implementation.
     * Call this from Swift during app initialization:
     * IOSHealthDataCallbackProvider.shared.register()
     */
    fun register() {
        callback = IOSHealthDataCallbackImpl()
    }
}

/**
 * Implementation of IOSHealthDataCallback that sends health data to the API.
 * This is called by Swift when health data is collected during background sync.
 */
class IOSHealthDataCallbackImpl : IOSHealthDataCallback, KoinComponent {

    private val logger = Logger.withTag("HealthDataCallback-iOS")
    private val appRepository: AppRepository by inject()
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onStepsCollected(steps: Long, startTimeMillis: Long, endTimeMillis: Long) {
        logger.i("📊 Received steps from HealthKit: $steps (sending to API...)")

        scope.launch {
            try {
                val startDateTime = Instant.fromEpochMilliseconds(startTimeMillis)
                    .toLocalDateTime(TimeZone.UTC)
                val endDateTime = Instant.fromEpochMilliseconds(endTimeMillis)
                    .toLocalDateTime(TimeZone.UTC)

                val stepsDto = StepsDataDto(
                    userId = "",
                    steps = steps,
                    start_timestamp = startDateTime,
                    end_timestamp = endDateTime,
                    appSource = "HealthKit"
                )

                val success = appRepository.sendStepsData(stepsDto)
                if (success) {
                    logger.i("✅ Steps data sent to API successfully")
                } else {
                    logger.e("❌ Failed to send steps data to API")
                }
            } catch (e: Exception) {
                logger.e("❌ Error sending steps data: ${e.message}")
            }
        }
    }

    override fun onCaloriesCollected(calories: Double, startTimeMillis: Long, endTimeMillis: Long) {
        logger.i("📊 Received calories from HealthKit: $calories (sending to API...)")

        scope.launch {
            try {
                val startDateTime = Instant.fromEpochMilliseconds(startTimeMillis)
                    .toLocalDateTime(TimeZone.UTC)
                val endDateTime = Instant.fromEpochMilliseconds(endTimeMillis)
                    .toLocalDateTime(TimeZone.UTC)

                val caloriesDto = CaloriesDataDto(
                    userId = "",
                    calories = calories.toInt(),
                    start_timestamp = startDateTime,
                    end_timestamp = endDateTime,
                    appSource = "HealthKit"
                )

                val success = appRepository.sendCaloriesData(caloriesDto)
                if (success) {
                    logger.i("✅ Calories data sent to API successfully")
                } else {
                    logger.e("❌ Failed to send calories data to API")
                }
            } catch (e: Exception) {
                logger.e("❌ Error sending calories data: ${e.message}")
            }
        }
    }

    override fun onDistanceCollected(distanceMeters: Double, startTimeMillis: Long, endTimeMillis: Long) {
        logger.i("📊 Received distance from HealthKit: $distanceMeters meters (sending to API...)")

        scope.launch {
            try {
                val startDateTime = Instant.fromEpochMilliseconds(startTimeMillis)
                    .toLocalDateTime(TimeZone.UTC)
                val endDateTime = Instant.fromEpochMilliseconds(endTimeMillis)
                    .toLocalDateTime(TimeZone.UTC)

                val distanceDto = DistanceDataDto(
                    userId = "",
                    distance = distanceMeters,
                    start_timestamp = startDateTime,
                    end_timestamp = endDateTime,
                    appSource = "HealthKit"
                )

                val success = appRepository.sendDistanceData(distanceDto)
                if (success) {
                    logger.i("✅ Distance data sent to API successfully")
                } else {
                    logger.e("❌ Failed to send distance data to API")
                }
            } catch (e: Exception) {
                logger.e("❌ Error sending distance data: ${e.message}")
            }
        }
    }

    override fun onSyncComplete(success: Boolean) {
        if (success) {
            logger.i("✅ Health data sync completed successfully")
        } else {
            logger.w("⚠️ Health data sync completed with errors")
        }
    }
}


