package com.lemurs.lemurs_app.util

import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.survey.SurveyWindows
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
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
 * notifications are registered ahead of time and iOS fires them itself, with the app not running.
 *
 * Each registration is a *one-shot* calendar trigger for a specific date. A repeating trigger would
 * be tidier, but it cannot be cancelled for a single day: removing it because the participant has
 * already submitted would silence that window permanently. One-shot registrations are cancellable,
 * at the cost of needing [IosNotificationSetup.refreshFromServer] to arm the next occurrence
 * whenever the app is foregrounded.
 *
 * Because iOS delivers these without consulting the app, completion is handled by cancelling at
 * submission rather than by checking at delivery time the way Android does.
 *
 * The time within a window is a fixed offset rather than randomised per day, since randomising
 * would need code to run each morning.
 */
actual class NotificationScheduler actual constructor() {
    private val logger = Logger.withTag("NotificationScheduler")

    /**
     * On iOS the day is not planned from an alarm; [IosNotificationSetup] registers notifications
     * directly. Kept as a no-op so shared code can call it unconditionally.
     */
    actual fun scheduleDailyNotificationSetup() {}

    /** Not needed: the next occurrence is armed whenever the app is foregrounded. */
    actual fun rescheduleDailySetupsForTomorrow() {}

    /**
     * Registers a one-shot notification for [windowName] at the next occurrence of [atLocalTime].
     *
     * Deliberately not a repeating trigger. A repeating one cannot be cancelled for a single day:
     * removing it because the participant already submitted would also remove every future day's
     * notification, and they would never be reminded about that window again. A one-shot fired for
     * a specific date can be cancelled freely, because [IosNotificationSetup] re-registers the next
     * occurrence each time the app comes to the foreground.
     *
     * [forceToday] is ignored; the next matching wall-clock time is always used.
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

        val components = nextOccurrenceComponents(atLocalTime)

        center.addNotificationRequest(
            UNNotificationRequest.requestWithIdentifier(
                identifier = identifier,
                content = content,
                trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                    components, false
                )
            )
        ) { error ->
            if (error != null) {
                logger.e("Failed to schedule '$windowName': ${error.localizedDescription}")
            } else {
                logger.w("Scheduled '$windowName' notification for the next $atLocalTime")
            }
        }
    }

    /**
     * Date components for the next time the clock reads [atLocalTime], today or tomorrow.
     *
     * Year, month and day are included so the trigger fires once rather than daily. Built from the
     * calendar rather than by adding 24 hours, so a daylight saving transition still lands on the
     * right wall-clock time.
     */
    private fun nextOccurrenceComponents(atLocalTime: LocalTime): NSDateComponents {
        val zone = SurveyWindows.systemZone()
        val today = SurveyWindows.localDate(zone = zone)
        val nowLocal = SurveyWindows.nowLocalTime(zone = zone)

        // If the slot has already passed today, aim at tomorrow's. Computed on the
        // local date rather than by adding 24 hours, so a daylight saving change
        // still lands on the intended wall-clock time.
        val targetDate =
            if (atLocalTime > nowLocal) today else today.plus(1, DateTimeUnit.DAY)

        return NSDateComponents().apply {
            year = targetDate.year.toLong()
            month = targetDate.monthNumber.toLong()
            day = targetDate.dayOfMonth.toLong()
            hour = atLocalTime.hour.toLong()
            minute = atLocalTime.minute.toLong()
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

    /**
     * Cancels every pending notification for one window.
     *
     * Called when the participant submits that window's survey, so they are not reminded to do
     * something they have already done. Safe when nothing is pending.
     *
     * Only affects the current occurrence: [IosNotificationSetup] re-registers the next one when
     * the app is next foregrounded, so cancelling today does not silence the window permanently.
     */
    fun cancelNotificationsFor(windowName: String) {
        UNUserNotificationCenter.currentNotificationCenter()
            .removePendingNotificationRequestsWithIdentifiers(identifiersFor(windowName))
        logger.w("Cancelled pending '$windowName' notifications after submission")
    }

    /** Clears every pending survey notification for [windowNames], before re-registering them. */
    fun clearWindowNotifications(windowNames: List<String>) {
        UNUserNotificationCenter.currentNotificationCenter()
            .removePendingNotificationRequestsWithIdentifiers(
                windowNames.flatMap(::identifiersFor) + LEGACY_IDENTIFIERS
            )
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

        /** Every identifier one window can have pending. */
        fun identifiersFor(windowName: String) = listOf(
            initialIdentifier(windowName),
            reminderIdentifier(windowName, isFinal = false),
            reminderIdentifier(windowName, isFinal = true),
            "lastChance-$windowName"
        )
    }
}
