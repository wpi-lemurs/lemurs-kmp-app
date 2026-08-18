package com.lemurs.lemurs_app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import co.touchlab.kermit.Logger

/**
 * Turns notification alarms into WorkManager jobs.
 *
 * The per-window actions that used to exist (morning, afternoon, late morning, late afternoon) are
 * collapsed into one initial and one last-chance action carrying [EXTRA_WINDOW_NAME], so a new
 * window in the database needs no new action, receiver entry, or worker.
 */
class AlarmReceiver : BroadcastReceiver() {
    private val logger = Logger.withTag("AlarmReceiver")

    override fun onReceive(context: Context, intent: Intent) {
        Log.w("AlarmReceiver", "onReceive: ${intent.action}")

        when (intent.action) {
            ACTION_DAILY_SETUP, ACTION_BACKUP_DAILY_SETUP ->
                enqueue(
                    context,
                    OneTimeWorkRequestBuilder<DailyNotificationSetupWorker>()
                        .setConstraints(notificationConstraints())
                        .build(),
                    "daily setup"
                )

            ACTION_INITIAL_NOTIFICATION -> {
                val windowName = intent.getStringExtra(EXTRA_WINDOW_NAME)
                if (windowName == null) {
                    Log.e("AlarmReceiver", "Initial notification alarm arrived with no window name")
                    return
                }
                enqueue(
                    context,
                    OneTimeWorkRequestBuilder<InitialSurveyNotificationWorker>()
                        .setInputData(InitialSurveyNotificationWorker.inputFor(windowName))
                        .setConstraints(notificationConstraints())
                        .build(),
                    "initial '$windowName' notification"
                )
            }

            ACTION_LAST_CHANCE_NOTIFICATION -> {
                val windowName = intent.getStringExtra(EXTRA_WINDOW_NAME)
                if (windowName == null) {
                    Log.e("AlarmReceiver", "Last-chance alarm arrived with no window name")
                    return
                }
                enqueue(
                    context,
                    OneTimeWorkRequestBuilder<LastChanceNotificationWorker>()
                        .setInputData(
                            workDataOf(InitialSurveyNotificationWorker.KEY_WINDOW_NAME to windowName)
                        )
                        .setConstraints(notificationConstraints())
                        .build(),
                    "last-chance '$windowName' notification"
                )
            }

            ACTION_NOTIFICATION_REMINDER ->
                enqueue(
                    context,
                    OneTimeWorkRequestBuilder<NotificationWorker>()
                        .setInputData(
                            workDataOf(
                                "title" to (intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"),
                                "body" to (intent.getStringExtra(EXTRA_BODY)
                                    ?: "Don't forget your survey!")
                            )
                        )
                        .setConstraints(notificationConstraints())
                        .build(),
                    "notification reminder"
                )

            else -> Log.w("AlarmReceiver", "Unknown action: ${intent.action}")
        }
    }

    /** Notification work must run even on a low battery and without a network. */
    private fun notificationConstraints(): Constraints = Constraints.Builder()
        .setRequiresBatteryNotLow(false)
        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
        .build()

    private fun enqueue(context: Context, request: OneTimeWorkRequest, description: String) {
        try {
            WorkManager.getInstance(context).enqueue(request)
            logger.w("Enqueued $description")
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Failed to enqueue $description: ${e.message}", e)
        }
    }

    companion object {
        const val ACTION_DAILY_SETUP = "com.lemurs.lemurs_app.DAILY_SETUP"
        const val ACTION_BACKUP_DAILY_SETUP = "com.lemurs.lemurs_app.BACKUP_DAILY_SETUP"
        const val ACTION_INITIAL_NOTIFICATION = "com.lemurs.lemurs_app.INITIAL_NOTIFICATION"
        const val ACTION_LAST_CHANCE_NOTIFICATION = "com.lemurs.lemurs_app.LAST_CHANCE_NOTIFICATION"
        const val ACTION_NOTIFICATION_REMINDER = "com.lemurs.lemurs_app.NOTIFICATION_REMINDER"
        const val EXTRA_WINDOW_NAME = "extra_window_name"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
    }
}
