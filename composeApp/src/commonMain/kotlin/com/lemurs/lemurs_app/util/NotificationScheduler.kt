package com.lemurs.lemurs_app.util

expect class NotificationScheduler(time: String) {
    fun scheduleNotify()
    fun scheduleReminder(delay: Long)
    fun scheduleFinalReminder(delay: Long, timeLeft: String)
    fun scheduleRandomReminders()
    fun scheduleNotifications(delay: Long, time: String, delayOne: Long, delayTwo: Long, timeLeft: Int)
    fun scheduleInitialMorningNotificationAtTime(hour: Int, minute: Int, forceToday: Boolean = false)
    fun scheduleInitialAfternoonNotificationAtTime(hour: Int, minute: Int, forceToday: Boolean = false)
    fun scheduleDailyNotificationSetup()
    fun scheduleReminderWithAlarm(delayMinutes: Long, title: String, body: String, requestCode: Int)
    fun scheduleLateMorningNotificationAtTime(hour: Int, minute: Int)
    fun scheduleLateAfternoonNotificationAtTime(hour: Int, minute: Int)
    fun rescheduleDailySetupsForTomorrow()
}