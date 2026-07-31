package com.lemurs.lemurs_app.survey

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins the wire format of `GET /api/survey/status`.
 *
 * These payloads are copied from the deployed dev API. If the server's shape drifts from what the
 * app expects, the failure should surface here rather than as an empty home screen on a phone.
 */
class SurveyStatusSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parses the status payload the dev server returns`() {
        val payload = """
            {
              "windows": [
                {"name":"afternoon","openTime":"15:00:00","closeTime":"20:00:00","surveyId":1},
                {"name":"morning","openTime":"08:00:00","closeTime":"13:00:00","surveyId":0}
              ],
              "completedWindows": [],
              "weeklyNextAvailable": "2025-12-12T18:05:37.281Z"
            }
        """.trimIndent()

        val status = json.decodeFromString<SurveyStatus>(payload)

        assertEquals(2, status.windows.size)
        val morning = status.windows.first { it.name == "morning" }
        assertEquals(LocalTime(8, 0), morning.openTime)
        assertEquals(LocalTime(13, 0), morning.closeTime)
        assertEquals(0, morning.surveyId)
        assertTrue(status.completedWindows.isEmpty())
        assertEquals("2025-12-12T18:05:37.281Z", status.weeklyNextAvailable.toString())
    }

    @Test
    fun `parses completed windows`() {
        val payload = """
            {
              "windows": [{"name":"morning","openTime":"08:00:00","closeTime":"13:00:00","surveyId":0}],
              "completedWindows": ["morning"],
              "weeklyNextAvailable": null
            }
        """.trimIndent()

        val status = json.decodeFromString<SurveyStatus>(payload)

        assertEquals(listOf("morning"), status.completedWindows)
        assertNull(status.weeklyNextAvailable)
    }

    @Test
    fun `tolerates a payload with no weekly instant`() {
        val payload = """
            {"windows":[],"completedWindows":[]}
        """.trimIndent()

        val status = json.decodeFromString<SurveyStatus>(payload)

        assertTrue(status.windows.isEmpty())
        assertNull(status.weeklyNextAvailable)
    }

    @Test
    fun `a completed window drives the UI state end to end`() {
        val payload = """
            {
              "windows": [
                {"name":"morning","openTime":"08:00:00","closeTime":"13:00:00","surveyId":0},
                {"name":"afternoon","openTime":"15:00:00","closeTime":"20:00:00","surveyId":1}
              ],
              "completedWindows": ["morning"],
              "weeklyNextAvailable": null
            }
        """.trimIndent()

        val status = json.decodeFromString<SurveyStatus>(payload)
        val eastern = TimeZone.of("America/New_York")
        val nineAm = LocalDateTime(LocalDate.parse("2026-06-15"), LocalTime(9, 0))
            .toInstant(eastern)

        val state = SurveyWindows.evaluate(
            status.windows,
            status.completedWindows,
            nineAm,
            eastern
        )

        // Previously a completed survey was indistinguishable from a closed one.
        assertTrue(state is SurveyWindowState.AlreadyCompleted)
    }
}
