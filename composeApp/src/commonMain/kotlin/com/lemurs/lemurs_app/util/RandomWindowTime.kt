package com.lemurs.lemurs_app.util

import com.lemurs.lemurs_app.survey.SurveyWindow
import kotlinx.datetime.LocalTime
import kotlin.random.Random

/**
 * Chooses a random time inside a survey window for its notification.
 *
 * Participants should not all be nudged at the same instant, or they learn to answer at a fixed
 * time each day. Android re-draws every morning from an alarm; iOS cannot, so it draws ahead and
 * persists the choice.
 *
 * The draw is bounded so the two reminders still land before the window closes, matching
 * [NotificationPlanner].
 */
object RandomWindowTime {

    /**
     * A random time within [window], never earlier than [notBefore].
     *
     * [notBefore] exists for the case where a window is already open when the draw happens: a
     * notification cannot be scheduled in the past, so the reachable range starts from now. Pass
     * null when drawing for a future date, where the whole window is available.
     *
     * Returns null when no usable time remains, which the caller should treat as "do not schedule".
     */
    fun draw(
        window: SurveyWindow,
        notBefore: LocalTime? = null,
        random: Random = Random.Default
    ): LocalTime? {
        if (!window.isWellFormed) return null

        val open = window.openTime.minuteOfDay()
        val close = window.closeTime.minuteOfDay()

        // Keep the full reminder set inside the window where it fits. Strictly
        // inside: a draw at exactly close-105 puts the final reminder on the
        // close itself, where the participant can no longer act on it.
        val latestStart = close - NotificationPlanner.FINAL_REMINDER_MINUTES.toInt() - 1

        val earliest = maxOf(open, notBefore?.minuteOfDay() ?: open)
        val latest = maxOf(latestStart, open)

        // Too little of the window remains for a nudge plus its reminders.
        if (earliest > latest) return null

        // Inclusive of both ends, so a window with exactly one usable minute works.
        val chosen = earliest + random.nextInt(latest - earliest + 1)
        return LocalTime.fromMinuteOfDay(chosen)
    }
}
