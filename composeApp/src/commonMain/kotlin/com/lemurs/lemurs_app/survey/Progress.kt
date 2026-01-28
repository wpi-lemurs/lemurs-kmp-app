package com.lemurs.lemurs_app.survey

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable


@Serializable
data class Progress(
    val earned: Double,
    val dailySurveysTotalCompleted: Int,
    val dailySurveysTotalGoal: Int,
    val dailySurveysTotalBonus: Double,
    val dailySurveysWeeklyCompleted: Int,
    val dailySurveysWeeklyGoal: Int,
    val dailySurveysWeeklyBonus: Double
)


suspend fun fetchAndParseProgress(): Progress {
    val api = SurveysApiImpl()

    return api.getProgress()
}
