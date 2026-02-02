package com.lemurs.lemurs_app.data.health

/**
 * Protocol interface for iOS HealthKit bridge.
 * This interface matches the HealthKitKotlinBridge Swift class.
 *
 * The actual implementation is in Swift (HealthKitKotlinBridge.swift).
 * Kotlin calls these methods via Objective-C interop.
 */
interface IOSHealthKitBridge {
    /**
     * Check if HealthKit is available on this device.
     */
    fun isHealthKitAvailable(): Boolean

    /**
     * Check if HealthKit permissions have been granted.
     * Note: On iOS, we can only check if we've requested authorization,
     * not whether the user actually granted it (for privacy reasons).
     */
    fun checkAuthorizationStatus(
        onResult: (Boolean) -> Unit,
        onError: (String) -> Unit
    )

    /**
     * Request HealthKit permissions from the user.
     * This will show the iOS Health permissions dialog.
     */
    fun requestAuthorization(
        onSuccess: (Boolean) -> Unit,
        onError: (String) -> Unit
    )

    /**
     * Get step count for a date range.
     * @param startTimeMillis Start of the date range (Unix epoch milliseconds)
     * @param endTimeMillis End of the date range (Unix epoch milliseconds)
     */
    fun getStepCount(
        startTimeMillis: Long,
        endTimeMillis: Long,
        onSuccess: (Long) -> Unit,
        onError: (String) -> Unit
    )

    /**
     * Get active calories burned for a date range.
     * @param startTimeMillis Start of the date range (Unix epoch milliseconds)
     * @param endTimeMillis End of the date range (Unix epoch milliseconds)
     */
    fun getActiveCaloriesBurned(
        startTimeMillis: Long,
        endTimeMillis: Long,
        onSuccess: (Double) -> Unit,
        onError: (String) -> Unit
    )

    /**
     * Get walking/running distance for a date range.
     * @param startTimeMillis Start of the date range (Unix epoch milliseconds)
     * @param endTimeMillis End of the date range (Unix epoch milliseconds)
     * @return Distance in meters
     */
    fun getDistance(
        startTimeMillis: Long,
        endTimeMillis: Long,
        onSuccess: (Double) -> Unit,
        onError: (String) -> Unit
    )

    /**
     * Get heart rate samples for a date range.
     * @param startTimeMillis Start of the date range (Unix epoch milliseconds)
     * @param endTimeMillis End of the date range (Unix epoch milliseconds)
     * @return List of heart rate values (beats per minute)
     */
    fun getHeartRateSamples(
        startTimeMillis: Long,
        endTimeMillis: Long,
        onSuccess: (List<Double>) -> Unit,
        onError: (String) -> Unit
    )

    /**
     * Get sleep analysis for a date range.
     * @param startTimeMillis Start of the date range (Unix epoch milliseconds)
     * @param endTimeMillis End of the date range (Unix epoch milliseconds)
     * @return Total sleep duration in minutes
     */
    fun getSleepAnalysis(
        startTimeMillis: Long,
        endTimeMillis: Long,
        onSuccess: (Long) -> Unit,
        onError: (String) -> Unit
    )

    /**
     * Enable background delivery for health data updates.
     * This allows the app to be notified when new health data is available.
     */
    fun enableBackgroundDelivery(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )
}

/**
 * Provider for accessing the Swift HealthKit bridge.
 * This is set from Swift side during app initialization.
 */
object IOSHealthKitBridgeProvider {
    var bridge: IOSHealthKitBridge? = null
}
