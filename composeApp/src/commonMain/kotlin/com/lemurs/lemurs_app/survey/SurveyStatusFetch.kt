package com.lemurs.lemurs_app.survey

import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.util.DemoMode
import kotlinx.datetime.LocalTime

/**
 * Fetches the survey status for the phone's current local date and zone.
 *
 * Returns null when the status could not be retrieved, which callers should treat as "unknown"
 * rather than "closed" — the alternative is telling a participant their survey is unavailable
 * because the network dropped.
 */
suspend fun fetchSurveyStatus(api: SurveysApi = SurveysApiImpl()): SurveyStatus? {
    if (DemoMode.isActive) {
        Logger.withTag("SurveyStatus").d("Demo mode: surveys always open")
        return DEMO_STATUS
    }

    val zone = SurveyWindows.systemZone()
    return try {
        api.getSurveyStatus(SurveyWindows.localDate(zone = zone).toString(), zone.id)
    } catch (e: Exception) {
        Logger.withTag("SurveyStatus").w("Couldn't fetch survey status: ${e.message}")
        null
    }
}

/** A window spanning the whole day, so demos never hit a closed survey. */
private val DEMO_STATUS = SurveyStatus(
    windows = listOf(
        SurveyWindow("morning", LocalTime(0, 0), LocalTime(23, 59, 59), 0)
    ),
    completedWindows = emptyList(),
    weeklyNextAvailable = null
)
