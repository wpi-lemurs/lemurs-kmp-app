package com.lemurs.lemurs_app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.OutOfQuotaPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.data.AndroidContextProvider
import java.util.concurrent.TimeUnit
import java.util.Calendar
import android.os.Build

actual class NotificationScheduler actual constructor(val time: String) {
    private val logger = Logger.withTag("NotificationScheduler")

    private fun getConstraints(): Constraints {
        val constraints = Constraints.Builder()
            // Make battery constraint less restrictive - only require battery not critically low
            .setRequiresBatteryNotLow(false)
            // Remove network constraint for better reliability
            .setRequiredNetworkType(androidx.work.NetworkType.NOT_REQUIRED)
            .build()
        return constraints
    }

    // Remove context from constructor - get it when needed
    private fun getContext(): Context {
        return requireNotNull(AndroidContextProvider.context) { "Context not available" }
    }

    private fun getWorkManager(): WorkManager {
        return WorkManager.getInstance(getContext())
    }

    private fun getAlarmManager(): AlarmManager {
        return getContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private fun createPendingIntent(action: String, requestCode: Int, title: String? = null, body: String? = null): PendingIntent {
        val intent = Intent(getContext(), AlarmReceiver::class.java).apply {
            this.action = action
            title?.let { putExtra(AlarmReceiver.EXTRA_TITLE, it) }
            body?.let { putExtra(AlarmReceiver.EXTRA_BODY, it) }
        }
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        return PendingIntent.getBroadcast(getContext(), requestCode, intent, flags)
    }

    //schedule notification for when the survey opens, beginning of two hour window
    actual fun scheduleNotify() {
//        logger.w("Please take your "+time+" survey now. Remember you can earn \$3 for completing this survey.")
        val dataRequest =
        OneTimeWorkRequestBuilder<NotificationWorker>()
                .setConstraints(getConstraints())
                .setInputData(workDataOf("title" to "Please take your "+time+" survey now.", "body" to "Remember you can earn \$3 for completing this survey."))
                .build()

        getWorkManager().enqueue(dataRequest)
    }

    //first reminder, one hour after survey opens
    actual fun scheduleReminder(delay: Long) {
//        logger.w("Reminder: Please take your "+time+" survey! Remember you can earn \$3 for completing this survey.")
        val dataRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .setInputData(workDataOf("title" to "Reminder: Please take your "+time+" survey!", "body" to "Remember you can earn \$3 for completing this survey."))
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .setRequiredNetworkType(androidx.work.NetworkType.NOT_REQUIRED)
                    .build()
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        getWorkManager().enqueue(dataRequest)
    }

    //second reminder, 105 min after survey opens
    actual fun scheduleFinalReminder(delay: Long, timeLeft: String) {
//        logger.w("Last Reminder: Please take your "+time+" survey! This is your last reminder, you have "+timeLeft+" minutes until the survey closes. \n Remember you can earn \$3 for completing this survey.")
        val dataRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .setInputData(workDataOf("title" to "Last Reminder: Please take your "+time+" survey!", "body" to "This is your last reminder. Remember you can earn \$3 for completing this survey."))
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .setRequiredNetworkType(androidx.work.NetworkType.NOT_REQUIRED)
                    .build()
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        getWorkManager().enqueue(dataRequest)
    }

    //scheduled every hour to pick random times if not picked yet
    actual fun scheduleRandomReminders(){
        val request =
            PeriodicWorkRequestBuilder<RandomNotificationsWorker>(1, TimeUnit.HOURS)
            .setConstraints(getConstraints())
            .build()

        getWorkManager().enqueueUniquePeriodicWork("random notification schedule", ExistingPeriodicWorkPolicy.KEEP, request)

    }

    //schedules all three notifications together
    actual fun scheduleNotifications(delay: Long, time: String, delayOne: Long, delayTwo: Long, timeLeft: Int){
        val dataRequest = OneTimeWorkRequestBuilder<NotificationScheduleWorker>()
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .setInputData(workDataOf("time" to time, "delayOne" to delayOne, "delayTwo" to delayTwo, "timeLeft" to timeLeft))
            .setConstraints(getConstraints())
            .build()

        getWorkManager().enqueue(dataRequest)
    }

    // NEW METHOD: Schedule initial morning notification at specific time of day using AlarmManager
    // forceToday: When true, always schedules for today even if time has passed (used for late schedulers)
    actual fun scheduleInitialMorningNotificationAtTime(hour: Int, minute: Int, forceToday: Boolean) {
        val calendar = Calendar.getInstance()
        val now = Calendar.getInstance()
        
        // Set target time for today
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // If the time has already passed today AND we're not forcing today, schedule for tomorrow
        // When forceToday is true (late scheduler), keep it for today to fire immediately
        if (!forceToday && calendar.before(now)) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        val delayMinutes = TimeUnit.MILLISECONDS.toMinutes(calendar.timeInMillis - now.timeInMillis)
        
        logger.w("Scheduling initial morning notification for ${hour}:${minute} in ${delayMinutes} minutes using AlarmManager")
        
        try {
            val alarmManager = getAlarmManager()
            val pendingIntent = createPendingIntent(
                AlarmReceiver.ACTION_MORNING_NOTIFICATION,
                MORNING_NOTIFICATION_REQUEST_CODE
            )
            
            // Use setExactAndAllowWhileIdle for reliable delivery even in Doze mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            
            logger.w("Morning notification alarm set successfully for ${calendar.time}")
        } catch (e: Exception) {
            logger.e("Failed to schedule morning notification alarm: ${e.message}", e)
            // Fallback to WorkManager if AlarmManager fails
            fallbackToWorkManager(hour, minute, "morning")
        }
    }
    
    private fun fallbackToWorkManager(hour: Int, minute: Int, type: String) {
        logger.w("Using WorkManager fallback for $type notification")
        val calendar = Calendar.getInstance()
        val now = Calendar.getInstance()
        
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        if (calendar.before(now)) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        val delayMillis = calendar.timeInMillis - now.timeInMillis
        
        val dataRequest = if (type == "morning") {
            OneTimeWorkRequestBuilder<InitialMorningNotificationWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setConstraints(getConstraints())
                .build()
        } else {
            OneTimeWorkRequestBuilder<InitialAfternoonNotificationWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setConstraints(getConstraints())
                .build()
        }

        getWorkManager().enqueueUniqueWork(
            "initial $type notification fallback",
            ExistingWorkPolicy.REPLACE,
            dataRequest
        )
    }

    // NEW METHOD: Schedule initial afternoon notification at specific time of day using AlarmManager
    // forceToday: When true, always schedules for today even if time has passed (used for late schedulers)
    actual fun scheduleInitialAfternoonNotificationAtTime(hour: Int, minute: Int, forceToday: Boolean) {
        val calendar = Calendar.getInstance()
        val now = Calendar.getInstance()
        
        // Set target time for today
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // If the time has already passed today AND we're not forcing today, schedule for tomorrow
        // When forceToday is true (late scheduler), keep it for today to fire immediately
        if (!forceToday && calendar.before(now)) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        val delayMinutes = TimeUnit.MILLISECONDS.toMinutes(calendar.timeInMillis - now.timeInMillis)
        
        logger.w("Scheduling initial afternoon notification for ${hour}:${minute} in ${delayMinutes} minutes using AlarmManager")
        
        try {
            val alarmManager = getAlarmManager()
            val pendingIntent = createPendingIntent(
                AlarmReceiver.ACTION_AFTERNOON_NOTIFICATION,
                AFTERNOON_NOTIFICATION_REQUEST_CODE
            )
            
            // Use setExactAndAllowWhileIdle for reliable delivery even in Doze mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            
            logger.w("Afternoon notification alarm set successfully for ${calendar.time}")
        } catch (e: Exception) {
            logger.e("Failed to schedule afternoon notification alarm: ${e.message}", e)
            // Fallback to WorkManager if AlarmManager fails
            fallbackToWorkManager(hour, minute, "afternoon")
        }
    }

    // NEW METHOD: Schedule daily notification setup with AlarmManager + multiple backups
    actual fun scheduleDailyNotificationSetup() {
        try {
            logger.w("NotificationScheduler: Starting scheduleDailyNotificationSetup with AlarmManager")
            
            // Schedule primary 6:30 AM alarm
            schedulePrimaryDailySetup()
            
            // Schedule first backup 6:40 AM alarm (in case primary fails)
            scheduleBackupDailySetup()
            
            // Schedule second backup 6:50 AM alarm (extra safety net)
            scheduleSecondBackupDailySetup()
            
            logger.w("NotificationScheduler: Daily notification setup alarms scheduled successfully (6:30, 6:40, 6:50 AM)")
        } catch (e: Exception) {
            logger.e("NotificationScheduler: Failed to schedule daily notification setup: ${e.message}", e)
            // Fallback to WorkManager if AlarmManager completely fails
            fallbackDailySetupToWorkManager()
            throw e // Re-throw to ensure OnCreateService knows about the failure
        }
    }
    
    private fun schedulePrimaryDailySetup() {
        val now = Calendar.getInstance()
        val next630 = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // If today's 6:30 AM has passed, schedule for tomorrow 6:30 AM
        if (next630.before(now)) {
            next630.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        logger.w("Scheduling primary daily setup alarm for ${next630.time}")
        
        val alarmManager = getAlarmManager()
        val pendingIntent = createPendingIntent(
            AlarmReceiver.ACTION_DAILY_SETUP,
            DAILY_SETUP_REQUEST_CODE
        )
        
        // Use setExactAndAllowWhileIdle for exact timing even in Doze mode
        // Note: This is a one-time alarm, will be rescheduled by DailyNotificationSetupWorker
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                next630.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                next630.timeInMillis,
                pendingIntent
            )
        }
    }
    
    private fun scheduleBackupDailySetup() {
        val now = Calendar.getInstance()
        val next640 = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 40)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // If today's 6:40 AM has passed, schedule for tomorrow 6:40 AM
        if (next640.before(now)) {
            next640.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        logger.w("Scheduling first backup daily setup alarm for ${next640.time}")
        
        val alarmManager = getAlarmManager()
        val pendingIntent = createPendingIntent(
            AlarmReceiver.ACTION_BACKUP_DAILY_SETUP,
            BACKUP_DAILY_SETUP_REQUEST_CODE
        )
        
        // Use setExactAndAllowWhileIdle for exact timing even in Doze mode
        // Note: This is a one-time alarm, will be rescheduled by DailyNotificationSetupWorker
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                next640.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                next640.timeInMillis,
                pendingIntent
            )
        }
    }
    
    private fun scheduleSecondBackupDailySetup() {
        val now = Calendar.getInstance()
        val next650 = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 50)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // If today's 6:50 AM has passed, schedule for tomorrow 6:50 AM
        if (next650.before(now)) {
            next650.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        logger.w("Scheduling second backup daily setup alarm for ${next650.time}")
        
        val alarmManager = getAlarmManager()
        val pendingIntent = createPendingIntent(
            AlarmReceiver.ACTION_BACKUP_DAILY_SETUP,
            SECOND_BACKUP_DAILY_SETUP_REQUEST_CODE
        )
        
        // Use setExactAndAllowWhileIdle for exact timing even in Doze mode
        // Note: This is a one-time alarm, will be rescheduled by DailyNotificationSetupWorker
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                next650.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                next650.timeInMillis,
                pendingIntent
            )
        }
    }
    
    private fun fallbackDailySetupToWorkManager() {
        logger.w("Using WorkManager fallback for daily notification setup")
        val now = Calendar.getInstance()
        val next630 = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        if (next630.before(now)) {
            next630.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        val delayMillis = next630.timeInMillis - now.timeInMillis
        
        val request = PeriodicWorkRequestBuilder<DailyNotificationSetupWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(getConstraints())
            .build()

        getWorkManager().enqueueUniquePeriodicWork("daily notification setup fallback", ExistingPeriodicWorkPolicy.KEEP, request)
    }

    // NEW METHOD: Schedule weekly survey notification (every 7 days at 9 PM)
    fun scheduleWeeklySurveyNotification() {
        try {
            logger.w("NotificationScheduler: Starting scheduleWeeklySurveyNotification")
            
            val context = getContext()
            val prefs = context.getSharedPreferences("lemurs_prefs", Context.MODE_PRIVATE)
            
            // Check if first open date is stored
            var firstOpenDate = prefs.getString("first_open_date", null)
            
            if (firstOpenDate == null) {
                // Store today as first open date (format: yyyy-MM-dd)
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                prefs.edit().putString("first_open_date", today).apply()
                firstOpenDate = today
                logger.w("NotificationScheduler: First open date set to $firstOpenDate")
            }
            
            // Parse the first open date
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val firstOpen = dateFormat.parse(firstOpenDate)
            
            // Calculate next weekly survey date (7th, 14th, 21st, etc. day at 9 PM)
            val firstOpenCalendar = Calendar.getInstance().apply { time = firstOpen }
            val now = Calendar.getInstance()
            
            // Find the next weekly survey date
            var nextSurveyDate = Calendar.getInstance().apply {
                time = firstOpen
                add(Calendar.DAY_OF_YEAR, 6) // First survey on 7th day
                set(Calendar.HOUR_OF_DAY, 21) // 9:00 PM
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            // Keep adding 7 days until we find a future date
            while (nextSurveyDate.before(now)) {
                nextSurveyDate.add(Calendar.DAY_OF_YEAR, 7)
            }
            
            val delayMillis = nextSurveyDate.timeInMillis - now.timeInMillis
            val delayMinutes = TimeUnit.MILLISECONDS.toMinutes(delayMillis)
            
            // Calculate which week this is
            val daysSinceFirstOpen = TimeUnit.MILLISECONDS.toDays(nextSurveyDate.timeInMillis - firstOpenCalendar.timeInMillis) + 1
            val weekNumber = (daysSinceFirstOpen / 7).toInt()
            
            logger.w("NotificationScheduler: Scheduling week $weekNumber survey for ${nextSurveyDate.time}")
            logger.w("NotificationScheduler: First open: $firstOpenDate, Next survey: ${dateFormat.format(nextSurveyDate.time)} at 9 PM")
            logger.w("NotificationScheduler: Delay: ${delayMinutes} minutes")
            
            val dataRequest = OneTimeWorkRequestBuilder<WeeklySurveyNotificationWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setConstraints(getConstraints())
                .build()

            getWorkManager().enqueueUniqueWork(
                "weekly survey notification",
                ExistingWorkPolicy.REPLACE,
                dataRequest
            )
            
            logger.w("NotificationScheduler: Weekly survey notification scheduled successfully")
        } catch (e: Exception) {
            logger.e("NotificationScheduler: Failed to schedule weekly survey notification: ${e.message}", e)
            throw e // Re-throw to ensure OnCreateService knows about the failure
        }
    }
    
    // Schedule reminder using AlarmManager for better reliability
    actual fun scheduleReminderWithAlarm(delayMinutes: Long, title: String, body: String, requestCode: Int) {
        try {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MINUTE, delayMinutes.toInt())
            
            logger.w("Scheduling reminder alarm for ${calendar.time} with title: $title")
            
            val alarmManager = getAlarmManager()
            val pendingIntent = createPendingIntent(
                AlarmReceiver.ACTION_NOTIFICATION_REMINDER,
                requestCode,
                title,
                body
            )
            
            // Use setExactAndAllowWhileIdle for reliable delivery
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            
            logger.w("Reminder alarm set successfully")
        } catch (e: Exception) {
            logger.e("Failed to schedule reminder alarm: ${e.message}", e)
            // Fallback to WorkManager
            scheduleReminder(delayMinutes)
        }
    }
    
    // TESTING METHODS - Manual triggers for immediate testing (ONE-TIME ONLY)
    
    /**
     * Manual trigger for morning notification set (for testing TODAY only)
     * This will fire the initial morning notification in 10 seconds and schedule the 2 reminders
     * Does NOT affect tomorrow's automatic scheduling
     */
    fun testMorningNotificationSetNow() {
        try {
            logger.w("TEST: Triggering morning notification set in 10 seconds")
            
            val alarmManager = getAlarmManager()
            val pendingIntent = createPendingIntent(
                AlarmReceiver.ACTION_MORNING_NOTIFICATION,
                MORNING_NOTIFICATION_REQUEST_CODE + 1000 // Different ID for testing
            )
            
            // Schedule to fire in 10 seconds from now
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.SECOND, 10)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            
            logger.w("TEST: Morning notification set scheduled for ${calendar.time}")
            logger.w("TEST: This will trigger: Initial notification + 2 reminders (60min & 105min later)")
        } catch (e: Exception) {
            logger.e("TEST: Failed to trigger morning notification set: ${e.message}", e)
        }
    }
    
    /**
     * Manual trigger for afternoon notification set (for testing TODAY only)
     * This will fire the initial afternoon notification in 20 seconds and schedule the 2 reminders
     * Does NOT affect tomorrow's automatic scheduling
     */
    fun testAfternoonNotificationSetNow() {
        try {
            logger.w("TEST: Triggering afternoon notification set in 20 seconds")
            
            val alarmManager = getAlarmManager()
            val pendingIntent = createPendingIntent(
                AlarmReceiver.ACTION_AFTERNOON_NOTIFICATION,
                AFTERNOON_NOTIFICATION_REQUEST_CODE + 1000 // Different ID for testing
            )
            
            // Schedule to fire in 20 seconds from now
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.SECOND, 20)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            
            logger.w("TEST: Afternoon notification set scheduled for ${calendar.time}")
            logger.w("TEST: This will trigger: Initial notification + 2 reminders (60min & 105min later)")
        } catch (e: Exception) {
            logger.e("TEST: Failed to trigger afternoon notification set: ${e.message}", e)
        }
    }
    
    /**
     * Manual trigger for daily setup (for testing TODAY only)
     * This will generate today's random times and schedule them in 5 seconds
     * Does NOT affect tomorrow's automatic 6:30 AM scheduling
     */
    fun testDailySetupNow() {
        try {
            logger.w("TEST: Triggering daily notification setup in 5 seconds")
            
            val alarmManager = getAlarmManager()
            val pendingIntent = createPendingIntent(
                AlarmReceiver.ACTION_DAILY_SETUP,
                DAILY_SETUP_REQUEST_CODE + 1000 // Different ID for testing
            )
            
            // Schedule to fire in 5 seconds from now
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.SECOND, 5)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            
            logger.w("TEST: Daily setup scheduled for ${calendar.time}")
            logger.w("TEST: This will generate random times for today and schedule notifications")
        } catch (e: Exception) {
            logger.e("TEST: Failed to trigger daily setup: ${e.message}", e)
        }
    }
    
    /**
     * Check notification system status (for debugging)
     */
    fun checkNotificationSystemStatus() {
        try {
            val prefs = getContext().getSharedPreferences("lemurs_notification_prefs", Context.MODE_PRIVATE)
            val isSetup = prefs.getBoolean("notification_system_setup", false)
            
            val now = Calendar.getInstance()
            
            logger.w("=== NOTIFICATION SYSTEM STATUS ===")
            logger.w("Setup completed: $isSetup")
            logger.w("Current time: ${now.time}")
            logger.w("Current hour: ${now.get(Calendar.HOUR_OF_DAY)}")
            logger.w("Current minute: ${now.get(Calendar.MINUTE)}")
            
            // Check if we have AndroidContextProvider set up
            val contextAvailable = AndroidContextProvider.context != null
            logger.w("AndroidContextProvider available: $contextAvailable")
            logger.w("=== END STATUS ===")
            
        } catch (e: Exception) {
            logger.e("Failed to check system status: ${e.message}", e)
        }
    }
    
    // Schedule late morning notification (when scheduler runs between 11 AM - 12:30 PM)
    // This sends a "last chance" notification without reminders
    actual fun scheduleLateMorningNotificationAtTime(hour: Int, minute: Int) {
        val calendar = Calendar.getInstance()
        val now = Calendar.getInstance()
        
        // Set target time for today
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // Always schedule for today (will fire immediately since time is in past)
        
        logger.w("Scheduling LATE morning notification (last chance) for ${hour}:${minute}")
        
        try {
            val alarmManager = getAlarmManager()
            val pendingIntent = createPendingIntent(
                AlarmReceiver.ACTION_LATE_MORNING_NOTIFICATION,
                MORNING_NOTIFICATION_REQUEST_CODE
            )
            
            // Use setExactAndAllowWhileIdle for reliable delivery even in Doze mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            
            logger.w("Late morning notification alarm set successfully")
        } catch (e: Exception) {
            logger.e("Failed to schedule late morning notification alarm: ${e.message}", e)
        }
    }
    
    // Schedule late afternoon notification (when scheduler runs between 6 PM - 7:30 PM)
    // This sends a "last chance" notification without reminders
    actual fun scheduleLateAfternoonNotificationAtTime(hour: Int, minute: Int) {
        val calendar = Calendar.getInstance()
        val now = Calendar.getInstance()
        
        // Set target time for today
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // Always schedule for today (will fire immediately since time is in past)
        
        logger.w("Scheduling LATE afternoon notification (last chance) for ${hour}:${minute}")
        
        try {
            val alarmManager = getAlarmManager()
            val pendingIntent = createPendingIntent(
                AlarmReceiver.ACTION_LATE_AFTERNOON_NOTIFICATION,
                AFTERNOON_NOTIFICATION_REQUEST_CODE
            )
            
            // Use setExactAndAllowWhileIdle for reliable delivery even in Doze mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            
            logger.w("Late afternoon notification alarm set successfully")
        } catch (e: Exception) {
            logger.e("Failed to schedule late afternoon notification alarm: ${e.message}", e)
        }
    }
    
    // Reschedule daily setup alarms for tomorrow (called after each successful run)
    actual fun rescheduleDailySetupsForTomorrow() {
        try {
            logger.w("Rescheduling daily setup alarms for tomorrow")
            
            val alarmManager = getAlarmManager()
            
            // Reschedule 6:30 AM for tomorrow
            val tomorrow630 = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 6)
                set(Calendar.MINUTE, 30)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            val pendingIntent630 = createPendingIntent(
                AlarmReceiver.ACTION_DAILY_SETUP,
                DAILY_SETUP_REQUEST_CODE
            )
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    tomorrow630.timeInMillis,
                    pendingIntent630
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    tomorrow630.timeInMillis,
                    pendingIntent630
                )
            }
            
            // Reschedule 6:40 AM backup for tomorrow
            val tomorrow640 = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 6)
                set(Calendar.MINUTE, 40)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            val pendingIntent640 = createPendingIntent(
                AlarmReceiver.ACTION_BACKUP_DAILY_SETUP,
                BACKUP_DAILY_SETUP_REQUEST_CODE
            )
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    tomorrow640.timeInMillis,
                    pendingIntent640
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    tomorrow640.timeInMillis,
                    pendingIntent640
                )
            }
            
            // Reschedule 6:50 AM backup for tomorrow
            val tomorrow650 = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 6)
                set(Calendar.MINUTE, 50)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            val pendingIntent650 = createPendingIntent(
                AlarmReceiver.ACTION_BACKUP_DAILY_SETUP,
                SECOND_BACKUP_DAILY_SETUP_REQUEST_CODE
            )
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    tomorrow650.timeInMillis,
                    pendingIntent650
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    tomorrow650.timeInMillis,
                    pendingIntent650
                )
            }
            
            logger.w("Successfully rescheduled daily setup alarms for tomorrow: ${tomorrow630.time}")
        } catch (e: Exception) {
            logger.e("Failed to reschedule daily setup alarms: ${e.message}", e)
        }
    }
    
    companion object {
        const val DAILY_SETUP_REQUEST_CODE = 1001
        const val BACKUP_DAILY_SETUP_REQUEST_CODE = 1002
        const val SECOND_BACKUP_DAILY_SETUP_REQUEST_CODE = 1007
        const val MORNING_NOTIFICATION_REQUEST_CODE = 1003
        const val AFTERNOON_NOTIFICATION_REQUEST_CODE = 1004
        const val FIRST_REMINDER_REQUEST_CODE = 1005
        const val FINAL_REMINDER_REQUEST_CODE = 1006
    }
}