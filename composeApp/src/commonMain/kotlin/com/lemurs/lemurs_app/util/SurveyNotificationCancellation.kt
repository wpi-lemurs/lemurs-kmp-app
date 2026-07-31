package com.lemurs.lemurs_app.util

/**
 * Cancels any pending notification for a survey window the participant has just completed.
 *
 * Called after a successful submission so nobody is reminded to do something they have already
 * done. Safe to call when nothing is pending.
 *
 * Only iOS needs this. Android re-checks completion against the server at delivery time and
 * suppresses the notification itself, whereas iOS hands its notifications to the system in advance
 * and gets no say once they are registered.
 */
expect fun cancelNotificationsForCompletedWindow(windowName: String)
