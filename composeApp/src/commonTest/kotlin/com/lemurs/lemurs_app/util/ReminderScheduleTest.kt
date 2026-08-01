package com.lemurs.lemurs_app.util

import com.lemurs.lemurs_app.survey.SurveyWindow
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers the reminder times iOS derives from a randomly drawn nudge.
 *
 * Android schedules its reminders when the nudge fires, so they are relative by construction. iOS
 * has to register all three up front, which means computing the reminder times from the draw — and
 * getting that wrong would put a reminder after the survey had closed.
 */
class ReminderScheduleTest {

    private val morning = SurveyWindow("morning", LocalTime(8, 0), LocalTime(13, 0), 0)

    /** Mirrors the derivation in IosNotificationSetup.registerWindow. */
    private fun reminderTimes(window: SurveyWindow, nudge: LocalTime): List<LocalTime> {
        val close = window.closeTime.minuteOfDay()
        val start = nudge.minuteOfDay()
        return listOf(
            NotificationPlanner.FIRST_REMINDER_MINUTES,
            NotificationPlanner.FINAL_REMINDER_MINUTES
        ).map { start + it.toInt() }
            .filter { it < close }
            .map { LocalTime.fromMinuteOfDay(it) }
    }

    @Test
    fun `reminders follow the nudge at 60 and 105 minutes`() {
        val times = reminderTimes(morning, LocalTime(9, 0))
        assertEquals(listOf(LocalTime(10, 0), LocalTime(10, 45)), times)
    }

    @Test
    fun `reminders move with the random draw`() {
        // The whole reason they are computed rather than fixed.
        assertEquals(
            listOf(LocalTime(9, 23), LocalTime(10, 8)),
            reminderTimes(morning, LocalTime(8, 23))
        )
        assertEquals(
            listOf(LocalTime(11, 47), LocalTime(12, 32)),
            reminderTimes(morning, LocalTime(10, 47))
        )
    }

    @Test
    fun `every reminder from a valid draw lands before the close`() {
        // RandomWindowTime bounds the draw so both reminders fit; this checks the
        // two agree, across the whole reachable range.
        val close = morning.closeTime.minuteOfDay()
        repeat(2000) {
            val nudge = RandomWindowTime.draw(morning)!!
            val times = reminderTimes(morning, nudge)
            assertEquals(2, times.size, "a valid draw at $nudge lost a reminder")
            for (t in times) {
                assertTrue(t.minuteOfDay() < close, "reminder at $t is past the close")
            }
        }
    }

    @Test
    fun `a reminder landing past the close is dropped, not clamped`() {
        // Only reachable for a window too short for the full set. Clamping would
        // deliver a reminder exactly as the survey became unavailable.
        val brief = SurveyWindow("brief", LocalTime(9, 0), LocalTime(9, 30), 3)
        assertTrue(reminderTimes(brief, LocalTime(9, 0)).isEmpty())
    }

    @Test
    fun `a partial set keeps only the reminder that fits`() {
        val medium = SurveyWindow("medium", LocalTime(9, 0), LocalTime(10, 30), 4)
        // 09:00 + 60 = 10:00 fits; + 105 = 10:45 does not.
        assertEquals(listOf(LocalTime(10, 0)), reminderTimes(medium, LocalTime(9, 0)))
    }

    @Test
    fun `the reminder offsets match Android`() {
        // The two platforms should nudge on the same cadence.
        assertEquals(60L, NotificationPlanner.FIRST_REMINDER_MINUTES)
        assertEquals(105L, NotificationPlanner.FINAL_REMINDER_MINUTES)
    }
}
