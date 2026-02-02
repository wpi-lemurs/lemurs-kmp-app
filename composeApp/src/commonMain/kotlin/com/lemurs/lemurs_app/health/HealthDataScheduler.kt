package com.lemurs.lemurs_app.health

/**
 * iOS Health Data Scheduler - expect declaration for multiplatform
 *
 * This provides a common interface for scheduling health data sync.
 * The actual implementation is in Swift using BGTaskScheduler.
 */
expect class HealthDataScheduler() {
    /**
     * Schedule periodic health data sync (every 15 minutes when possible)
     * On iOS, the system determines the actual execution time based on usage patterns
     */
    fun scheduleHealth()

    /**
     * Schedule an immediate one-time health data sync for testing
     */
    fun scheduleOneTime()
}
