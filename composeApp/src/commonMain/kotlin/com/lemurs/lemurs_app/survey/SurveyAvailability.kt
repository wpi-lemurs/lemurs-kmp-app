package com.lemurs.lemurs_app.survey

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable


@Serializable
data class SurveyAvailability(
    val type: String,
    val nextAvailable: Instant
)


suspend fun fetchAndParseAvailability(): Map<String, Instant> {
    val api = SurveysApiImpl()
    val availabilities = api.getSurveyAvailability()
    val dict = mutableMapOf<String, Instant>()
    for (availability in availabilities) {
        dict[availability.type] = availability.nextAvailable
    }
    return dict
}
