package com.lemurs.lemurs_app.survey

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.until
import kotlinx.serialization.Serializable

/**
 * A survey window as the participant experiences it: a pair of wall-clock times
 * in whatever timezone the phone is currently in.
 *
 * The times are deliberately *not* absolute instants. "The morning survey opens
 * at 08:00" means 08:00 local to the participant, so a participant in Worcester
 * and a participant in Mumbai both answer it at 08:00 their own time.
 *
 * Windows are assumed not to wrap past midnight (i.e. [openTime] < [closeTime]).
 * See [isWellFormed].
 */
@Serializable
data class SurveyWindow(
    val name: String,
    val openTime: LocalTime,
    val closeTime: LocalTime,
    val surveyId: Int
) {
    val isWellFormed: Boolean get() = openTime < closeTime
}

/**
 * The response from `GET /api/survey/status`.
 *
 * Deliberately contains no absolute time for the daily surveys. The server states what the windows
 * are and what has already been submitted on the participant's local date; the phone decides what is
 * open by comparing against its own clock.
 */
@Serializable
data class SurveyStatus(
    val windows: List<SurveyWindow> = emptyList(),
    val completedWindows: List<String> = emptyList(),
    /** The weekly survey is gated on elapsed days, so this genuinely is an instant. */
    val weeklyNextAvailable: Instant? = null,
    val studyConcluded: Boolean = false
)

/** What the participant should be shown for the daily survey right now. */
sealed interface SurveyWindowState {

    /** A window is open and the participant has not yet completed it. */
    data class Open(val window: SurveyWindow, val secondsUntilClose: Long) : SurveyWindowState

    /** A window is open but the participant already submitted it today. */
    data class AlreadyCompleted(
        val window: SurveyWindow,
        val secondsUntilNextOpen: Long?
    ) : SurveyWindowState

    /** No window is open. */
    data class Closed(val secondsUntilNextOpen: Long?) : SurveyWindowState

    /** The 28-day study period has concluded. */
    data object StudyConcluded : SurveyWindowState
}

/**
 * All survey-window reasoning in the app goes through here.
 *
 * Every function takes the timezone explicitly (defaulting to the phone's
 * current zone) so that behaviour is testable and so no caller can accidentally
 * reason in a zone other than the participant's own.
 */
object SurveyWindows {

    fun systemZone(): TimeZone = TimeZone.currentSystemDefault()

    /** The participant's local calendar date. This is what "today" means for them. */
    fun localDate(
        at: Instant = Clock.System.now(),
        zone: TimeZone = systemZone()
    ): LocalDate = at.toLocalDateTime(zone).date

    /** The participant's local wall-clock time. */
    fun nowLocalTime(
        at: Instant = Clock.System.now(),
        zone: TimeZone = systemZone()
    ): LocalTime = at.toLocalDateTime(zone).time

    /**
     * The window containing [at], or null if none is open.
     *
     * Open is inclusive and close is exclusive, so a window of 08:00-13:00 is
     * open at exactly 08:00:00 and closed at exactly 13:00:00.
     */
    fun currentWindow(
        windows: List<SurveyWindow>,
        at: Instant = Clock.System.now(),
        zone: TimeZone = systemZone()
    ): SurveyWindow? {
        val time = at.toLocalDateTime(zone).time
        return windows
            .filter { it.isWellFormed }
            .firstOrNull { time >= it.openTime && time < it.closeTime }
    }

    /** Seconds from [at] until [window] closes today. Negative if already past. */
    fun secondsUntilClose(
        window: SurveyWindow,
        at: Instant = Clock.System.now(),
        zone: TimeZone = systemZone()
    ): Long {
        val closeInstant = LocalDateTime(localDate(at, zone), window.closeTime).toInstant(zone)
        return at.until(closeInstant, DateTimeUnit.SECOND)
    }

    /**
     * Seconds from [at] until the next window opens, or null if [windows] has no
     * usable entries.
     *
     * Looks at the rest of today first, then falls through to the earliest window
     * tomorrow. Computed through local date-times rather than by adding 24h, so a
     * day that is 23 or 25 hours long across a DST transition still lands on the
     * correct wall-clock opening time.
     */
    fun secondsUntilNextOpen(
        windows: List<SurveyWindow>,
        at: Instant = Clock.System.now(),
        zone: TimeZone = systemZone()
    ): Long? {
        val usable = windows.filter { it.isWellFormed }
        if (usable.isEmpty()) return null

        val today = localDate(at, zone)

        val laterToday = usable
            .map { LocalDateTime(today, it.openTime).toInstant(zone) }
            .filter { it > at }
            .minOrNull()

        val next = laterToday ?: usable
            .minByOrNull { it.openTime }
            ?.let { LocalDateTime(today.plus(1, DateTimeUnit.DAY), it.openTime).toInstant(zone) }
            ?: return null

        return at.until(next, DateTimeUnit.SECOND)
    }

    /**
     * The single decision the UI needs: given the windows and the set of window
     * names the participant has already submitted on their local date today, what
     * state is the daily survey in?
     */
    fun evaluate(
        windows: List<SurveyWindow>,
        completedWindowNames: Collection<String>,
        at: Instant = Clock.System.now(),
        zone: TimeZone = systemZone()
    ): SurveyWindowState = evaluate(windows, completedWindowNames, false, at, zone)

    fun evaluate(
        windows: List<SurveyWindow>,
        completedWindowNames: Collection<String>,
        studyConcluded: Boolean,
        at: Instant = Clock.System.now(),
        zone: TimeZone = systemZone()
    ): SurveyWindowState {
        if (studyConcluded) return SurveyWindowState.StudyConcluded

        val window = currentWindow(windows, at, zone)
            ?: return SurveyWindowState.Closed(secondsUntilNextOpen(windows, at, zone))

        return if (window.name in completedWindowNames) {
            SurveyWindowState.AlreadyCompleted(window, secondsUntilNextOpen(windows, at, zone))
        } else {
            SurveyWindowState.Open(window, secondsUntilClose(window, at, zone))
        }
    }

    fun evaluate(
        status: SurveyStatus,
        at: Instant = Clock.System.now(),
        zone: TimeZone = systemZone()
    ): SurveyWindowState = evaluate(status.windows, status.completedWindows, status.studyConcluded, at, zone)
}
