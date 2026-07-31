package com.lemurs.lemurs_app.util

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.data.datastore.NotificationTimesImpl
import com.lemurs.lemurs_app.data.local.UseCaseResult
import com.lemurs.lemurs_app.survey.SurveyWindow
import com.lemurs.lemurs_app.survey.SurveyWindows
import com.lemurs.lemurs_app.survey.fetchSurveyStatus
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val REWARD_BODY = "Remember you can earn \$3 for completing this survey."

/** Sends a notification whose text is supplied in the work request. */
class NotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams), KoinComponent {

    private val notificationUtil = NotificationUtil()
    private val logger = Logger.withTag("NotificationWorker")

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(value = Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        return try {
            val title = requireNotNull(inputData.getString("title"))
            val body = requireNotNull(inputData.getString("body"))

            when (notificationUtil.sendNotificationText(title, body)) {
                is UseCaseResult.Success<*> -> Result.success()
                else -> if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    // Reminders matter enough to send even if the pre-check fails.
                    logger.w("Max retries reached, sending without checks")
                    notificationUtil.sendNotificationWithoutCheck(title, body)
                    Result.success()
                }
            }
        } catch (e: Exception) {
            logger.e("Exception occurred: ${e.message}", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}

/**
 * Sends the opening nudge for one window and schedules its two reminders.
 *
 * Replaces the previous separate morning and afternoon workers, which were identical apart from
 * their strings and reminder request codes.
 */
class InitialSurveyNotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams), KoinComponent {

    private val notificationUtil = NotificationUtil()
    private val notificationTimes: NotificationTimesImpl by inject()
    private val logger = Logger.withTag("InitialSurveyNotificationWorker")

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(value = Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        val windowName = inputData.getString(KEY_WINDOW_NAME) ?: return Result.failure()
        val firstDelay = inputData.getLong(KEY_FIRST_REMINDER, NotificationPlanner.FIRST_REMINDER_MINUTES)
        val finalDelay = inputData.getLong(KEY_FINAL_REMINDER, NotificationPlanner.FINAL_REMINDER_MINUTES)

        return try {
            val scheduler = NotificationScheduler()
            scheduler.scheduleReminder(
                windowName,
                firstDelay,
                "Reminder: Please take your $windowName survey!",
                REWARD_BODY,
                isFinal = false
            )
            scheduler.scheduleReminder(
                windowName,
                finalDelay,
                "Last Reminder: Please take your $windowName survey!",
                "This is your last reminder. $REWARD_BODY",
                isFinal = true
            )

            when (
                notificationUtil.sendNotificationText(
                    "Please take your $windowName survey now.",
                    REWARD_BODY
                )
            ) {
                is UseCaseResult.Success<*> -> {
                    // Record when the nudge went out so the submission can be timed
                    // against it. Nothing wrote this before, which is why
                    // notification_start has always equalled the submission time.
                    notificationTimes.updateWindowTime(windowName, Clock.System.now().toString())
                    logger.w("Sent '$windowName' notification and scheduled reminders")
                    Result.success()
                }

                else -> Result.retry()
            }
        } catch (e: Exception) {
            logger.e("Exception for '$windowName': ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        const val KEY_WINDOW_NAME = "windowName"
        const val KEY_FIRST_REMINDER = "firstReminderMinutes"
        const val KEY_FINAL_REMINDER = "finalReminderMinutes"

        fun inputFor(
            windowName: String,
            firstReminderMinutes: Long = NotificationPlanner.FIRST_REMINDER_MINUTES,
            finalReminderMinutes: Long = NotificationPlanner.FINAL_REMINDER_MINUTES
        ): Data = workDataOf(
            KEY_WINDOW_NAME to windowName,
            KEY_FIRST_REMINDER to firstReminderMinutes,
            KEY_FINAL_REMINDER to finalReminderMinutes
        )
    }
}

/**
 * Sends a single "last call" with no reminders.
 *
 * Used when a window is open but too little of it remains for the full set.
 */
class LastChanceNotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams), KoinComponent {

    private val notificationUtil = NotificationUtil()
    private val notificationTimes: NotificationTimesImpl by inject()
    private val logger = Logger.withTag("LastChanceNotificationWorker")

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(value = Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        val windowName = inputData.getString(InitialSurveyNotificationWorker.KEY_WINDOW_NAME)
            ?: return Result.failure()

        return try {
            when (
                notificationUtil.sendNotificationText(
                    "Last chance: your $windowName survey closes soon!",
                    "Complete it now to earn \$3."
                )
            ) {
                is UseCaseResult.Success<*> -> {
                    notificationTimes.updateWindowTime(windowName, Clock.System.now().toString())
                    Result.success()
                }

                else -> Result.retry()
            }
        } catch (e: Exception) {
            logger.e("Exception for '$windowName': ${e.message}", e)
            Result.retry()
        }
    }
}

/**
 * Plans and schedules the day's survey notifications.
 *
 * Runs from an alarm early each morning. Times are derived from the survey windows themselves via
 * [NotificationPlanner] rather than being hardcoded, so a window changed in the database moves its
 * notifications with it.
 */
class DailyNotificationSetupWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams), KoinComponent {

