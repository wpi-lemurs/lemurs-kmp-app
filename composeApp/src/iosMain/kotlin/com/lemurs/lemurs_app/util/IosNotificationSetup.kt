package com.lemurs.lemurs_app.util

import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.data.datastore.NotificationTimesImpl
import com.lemurs.lemurs_app.survey.SurveyWindow
import com.lemurs.lemurs_app.survey.SurveyWindows
import com.lemurs.lemurs_app.survey.fetchSurveyStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.systemTimeZone
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Registers iOS notifications against the survey windows the server reports.
 *
 * Called from Swift on launch and on every foreground. Replaces the previous hardcoded 08:00 and
 * 15:00 triggers, which had drifted from the database in the same way the Android thresholds had.
 *
 * Registrations are one-shot, so running this on foreground is what arms the next occurrence. It is
 * also where completion is honoured: a window already submitted today is skipped, which together
 * with [cancelForCompletedWindow] is what stops a participant being reminded about a survey they
 * have already done.
 *
 * Each window gets three notifications, matching Android: an opening nudge and two reminders at
 * +60 and +105 minutes. All three are registered together, since iOS cannot schedule the reminders
 * when the nudge fires the way Android does.
 *
 * The time within each window is random, so participants are not all nudged at the same instant and
 * do not settle into answering at a fixed time. iOS cannot wake before dawn to draw one, so the
 * draw happens while the app is open and is stored per date: the next day's time is chosen today,
 * and re-opening the app reuses it rather than moving a notification that is already pending.
 */
object IosNotificationSetup : KoinComponent {
    private val logger = Logger.withTag("IosNotificationSetup")
    private val notificationTimes: NotificationTimesImpl by inject()

    fun refreshFromServer() {
        CoroutineScope(Dispatchers.Main).launch {
            val scheduler = NotificationScheduler()

            // Drop the previous hardcoded 08:00/15:00 triggers first, and do it
            // whether or not the fetch succeeds. They repeat daily, so leaving
            // them in place would mean an upgrading participant is notified on
            // the old fixed schedule indefinitely.
            scheduler.clearLegacyNotifications()

            val status = fetchSurveyStatus()
            if (status == null) {
                logger.w("Couldn't fetch survey windows; will register on the next launch")
                return@launch
            }
            val usable = status.windows.filter { it.isWellFormed }
            if (usable.isEmpty()) {
                logger.w("No usable windows returned")
                return@launch
            }

            // Clear everything first, then re-register only what is still needed.
            // Registrations are one-shot, so this is also what re-arms tomorrow.
            scheduler.clearWindowNotifications(usable.map { it.name })

            val zone = SurveyWindows.systemZone()
            val today = SurveyWindows.localDate(zone = zone)
            val tomorrow = today.plus(1, DateTimeUnit.DAY)
            val nowLocalTime = SurveyWindows.nowLocalTime(zone = zone)

            for (window in usable) {
                val completedToday = window.name in status.completedWindows

                // Today's slot, if one is still worth using. Skipped once the
                // survey is done, and null when too little of the window is left.
                val todaysTime =
                    if (completedToday) null
                    else plannedTime(window, today, notBefore = nowLocalTime)

                if (todaysTime != null) {
                    registerWindow(scheduler, window, today, todaysTime)
                    continue
                }

                // Otherwise aim at tomorrow, drawing from the whole window since
                // nothing has passed yet. Drawing now, while the app is open, is
                // what lets iOS randomise at all -- it cannot wake tomorrow to do it.
                val tomorrowsTime = plannedTime(window, tomorrow, notBefore = null)
                if (tomorrowsTime == null) {
                    logger.w("No usable notification time for '${window.name}'")
                    continue
                }
                registerWindow(scheduler, window, tomorrow, tomorrowsTime)
            }

            // Yesterday's draws are no longer reachable; keep only what is in use.
            notificationTimes.prunePlannedWindowTimes(
                setOf(today.toString(), tomorrow.toString())
            )

            status.weeklyNextAvailable?.let { instant ->
                val date = NSDate.dateWithTimeIntervalSince1970(
                    instant.toEpochMilliseconds() / 1000.0
                )
                scheduler.scheduleWeeklySurveyNotificationAt(date)
            }

            logger.w("Registered notifications for ${usable.size} window(s) in ${SurveyWindows.systemZone().id}")
        }
    }

