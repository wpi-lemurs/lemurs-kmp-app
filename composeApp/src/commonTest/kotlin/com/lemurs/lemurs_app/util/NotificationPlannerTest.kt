package com.lemurs.lemurs_app.util

import com.lemurs.lemurs_app.survey.SurveyWindow
import kotlinx.datetime.LocalTime
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NotificationPlannerTest {

    private val morning = SurveyWindow("morning", LocalTime(8, 0), LocalTime(13, 0), 0)
    private val afternoon = SurveyWindow("afternoon", LocalTime(15, 0), LocalTime(20, 0), 1)
    private val windows = listOf(morning, afternoon)

    /** Deterministic so the "random" start time is assertable. */
    private fun fixedRandom(value: Int) = object : Random() {
        override fun nextBits(bitCount: Int): Int = value
        override fun nextInt(until: Int): Int = value % until
    }

    // --- Before the window opens ------------------------------------------

    @Test
    fun `schedules a notification inside the window when planned before it opens`() {
        val plans = NotificationPlanner.plan(windows, LocalTime(6, 30), fixedRandom(45))

        val morningPlan = assertIs<WindowNotificationPlan.Notify>(plans.first { it.windowName == "morning" })
        // 08:00 + 45 minutes
        assertEquals(LocalTime(8, 45), morningPlan.atLocalTime)
        assertEquals(60L, morningPlan.firstReminderDelayMinutes)
        assertEquals(105L, morningPlan.finalReminderDelayMinutes)
    }

    @Test
    fun `the random start always leaves room for both reminders`() {
        // Sweep every possible random draw and check the invariant holds.
        for (draw in 0 until 300) {
            val plans = NotificationPlanner.plan(listOf(morning), LocalTime(6, 30), fixedRandom(draw))
            val plan = assertIs<WindowNotificationPlan.Notify>(plans.single())

            val startMinute = plan.atLocalTime.minuteOfDay()
            val finalReminder = startMinute + plan.finalReminderDelayMinutes
            assertTrue(
                finalReminder <= morning.closeTime.minuteOfDay(),
                "final reminder at $finalReminder overruns close for draw $draw"
            )
            assertTrue(startMinute >= morning.openTime.minuteOfDay(), "start before open for draw $draw")
        }
    }

    // --- The window is already open ---------------------------------------

    @Test
    fun `notifies immediately when setup runs after the window opened`() {
        // 09:00, well inside the morning window: nudge now rather than waiting.
        val plans = NotificationPlanner.plan(listOf(morning), LocalTime(9, 0), fixedRandom(0))

        val plan = assertIs<WindowNotificationPlan.Notify>(plans.single())
        assertEquals(LocalTime(9, 0), plan.atLocalTime)
        assertEquals(105L, plan.finalReminderDelayMinutes)
    }

    @Test
    fun `reminders are clamped so they never fire after the window closes`() {
        // 11:30 leaves 90 minutes, less than the usual 105.
        val plans = NotificationPlanner.plan(listOf(morning), LocalTime(11, 30), fixedRandom(0))

        val plan = assertIs<WindowNotificationPlan.Notify>(plans.single())
        assertEquals(60L, plan.firstReminderDelayMinutes)
        assertEquals(90L, plan.finalReminderDelayMinutes, "should be clamped to the close")
    }

    @Test
    fun `sends a last chance when too little of the window remains for reminders`() {
        // 12:00 leaves 60 minutes: not enough for a 105-minute set, but worth one call.
        val plans = NotificationPlanner.plan(listOf(morning), LocalTime(12, 0), fixedRandom(0))
        assertIs<WindowNotificationPlan.LastChance>(plans.single())
    }

    @Test
    fun `skips a window that is nearly closed`() {
        // 12:45 leaves 15 minutes, under the 30-minute margin.
        val plans = NotificationPlanner.plan(listOf(morning), LocalTime(12, 45), fixedRandom(0))
        val skip = assertIs<WindowNotificationPlan.Skip>(plans.single())
        assertTrue(skip.reason.contains("30"))
    }

    @Test
    fun `skips a window that has already closed`() {
        val plans = NotificationPlanner.plan(listOf(morning), LocalTime(14, 0), fixedRandom(0))
        val skip = assertIs<WindowNotificationPlan.Skip>(plans.single())
        assertEquals("window already closed", skip.reason)
    }

    @Test
    fun `plans each window independently`() {
        // 14:00: morning is over, afternoon has not started.
        val plans = NotificationPlanner.plan(windows, LocalTime(14, 0), fixedRandom(30))

        assertIs<WindowNotificationPlan.Skip>(plans.first { it.windowName == "morning" })
        val afternoonPlan = assertIs<WindowNotificationPlan.Notify>(plans.first { it.windowName == "afternoon" })
        assertEquals(LocalTime(15, 30), afternoonPlan.atLocalTime)
    }

    // --- The drift this replaces ------------------------------------------

    @Test
    fun `the morning window stays live until 13 00 rather than 11 00`() {
        // The old code treated 11:00 as the morning cutoff and skipped the
        // notification, while the database said the window ran to 13:00. A
        // participant opening the app at 11:30 had a live survey and no nudge.
        val plans = NotificationPlanner.plan(listOf(morning), LocalTime(11, 15), fixedRandom(0))
        assertIs<WindowNotificationPlan.Notify>(plans.single())
    }

    @Test
    fun `the afternoon window stays live until 20 00 rather than 18 00`() {
        val plans = NotificationPlanner.plan(listOf(afternoon), LocalTime(18, 10), fixedRandom(0))
        assertIs<WindowNotificationPlan.Notify>(plans.single())
    }

    @Test
    fun `changing the window in the database moves the notifications with it`() {
        val shifted = listOf(SurveyWindow("morning", LocalTime(10, 0), LocalTime(14, 0), 0))
        val plans = NotificationPlanner.plan(shifted, LocalTime(6, 0), fixedRandom(20))

        val plan = assertIs<WindowNotificationPlan.Notify>(plans.single())
        assertEquals(LocalTime(10, 20), plan.atLocalTime, "should follow the new open time")
    }

    // --- Edge cases -------------------------------------------------------

    @Test
    fun `a window shorter than the reminder set still gets an opening nudge`() {
        val brief = listOf(SurveyWindow("brief", LocalTime(9, 0), LocalTime(10, 0), 3))
        val plans = NotificationPlanner.plan(brief, LocalTime(7, 0), fixedRandom(0))

        val plan = assertIs<WindowNotificationPlan.Notify>(plans.single())
        assertEquals(LocalTime(9, 0), plan.atLocalTime)
        // Both reminders clamp to the 60-minute window.
        assertEquals(60L, plan.firstReminderDelayMinutes)
        assertEquals(60L, plan.finalReminderDelayMinutes)
    }

    @Test
    fun `a malformed window is skipped rather than scheduled`() {
        val backwards = listOf(SurveyWindow("backwards", LocalTime(20, 0), LocalTime(8, 0), 9))
        val plans = NotificationPlanner.plan(backwards, LocalTime(9, 0), fixedRandom(0))

        val skip = assertIs<WindowNotificationPlan.Skip>(plans.single())
        assertEquals("window is not well formed", skip.reason)
    }

    @Test
    fun `an empty window list produces no plans`() {
        assertTrue(NotificationPlanner.plan(emptyList(), LocalTime(9, 0), fixedRandom(0)).isEmpty())
    }

    @Test
    fun `minute of day round trips`() {
        for (minute in intArrayOf(0, 1, 59, 60, 480, 780, 900, 1200, 1439)) {
            assertEquals(minute, LocalTime.fromMinuteOfDay(minute).minuteOfDay())
        }
    }
}
