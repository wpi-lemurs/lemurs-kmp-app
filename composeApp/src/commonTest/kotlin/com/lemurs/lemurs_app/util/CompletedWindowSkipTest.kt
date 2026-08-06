package com.lemurs.lemurs_app.util

import com.lemurs.lemurs_app.survey.SurveyWindow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers the rule iOS uses to decide whether a window still needs a notification.
 *
 * iOS registers notifications ahead of time and cannot check completion when they fire, so this
 * decision is made at registration. The logic is small but easy to get subtly wrong: skipping a
 * completed window is right only while its slot is still ahead, because once the slot has passed
 * the next occurrence belongs to tomorrow, which has not been completed.
 */
class CompletedWindowSkipTest {

    private val morning = SurveyWindow("morning", LocalTime(8, 0), LocalTime(13, 0), 0)
    private val afternoon = SurveyWindow("afternoon", LocalTime(15, 0), LocalTime(20, 0), 1)

    /** Mirrors the decision in IosNotificationSetup.refreshFromServer. */
    private fun shouldSkip(
        window: SurveyWindow,
        slotLocalTime: LocalTime,
        nowLocalTime: LocalTime,
        completedWindows: List<String>
    ): Boolean {
        val slotStillToday = slotLocalTime.minuteOfDay() > nowLocalTime.minuteOfDay()
        return slotStillToday && window.name in completedWindows
    }

    @Test
    fun `a completed window is skipped while its slot is still ahead`() {
        // Submitted at 15:05; the 15:30 nudge has not fired yet. This is the case
        // a tester actually hit: submitted around 3pm, notified at 3:30.
        assertTrue(
            shouldSkip(afternoon, LocalTime(15, 30), LocalTime(15, 5), listOf("afternoon"))
        )
    }

    @Test
    fun `a completed window is armed again once its slot has passed`() {
        // 16:00: today's 15:30 slot is gone, so the next occurrence is tomorrow's,
        // which the participant has not done. Skipping here would mean never being
        // notified about that window again.
        assertFalse(
            shouldSkip(afternoon, LocalTime(15, 30), LocalTime(16, 0), listOf("afternoon"))
        )
    }

    @Test
    fun `an uncompleted window is always armed`() {
        assertFalse(shouldSkip(morning, LocalTime(8, 30), LocalTime(7, 0), emptyList()))
        assertFalse(shouldSkip(morning, LocalTime(8, 30), LocalTime(9, 0), emptyList()))
    }

    @Test
    fun `completing one window does not silence the other`() {
        // The afternoon is done; the morning nudge must still be armed for tomorrow.
        assertFalse(
            shouldSkip(morning, LocalTime(8, 30), LocalTime(16, 0), listOf("afternoon"))
        )
    }

    @Test
    fun `the slot exactly now is treated as passed`() {
        // Not "still ahead", so it arms tomorrow rather than being skipped. Firing
        // a notification at the very instant of submission would be noise.
        assertFalse(
            shouldSkip(afternoon, LocalTime(15, 30), LocalTime(15, 30), listOf("afternoon"))
        )
    }

    @Test
    fun `three-day candidate dates generate consecutive days starting from today`() {
        val today = kotlinx.datetime.LocalDate(2026, 6, 1)
        val candidateDates = listOf(
            today,
            today.plus(1, kotlinx.datetime.DateTimeUnit.DAY),
            today.plus(2, kotlinx.datetime.DateTimeUnit.DAY)
        )
        assertEquals(3, candidateDates.size)
        assertEquals(kotlinx.datetime.LocalDate(2026, 6, 1), candidateDates[0])
        assertEquals(kotlinx.datetime.LocalDate(2026, 6, 2), candidateDates[1])
        assertEquals(kotlinx.datetime.LocalDate(2026, 6, 3), candidateDates[2])
    }

    @Test
    fun `completing today window skips today but arms tomorrow and day after tomorrow`() {
        val today = kotlinx.datetime.LocalDate(2026, 6, 1)
        val candidateDates = listOf(
            today,
            today.plus(1, kotlinx.datetime.DateTimeUnit.DAY),
            today.plus(2, kotlinx.datetime.DateTimeUnit.DAY)
        )
        val completedWindows = listOf("morning")

        val scheduledDates = candidateDates.filterNot { date ->
            date == today && "morning" in completedWindows
        }

        assertEquals(2, scheduledDates.size)
        assertEquals(kotlinx.datetime.LocalDate(2026, 6, 2), scheduledDates[0])
        assertEquals(kotlinx.datetime.LocalDate(2026, 6, 3), scheduledDates[1])
    }

    @Test
    fun `late-day launch arms tomorrow and day after tomorrow for all windows`() {
        val today = kotlinx.datetime.LocalDate(2026, 6, 1)
        val candidateDates = listOf(
            today,
            today.plus(1, kotlinx.datetime.DateTimeUnit.DAY),
            today.plus(2, kotlinx.datetime.DateTimeUnit.DAY)
        )
        val nowLocalTime = LocalTime(21, 0) // After all windows close

        for (window in listOf(morning, afternoon)) {
            val validDates = candidateDates.mapNotNull { date ->
                val notBefore = if (date == today) nowLocalTime else null
                val drawn = RandomWindowTime.draw(window, notBefore = notBefore)
                if (drawn != null) date to drawn else null
            }
            // Today returns null because 21:00 is after close, but tomorrow and day 3 draw valid times
            assertEquals(2, validDates.size, "Window ${window.name} should have 2 valid future dates")
            assertEquals(kotlinx.datetime.LocalDate(2026, 6, 2), validDates[0].first)
            assertEquals(kotlinx.datetime.LocalDate(2026, 6, 3), validDates[1].first)
        }
    }

    @Test
    fun `the drawn time lands inside the window`() {
        // Guards the registration time itself, not just the skip decision.
        for (window in listOf(morning, afternoon)) {
            repeat(200) {
                val drawn = RandomWindowTime.draw(window)
                assertTrue(drawn != null, "${window.name} produced no time")
                assertTrue(
                    drawn.minuteOfDay() >= window.openTime.minuteOfDay(),
                    "${window.name} drew $drawn before open"
                )
                assertTrue(
                    drawn.minuteOfDay() < window.closeTime.minuteOfDay(),
                    "${window.name} drew $drawn after close"
                )
            }
        }
    }
}
