package com.lemurs.lemurs_app.data.screentime

import co.touchlab.kermit.Logger

/**
 * iOS implementation of ScreentimeScheduler.
 * Schedules periodic screen time data collection using BGTaskScheduler via Swift bridge.
 */
actual class ScreentimeScheduler {
    private val logger = Logger.withTag("ScreentimeScheduler-iOS")

    /**
     * Schedule periodic screen time collection (every 15 minutes).
     * Delegates to Swift BGTaskScheduler bridge.
     */
    actual fun schedule() {
        logger.w("Scheduling screen time collection (15 minute interval)")

        val bridge = IOSScreenTimeSchedulerProvider.bridge
        if (bridge != null) {
            bridge.schedule()
            logger.w("Screen time collection scheduled via Swift bridge")
        } else {
            logger.e("Swift bridge not available - cannot schedule screen time collection")
            logger.w("Make sure registerScreenTimeSchedulerWithKotlin() is called in iOSApp.swift")
        }
    }

    /**
     * Schedule quick screen time collection for testing (triggers immediately).
     */
    actual fun scheduleQuick() {
        logger.w("Scheduling quick screen time collection (immediate)")

        val bridge = IOSScreenTimeSchedulerProvider.bridge
        if (bridge != null) {
            bridge.scheduleQuick()
            logger.w("Quick screen time collection triggered via Swift bridge")
        } else {
            logger.e("Swift bridge not available - cannot schedule quick collection")
        }
    }
}

/**
 * Provider for accessing the Swift screen time scheduler bridge.
 * Set from Swift side during app initialization.
 */
object IOSScreenTimeSchedulerProvider {
    var bridge: IOSScreenTimeSchedulerBridge? = null
}

/**
 * Interface to the Swift ScreenTimeSchedulerBridgeAdapter.
 * This matches the @objc methods exposed from Swift.
 */
interface IOSScreenTimeSchedulerBridge {
    /**
     * Schedule periodic screen time collection (every 15 minutes)
     */
    fun schedule()

    /**
     * Schedule quick screen time collection for testing (immediate)
     */
    fun scheduleQuick()

    /**
     * Cancel all scheduled screen time tasks
     */
    fun cancelAll()
}

