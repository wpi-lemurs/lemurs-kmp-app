package com.lemurs.lemurs_app.util

import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.survey.SurveyWindows
import com.lemurs.lemurs_app.survey.fetchSurveyStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.systemTimeZone

/**
 * Registers iOS notifications against the survey windows the server reports.
 *
 * Called from Swift on launch. Replaces the previous hardcoded 08:00 and 15:00 triggers, which had
 * drifted from the database in the same way the Android thresholds had.
 *
 * Because iOS cannot run code each morning to re-plan, each window's notification is registered as a
 * repeating trigger at a fixed offset into the window rather than at a random time. It still tracks
 * the window: change the window in the database and the notification moves with it on next launch.
 */
object IosNotificationSetup {
    private val logger = Logger.withTag("IosNotificationSetup")

    /** How far into a window its notification fires, when the window is long enough. */
    private const val OFFSET_INTO_WINDOW_MINUTES = 30

    fun refreshFromServer() {
        CoroutineScope(Dispatchers.Main).launch {
            val scheduler = NotificationScheduler()

            // Drop the previous hardcoded 08:00/15:00 triggers first, and do it
            // whether or not the fetch succeeds. They repeat daily, so leaving
            // them in place would mean an upgrading participant is notified on
            // the old fixed schedule indefinitely.
            scheduler.clearLegacyNotifications()

            val status = fetchSurveyStatus()
            if (status == null) {
                logger.w("Couldn't fetch survey windows; will register on the next launch")
                return@launch
            }
            val usable = status.windows.filter { it.isWellFormed }
            if (usable.isEmpty()) {
                logger.w("No usable windows returned")
                return@launch
            }

            scheduler.clearWindowNotifications(usable.map { it.name })

            for (window in usable) {
                val open = window.openTime.minuteOfDay()
                val close = window.closeTime.minuteOfDay()

                // Keep the whole reminder set inside the window where possible,
                // and never schedule past the close.
                val latestUseful = close - NotificationPlanner.FINAL_REMINDER_MINUTES.toInt()
                val target = (open + OFFSET_INTO_WINDOW_MINUTES).coerceAtMost(
                    latestUseful.coerceAtLeast(open)
                )

                scheduler.scheduleInitialNotification(
                    window.name,
                    kotlinx.datetime.LocalTime.fromMinuteOfDay(target),
                    forceToday = false
                )
            }

            status.weeklyNextAvailable?.let { instant ->
                val date = NSDate.dateWithTimeIntervalSince1970(
                    instant.toEpochMilliseconds() / 1000.0
                )
                scheduler.scheduleWeeklySurveyNotificationAt(date)
            }

            logger.w("Registered notifications for ${usable.size} window(s) in ${SurveyWindows.systemZone().id}")
        }
    }
}
