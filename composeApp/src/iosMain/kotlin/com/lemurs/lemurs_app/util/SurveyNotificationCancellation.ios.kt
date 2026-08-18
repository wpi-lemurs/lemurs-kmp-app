package com.lemurs.lemurs_app.util

actual fun cancelNotificationsForCompletedWindow(windowName: String) {
    IosNotificationSetup.cancelForCompletedWindow(windowName)
}
