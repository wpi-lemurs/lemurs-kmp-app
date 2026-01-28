package com.lemurs.lemurs_app.util

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.data.local.UseCaseResult
import org.koin.core.component.KoinComponent
import java.util.concurrent.TimeUnit

class NotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams), KoinComponent {
    val notificationUtil = NotificationUtil()
    private val logger = Logger.withTag("NotificationWorker")

    /**
     * calls passive data use case to do work
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(value = Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        try {
            val title = requireNotNull(inputData.getString("title"))
            val body = requireNotNull(inputData.getString("body"))
            
            logger.w("NotificationWorker: Attempting to send notification - Title: $title, Body: $body")
            
            return when (val result = notificationUtil.sendNotificationText(title, body)) {
                is UseCaseResult.Success<*> -> {
                    logger.w("NotificationWorker: Successfully sent notification")
                    Result.success()
                }
                is UseCaseResult.Failure<*> -> {
                    logger.e("NotificationWorker: Failed to send notification")
                    // Retry up to 3 times, then send anyway for critical notifications
                    if (runAttemptCount < 3) {
                        logger.w("NotificationWorker: Retrying (attempt ${runAttemptCount + 1}/3)")
                        Result.retry()
                    } else {
                        logger.w("NotificationWorker: Max retries reached, attempting to send without checks")
                        // For critical notifications like reminders, try to send anyway
                        try {
                            notificationUtil.sendNotificationWithoutCheck(title, body)
                            Result.success()
                        } catch (e: Exception) {
                            logger.e("NotificationWorker: Final attempt failed: ${e.message}")
                            Result.failure()
                        }
                    }
                }
                else -> {
                    logger.e("NotificationWorker: Unknown result, will retry")
                    if (runAttemptCount < 2) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }
            }
        } catch (e: Exception) {
            logger.e("NotificationWorker: Exception occurred: ${e.message}", e)
            return if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}

class RandomNotificationsWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams), KoinComponent {

    val notificationUseCase = NotificationUseCase()

    override suspend fun doWork(): Result {
        return when (notificationUseCase.randomize()) {
            is UseCaseResult.Success<*> -> Result.success()
            is UseCaseResult.Failure<*> -> Result.retry()
            else -> Result.retry()
        }
    }
}

class NotificationScheduleWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams), KoinComponent {

    val notificationUseCase = NotificationUseCase()

    val time = requireNotNull(inputData.getString("time"))
    val delayOne = requireNotNull(inputData.getLong("delayOne", 60))
    val delayTwo = requireNotNull(inputData.getLong("delayTwo", 105))
    val timeLeft = requireNotNull(inputData.getInt("timeLeft", 45))
    override suspend fun doWork(): Result {
        return when (notificationUseCase.schedule(time, delayOne, delayTwo, timeLeft)) {
            is UseCaseResult.Success<*> -> Result.success()
            is UseCaseResult.Failure<*> -> Result.retry()
            else -> Result.retry()
        }
    }
}

// NEW WORKER: Handles the initial morning notification
class InitialMorningNotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams), KoinComponent {

    val notificationUtil = NotificationUtil()
    private val logger = Logger.withTag("InitialMorningNotificationWorker")

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(value = Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        try {
            val title = "Please take your morning survey now."
            val body = "Remember you can earn \$3 for completing this survey."
            
            logger.w("InitialMorningNotificationWorker: Sending initial morning notification")
            
            // After sending the initial notification, schedule the reminders using AlarmManager
            val notificationScheduler = NotificationScheduler("morning")
            notificationScheduler.scheduleReminderWithAlarm(
                60, 
                "Reminder: Please take your morning survey!", 
                "Remember you can earn \$3 for completing this survey.",
                NotificationScheduler.FIRST_REMINDER_REQUEST_CODE
            )
            notificationScheduler.scheduleReminderWithAlarm(
                105, 
                "Last Reminder: Please take your morning survey!", 
                "This is your last reminder. Remember you can earn \$3 for completing this survey.",
                NotificationScheduler.FINAL_REMINDER_REQUEST_CODE
            )
            
            val result = when (notificationUtil.sendNotificationText(title, body)) {
                is UseCaseResult.Success<*> -> {
                    logger.w("InitialMorningNotificationWorker: Successfully sent morning notification and scheduled reminders")
                    Result.success()
                }
                is UseCaseResult.Failure<*> -> {
                    logger.e("InitialMorningNotificationWorker: Failed to send morning notification, will retry")
                    Result.retry()
                }
                else -> {
                    logger.e("InitialMorningNotificationWorker: Unknown result, will retry")
                    Result.retry()
                }
            }
            
            return result
            
        } catch (e: Exception) {
            logger.e("InitialMorningNotificationWorker: Exception occurred: ${e.message}", e)
            return Result.retry()
        }
    }
}

// NEW WORKER: Handles the initial afternoon notification
class InitialAfternoonNotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams), KoinComponent {

    val notificationUtil = NotificationUtil()
    private val logger = Logger.withTag("InitialAfternoonNotificationWorker")

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(value = Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        try {
            val title = "Please take your afternoon survey now."
            val body = "Remember you can earn \$3 for completing this survey."
            
            logger.w("InitialAfternoonNotificationWorker: Sending initial afternoon notification")
            
            // After sending the initial notification, schedule the reminders using AlarmManager
            val notificationScheduler = NotificationScheduler("afternoon")
            notificationScheduler.scheduleReminderWithAlarm(
                60, 
                "Reminder: Please take your afternoon survey!", 
                "Remember you can earn \$3 for completing this survey.",
                NotificationScheduler.FIRST_REMINDER_REQUEST_CODE + 100 // Offset for afternoon
            )
            notificationScheduler.scheduleReminderWithAlarm(
                105, 
                "Last Reminder: Please take your afternoon survey!", 
                "This is your last reminder. Remember you can earn \$3 for completing this survey.",
                NotificationScheduler.FINAL_REMINDER_REQUEST_CODE + 100 // Offset for afternoon
            )
            
            val result = when (notificationUtil.sendNotificationText(title, body)) {
                is UseCaseResult.Success<*> -> {
                    logger.w("InitialAfternoonNotificationWorker: Successfully sent afternoon notification and scheduled reminders")
                    Result.success()
                }
                is UseCaseResult.Failure<*> -> {
                    logger.e("InitialAfternoonNotificationWorker: Failed to send afternoon notification, will retry")
                    Result.retry()
                }
                else -> {
                    logger.e("InitialAfternoonNotificationWorker: Unknown result, will retry")
                    Result.retry()
                }
            }
            
            return result
            
        } catch (e: Exception) {
            logger.e("InitialAfternoonNotificationWorker: Exception occurred: ${e.message}", e)
            return Result.retry()
        }
    }
}

// NEW WORKER: Sets up daily notification scheduling
class DailyNotificationSetupWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams), KoinComponent {

    val notificationUseCase = NotificationUseCase()
    private val logger = Logger.withTag("DailyNotificationSetupWorker")

    override suspend fun doWork(): Result {
        try {
            // CRITICAL: Only allow ONE scheduler to run per day (prevent backup schedulers from running)
            val prefs = applicationContext.getSharedPreferences("lemurs_prefs", Context.MODE_PRIVATE)
            val lastRunDate = prefs.getString("daily_setup_completed_date", "")
            val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            
            if (lastRunDate == todayDate) {
                logger.w("DailyNotificationSetupWorker: Daily scheduler already ran today ($todayDate), skipping this backup scheduler")
                // Still reschedule for tomorrow even if skipping today
                val notificationScheduler = NotificationScheduler("")
                notificationScheduler.rescheduleDailySetupsForTomorrow()
                return Result.success()
            }
            
            // Mark today as completed so backup schedulers won't run
            prefs.edit().putString("daily_setup_completed_date", todayDate).apply()
            
            logger.w("DailyNotificationSetupWorker: Starting daily notification setup")
            
            // Check current time to handle late execution
            val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val currentMinute = java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE)
            logger.w("DailyNotificationSetupWorker: Current time is ${currentHour}:${currentMinute}")
            
            // Generate random time for morning (8:00 AM - 10:59 AM to ensure reminders before noon)
            val randomMorningHour = (8..10).random()  // 8, 9, or 10 (max 10:59 AM with minutes)
            val randomMorningMinute = (0..59).random()
            
            // Generate random time for afternoon (3:00 PM - 5:59 PM to ensure reminders before 8 PM)  
            // CRITICAL FIX: Changed from (15..18) to (15..17) to prevent notifications after 6 PM
            val randomAfternoonHour = (15..17).random()  // 15 (3 PM), 16 (4 PM), or 17 (5 PM) only
            val randomAfternoonMinute = (0..59).random()
            
            logger.w("DailyNotificationSetupWorker: Random morning time: ${randomMorningHour}:${randomMorningMinute}")
            logger.w("DailyNotificationSetupWorker: Random afternoon time: ${randomAfternoonHour}:${randomAfternoonMinute}")
            
            // Schedule the initial notifications using AlarmManager for reliability
            val notificationScheduler = NotificationScheduler("")
            
            // Handle morning notification based on current time
            when {
                currentHour < 8 -> {
                    // Before 8 AM: Schedule morning notification normally
                    logger.w("DailyNotificationSetupWorker: Scheduling morning notification for today at ${randomMorningHour}:${randomMorningMinute}")
                    notificationScheduler.scheduleInitialMorningNotificationAtTime(randomMorningHour, randomMorningMinute, forceToday = false)
                }
                currentHour < 11 -> {
                    // Between 8-11 AM: Schedule morning notification for 30 seconds from now
                    // This ensures we get initial + 2 reminders before 1 PM survey close
                    logger.w("DailyNotificationSetupWorker: Late execution but survey still available, scheduling morning notification in 30 seconds")
                    val calendar = java.util.Calendar.getInstance()
                    calendar.add(java.util.Calendar.SECOND, 30)
                    notificationScheduler.scheduleInitialMorningNotificationAtTime(
                        calendar.get(java.util.Calendar.HOUR_OF_DAY),
                        calendar.get(java.util.Calendar.MINUTE),
                        forceToday = true  // Force scheduling for today even though time has "passed"
                    )
                }
                currentHour < 12 || (currentHour == 12 && currentMinute < 30) -> {
                    // Between 11 AM - 12:30 PM: Survey still open but not enough time for reminders
                    // Send just the initial notification immediately with modified message
                    logger.w("DailyNotificationSetupWorker: Late execution (${currentHour}:${currentMinute}), sending last chance morning notification")
                    val calendar = java.util.Calendar.getInstance()
                    calendar.add(java.util.Calendar.SECOND, 10)
                    // Schedule a special late morning notification without reminders
                    notificationScheduler.scheduleLateMorningNotificationAtTime(
                        calendar.get(java.util.Calendar.HOUR_OF_DAY),
                        calendar.get(java.util.Calendar.MINUTE)
                    )
                }
                else -> {
                    // After 12:30 PM: Skip morning notification (too close to survey close at 1 PM)
                    logger.w("DailyNotificationSetupWorker: Too late for morning notification (after 12:30 PM), skipping")
                }
            }
            
            // Handle afternoon notification based on current time
            when {
                currentHour < 15 -> {
                    // Before 3 PM: Schedule afternoon notification normally
                    logger.w("DailyNotificationSetupWorker: Scheduling afternoon notification for today at ${randomAfternoonHour}:${randomAfternoonMinute}")
                    notificationScheduler.scheduleInitialAfternoonNotificationAtTime(randomAfternoonHour, randomAfternoonMinute, forceToday = false)
                }
                currentHour < 18 -> {
                    // Between 3-6 PM: Schedule afternoon notification for 30 seconds from now
                    // This ensures we get initial + 2 reminders before 8 PM survey close
                    logger.w("DailyNotificationSetupWorker: Late execution but survey still available, scheduling afternoon notification in 30 seconds")
                    val calendar = java.util.Calendar.getInstance()
                    calendar.add(java.util.Calendar.SECOND, 30)
                    notificationScheduler.scheduleInitialAfternoonNotificationAtTime(
                        calendar.get(java.util.Calendar.HOUR_OF_DAY),
                        calendar.get(java.util.Calendar.MINUTE),
                        forceToday = true  // Force scheduling for today even though time has "passed"
                    )
                }
                currentHour < 19 || (currentHour == 19 && currentMinute < 30) -> {
                    // Between 6-7:30 PM: Survey still open but not enough time for reminders
                    // Send just the initial notification immediately with modified message
                    logger.w("DailyNotificationSetupWorker: Late execution (${currentHour}:${currentMinute}), sending last chance afternoon notification")
                    val calendar = java.util.Calendar.getInstance()
                    calendar.add(java.util.Calendar.SECOND, 10)
                    // Schedule a special late afternoon notification without reminders
                    notificationScheduler.scheduleLateAfternoonNotificationAtTime(
                        calendar.get(java.util.Calendar.HOUR_OF_DAY),
                        calendar.get(java.util.Calendar.MINUTE)
                    )
                }
                else -> {
                    // After 7:30 PM: Skip afternoon notification (too close to survey close at 8 PM)
                    logger.w("DailyNotificationSetupWorker: Too late for afternoon notification (after 7:30 PM), skipping")
                }
            }
            
            // IMPORTANT: Reschedule tomorrow's daily setup alarms since we're using one-time alarms
            notificationScheduler.rescheduleDailySetupsForTomorrow()
            
            logger.w("DailyNotificationSetupWorker: Successfully completed daily notification setup and rescheduled for tomorrow")
            return Result.success()
            
        } catch (e: Exception) {
            logger.e("DailyNotificationSetupWorker: Exception occurred: ${e.message}", e)
            // Retry up to 2 times for critical setup
            return if (runAttemptCount < 2) {
                logger.w("DailyNotificationSetupWorker: Retrying setup (attempt ${runAttemptCount + 1}/3)")
                Result.retry()
            } else {
                logger.e("DailyNotificationSetupWorker: Max retries reached, setup failed")
                Result.failure()
            }
        }
    }
}

// NEW WORKER: Handles weekly survey notifications
class WeeklySurveyNotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams), KoinComponent {

    val notificationUtil = NotificationUtil()
    private val logger = Logger.withTag("WeeklySurveyNotificationWorker")

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(value = Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        try {
            // Check if weekly survey is still available today at 7:00 PM
            if (!checkWeeklySurveyAvailable()) {
                logger.w("Weekly survey not available today, skipping notification")
                return Result.success()
            }

            val title = "Time for Your Weekly Survey"
            val body = "Don't forget your weekly survey! Earn $10 for completing it today."
            
            logger.w("WeeklySurveyNotificationWorker: Sending weekly survey notification")
            
            val result = when (notificationUtil.sendNotificationText(title, body)) {
                is UseCaseResult.Success<*> -> {
                    logger.w("WeeklySurveyNotificationWorker: Successfully sent weekly survey notification")
                    
                    // Reschedule this worker for next week (7 days from now)
                    logger.w("WeeklySurveyNotificationWorker: Rescheduling weekly survey notification for next week")
                    val nextWeekRequest = OneTimeWorkRequestBuilder<WeeklySurveyNotificationWorker>()
                        .setInitialDelay(7, TimeUnit.DAYS)
                        .setConstraints(Constraints.Builder()
                            .setRequiresBatteryNotLow(false)
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build())
                        .build()

                    WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                        "weekly survey notification",
                        ExistingWorkPolicy.REPLACE,
                        nextWeekRequest
                    )
                    
                    logger.w("WeeklySurveyNotificationWorker: Successfully rescheduled for next week")
                    Result.success()
                }
                is UseCaseResult.Failure<*> -> {
                    logger.e("WeeklySurveyNotificationWorker: Failed to send weekly survey notification, will retry")
                    Result.retry()
                }
                else -> {
                    logger.e("WeeklySurveyNotificationWorker: Unknown result, will retry")
                    Result.retry()
                }
            }
            
            return result
            
        } catch (e: Exception) {
            logger.e("WeeklySurveyNotificationWorker: Exception occurred: ${e.message}", e)
            return Result.retry()
        }
    }

    private suspend fun checkWeeklySurveyAvailable(): Boolean {
        // Check if weekly survey is still available at 7:00 PM today
        val calendar = java.util.Calendar.getInstance()
        val today7PM = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 19)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        
        // If it's not yet 7:00 PM, check if 7:00 PM is still within weekly survey availability
        if (calendar.before(today7PM)) {
            try {
                val availability = com.lemurs.lemurs_app.survey.fetchAndParseAvailability()
                val weeklyAvailability = availability["weekly"]
                if (weeklyAvailability != null) {
                    val now = kotlinx.datetime.Clock.System.now()
                    val sevenPM = kotlinx.datetime.Instant.fromEpochMilliseconds(today7PM.timeInMillis)
                    // If 7:00 PM time is before or equal to weekly survey availability, it's still available
                    return sevenPM <= weeklyAvailability
                }
            } catch (e: Exception) {
                logger.w("Couldn't check weekly survey availability: ${e.message}")
            }
        }
        
        return false
    }
}