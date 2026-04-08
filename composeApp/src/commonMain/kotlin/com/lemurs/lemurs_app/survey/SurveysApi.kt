package com.lemurs.lemurs_app.survey

import io.ktor.client.statement.HttpResponse
import com.lemurs.lemurs_app.survey.DangerAlertTrigger

interface SurveysApi {
    suspend fun getDailySurvey(): List<Surveys>
    suspend fun getWeeklySurvey(): List<Surveys>
    suspend fun getDangerAlertTriggers(): List<DangerAlertTrigger>
    suspend fun postDailySurvey(surveySubmission: SurveySubmission): HttpResponse
    suspend fun postWeeklySurvey(surveySubmission: SurveySubmission): HttpResponse
    suspend fun postDemographics(demographicsSubmission: DemographicsSubmission): HttpResponse
    suspend fun getDemographics(): List<Demographic>
    suspend fun getSurveyAvailability(): List<SurveyAvailability>
    suspend fun getProgress(): Progress //i know this does not really belong in surveys api, but i wanted to keep it in one interface
}
