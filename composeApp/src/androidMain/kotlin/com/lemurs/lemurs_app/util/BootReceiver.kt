package com.lemurs.lemurs_app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import co.touchlab.kermit.Logger

class BootReceiver : BroadcastReceiver() {
    private val logger = Logger.withTag("BootReceiver")

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            logger.w("BootReceiver: Device boot completed, restoring notification alarms")
            
            try {
                // Check if notifications have been set up before (to avoid setting up during first boot)
                val prefs = context.getSharedPreferences("lemurs_notification_prefs", Context.MODE_PRIVATE)
                val isSetup = prefs.getBoolean("notification_system_setup", false)
                
                if (isSetup) {
                    logger.w("BootReceiver: Notification system was previously setup, restoring alarms")
                    
                    // Restore the persistent daily scheduling alarms
                    val notificationScheduler = NotificationScheduler("")
                    notificationScheduler.scheduleDailyNotificationSetup()
                    
                    logger.w("BootReceiver: Notification alarms restored successfully")
                } else {
                    logger.w("BootReceiver: Notification system not previously setup, skipping restoration")
                }
            } catch (e: Exception) {
                logger.e("BootReceiver: Failed to restore notification alarms: ${e.message}", e)
            }
        }
    }
}