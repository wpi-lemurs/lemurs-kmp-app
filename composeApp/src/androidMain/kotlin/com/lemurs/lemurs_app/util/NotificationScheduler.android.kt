package com.lemurs.lemurs_app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.data.AndroidContextProvider
import kotlinx.datetime.LocalTime
import java.util.Calendar
import java.util.concurrent.TimeUnit

actual class NotificationScheduler actual constructor() {
    private val logger = Logger.withTag("NotificationScheduler")

    private fun context(): Context =
        requireNotNull(AndroidContextProvider.context) { "Context not available" }

    private fun alarmManager(): AlarmManager =
        context().getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun constraints(): Constraints = Constraints.Builder()
        .setRequiresBatteryNotLow(false)
        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
        .build()

    private fun pendingIntent(
        action: String,
        requestCode: Int,
        extras: Map<String, String> = emptyMap()
    ): PendingIntent {
        val intent = Intent(context(), AlarmReceiver::class.java).apply {
            this.action = action
            extras.forEach { (key, value) -> putExtra(key, value) }
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context(), requestCode, intent, flags)
    }

    /** Exact delivery, so a reminder still arrives while the device is dozing. */
    private fun setExact(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        val manager = alarmManager()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            manager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun todayAt(hour: Int, minute: Int): Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    actual fun scheduleDailyNotificationSetup() {
        try {
            // Three alarms for redundancy; the worker itself ensures only the
            // first to arrive actually plans the day.
            SETUP_TIMES.forEachIndexed { index, (hour, minute) ->
                val target = todayAt(hour, minute)
                if (target.before(Calendar.getInstance())) {
                    target.add(Calendar.DAY_OF_YEAR, 1)
                }
                setExact(
                    target.timeInMillis,
                    pendingIntent(setupActionFor(index), DAILY_SETUP_REQUEST_CODE + index)
                )
            }
            logger.w("Daily setup alarms scheduled")
        } catch (e: Exception) {
            logger.e("Failed to schedule daily setup: ${e.message}", e)
            throw e
        }
    }

    actual fun rescheduleDailySetupsForTomorrow() {
        try {
            SETUP_TIMES.forEachIndexed { index, (hour, minute) ->
                val target = todayAt(hour, minute).apply { add(Calendar.DAY_OF_YEAR, 1) }
                setExact(
                    target.timeInMillis,
                    pendingIntent(setupActionFor(index), DAILY_SETUP_REQUEST_CODE + index)
                )
            }
            logger.w("Daily setup alarms re-armed for tomorrow")
        } catch (e: Exception) {
            logger.e("Failed to reschedule daily setup: ${e.message}", e)
        }
    }

    actual fun scheduleInitialNotification(
        windowName: String,
        atLocalTime: LocalTime,
        forceToday: Boolean
    ) {
        val target = todayAt(atLocalTime.hour, atLocalTime.minute)
        if (!forceToday && target.before(Calendar.getInstance())) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        try {
            setExact(
                target.timeInMillis,
                pendingIntent(
                    AlarmReceiver.ACTION_INITIAL_NOTIFICATION,
                    requestCodeFor(windowName, KIND_INITIAL),
                    mapOf(AlarmReceiver.EXTRA_WINDOW_NAME to windowName)
                )
            )
            logger.w("'$windowName' notification alarm set for ${target.time}")
        } catch (e: Exception) {
            logger.e("Failed to set '$windowName' alarm, falling back to WorkManager: ${e.message}", e)
            val delay = (target.timeInMillis - System.currentTimeMillis()).coerceAtLeast(0)
            WorkManager.getInstance(context()).enqueueUniqueWork(
                "initial $windowName notification fallback",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<InitialSurveyNotificationWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(InitialSurveyNotificationWorker.inputFor(windowName))
                    .setConstraints(constraints())
                    .build()
            )
        }
    }

    actual fun scheduleLastChanceNotification(windowName: String) {
        try {
            setExact(
                System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10),
                pendingIntent(
                    AlarmReceiver.ACTION_LAST_CHANCE_NOTIFICATION,
                    requestCodeFor(windowName, KIND_LAST_CHANCE),
                    mapOf(AlarmReceiver.EXTRA_WINDOW_NAME to windowName)
                )
            )
            logger.w("'$windowName' last-chance alarm set")
        } catch (e: Exception) {
            logger.e("Failed to set '$windowName' last-chance alarm: ${e.message}", e)
        }
    }

    actual fun scheduleReminder(
        windowName: String,
        delayMinutes: Long,
        title: String,
        body: String,
        isFinal: Boolean
    ) {
        val kind = if (isFinal) KIND_FINAL_REMINDER else KIND_FIRST_REMINDER
        try {
            setExact(
                System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(delayMinutes),
                pendingIntent(
                    AlarmReceiver.ACTION_NOTIFICATION_REMINDER,
                    requestCodeFor(windowName, kind),
                    mapOf(
                        AlarmReceiver.EXTRA_TITLE to title,
                        AlarmReceiver.EXTRA_BODY to body
                    )
                )
            )
            logger.w("'$windowName' reminder set for +${delayMinutes}m")
        } catch (e: Exception) {
            logger.e("Failed to set '$windowName' reminder: ${e.message}", e)
        }
    }

    actual fun scheduleWeeklySurveyNotification() {
        try {
            val prefs = context().getSharedPreferences("lemurs_prefs", Context.MODE_PRIVATE)
            val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)

            val firstOpen = prefs.getString("first_open_date", null) ?: run {
                val today = format.format(java.util.Date())
                prefs.edit().putString("first_open_date", today).apply()
                today
            }

            val now = Calendar.getInstance()
            val next = Calendar.getInstance().apply {
                time = requireNotNull(format.parse(firstOpen))
                add(Calendar.DAY_OF_YEAR, 6) // first weekly falls on day 7
                set(Calendar.HOUR_OF_DAY, WEEKLY_HOUR)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            while (next.before(now)) {
                next.add(Calendar.DAY_OF_YEAR, 7)
            }

            val delayMinutes = TimeUnit.MILLISECONDS.toMinutes(next.timeInMillis - now.timeInMillis)
            WorkManager.getInstance(context()).enqueueUniqueWork(
                WEEKLY_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<WeeklySurveyNotificationWorker>()
                    .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                    .setConstraints(constraints())
                    .build()
            )
            logger.w("Weekly survey notification scheduled for ${next.time}")
        } catch (e: Exception) {
            logger.e("Failed to schedule weekly survey notification: ${e.message}", e)
            throw e
        }
    }

    actual fun cancelAll(windowNames: List<String>) {
        try {
            val manager = alarmManager()

            // The setup alarms first: they are what re-arm themselves each morning, so
            // leaving them would let the chain plan a new day indefinitely.
            SETUP_TIMES.forEachIndexed { index, _ ->
                manager.cancel(pendingIntent(setupActionFor(index), DAILY_SETUP_REQUEST_CODE + index))
            }

            // Anything already armed for today. PendingIntent matching ignores extras, so
            // the request code and action are what identify these.
            windowNames.forEach { name ->
                manager.cancel(
                    pendingIntent(
                        AlarmReceiver.ACTION_INITIAL_NOTIFICATION,
                        requestCodeFor(name, KIND_INITIAL)
                    )
                )
                manager.cancel(
                    pendingIntent(
                        AlarmReceiver.ACTION_NOTIFICATION_REMINDER,
                        requestCodeFor(name, KIND_FIRST_REMINDER)
                    )
                )
                manager.cancel(
                    pendingIntent(
                        AlarmReceiver.ACTION_NOTIFICATION_REMINDER,
                        requestCodeFor(name, KIND_FINAL_REMINDER)
                    )
                )
                manager.cancel(
                    pendingIntent(
                        AlarmReceiver.ACTION_LAST_CHANCE_NOTIFICATION,
                        requestCodeFor(name, KIND_LAST_CHANCE)
                    )
                )
            }

            val workManager = WorkManager.getInstance(context())
            workManager.cancelUniqueWork(WEEKLY_WORK_NAME)
            windowNames.forEach { workManager.cancelUniqueWork("initial $it notification fallback") }

            logger.w("Cancelled all notification alarms and work")
        } catch (e: Exception) {
            logger.e("Failed to cancel notifications: ${e.message}", e)
        }
    }

    companion object {
        const val DAILY_SETUP_REQUEST_CODE = 1001
        internal const val WEEKLY_WORK_NAME = "weekly survey notification"
        private const val WEEKLY_HOUR = 21

        /** Primary plus two backups, in case the device is asleep or busy. */
        private val SETUP_TIMES = listOf(6 to 30, 6 to 40, 6 to 50)

        private const val KIND_INITIAL = 0
        private const val KIND_FIRST_REMINDER = 1
        private const val KIND_FINAL_REMINDER = 2
        private const val KIND_LAST_CHANCE = 3

        private fun setupActionFor(index: Int) =
            if (index == 0) AlarmReceiver.ACTION_DAILY_SETUP else AlarmReceiver.ACTION_BACKUP_DAILY_SETUP

        /**
         * A stable request code per (window, kind).
         *
         * Alarms are replaced by matching request code, so distinct windows must not collide.
         * Derived from the name rather than a fixed constant per window, since the windows come
         * from the database.
         */
        internal fun requestCodeFor(windowName: String, kind: Int): Int =
            2000 + (windowName.hashCode() and 0xFFFF) * 4 + kind
    }
}
