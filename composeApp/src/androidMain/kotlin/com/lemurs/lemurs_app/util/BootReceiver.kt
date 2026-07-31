package com.lemurs.lemurs_app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import co.touchlab.kermit.Logger

/**
 * Restores notification alarms after events that invalidate them.
 *
 * A reboot clears alarms outright. A timezone or clock change does not, but it silently moves them:
 * alarms are set for an absolute instant, so a participant who relocates would keep being notified
 * at their old local time indefinitely. In that case today's schedule is discarded so the setup
 * worker re-plans against the new zone.
 */
class BootReceiver : BroadcastReceiver() {
    private val logger = Logger.withTag("BootReceiver")

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action !in HANDLED_ACTIONS) return

        logger.w("Handling $action, restoring notification alarms")

        try {
            val prefs =
                context.getSharedPreferences("lemurs_notification_prefs", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("notification_system_setup", false)) {
                logger.w("Notifications never set up, nothing to restore")
                return
            }

            if (action == Intent.ACTION_TIMEZONE_CHANGED || action == Intent.ACTION_TIME_CHANGED) {
                // Clear today's marker so the setup worker re-plans rather than
                // treating the day as already handled.
                context.getSharedPreferences("lemurs_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .remove("daily_setup_completed_date")
                    .apply()
                logger.w("Cleared today's schedule after a clock change")
            }

            NotificationScheduler().scheduleDailyNotificationSetup()
            logger.w("Notification alarms restored")
        } catch (e: Exception) {
            logger.e("Failed to restore notification alarms: ${e.message}", e)
        }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED
        )
    }
}
