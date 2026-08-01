package com.lemurs.lemurs_app.util

import com.lemurs.lemurs_app.survey.SurveyWindow
import kotlinx.datetime.LocalTime
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RandomWindowTimeTest {

    private val morning = SurveyWindow("morning", LocalTime(8, 0), LocalTime(13, 0), 0)
    private val afternoon = SurveyWindow("afternoon", LocalTime(15, 0), LocalTime(20, 0), 1)

    @Test
    fun `a drawn time always leaves room for both reminders`() {
        // The whole point of bounding the draw: a notification at 12:55 would put
        // its final reminder after the window had closed.
        val latestAcceptable =
            morning.closeTime.minuteOfDay() - NotificationPlanner.FINAL_REMINDER_MINUTES.toInt()

        repeat(2000) {
            val drawn = assertNotNull(RandomWindowTime.draw(morning))
            val minute = drawn.minuteOfDay()
            assertTrue(minute >= morning.openTime.minuteOfDay(), "drew $drawn before open")
            assertTrue(minute <= latestAcceptable, "drew $drawn too late for reminders")
        }
    }

    @Test
    fun `the draw actually varies`() {
        // A fixed time is the behaviour being replaced, so this is the property
        // that matters: participants must not all be nudged at the same instant.
        val drawn = List(200) { RandomWindowTime.draw(morning)?.toString() }.toSet()
        assertTrue(drawn.size > 20, "expected a spread of times, got ${drawn.size} distinct")
    }

    @Test
    fun `the whole usable window is reachable`() {
        val seen = List(5000) { RandomWindowTime.draw(morning)!!.minuteOfDay() }
        val open = morning.openTime.minuteOfDay()
        val latest =
            morning.closeTime.minuteOfDay() - NotificationPlanner.FINAL_REMINDER_MINUTES.toInt()

        // Both ends inclusive, so 08:00 and 11:15 can both be chosen.
        assertEquals(open, seen.min(), "the open time should be reachable")
        assertEquals(latest, seen.max(), "the latest usable time should be reachable")
    }

    @Test
    fun `notBefore keeps the draw in the future`() {
        // Drawing for today when the window is already open: 08:00 has passed.
        val now = LocalTime(10, 0)
        repeat(500) {
            val drawn = assertNotNull(RandomWindowTime.draw(morning, notBefore = now))
            assertTrue(drawn.minuteOfDay() >= now.minuteOfDay(), "drew $drawn in the past")
        }
    }

    @Test
    fun `returns null once too little of the window remains`() {
        // 12:00 leaves an hour, less than the 105 minutes a full set needs.
        assertNull(RandomWindowTime.draw(morning, notBefore = LocalTime(12, 0)))
    }

    @Test
    fun `returns null after the window has closed`() {
        assertNull(RandomWindowTime.draw(morning, notBefore = LocalTime(14, 0)))
    }

    @Test
    fun `a window shorter than the reminder set still yields its open time`() {
        val brief = SurveyWindow("brief", LocalTime(9, 0), LocalTime(9, 30), 3)
        val drawn = assertNotNull(RandomWindowTime.draw(brief))
        assertEquals(brief.openTime, drawn, "should clamp to the open time")
    }

    @Test
    fun `a malformed window yields nothing`() {
        val backwards = SurveyWindow("backwards", LocalTime(20, 0), LocalTime(8, 0), 9)
        assertNull(RandomWindowTime.draw(backwards))
    }

    @Test
    fun `each window draws within its own bounds`() {
        repeat(500) {
            val m = assertNotNull(RandomWindowTime.draw(morning))
            val a = assertNotNull(RandomWindowTime.draw(afternoon))
            assertTrue(m.minuteOfDay() in 480..675, "morning drew $m")   // 08:00-11:15
            assertTrue(a.minuteOfDay() in 900..1095, "afternoon drew $a") // 15:00-18:15
        }
    }

    @Test
    fun `the draw is reproducible for a given random source`() {
        // Persisting the choice relies on the same seed giving the same answer.
        val first = RandomWindowTime.draw(morning, random = Random(42))
        val second = RandomWindowTime.draw(morning, random = Random(42))
        assertEquals(first, second)
    }
}
