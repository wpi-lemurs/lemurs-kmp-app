package com.lemurs.lemurs_app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import co.touchlab.kermit.Logger

class AlarmReceiver : BroadcastReceiver() {
    private val logger = Logger.withTag("AlarmReceiver")

    override fun onReceive(context: Context, intent: Intent) {
        // Use Android Log as primary logging to ensure it works even if Kermit isn't initialized
        Log.w("AlarmReceiver", "onReceive called with action: ${intent.action}")
        
        // REMOVED TEST NOTIFICATION - it was sending notification for every alarm
        
        try {
            // Try Kermit logger as well
            logger.w("AlarmReceiver: Received alarm intent with action: ${intent.action}")
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Kermit logger failed: ${e.message}")
        }
        
        when (intent.action) {
            ACTION_DAILY_SETUP -> {
                Log.w("AlarmReceiver", "ACTION_DAILY_SETUP received, triggering daily setup")
                logger.w("AlarmReceiver: Triggering daily notification setup")
                triggerDailySetup(context)
            }
            ACTION_BACKUP_DAILY_SETUP -> {
                Log.w("AlarmReceiver", "ACTION_BACKUP_DAILY_SETUP received, triggering backup daily setup")
                logger.w("AlarmReceiver: Triggering backup daily notification setup")
                triggerDailySetup(context)
            }
            ACTION_MORNING_NOTIFICATION -> {
                Log.w("AlarmReceiver", "ACTION_MORNING_NOTIFICATION received, triggering morning notification")
                logger.w("AlarmReceiver: Triggering morning notification")
                triggerMorningNotification(context)
            }
            ACTION_AFTERNOON_NOTIFICATION -> {
                Log.w("AlarmReceiver", "ACTION_AFTERNOON_NOTIFICATION received, triggering afternoon notification")
                logger.w("AlarmReceiver: Triggering afternoon notification")
                triggerAfternoonNotification(context)
            }
            ACTION_NOTIFICATION_REMINDER -> {
                Log.w("AlarmReceiver", "ACTION_NOTIFICATION_REMINDER received, triggering reminder")
                logger.w("AlarmReceiver: Triggering notification reminder")
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"
                val body = intent.getStringExtra(EXTRA_BODY) ?: "Don't forget your survey!"
                triggerNotificationReminder(context, title, body)
            }
            ACTION_LATE_MORNING_NOTIFICATION -> {
                Log.w("AlarmReceiver", "ACTION_LATE_MORNING_NOTIFICATION received, triggering late morning notification")
                logger.w("AlarmReceiver: Triggering late morning notification")
                triggerLateMorningNotification(context)
            }
            ACTION_LATE_AFTERNOON_NOTIFICATION -> {
                Log.w("AlarmReceiver", "ACTION_LATE_AFTERNOON_NOTIFICATION received, triggering late afternoon notification")
                logger.w("AlarmReceiver: Triggering late afternoon notification")
                triggerLateAfternoonNotification(context)
            }
            else -> {
                Log.w("AlarmReceiver", "Unknown action received: ${intent.action}")
            }
        }
    }