    private val notificationTimes: NotificationTimesImpl by inject()
    private val logger = Logger.withTag("DailyNotificationSetupWorker")

    override suspend fun doWork(): Result {
        return try {
            val scheduler = NotificationScheduler()

            // Three alarms are armed for redundancy; only the first to arrive
            // should plan the day.
            val today = SurveyWindows.localDate().toString()
            val prefs = applicationContext.getSharedPreferences("lemurs_prefs", Context.MODE_PRIVATE)
            if (prefs.getString("daily_setup_completed_date", "") == today) {
                logger.w("Setup already ran for $today, skipping backup run")
                scheduler.rescheduleDailySetupsForTomorrow()
                return Result.success()
            }
            prefs.edit().putString("daily_setup_completed_date", today).apply()

            val windows = loadWindows()
            if (windows.isEmpty()) {
                logger.w("No survey windows known yet, nothing to schedule")
                scheduler.rescheduleDailySetupsForTomorrow()
                return Result.success()
            }

            val nowLocalTime = SurveyWindows.nowLocalTime()
            logger.w("Planning notifications at $nowLocalTime for ${windows.size} window(s)")

            for (plan in NotificationPlanner.plan(windows, nowLocalTime)) {
                when (plan) {
                    is WindowNotificationPlan.Notify -> {
                        logger.w("'${plan.windowName}' notification at ${plan.atLocalTime}")
                        // forceToday because a setup that ran late must still fire
                        // inside today's window rather than slipping a day.
                        scheduler.scheduleInitialNotification(
                            plan.windowName,
                            plan.atLocalTime,
                            forceToday = plan.atLocalTime <= nowLocalTime
                        )
                    }

                    is WindowNotificationPlan.LastChance -> {
                        logger.w("'${plan.windowName}' last chance, no reminders")
                        scheduler.scheduleLastChanceNotification(plan.windowName)
                    }

                    is WindowNotificationPlan.Skip ->
                        logger.w("Skipping '${plan.windowName}': ${plan.reason}")
                }
            }

            notificationTimes.updateDate(today)
            scheduler.rescheduleDailySetupsForTomorrow()
            Result.success()
        } catch (e: Exception) {
            logger.e("Exception occurred: ${e.message}", e)
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    /**
     * The survey windows, refreshed from the API when reachable and otherwise read from cache.
     *
     * This runs before dawn, when connectivity is least reliable, so a cached copy is what makes the
     * schedule survive a night with no network.
     */
    private suspend fun loadWindows(): List<SurveyWindow> {
        val status = fetchSurveyStatus()
        if (status != null && status.windows.isNotEmpty()) {
            notificationTimes.updateCachedWindows(
                Json.encodeToString(ListSerializer(SurveyWindow.serializer()), status.windows)
            )
            return status.windows
        }

        return try {
            val cached = notificationTimes.getCachedWindows().first()
            if (cached.isEmpty()) {
                emptyList()
            } else {
                Json.decodeFromString(ListSerializer(SurveyWindow.serializer()), cached)
            }
        } catch (e: Exception) {
            logger.e("Couldn't read cached windows: ${e.message}", e)
            emptyList()
        }
    }
}

/** Sends the weekly survey notification. */
class WeeklySurveyNotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams), KoinComponent {

    private val notificationUtil = NotificationUtil()
    private val notificationTimes: NotificationTimesImpl by inject()
    private val logger = Logger.withTag("WeeklySurveyNotificationWorker")

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(value = Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        return try {
            if (!isWeeklyOpen()) {
                logger.w("Weekly survey not open yet, skipping notification")
                NotificationScheduler().scheduleWeeklySurveyNotification()
                return Result.success()
            }

            when (
                notificationUtil.sendNotificationText(
                    "Time for Your Weekly Survey",
                    "Don't forget your weekly survey! Earn \$10 for completing it today."
                )
            ) {
                is UseCaseResult.Success<*> -> {
                    notificationTimes.updateWindowTime(WEEKLY_WINDOW, Clock.System.now().toString())
                    NotificationScheduler().scheduleWeeklySurveyNotification()
                    Result.success()
                }

                else -> Result.retry()
            }
        } catch (e: Exception) {
            logger.e("Exception occurred: ${e.message}", e)
            Result.retry()
        }
    }

    /**
     * Whether the weekly survey is open now.
     *
     * The weekly survey is gated on an absolute instant (days since enrolment), not a time of day,
     * so this is a straight comparison against the clock.
     *
     * The previous version compared the wrong way round -- it asked whether 7 PM came *before* the
     * next-available time, which is true precisely when the survey is still locked -- and so
     * suppressed the notification on exactly the days it should have fired.
     */
    private suspend fun isWeeklyOpen(): Boolean {
        val status = fetchSurveyStatus()
        if (status == null) {
            logger.w("Couldn't fetch weekly availability; sending anyway")
            return true
        }
        val nextAvailable = status.weeklyNextAvailable ?: return true
        return nextAvailable <= Clock.System.now()
    }

    private companion object {
        const val WEEKLY_WINDOW = "weekly"
    }
}
