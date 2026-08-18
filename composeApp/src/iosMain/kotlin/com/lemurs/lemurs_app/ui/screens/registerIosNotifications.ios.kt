package com.lemurs.lemurs_app.ui.screens

import com.lemurs.lemurs_app.util.IosNotificationSetup

/**
 * Re-registers iOS notifications once the app has fresh survey data.
 *
 * The launch-time call in iOSApp.swift runs before login, when the windows cannot be fetched, so
 * this is what actually establishes them on a first run.
 */
actual fun registerIosNotifications() {
    IosNotificationSetup.refreshFromServer()
}
