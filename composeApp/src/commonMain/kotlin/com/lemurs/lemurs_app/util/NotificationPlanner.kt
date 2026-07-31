package com.lemurs.lemurs_app.util

import com.lemurs.lemurs_app.survey.SurveyWindow
import kotlinx.datetime.LocalTime
import kotlin.random.Random

/** What should happen for one survey window today. */
sealed interface WindowNotificationPlan {
    val windowName: String

    /** Send the opening nudge at [atLocalTime], then two reminders after it. */
    data class Notify(
        override val windowName: String,
        val atLocalTime: LocalTime,
        val firstReminderDelayMinutes: Long,
        val finalReminderDelayMinutes: Long
    ) : WindowNotificationPlan

    /** Too little of the window is left for reminders; send a single last call now. */
    data class LastChance(override val windowName: String) : WindowNotificationPlan

    /** Nothing useful can be sent for this window today. */
    data class Skip(override val windowName: String, val reason: String) : WindowNotificationPlan
}

/**
 * Decides when today's notifications should fire.
 *
 * Times are derived from the survey windows themselves rather than hardcoded, so changing a window
 * in the database moves its notifications with it. The previous implementation assumed the windows
 * closed at 11:00 and 18:00 while the database said 13:00 and 20:00, and the two had drifted apart
 * with no way to notice.
 *
 * All reasoning is in local wall-clock time. The caller supplies the participant's current local
 * time, so this is correct in any timezone.
 */
object NotificationPlanner {

    /** First reminder, this many minutes after the opening nudge. */
    const val FIRST_REMINDER_MINUTES = 60L

    /** Final reminder, this many minutes after the opening nudge. */
    const val FINAL_REMINDER_MINUTES = 105L

    /** Below this much time left, a full set of reminders will not fit. */
    const val LAST_CHANCE_MARGIN_MINUTES = 30L

    fun plan(
        windows: List<SurveyWindow>,
        nowLocalTime: LocalTime,
        random: Random = Random.Default
    ): List<WindowNotificationPlan> = windows.map { window ->
        planWindow(window, nowLocalTime, random)
    }

    private fun planWindow(
        window: SurveyWindow,
        nowLocalTime: LocalTime,
        random: Random
    ): WindowNotificationPlan {
        if (!window.isWellFormed) {
            return WindowNotificationPlan.Skip(window.name, "window is not well formed")
        }

        val open = window.openTime.minuteOfDay()
        val close = window.closeTime.minuteOfDay()
        val now = nowLocalTime.minuteOfDay()

        if (now >= close) {
            return WindowNotificationPlan.Skip(window.name, "window already closed")
        }

        // The latest start that still leaves room for the full reminder set.
        // Used when picking a slot ahead of time, so a scheduled notification
        // always gets both reminders.
        val latestFullStart = close - FINAL_REMINDER_MINUTES.toInt()

        val minutesLeft = close - now

        // Before the window opens: pick a random time inside it, so participants
        // are not all nudged at the same instant.
        if (now < open) {
            if (latestFullStart <= open) {
                // The window is too short for the full set; nudge on the dot.
                return notifyAt(window, window.openTime, close)
            }
            val start = open + random.nextInt(latestFullStart - open)
            return notifyAt(window, LocalTime.fromMinuteOfDay(start), close)
        }

        // The window is already open. Nudge now rather than waiting for a slot
        // that has passed, provided at least the first reminder still lands
        // strictly before the close -- otherwise every reminder would arrive as
        // the window shuts, which is just noise.
        if (minutesLeft > FIRST_REMINDER_MINUTES) {
            return notifyAt(window, nowLocalTime, close)
        }

        // Too late for reminders, but still worth one last call.
        if (minutesLeft > LAST_CHANCE_MARGIN_MINUTES) {
            return WindowNotificationPlan.LastChance(window.name)
        }

        return WindowNotificationPlan.Skip(window.name, "less than $LAST_CHANCE_MARGIN_MINUTES minutes left")
    }

    /** Builds a [WindowNotificationPlan.Notify], clamping reminders so neither lands after close. */
    private fun notifyAt(
        window: SurveyWindow,
        start: LocalTime,
        closeMinuteOfDay: Int
    ): WindowNotificationPlan.Notify {
        val minutesLeft = (closeMinuteOfDay - start.minuteOfDay()).toLong()
        return WindowNotificationPlan.Notify(
            windowName = window.name,
            atLocalTime = start,
            firstReminderDelayMinutes = FIRST_REMINDER_MINUTES.coerceAtMost(minutesLeft),
            finalReminderDelayMinutes = FINAL_REMINDER_MINUTES.coerceAtMost(minutesLeft)
        )
    }
}

internal fun LocalTime.minuteOfDay(): Int = hour * 60 + minute

internal fun LocalTime.Companion.fromMinuteOfDay(minuteOfDay: Int): LocalTime {
    val clamped = minuteOfDay.coerceIn(0, 24 * 60 - 1)
    return LocalTime(clamped / 60, clamped % 60)
}
