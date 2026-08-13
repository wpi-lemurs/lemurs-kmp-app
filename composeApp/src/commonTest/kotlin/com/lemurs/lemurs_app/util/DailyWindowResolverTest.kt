package com.lemurs.lemurs_app.util

import com.lemurs.lemurs_app.survey.SurveyStatus
import com.lemurs.lemurs_app.survey.SurveyWindow
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DailyWindowResolverTest {

    private val morning = SurveyWindow("morning", LocalTime(8, 0), LocalTime(13, 0), 0)
    private val afternoon = SurveyWindow("afternoon", LocalTime(15, 0), LocalTime(20, 0), 1)
    private val cached = listOf(morning, afternoon)

    // --- The study is over -------------------------------------------------

    /**
     * The case the emulator test in the handoff could not reach.
     *
     * That test used a fresh login, so the cache was empty and the worker did nothing for the right
     * reason by accident. A participant who reaches day 29 normally has a cache populated on day 28,
     * and an empty `windows` list is indistinguishable from a failed fetch unless `studyConcluded`
     * is consulted.
     */
    @Test
    fun `a concluded study does not fall back to cached windows`() {
        val status = SurveyStatus(windows = emptyList(), studyConcluded = true)

        val source = DailyWindowResolver.resolve(status, cachedWindows = cached)

        assertIs<DailyWindowSource.StudyConcluded>(source)
    }

    /** A concluded study wins even if the server still lists windows, since it is the stronger claim. */
    @Test
    fun `a concluded study wins over any windows the server still reports`() {
        val status = SurveyStatus(windows = cached, studyConcluded = true)

        val source = DailyWindowResolver.resolve(status, cachedWindows = cached)

        assertIs<DailyWindowSource.StudyConcluded>(source)
    }

    /**
     * The names are what the teardown cancels per-window alarms by. A concluded study reports no
     * windows, so the cache is the only place left to learn them.
     */
    @Test
    fun `a concluded study reports the window names it knows from the cache`() {
        val status = SurveyStatus(windows = emptyList(), studyConcluded = true)

        val source = assertIs<DailyWindowSource.StudyConcluded>(
            DailyWindowResolver.resolve(status, cachedWindows = cached)
        )

        assertEquals(listOf("morning", "afternoon"), source.knownWindowNames)
    }

    @Test
    fun `window names are not duplicated when the server and cache agree`() {
        val status = SurveyStatus(windows = cached, studyConcluded = true)

        val source = assertIs<DailyWindowSource.StudyConcluded>(
            DailyWindowResolver.resolve(status, cachedWindows = cached)
        )

        assertEquals(listOf("morning", "afternoon"), source.knownWindowNames)
    }

    /** Concluding must be distinguishable from "ask again tomorrow". */
    @Test
    fun `a concluded study is not reported as unknown`() {
        val status = SurveyStatus(windows = emptyList(), studyConcluded = true)

        val source = DailyWindowResolver.resolve(status, cachedWindows = emptyList())

        assertIs<DailyWindowSource.StudyConcluded>(source)
    }

    // --- The study is running ----------------------------------------------

    @Test
    fun `live windows are used and marked as not from cache`() {
        val status = SurveyStatus(windows = cached, studyConcluded = false)

        val source = assertIs<DailyWindowSource.Plan>(
            DailyWindowResolver.resolve(status, cachedWindows = emptyList())
        )

        assertEquals(cached, source.windows)
        assertEquals(false, source.fromCache)
    }

    /**
     * The offline path, which must keep working. This runs before dawn, when connectivity is least
     * reliable, and the cache is the only reason a schedule survives a night with no network.
     */
    @Test
    fun `a failed fetch falls back to the cache`() {
        val source = assertIs<DailyWindowSource.Plan>(
            DailyWindowResolver.resolve(status = null, cachedWindows = cached)
        )

        assertEquals(cached, source.windows)
        assertEquals(true, source.fromCache)
    }

    @Test
    fun `a failed fetch with no cache is unknown rather than concluded`() {
        val source = DailyWindowResolver.resolve(status = null, cachedWindows = emptyList())

        assertIs<DailyWindowSource.Unknown>(source)
    }

    /**
     * An empty window list from a *running* study still means "nothing today", not "stop" — the
     * cache is the right answer, since the server may simply have no availability rows configured.
     */
    @Test
    fun `an empty window list from a running study still uses the cache`() {
        val status = SurveyStatus(windows = emptyList(), studyConcluded = false)

        val source = assertIs<DailyWindowSource.Plan>(
            DailyWindowResolver.resolve(status, cachedWindows = cached)
        )

        assertEquals(true, source.fromCache)
    }
}
