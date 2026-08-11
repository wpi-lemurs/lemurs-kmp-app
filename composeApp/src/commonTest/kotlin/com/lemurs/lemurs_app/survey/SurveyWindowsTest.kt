package com.lemurs.lemurs_app.survey

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SurveyWindowsTest {

    private val eastern = TimeZone.of("America/New_York")
    private val kolkata = TimeZone.of("Asia/Kolkata")

    private val morning = SurveyWindow("morning", LocalTime(8, 0), LocalTime(13, 0), 0)
    private val afternoon = SurveyWindow("afternoon", LocalTime(15, 0), LocalTime(20, 0), 1)
    private val windows = listOf(morning, afternoon)

    /** The wall-clock moment [time] on [date] as experienced in [zone]. */
    private fun at(date: String, time: LocalTime, zone: TimeZone) =
        LocalDateTime(
            kotlinx.datetime.LocalDate.parse(date),
            time
        ).toInstant(zone)

    // --- The bug this whole change exists to fix ---------------------------

    @Test
    fun `a Kolkata morning is outside the Eastern window but still opens locally`() {
        // 09:00 in Kolkata is 23:30 the previous day in New York. Under the old
        // server-side logic the window was evaluated in Eastern, so this
        // participant was told the survey was closed during their own morning.
        val nineAmKolkata = at("2026-03-10", LocalTime(9, 0), kolkata)

        assertEquals(morning, SurveyWindows.currentWindow(windows, nineAmKolkata, kolkata))

        // Same instant, judged in Eastern: closed. That disagreement was the bug.
        assertNull(SurveyWindows.currentWindow(windows, nineAmKolkata, eastern))
    }

    @Test
    fun `participants in different zones both open the morning survey at their own 8am`() {
        for (zone in listOf(eastern, kolkata, TimeZone.of("America/Los_Angeles"))) {
            val eightLocal = at("2026-06-15", LocalTime(8, 0), zone)
            assertEquals(
                morning,
                SurveyWindows.currentWindow(windows, eightLocal, zone),
                "morning should be open at 08:00 in $zone"
            )
        }
    }

    // --- Boundaries -------------------------------------------------------

    @Test
    fun `open time is inclusive`() {
        val exactlyOpen = at("2026-06-15", LocalTime(8, 0), eastern)
        assertEquals(morning, SurveyWindows.currentWindow(windows, exactlyOpen, eastern))
    }

    @Test
    fun `close time is exclusive`() {
        val exactlyClose = at("2026-06-15", LocalTime(13, 0), eastern)
        assertNull(SurveyWindows.currentWindow(windows, exactlyClose, eastern))
    }

    @Test
    fun `one second before close is still open`() {
        val justBefore = at("2026-06-15", LocalTime(12, 59, 59), eastern)
        assertEquals(morning, SurveyWindows.currentWindow(windows, justBefore, eastern))
    }

    @Test
    fun `the gap between windows is closed`() {
        val twoPm = at("2026-06-15", LocalTime(14, 0), eastern)
        assertNull(SurveyWindows.currentWindow(windows, twoPm, eastern))
    }

    @Test
    fun `a malformed window is never open`() {
        val backwards = SurveyWindow("backwards", LocalTime(20, 0), LocalTime(8, 0), 9)
        val noon = at("2026-06-15", LocalTime(12, 0), eastern)
        assertNull(SurveyWindows.currentWindow(listOf(backwards), noon, eastern))
    }

    // --- Countdowns -------------------------------------------------------

    @Test
    fun `secondsUntilClose counts down within the window`() {
        val noon = at("2026-06-15", LocalTime(12, 0), eastern)
        assertEquals(3600L, SurveyWindows.secondsUntilClose(morning, noon, eastern))
    }

    @Test
    fun `secondsUntilNextOpen finds the afternoon window later the same day`() {
        val twoPm = at("2026-06-15", LocalTime(14, 0), eastern)
        assertEquals(3600L, SurveyWindows.secondsUntilNextOpen(windows, twoPm, eastern))
    }

    @Test
    fun `secondsUntilNextOpen rolls over to tomorrow after the last window closes`() {
        val ninePm = at("2026-06-15", LocalTime(21, 0), eastern)
        // 21:00 today to 08:00 tomorrow is 11 hours.
        assertEquals(11 * 3600L, SurveyWindows.secondsUntilNextOpen(windows, ninePm, eastern))
    }

    @Test
    fun `secondsUntilNextOpen is null when there are no windows`() {
        val noon = at("2026-06-15", LocalTime(12, 0), eastern)
        assertNull(SurveyWindows.secondsUntilNextOpen(emptyList(), noon, eastern))
    }

    // --- Daylight saving --------------------------------------------------

    @Test
    fun `spring forward still opens at 8am wall-clock the next morning`() {
        // 2026-03-08 is the US spring-forward date: the clock jumps 02:00 -> 03:00,
        // so the span from 21:00 the night before to 08:00 is only 10 real hours.
        // A countdown that assumed a fixed 11h gap would tell the participant the
        // survey opens an hour later than it actually does.
        val nightBefore = at("2026-03-07", LocalTime(21, 0), eastern)
        val seconds = SurveyWindows.secondsUntilNextOpen(windows, nightBefore, eastern)

        assertEquals(10 * 3600L, seconds, "spring-forward night is an hour shorter")

        // The countdown must land exactly on 08:00 local, not 07:00 or 09:00.
        val opensAt = nightBefore.plus(seconds!!, DateTimeUnit.SECOND)
        assertEquals(LocalTime(8, 0), SurveyWindows.nowLocalTime(opensAt, eastern))
    }

    @Test
    fun `fall back still opens at 8am wall-clock the next morning`() {
        // 2026-11-01 is the US fall-back date: 01:00-02:00 happens twice, so the
        // same span is 12 real hours.
        val nightBefore = at("2026-10-31", LocalTime(21, 0), eastern)
        val seconds = SurveyWindows.secondsUntilNextOpen(windows, nightBefore, eastern)

        assertEquals(12 * 3600L, seconds, "fall-back night is an hour longer")

        val opensAt = nightBefore.plus(seconds!!, DateTimeUnit.SECOND)
        assertEquals(LocalTime(8, 0), SurveyWindows.nowLocalTime(opensAt, eastern))
    }

    // --- evaluate() -------------------------------------------------------

    @Test
    fun `evaluate reports Open when the window is live and nothing submitted`() {
        val nineAm = at("2026-06-15", LocalTime(9, 0), eastern)
        val state = SurveyWindows.evaluate(windows, emptyList(), nineAm, eastern)

        val open = assertIs<SurveyWindowState.Open>(state)
        assertEquals(morning, open.window)
        assertEquals(4 * 3600L, open.secondsUntilClose)
    }

    @Test
    fun `evaluate reports AlreadyCompleted once that window is submitted`() {
        val nineAm = at("2026-06-15", LocalTime(9, 0), eastern)
        val state = SurveyWindows.evaluate(windows, listOf("morning"), nineAm, eastern)

        val done = assertIs<SurveyWindowState.AlreadyCompleted>(state)
        assertEquals(morning, done.window)
        // Next open is the afternoon window at 15:00, six hours out.
        assertEquals(6 * 3600L, done.secondsUntilNextOpen)
    }

    @Test
    fun `completing the morning does not close the afternoon`() {
        val fourPm = at("2026-06-15", LocalTime(16, 0), eastern)
        val state = SurveyWindows.evaluate(windows, listOf("morning"), fourPm, eastern)

        val open = assertIs<SurveyWindowState.Open>(state)
        assertEquals(afternoon, open.window)
    }

    @Test
    fun `evaluate reports Closed outside every window`() {
        val twoPm = at("2026-06-15", LocalTime(14, 0), eastern)
        val state = SurveyWindows.evaluate(windows, emptyList(), twoPm, eastern)

        val closed = assertIs<SurveyWindowState.Closed>(state)
        assertEquals(3600L, closed.secondsUntilNextOpen)
    }

    @Test
    fun `local date is the participants own date, not the servers`() {
        // 07:00 in Kolkata on the 16th is still the 15th in New York.
        val earlyKolkata = at("2026-06-16", LocalTime(7, 0), kolkata)

        assertEquals("2026-06-16", SurveyWindows.localDate(earlyKolkata, kolkata).toString())
        assertEquals("2026-06-15", SurveyWindows.localDate(earlyKolkata, eastern).toString())
    }

    @Test
    fun `windows are matched by name so ids may be reassigned freely`() {
        val renumbered = listOf(morning.copy(surveyId = 7))
        val nineAm = at("2026-06-15", LocalTime(9, 0), eastern)
        val state = SurveyWindows.evaluate(renumbered, listOf("morning"), nineAm, eastern)
        assertIs<SurveyWindowState.AlreadyCompleted>(state)
    }

    @Test
    fun `a well-formed window is recognised as such`() {
        assertTrue(morning.isWellFormed)
        assertTrue(afternoon.isWellFormed)
    }

    @Test
    fun `evaluate returns StudyConcluded when studyConcluded is true`() {
        val nineAm = at("2026-06-15", LocalTime(9, 0), eastern)
        val state = SurveyWindows.evaluate(windows, emptyList(), studyConcluded = true, at = nineAm, zone = eastern)
        assertIs<SurveyWindowState.StudyConcluded>(state)
    }
}