    private fun triggerDailySetup(context: Context) {
        Log.w("AlarmReceiver", "triggerDailySetup: Starting WorkManager enqueue")
        
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<DailyNotificationSetupWorker>()
            .setConstraints(constraints)
            .build()

        try {
            WorkManager.getInstance(context).enqueue(workRequest)
            Log.w("AlarmReceiver", "triggerDailySetup: WorkManager enqueue successful")
            logger.w("AlarmReceiver: Daily setup work request enqueued")
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "triggerDailySetup: WorkManager enqueue failed: ${e.message}", e)
        }
    }

    private fun triggerMorningNotification(context: Context) {
        Log.w("AlarmReceiver", "triggerMorningNotification: Starting WorkManager enqueue")
        
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<InitialMorningNotificationWorker>()
            .setConstraints(constraints)
            .build()

        try {
            WorkManager.getInstance(context).enqueue(workRequest)
            Log.w("AlarmReceiver", "triggerMorningNotification: WorkManager enqueue successful")
            logger.w("AlarmReceiver: Morning notification work request enqueued")
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "triggerMorningNotification: WorkManager enqueue failed: ${e.message}", e)
        }
    }

    private fun triggerAfternoonNotification(context: Context) {
        Log.w("AlarmReceiver", "triggerAfternoonNotification: Starting WorkManager enqueue")
        
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<InitialAfternoonNotificationWorker>()
            .setConstraints(constraints)
            .build()

        try {
            WorkManager.getInstance(context).enqueue(workRequest)
            Log.w("AlarmReceiver", "triggerAfternoonNotification: WorkManager enqueue successful")
            logger.w("AlarmReceiver: Afternoon notification work request enqueued")
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "triggerAfternoonNotification: WorkManager enqueue failed: ${e.message}", e)
        }
    }

    private fun triggerNotificationReminder(context: Context, title: String, body: String) {
        Log.w("AlarmReceiver", "triggerNotificationReminder: Starting WorkManager enqueue for reminder")
        
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInputData(workDataOf(
                "title" to title,
                "body" to body
            ))
            .setConstraints(constraints)
            .build()

        try {
            WorkManager.getInstance(context).enqueue(workRequest)
            Log.w("AlarmReceiver", "triggerNotificationReminder: WorkManager enqueue successful")
            logger.w("AlarmReceiver: Notification reminder work request enqueued")
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "triggerNotificationReminder: WorkManager enqueue failed: ${e.message}", e)
        }
    }

    private fun triggerLateMorningNotification(context: Context) {
        Log.w("AlarmReceiver", "triggerLateMorningNotification: Starting WorkManager enqueue for late morning notification")
        
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<LateMorningNotificationWorker>()
            .setConstraints(constraints)
            .build()

        try {
            WorkManager.getInstance(context).enqueue(workRequest)
            Log.w("AlarmReceiver", "triggerLateMorningNotification: WorkManager enqueue successful")
            logger.w("AlarmReceiver: Late morning notification work request enqueued")
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "triggerLateMorningNotification: WorkManager enqueue failed: ${e.message}", e)
        }
    }

    private fun triggerLateAfternoonNotification(context: Context) {
        Log.w("AlarmReceiver", "triggerLateAfternoonNotification: Starting WorkManager enqueue for late afternoon notification")
        
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<LateAfternoonNotificationWorker>()
            .setConstraints(constraints)
            .build()

        try {
            WorkManager.getInstance(context).enqueue(workRequest)
            Log.w("AlarmReceiver", "triggerLateAfternoonNotification: WorkManager enqueue successful")
            logger.w("AlarmReceiver: Late afternoon notification work request enqueued")
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "triggerLateAfternoonNotification: WorkManager enqueue failed: ${e.message}", e)
        }
    }

    companion object {
        const val ACTION_DAILY_SETUP = "com.lemurs.lemurs_app.DAILY_SETUP"
        const val ACTION_BACKUP_DAILY_SETUP = "com.lemurs.lemurs_app.BACKUP_DAILY_SETUP"
        const val ACTION_MORNING_NOTIFICATION = "com.lemurs.lemurs_app.MORNING_NOTIFICATION"
        const val ACTION_AFTERNOON_NOTIFICATION = "com.lemurs.lemurs_app.AFTERNOON_NOTIFICATION"
        const val ACTION_NOTIFICATION_REMINDER = "com.lemurs.lemurs_app.NOTIFICATION_REMINDER"
        const val ACTION_LATE_MORNING_NOTIFICATION = "com.lemurs.lemurs_app.LATE_MORNING_NOTIFICATION"
        const val ACTION_LATE_AFTERNOON_NOTIFICATION = "com.lemurs.lemurs_app.LATE_AFTERNOON_NOTIFICATION"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
    }
}