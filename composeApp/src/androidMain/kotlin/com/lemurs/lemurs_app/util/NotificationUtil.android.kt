package com.lemurs.lemurs_app.util

import android.Manifest

import android.app.NotificationManager
import androidx.compose.runtime.mutableStateOf
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.data.AndroidActivityLauncherProvider
import com.lemurs.lemurs_app.data.AndroidContextProvider
import com.lemurs.lemurs_app.data.local.UseCaseResult
import com.lemurs.lemurs_app.R
import com.lemurs.lemurs_app.survey.fetchAndParseAvailability
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.until


actual class NotificationUtil {
    val logger = Logger.withTag("NotificationUtil")

    private val context = AndroidContextProvider.context

    //requests permissions from the user - nullable because it's not available in WorkManager context
    private val activityResultLauncher: ActivityResultLauncher<Array<String>>? =
        AndroidActivityLauncherProvider.activityLauncherMultiple


    actual fun checkSurveyCompleted(): Boolean {
        // Check if context is available
        if (context == null) {
            logger.e("Context is null, cannot check survey completion")
            return false
        }
        
        var availability: Long?
        var localAvailability = mutableStateOf<Map<String, Instant>?>(null)
        try {
            runBlocking {
                localAvailability.value = fetchAndParseAvailability()
            }
        } catch (e: Exception) {
            logger.w("Couldn't fetch survey availability data: ${e.message}")
            //if api call fails, assume survey is available
            return false
        }
        val local = localAvailability.value
        if (local != null && local.containsKey("daily")) {
            val now = Clock.System.now()
            availability = now.until(local["daily"]!!, DateTimeUnit.SECOND)

            var timeUp = false
            if (availability <= 0L) {
                timeUp = true
            }
            return !timeUp
        } else {
            return true
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(value = Manifest.permission.POST_NOTIFICATIONS)
    actual fun sendNotificationText(title: String, body: String): UseCaseResult<Any> {
        // Check if context is available
        if (context == null) {
            logger.e("Context is null, cannot send notification")
            return UseCaseResult.Failure()
        }
        
        //if survey is not completed, send notification:
        if (!checkSurveyCompleted()) {
            logger.w("sending notifs, survey not completed")
            //request permissions if they are off
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                logger.w("notifications disabled, requesting permissions")
                activityResultLauncher?.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                    ?: logger.w("Cannot request permissions from WorkManager context")
            }

            val notificationManager = NotificationManagerCompat.from(context)
            //you have to make this call because if android upgrades to another version and becomes incompatible with
            //how this notification system is set up then it will crash
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                logger.w("launching notification permission request")
                activityResultLauncher?.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                    ?: logger.w("Cannot request permissions from WorkManager context")
                return UseCaseResult.Failure()
            } else {
                //this creates a channel that the notification is sent through
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                    logger.w("creating channel to send notifications")
                    val name = context.getString(R.string.channel_name)
                    val description = context.getString(R.string.channel_description)
                    val importance = NotificationManager.IMPORTANCE_DEFAULT
                    val channelBuilder = NotificationChannelCompat.Builder(
                        context.getString(R.string.channel_id),
                        importance
                    )
                    channelBuilder.setName(name)
                    channelBuilder.setDescription(description)
                    val channel = channelBuilder.build()
                    notificationManager.createNotificationChannel(channel)

                    //this creates the notification that will be sent
                    val notificationCompatBuilder: NotificationCompat.Builder =
                        NotificationCompat.Builder(
                            context,
                            context.getString(R.string.channel_id)
                        )
                            //we can change the icon when we finalize the app icon
                            .setSmallIcon(R.drawable.lemursappicon)
                            .setContentTitle(title)
                            .setContentText(body)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

                    notificationManager.notify(10, notificationCompatBuilder.build())
                    return UseCaseResult.Success(Any())
                } else {
                    logger.w("android version not supported")
                }

            }
            return UseCaseResult.Failure()
        }
        logger.w("not sending notifs, survey already completed")
        return UseCaseResult.Success(Any())
    }

    // Direct notification method for BroadcastReceiver context (doesn't use AndroidContextProvider)
    fun sendNotificationTextDirect(directContext: android.content.Context, title: String, body: String) {
        try {
            val notificationManager = NotificationManagerCompat.from(directContext)
            
            // Create notification channel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channelId = "lemurs_direct_channel"
                val name = "Direct Notifications"
                val description = "Direct notification channel for alarms"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channelBuilder = NotificationChannelCompat.Builder(channelId, importance)
                channelBuilder.setName(name)
                channelBuilder.setDescription(description)
                val channel = channelBuilder.build()
                notificationManager.createNotificationChannel(channel)
                
                // Create and send notification
                val notificationBuilder = NotificationCompat.Builder(directContext, channelId)
                    .setSmallIcon(R.drawable.lemursappicon)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                
                notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
                logger.w("Direct notification sent: $title")
            }
        } catch (e: Exception) {
            logger.e("Failed to send direct notification: ${e.message}", e)
        }
    }
    
    // Send notification without checking survey completion (for critical reminders)
    actual fun sendNotificationWithoutCheck(title: String, body: String): UseCaseResult<Any> {
        // Check if context is available
        if (context == null) {
            logger.e("Context is null, cannot send notification")
            return UseCaseResult.Failure()
        }

        logger.w("Sending notification without survey check - Title: $title")
        
        //request permissions if they are off
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            logger.w("notifications disabled, requesting permissions")
            activityResultLauncher?.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                ?: logger.w("Cannot request permissions from WorkManager context")
        }

        val notificationManager = NotificationManagerCompat.from(context)
        
        //you have to make this call because if android upgrades to another version and becomes incompatible with
        //how this notification system is set up then it will crash
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            logger.w("launching notification permission request")
            activityResultLauncher?.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                ?: logger.w("Cannot request permissions from WorkManager context")
            return UseCaseResult.Failure()
        } else {
            //this creates a channel that the notification is sent through
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                logger.w("creating channel to send notifications")
                val name = context.getString(R.string.channel_name)
                val description = context.getString(R.string.channel_description)
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channelBuilder = NotificationChannelCompat.Builder(
                    context.getString(R.string.channel_id),
                    importance
                )
                channelBuilder.setName(name)
                channelBuilder.setDescription(description)
                val channel = channelBuilder.build()
                notificationManager.createNotificationChannel(channel)

                //this creates the notification that will be sent
                val notificationCompatBuilder: NotificationCompat.Builder =
                    NotificationCompat.Builder(
                        context,
                        context.getString(R.string.channel_id)
                    )
                        //we can change the icon when we finalize the app icon
                        .setSmallIcon(R.drawable.lemursappicon)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)

                notificationManager.notify(20, notificationCompatBuilder.build()) // Different ID than regular notifications
                logger.w("Force notification sent successfully")
                return UseCaseResult.Success(Any())
            } else {
                logger.w("android version not supported")
            }
        }
        return UseCaseResult.Failure()
    }

}
