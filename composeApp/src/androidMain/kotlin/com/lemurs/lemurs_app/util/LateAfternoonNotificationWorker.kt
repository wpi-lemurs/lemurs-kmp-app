package com.lemurs.lemurs_app.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import co.touchlab.kermit.Logger

class LateAfternoonNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val logger = Logger.withTag("LateAfternoonNotificationWorker")
    private val notificationUtil = NotificationUtil()
    
    override suspend fun doWork(): Result {
        Log.w("LateAfternoonNotificationWorker", "Starting late afternoon notification worker")
        logger.w("LateAfternoonNotificationWorker: Starting late afternoon notification")
        
        return try {
            // Send the "last chance" afternoon notification
            val title = "Don't forget your afternoon survey!"
            val body = "You still have time to complete your afternoon survey before the day ends."
            
            Log.w("LateAfternoonNotificationWorker", "Sending late afternoon notification")
            notificationUtil.sendNotificationWithoutCheck(title, body)
            
            Log.w("LateAfternoonNotificationWorker", "Late afternoon notification sent successfully")
            logger.w("LateAfternoonNotificationWorker: Late afternoon notification completed")
            Result.success()
        } catch (e: Exception) {
            Log.e("LateAfternoonNotificationWorker", "Failed to send late afternoon notification: ${e.message}", e)
            logger.e("LateAfternoonNotificationWorker: Failed to send notification", e)
            Result.failure()
        }
    }
}