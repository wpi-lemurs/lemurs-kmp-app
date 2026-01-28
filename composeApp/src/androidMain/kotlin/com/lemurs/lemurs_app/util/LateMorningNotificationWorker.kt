package com.lemurs.lemurs_app.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import co.touchlab.kermit.Logger

class LateMorningNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val logger = Logger.withTag("LateMorningNotificationWorker")
    private val notificationUtil = NotificationUtil()
    
    override suspend fun doWork(): Result {
        Log.w("LateMorningNotificationWorker", "Starting late morning notification worker")
        logger.w("LateMorningNotificationWorker: Starting late morning notification")
        
        return try {
            // Send the "last chance" morning notification
            val title = "Don't forget your morning survey!"
            val body = "You still have time to complete your morning survey before the afternoon."
            
            Log.w("LateMorningNotificationWorker", "Sending late morning notification")
            notificationUtil.sendNotificationWithoutCheck(title, body)
            
            Log.w("LateMorningNotificationWorker", "Late morning notification sent successfully")
            logger.w("LateMorningNotificationWorker: Late morning notification completed")
            Result.success()
        } catch (e: Exception) {
            Log.e("LateMorningNotificationWorker", "Failed to send late morning notification: ${e.message}", e)
            logger.e("LateMorningNotificationWorker: Failed to send notification", e)
            Result.failure()
        }
    }
}