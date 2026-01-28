package com.lemurs.lemurs_app.survey

import co.touchlab.kermit.Logger
import kotlinx.serialization.Serializable


@Serializable
data class Questions(
    val id: Int,
    val question: String,
    val style: String,
    val options: List<String?>?,
    val parentQuestionId: Int?,
    val prerequisiteQuestionId: Int?,
    val prerequisiteAnswer: String?,
    val isTriggerQuestion: Boolean,
    val triggerThreshold: Int?
)

@Serializable
data class Surveys(
    val id: Int,
    val name: String,
    val questions: List<Questions>
)

suspend fun fetchAndParseDailySurvey(): List<Surveys> {
    val api = SurveysApiImpl()

    return api.getDailySurvey()
}

suspend fun fetchAndParseWeeklySurvey(): List<Surveys> {
    val api = SurveysApiImpl()
    return api.getWeeklySurvey()
}

suspend fun fetchDemographic() :List<Demographic>{
    Logger.withTag("Questions").w("fetching demographic")
    val api = SurveysApiImpl()
    val dem = api.getDemographics()

    Logger.withTag("Questions").w("fetched demographic in Questions"+ dem.toString())
    return dem
}
