package com.lemurs.lemurs_app.util

import co.touchlab.kermit.Logger
import kotlinx.datetime.LocalTime
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.timeIntervalSinceNow
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

/**
 * iOS notification scheduling.
 *
 * iOS has no AlarmManager equivalent and no way to wake the app before dawn to re-plan the day, so
 * the approach differs from Android by necessity: notifications are registered once as *repeating
 * calendar triggers*, and iOS fires them itself.
 *
 * This turns out to suit the problem well. A [UNCalendarNotificationTrigger] built from hour and
 * minute components fires at that wall-clock time in whatever timezone the phone is currently in,
 * and re-anchors automatically when the participant relocates. Android needs an explicit
 * TIMEZONE_CHANGED receiver to get the same behaviour.
 *
 * The tradeoff is that the time within a window cannot be randomised per day, since that would need
 * code to run each morning. Notifications land at a fixed offset into each window instead.
 */
actual class NotificationScheduler actual constructor() {
    private val logger = Logger.withTag("NotificationScheduler")

    /**
     * On iOS the day is not planned from an alarm; [scheduleWindowNotifications] registers repeating
     * triggers instead. Kept as a no-op so shared code can call it unconditionally.
     */
    actual fun scheduleDailyNotificationSetup() {}

    /** Not needed: repeating triggers re-arm themselves. */
    actual fun rescheduleDailySetupsForTomorrow() {}

    /**
     * Registers a repeating daily notification for [windowName] at [atLocalTime].
     *
     * [forceToday] is ignored: a repeating calendar trigger has no notion of a single day, and iOS
     * will fire the next matching wall-clock time on its own.
     */
    actual fun scheduleInitialNotification(
        windowName: String,
        atLocalTime: LocalTime,
        forceToday: Boolean
    ) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        val identifier = initialIdentifier(windowName)
        center.removePendingNotificationRequestsWithIdentifiers(listOf(identifier))

        val content = UNMutableNotificationContent().apply {
            setTitle("Please take your $windowName survey now.")
            setBody(REWARD_BODY)
            setSound(UNNotificationSound.defaultSound())
        }

        val components = NSDateComponents().apply {
            hour = atLocalTime.hour.toLong()
            minute = atLocalTime.minute.toLong()
        }

        center.addNotificationRequest(
            UNNotificationRequest.requestWithIdentifier(
                identifier = identifier,
                content = content,
                // repeats = true: fires daily at this wall-clock time, following
                // the phone's current timezone.
                trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                    components, true
                )
            )
        ) { error ->
            if (error != null) {
                logger.e("Failed to schedule '$windowName': ${error.localizedDescription}")
            } else {
                logger.w("Scheduled '$windowName' notification for $atLocalTime daily")
            }
        }
    }

    /** Sends a single last call shortly from now, with no repeat. */
    actual fun scheduleLastChanceNotification(windowName: String) {
        val content = UNMutableNotificationContent().apply {
            setTitle("Last chance: your $windowName survey closes soon!")
            setBody("Complete it now to earn \$3.")
            setSound(UNNotificationSound.defaultSound())
        }

        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(
            UNNotificationRequest.requestWithIdentifier(
                identifier = "lastChance-$windowName",
                content = content,
                trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(10.0, false)
            )
        ) { error ->
            if (error != null) {
                logger.e("Failed to schedule '$windowName' last chance: ${error.localizedDescription}")
            }
        }
    }

    actual fun scheduleReminder(
        windowName: String,
        delayMinutes: Long,
        title: String,
        body: String,
        isFinal: Boolean
    ) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(UNNotificationSound.defaultSound())
        }

        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(
            UNNotificationRequest.requestWithIdentifier(
                identifier = reminderIdentifier(windowName, isFinal),
                content = content,
                trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                    (delayMinutes * 60).toDouble(), false
                )
            )
        ) { error ->
            if (error != null) {
                logger.e("Failed to schedule '$windowName' reminder: ${error.localizedDescription}")
            }
        }
    }

    /**
     * Schedules the weekly survey notification.
     *
     * Unlike the daily windows this is a one-off at an absolute instant, so it is re-registered each
     * time the app learns a new next-available date.
     */
    actual fun scheduleWeeklySurveyNotification() {
        // The instant comes from the server and is applied via
        // scheduleWeeklySurveyNotificationAt; nothing to do without it.
        logger.w("Weekly notification is scheduled from the fetched next-available date")
    }

    /** Registers the weekly notification for an absolute [date]. */
    fun scheduleWeeklySurveyNotificationAt(date: NSDate) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.removePendingNotificationRequestsWithIdentifiers(listOf(WEEKLY_IDENTIFIER))

        if (date.timeIntervalSinceNow <= 0) {
            logger.w("Weekly survey date is in the past, not scheduling")
            return
        }

        val content = UNMutableNotificationContent().apply {
            setTitle("Time for Your Weekly Survey")
            setBody("Don't forget your weekly survey! Earn \$10 for completing it today.")
            setSound(UNNotificationSound.defaultSound())
        }

        val components = NSCalendar.currentCalendar.components(
            NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or
                NSCalendarUnitHour or NSCalendarUnitMinute,
            fromDate = date
        )

        center.addNotificationRequest(
            UNNotificationRequest.requestWithIdentifier(
                identifier = WEEKLY_IDENTIFIER,
                content = content,
                trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                    components, false
                )
            )
        ) { error ->
            if (error != null) {
                logger.e("Failed to schedule weekly notification: ${error.localizedDescription}")
            } else {
                logger.w("Scheduled weekly survey notification")
            }
        }
    }

    /**
     * Removes the notifications registered by the previous hardcoded schedule.
     *
     * Safe to call when none exist, and worth calling on every launch: they repeat daily, so an
     * upgrading participant would otherwise keep receiving 08:00 and 15:00 alerts alongside the
     * window-derived ones.
     */
    fun clearLegacyNotifications() {
        UNUserNotificationCenter.currentNotificationCenter()
            .removePendingNotificationRequestsWithIdentifiers(LEGACY_IDENTIFIERS)
    }

    /** Clears every pending survey notification for [windowNames], before re-registering them. */
    fun clearWindowNotifications(windowNames: List<String>) {
        val identifiers = windowNames.flatMap {
            listOf(
                initialIdentifier(it),
                reminderIdentifier(it, isFinal = false),
                reminderIdentifier(it, isFinal = true),
                "lastChance-$it"
            )
        } + LEGACY_IDENTIFIERS

        UNUserNotificationCenter.currentNotificationCenter()
            .removePendingNotificationRequestsWithIdentifiers(identifiers)
    }

    private companion object {
        const val WEEKLY_IDENTIFIER = "weeklySurvey"
        const val REWARD_BODY = "Remember you can earn \$3 for completing this survey."

        /**
         * Identifiers used by the previous hardcoded 08:00/15:00 schedule.
         *
         * These are repeating triggers, so an upgrading participant keeps receiving them forever
         * unless they are explicitly removed — they would fire alongside the new window-derived
         * ones rather than being replaced by them.
         */
        val LEGACY_IDENTIFIERS = listOf("morningSurvey", "afternoonSurvey")

        fun initialIdentifier(windowName: String) = "initial-$windowName"
        fun reminderIdentifier(windowName: String, isFinal: Boolean) =
            if (isFinal) "finalReminder-$windowName" else "firstReminder-$windowName"
    }
}
