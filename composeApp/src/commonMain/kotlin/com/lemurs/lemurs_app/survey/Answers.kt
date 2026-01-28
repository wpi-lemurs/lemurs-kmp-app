package com.lemurs.lemurs_app.survey

import co.touchlab.kermit.Logger
import io.ktor.client.statement.HttpResponse
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable


@Serializable
data class SurveySubmission(
    val timestamp: Instant,
    val surveys: List<CompletedSurveys>,
    val notificationStart : Instant
)

@Serializable
data class DemographicsSubmission(
    val submission: ArrayList<Demographic>
)

@Serializable
data class Demographic(
    val keyword: String,
    val value: String
)

@Serializable
data class CompletedSurveys(
    val id: Int,
    val answers: List<Answers>
)

@Serializable
data class Answers(
    val id: Int,
    val answer: String // we have answers with open response or with numbers, i feel like we should just make it a string?
)


suspend fun postDailySurvey(surveySubmission: SurveySubmission): HttpResponse {
    val api = SurveysApiImpl()
    return api.postDailySurvey(surveySubmission)
}

suspend fun postWeeklySurvey(surveySubmission: SurveySubmission): HttpResponse {
    val api = SurveysApiImpl()

    return api.postWeeklySurvey(surveySubmission)
}
suspend fun postDemographics(demographicsSubmission: DemographicsSubmission): HttpResponse {
    Logger.withTag("DemographicsViewModel").w("demographics submissions from answers: "+ demographicsSubmission)
    val api = SurveysApiImpl()
    return api.postDemographics(demographicsSubmission)
}
