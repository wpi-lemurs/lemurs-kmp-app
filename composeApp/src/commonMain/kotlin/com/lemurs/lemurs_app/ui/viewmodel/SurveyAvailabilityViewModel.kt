package com.lemurs.lemurs_app.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.survey.fetchAndParseAvailability
import com.lemurs.lemurs_app.util.Constants
import com.lemurs.lemurs_app.util.DemoMode
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.until
import kotlin.time.Duration.Companion.hours

class SurveyAvailabilityViewModel : ViewModel() {
    private var availability = mutableStateOf<Map<String, Instant>?>(null)
    val logger = Logger.withTag("SurveyAvailability")

    // Debug mode should be disabled by default to prevent interference with normal survey flow
    private var debugModeEnabled = mutableStateOf(Constants.debugModeEnabled)

    fun getAvailability(): Map<String, Instant>? {
        // In demo mode, return mock availability (surveys always available)
        if (DemoMode.isActive) {
            logger.d("Demo mode: surveys always available")
            val now = Clock.System.now()
            return mapOf(
                "daily" to now.minus(1.hours),
                "weekly" to now.minus(1.hours)
            )
        }

        if (availability.value == null) {
            runBlocking {
                refreshAvailability()
            }
        }

        return availability.value
    }

    suspend fun refreshAvailability() {
        // In demo mode, don't make API calls
        if (DemoMode.isActive) {
            logger.d("Demo mode: skipping availability refresh")
            return
        }

        try {
            availability.value = fetchAndParseAvailability()
        } catch (e: Exception) {
            logger.w("Couldn't fetch survey availability data: ${e.message}")
        }
    }

    fun secondsUntilAvailable(type: String): Long? {
        // If demo mode is enabled, always return that surveys are available
        if (DemoMode.isActive) {
            logger.d("Demo mode enabled - survey '$type' is always available")
            return -1L // Negative value indicates survey is available
        }

        // If debug mode is enabled, always return that surveys are available
        if (debugModeEnabled.value) {
            logger.d("Debug mode enabled - survey '$type' is always available")
            return -1L // Negative value indicates survey is available
        }

        val localAvailability = getAvailability()
        if (localAvailability != null && localAvailability.containsKey(type)) {
            val now = Clock.System.now()
            return now.until(localAvailability[type]!!, DateTimeUnit.SECOND)
        }
        return null
    }

    /**
     * Enable debug mode to bypass survey timers for testing purposes.
     * This allows taking surveys multiple times without waiting for the timer.
     * WARNING: Only use this for testing - it can interfere with normal survey submission flow.
     */
    fun enableDebugMode() {
        debugModeEnabled.value = true
        logger.w("Debug mode ENABLED - Survey timers bypassed for testing")
    }

    /**
     * Disable debug mode to restore normal survey timer behavior.
     */
    fun disableDebugMode() {
        debugModeEnabled.value = false
        logger.w("Debug mode DISABLED - Survey timers restored to normal behavior")
    }

    /**
     * Check if debug mode is currently enabled
     */
    fun isDebugModeEnabled(): Boolean {
        return debugModeEnabled.value
    }
}