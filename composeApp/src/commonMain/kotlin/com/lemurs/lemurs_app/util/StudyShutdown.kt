package com.lemurs.lemurs_app.util

import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.health.HealthDataScheduler

/**
 * Stops everything the app does on its own, permanently.
 *
 * Called when the server reports the study concluded. Deliberately reachable from the background
 * setup as well as the UI: a participant who simply stops opening the app must still go quiet, and
 * a teardown that only runs on the home screen would leave them collecting indefinitely.
 *
 * Safe to call more than once — cancelling work or an alarm that is already gone is a no-op — which
 * matters because both callers can fire on the same day.
 */
object StudyShutdown {

    private val logger = Logger.withTag("StudyShutdown")

    fun run(windowNames: List<String>) {
        logger.w("Study concluded; stopping background collection and notifications")
        HealthDataScheduler().cancelAll()
        NotificationScheduler().cancelAll(windowNames)
    }
}
