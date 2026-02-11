package com.lemurs.lemurs_app.data.screentime

import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.data.local.UseCaseResult
import com.lemurs.lemurs_app.data.local.passiveData.Screentime
import com.lemurs.lemurs_app.data.local.passiveData.ScreentimeDAO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.koin.core.component.KoinComponent

/**
 * iOS implementation of ScreentimeUseCase.
 * Collects screen time data using iOS Screen Time API (via Swift bridge).
 *
 * On iOS, screen time data is accessed through:
 * - DeviceActivityReport (iOS 15+) for detailed app usage
 * - Family Controls framework for screen time permissions
 */
class ScreentimeUseCase(
    private val screentimeDAO: ScreentimeDAO
) : KoinComponent {
    private val logger = Logger.withTag("ScreentimeUseCase-iOS")

    /**
     * Collect screen time data for the last 15 minutes.
     */
    suspend fun getScreenTime(): UseCaseResult<Any> {
        logger.w("Starting screen time data collection on iOS")

        if (!checkPermissions()) {
            logger.w("Screen time permissions not granted")
            return UseCaseResult.Failure()
        }

        val screentimeDataList = getUsageStats()

        if (screentimeDataList.isEmpty()) {
            logger.w("No screen time data collected")
            return UseCaseResult.Success(Any())
        }

        CoroutineScope(Dispatchers.IO).launch {
            val uniqueData = screentimeDataList.distinctBy {
                Triple(it.appName, it.totalTime, it.lastTimeUsed)
            }
            logger.w("Collected ${screentimeDataList.size} entries, inserting ${uniqueData.size} unique entries")
            screentimeDAO.insertScreentimeListData(uniqueData)
        }

        logger.w("Done adding screen time data")
        return UseCaseResult.Success(Any())
    }

    private fun getUsageStats(): List<Screentime> {
        logger.w("Getting usage stats from iOS Screen Time API")

        val now = Clock.System.now().toEpochMilliseconds()
        val fifteenMinAgo = now - (15 * 60 * 1000)

        logger.w("Querying screen time from ${Instant.fromEpochMilliseconds(fifteenMinAgo)} to ${Instant.fromEpochMilliseconds(now)}")

        // Get data from Swift bridge
        val bridge = IOSScreenTimeDataProvider.bridge
        val screentimeDataList = if (bridge != null) {
            logger.w("Calling Swift bridge to get screen time data")
            bridge.getUsageStats(fifteenMinAgo, now)
        } else {
            logger.e("Swift bridge not available - cannot collect screen time data")
            logger.w("Make sure ScreenTimeBridge is properly registered")
            emptyList()
        }

        logger.w("iOS Screen Time API returned ${screentimeDataList.size} entries")

        return screentimeDataList
    }

    private fun checkPermissions(): Boolean {
        logger.w("Checking screen time permissions on iOS")

        val bridge = IOSScreenTimeDataProvider.bridge
        val hasPermission = if (bridge != null) {
            bridge.isAuthorized()
        } else {
            logger.e("Swift bridge not available - cannot check permissions")
            false
        }

        logger.w("iOS Screen Time permissions: $hasPermission")

        return hasPermission
    }
}

/**
 * Provider for accessing the Swift screen time data bridge.
 * Set from Swift side during app initialization.
 */
object IOSScreenTimeDataProvider {
    var bridge: IOSScreenTimeDataBridge? = null
}

/**
 * Interface to the Swift screen time data collector.
 * This matches the @objc methods exposed from Swift.
 */
interface IOSScreenTimeDataBridge {
    /**
     * Get usage statistics for the specified time range.
     * @param startTimeMillis Start of the time range (Unix epoch ms)
     * @param endTimeMillis End of the time range (Unix epoch ms)
     * @return List of Screentime objects with app usage data
     */
    fun getUsageStats(startTimeMillis: Long, endTimeMillis: Long): List<Screentime>

    /**
     * Check if Screen Time authorization is granted.
     * @return true if authorized, false otherwise
     */
    fun isAuthorized(): Boolean
}


