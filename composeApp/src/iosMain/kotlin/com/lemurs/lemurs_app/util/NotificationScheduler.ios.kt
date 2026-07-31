package com.lemurs.lemurs_app.util

import kotlinx.datetime.LocalTime

/**
 * iOS notification scheduling is not implemented yet.
 *
 * These are deliberate no-ops rather than a partial implementation: iOS needs UNUserNotificationCenter
 * with locally scheduled triggers, which has no equivalent to Android's AlarmManager and is best done
 * as its own piece of work. Until then the iOS app shows survey windows correctly but sends no
 * reminders.
 */
actual class NotificationScheduler actual constructor() {
    actual fun scheduleDailyNotificationSetup() {}
    actual fun rescheduleDailySetupsForTomorrow() {}
    actual fun scheduleInitialNotification(
        windowName: String,
        atLocalTime: LocalTime,
        forceToday: Boolean
    ) {}

    actual fun scheduleLastChanceNotification(windowName: String) {}
    actual fun scheduleReminder(
        windowName: String,
        delayMinutes: Long,
        title: String,
        body: String,
        isFinal: Boolean
    ) {}

    actual fun scheduleWeeklySurveyNotification() {}
}
