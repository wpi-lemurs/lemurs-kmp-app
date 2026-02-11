package com.lemurs.lemurs_app.util

import com.lemurs.lemurs_app.data.local.UseCaseResult
import platform.Foundation.*
import platform.UserNotifications.*

actual class NotificationUtil {
    actual fun checkSurveyCompleted(): Boolean {
        return true
    }

    actual fun sendNotificationText(
        title: String,
        body: String
    ): UseCaseResult<Any> {
        val content = UNMutableNotificationContent().also {
            it.setTitle(title)
            it.setBody(body)
            it.setSound(UNNotificationSound.defaultSound())
        }
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(1.0, false)
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = NSUUID().UUIDString,
            content = content,
            trigger = trigger
        )
        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request, null)
        return UseCaseResult.Success(Unit)
    }

    actual fun sendNotificationWithoutCheck(
        title: String,
        body: String
    ): UseCaseResult<Any> {
        return sendNotificationText(title, body)
    }

    fun requestNotificationPermission() {
        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
            completionHandler = { _, _ -> }
        )
    }

    fun scheduleDailySurveyNotifications() {
        val center = UNUserNotificationCenter.currentNotificationCenter()

        // Remove existing daily survey notifications to avoid duplicates
        center.removePendingNotificationRequestsWithIdentifiers(listOf("morningSurvey", "afternoonSurvey"))

        // Morning
        val morningContent = UNMutableNotificationContent()
        morningContent.setTitle("Morning Survey")
        morningContent.setBody("The morning survey is now open! Please open the app to complete it.")
        morningContent.setSound(UNNotificationSound.defaultSound())
        val morningDate = NSDateComponents().apply {
            hour = 8
            minute = 0
        }
        val morningTrigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            morningDate, true
        )
        val morningRequest = UNNotificationRequest.requestWithIdentifier(
            identifier = "morningSurvey",
            content = morningContent,
            trigger = morningTrigger
        )
        center.addNotificationRequest(morningRequest) { error ->
            if (error != null) {
                println("❌ Failed to schedule morning notification: ${error.localizedDescription}")
            } else {
                println("✅ Scheduled morning survey notification for 08:00")
            }
        }
        // Afternoon
        val afternoonContent = UNMutableNotificationContent()
        afternoonContent.setTitle("Afternoon Survey")
        afternoonContent.setBody("The afternoon survey is now open! Please open the app to complete it.")
        afternoonContent.setSound(UNNotificationSound.defaultSound())
        val afternoonDate = NSDateComponents().apply {
            hour = 15
            minute = 0
        }
        val afternoonTrigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            afternoonDate, true
        )
        val afternoonRequest = UNNotificationRequest.requestWithIdentifier(
            identifier = "afternoonSurvey",
            content = afternoonContent,
            trigger = afternoonTrigger
        )
        center.addNotificationRequest(afternoonRequest) { error ->
            if (error != null) {
                println("❌ Failed to schedule afternoon notification: ${error.localizedDescription}")
            } else {
                println("✅ Scheduled afternoon survey notification for 15:00")
            }
        }
    }

    fun scheduleWeeklySurveyNotification(nextWeeklySurvey: String) {
        val center = UNUserNotificationCenter.currentNotificationCenter()

        // Remove existing weekly survey notification to avoid duplicates
        center.removePendingNotificationRequestsWithIdentifiers(listOf("weeklySurvey"))
        
        val weeklyContent = UNMutableNotificationContent()
        weeklyContent.setTitle("Weekly Survey")
        weeklyContent.setBody("The weekly survey is now open! Please open the app to complete it.")
        weeklyContent.setSound(UNNotificationSound.defaultSound())
        val formatter = NSDateFormatter().apply {
            dateFormat = "yyyy-MM-dd HH:mm:ss.SSS"
            timeZone = NSTimeZone.systemTimeZone
        }
        val date = formatter.dateFromString(nextWeeklySurvey)
        val calendar = NSCalendar.currentCalendar
        val components = calendar.components(
            NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or NSCalendarUnitHour or NSCalendarUnitMinute,
            fromDate = date!!
        )
        val weeklyTrigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            components, false
        )
        val weeklyRequest = UNNotificationRequest.requestWithIdentifier(
            identifier = "weeklySurvey",
            content = weeklyContent,
            trigger = weeklyTrigger
        )
        center.addNotificationRequest(weeklyRequest) { error ->
            if (error != null) {
                println("❌ Failed to schedule weekly notification: ${error.localizedDescription}")
            } else {
                println("✅ Scheduled weekly survey notification for $nextWeeklySurvey")
            }
        }
    }
}
