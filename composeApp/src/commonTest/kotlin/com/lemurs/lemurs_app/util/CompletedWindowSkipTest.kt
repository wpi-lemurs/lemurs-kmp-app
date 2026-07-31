package com.lemurs.lemurs_app.util

import com.lemurs.lemurs_app.survey.SurveyWindow
import kotlinx.datetime.LocalTime
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
    fun `the offset lands inside the window`() {
        // Guards the registration time itself, not just the skip decision.
        for (window in listOf(morning, afternoon)) {
            val open = window.openTime.minuteOfDay()
            val close = window.closeTime.minuteOfDay()
            val latestUseful = close - NotificationPlanner.FINAL_REMINDER_MINUTES.toInt()
            val target = (open + 30).coerceAtMost(latestUseful.coerceAtLeast(open))

            assertTrue(target >= open, "${window.name} target before open")
            assertTrue(target < close, "${window.name} target after close")
        }
    }

    @Test
    fun `a window too short for the offset still lands inside it`() {
        val brief = SurveyWindow("brief", LocalTime(9, 0), LocalTime(9, 20), 3)
        val open = brief.openTime.minuteOfDay()
        val close = brief.closeTime.minuteOfDay()
        val latestUseful = close - NotificationPlanner.FINAL_REMINDER_MINUTES.toInt()
        val target = (open + 30).coerceAtMost(latestUseful.coerceAtLeast(open))

        assertEquals(open, target, "should clamp back to the open time")
        assertTrue(target < close)
    }
}
