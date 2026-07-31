package com.lemurs.lemurs_app.util

import com.lemurs.lemurs_app.data.local.UseCaseResult
import platform.Foundation.*
import platform.UserNotifications.*

actual class NotificationUtil {
    /**
     * Whether the open window has already been submitted.
     *
     * Always false on iOS: notifications here are pre-registered repeating triggers that iOS fires
     * itself, so there is no point at which the app can run this check and suppress one. Returning
     * false keeps the meaning honest ("not known to be complete") rather than claiming completion
     * and, on any future caller, silently withholding a reminder.
     */
    actual fun checkSurveyCompleted(): Boolean = false

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
}
