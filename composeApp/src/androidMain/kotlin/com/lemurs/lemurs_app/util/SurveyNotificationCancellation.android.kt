package com.lemurs.lemurs_app.util

/**
 * Nothing to do on Android.
 *
 * The notification workers call [NotificationUtil.checkSurveyCompleted] before sending, which asks
 * the server whether the open window has already been submitted. A completed survey therefore
 * suppresses its own reminders without anything needing to be cancelled in advance.
 */
actual fun cancelNotificationsForCompletedWindow(windowName: String) {
}
