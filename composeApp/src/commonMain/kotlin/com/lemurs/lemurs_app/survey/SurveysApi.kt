package com.lemurs.lemurs_app.survey

import io.ktor.client.statement.HttpResponse
import com.lemurs.lemurs_app.survey.DangerAlertTrigger

interface SurveysApi {
    /** The questions for one window, identified by the name the status endpoint reported. */
    suspend fun getDailySurvey(windowName: String): List<Surveys>
    suspend fun getWeeklySurvey(): List<Surveys>
    suspend fun getDangerAlertTriggers(): List<DangerAlertTrigger>
    suspend fun postDailySurvey(surveySubmission: SurveySubmission): HttpResponse
    suspend fun postWeeklySurvey(surveySubmission: SurveySubmission): HttpResponse
    suspend fun postDemographics(demographicsSubmission: DemographicsSubmission): HttpResponse
    suspend fun getDemographics(): List<Demographic>

    /**
     * The survey windows plus which of them today's local date has already seen submitted.
     *
     * [localDate] and [tzId] are the phone's own date and zone: the server uses them to decide
     * which submissions count as "today" for this participant, and never consults its own clock.
     */
    suspend fun getSurveyStatus(localDate: String, tzId: String): SurveyStatus
    suspend fun getProgress(): Progress //i know this does not really belong in surveys api, but i wanted to keep it in one interface
}
