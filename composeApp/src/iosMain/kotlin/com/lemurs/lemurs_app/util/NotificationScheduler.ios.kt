package com.lemurs.lemurs_app.util

actual class NotificationScheduler actual constructor(val time: String) {
    actual fun scheduleNotify() {}
    actual fun scheduleReminder(delay: Long) {}
    actual fun scheduleFinalReminder(delay: Long, timeLeft: String) {}
    actual fun scheduleRandomReminders() {}
    actual fun scheduleNotifications(delay: Long, time: String, delayOne: Long, delayTwo: Long, timeLeft: Int) {}
    actual fun scheduleInitialMorningNotificationAtTime(hour: Int, minute: Int, forceToday: Boolean) {}
    actual fun scheduleInitialAfternoonNotificationAtTime(hour: Int, minute: Int, forceToday: Boolean) {}
    actual fun scheduleDailyNotificationSetup() {}
    actual fun scheduleReminderWithAlarm(delayMinutes: Long, title: String, body: String, requestCode: Int) {}
    actual fun scheduleLateMorningNotificationAtTime(hour: Int, minute: Int) {}
    actual fun scheduleLateAfternoonNotificationAtTime(hour: Int, minute: Int) {}
    actual fun rescheduleDailySetupsForTomorrow() {}
}