    /**
     * Registers the nudge and its two reminders for one window on one date.
     *
     * All three are registered together, because iOS has no way to schedule the reminders when the
     * nudge fires the way Android does — nothing of ours runs at that moment. Their times are fixed
     * offsets from the nudge, so they follow the random draw automatically.
     *
     * A reminder falling at or past the close is dropped rather than clamped onto it: a reminder
     * arriving exactly as the window shuts is noise, and the participant can no longer act on it.
     */
    private fun registerWindow(
        scheduler: NotificationScheduler,
        window: SurveyWindow,
        date: LocalDate,
        atLocalTime: LocalTime
    ) {
        scheduler.scheduleInitialNotificationOn(window.name, atLocalTime, date)
        logger.w("'${window.name}' notification at $atLocalTime on $date")

        val close = window.closeTime.minuteOfDay()
        val start = atLocalTime.minuteOfDay()

        val reminders = listOf(
            NotificationPlanner.FIRST_REMINDER_MINUTES to false,
            NotificationPlanner.FINAL_REMINDER_MINUTES to true
        )

        for ((delay, isFinal) in reminders) {
            val at = start + delay.toInt()
            if (at >= close) {
                logger.w("Dropping '${window.name}' ${if (isFinal) "final" else "first"} reminder: past close")
                continue
            }
            val label = if (isFinal) "Last Reminder" else "Reminder"
            val body =
                if (isFinal) "This is your last reminder. $REWARD_BODY" else REWARD_BODY
            scheduler.scheduleReminderOn(
                windowName = window.name,
                onDate = date,
                atLocalTime = LocalTime.fromMinuteOfDay(at),
                title = "$label: Please take your ${window.name} survey!",
                body = body,
                isFinal = isFinal
            )
        }
    }

    /**
     * The notification time for [window] on [date], drawn once and then reused.
     *
     * Reusing the stored draw is what keeps a pending notification still: re-opening the app would
     * otherwise re-roll and move it. A time already stored is returned even if it now falls before
     * [notBefore], since that notification has already fired or is imminent — the caller moves on
     * to the next day rather than drawing a replacement.
     *
     * Returns null when the window has too little time left to be worth a nudge.
     */
    private suspend fun plannedTime(
        window: SurveyWindow,
        date: LocalDate,
        notBefore: LocalTime?
    ): LocalTime? {
        val key = date.toString()
        val stored = notificationTimes.getPlannedWindowTime(window.name, key).first()
        if (stored.isNotEmpty()) {
            val parsed = runCatching { LocalTime.parse(stored) }.getOrNull()
            if (parsed != null) {
                // Only usable if it is still ahead of us on the day in question.
                return if (notBefore == null || parsed > notBefore) parsed else null
            }
            logger.w("Ignoring unparseable stored time '$stored' for '${window.name}'")
        }

        val drawn = RandomWindowTime.draw(window, notBefore) ?: return null
        notificationTimes.updatePlannedWindowTime(window.name, key, drawn.toString())
        return drawn
    }

    /**
     * Cancels the pending notifications for a window the participant has just submitted.
     *
     * iOS fires its notifications itself, with the app not running, so there is no opportunity to
     * check completion at delivery time the way Android does. Cancelling at submission is what
     * prevents a reminder for a survey that is already done.
     *
     * The next occurrence is re-registered by [refreshFromServer] on the next foreground.
     */
    fun cancelForCompletedWindow(windowName: String) {
        NotificationScheduler().cancelNotificationsFor(windowName)
    }

    private const val REWARD_BODY = "Remember you can earn \$3 for completing this survey."
}
