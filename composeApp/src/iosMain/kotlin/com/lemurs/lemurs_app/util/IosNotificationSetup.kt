package com.lemurs.lemurs_app.util

import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.survey.SurveyWindows
import com.lemurs.lemurs_app.survey.fetchSurveyStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.systemTimeZone

/**
 * Registers iOS notifications against the survey windows the server reports.
 *
 * Called from Swift on launch and on every foreground. Replaces the previous hardcoded 08:00 and
 * 15:00 triggers, which had drifted from the database in the same way the Android thresholds had.
 *
 * Registrations are one-shot, so running this on foreground is what arms the next occurrence. It is
 * also where completion is honoured: a window already submitted today is skipped, which together
 * with [cancelForCompletedWindow] is what stops a participant being reminded about a survey they
 * have already done.
 *
 * The time within a window is a fixed offset rather than randomised per day, since iOS cannot run
 * code each morning to re-plan.
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

            // Clear everything first, then re-register only what is still needed.
            // Registrations are one-shot, so this is also what re-arms tomorrow.
            scheduler.clearWindowNotifications(usable.map { it.name })

            val nowLocalTime = SurveyWindows.nowLocalTime()

            for (window in usable) {
                val open = window.openTime.minuteOfDay()
                val close = window.closeTime.minuteOfDay()

                // Keep the whole reminder set inside the window where possible,
                // and never schedule past the close.
                val latestUseful = close - NotificationPlanner.FINAL_REMINDER_MINUTES.toInt()
                val target = (open + OFFSET_INTO_WINDOW_MINUTES).coerceAtMost(
                    latestUseful.coerceAtLeast(open)
                )

                // A window already submitted today needs no notification, but only
                // if the slot is still ahead of us. Once it has passed, the next
                // occurrence is tomorrow, which the participant has not done yet.
                val slotStillToday = target > nowLocalTime.minuteOfDay()
                if (slotStillToday && window.name in status.completedWindows) {
                    logger.w("Skipping '${window.name}': already submitted today")
                    continue
                }

                scheduler.scheduleInitialNotification(
                    window.name,
                    LocalTime.fromMinuteOfDay(target),
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

    /**
     * Cancels the pending notifications for a window the participant has just submitted.
     *
     * iOS fires its notifications itself, with the app not running, so there is no opportunity to
     * check completion at delivery time the way Android does. Cancelling at submission is what
     * prevents a reminder for a survey that is already done.
     *
     * The next occurrence is re-registered by [refreshFromServer] on the next foreground.
     */
    fun cancelForCompletedWindow(windowName: String) {
        NotificationScheduler().cancelNotificationsFor(windowName)
    }
}
