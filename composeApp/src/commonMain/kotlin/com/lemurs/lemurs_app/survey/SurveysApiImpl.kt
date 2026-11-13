package com.lemurs.lemurs_app.survey

import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.data.api.WebAPIAuthorizationService
import com.lemurs.lemurs_app.util.Constants
import com.lemurs.lemurs_app.survey.DangerAlertTrigger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path

class SurveysApiImpl(
    val client: HttpClient = WebAPIAuthorizationService().getHttpClient()
) : SurveysApi {

    override suspend fun getDailySurvey(windowName: String): List<Surveys> {
        return client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.LEMURS_API_URL
                path("/api/survey/daily")
                parameters.append("windowName", windowName)
            }
        }.body<List<Surveys>>()
    }

    override suspend fun getWeeklySurvey(): List<Surveys> {
        return client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.LEMURS_API_URL
                path("/api/survey/weekly")
            }
        }.body<List<Surveys>>()
    }

    override suspend fun getDangerAlertTriggers(): List<DangerAlertTrigger> {
        val response = client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.LEMURS_API_URL
                path("/api/danger-alert-triggers")
            }
        }

        if (response.status.value !in 200..299) {
            Logger.withTag("SurveysApiImpl").w(
                "Danger alert trigger fetch returned ${response.status.value}; using empty trigger list"
            )
            return emptyList()
        }

        return try {
            response.body<List<DangerAlertTrigger>>()
        } catch (e: Exception) {
            Logger.withTag("SurveysApiImpl").w(
                "Danger alert trigger payload parse failed (${e.message}); body=${response.bodyAsText()}. Using empty trigger list"
            )
            emptyList()
        }
    }

    override suspend fun postDailySurvey(surveySubmission: SurveySubmission): HttpResponse {
        return client.post {
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.LEMURS_API_URL
                path("/api/survey/daily")
            }
            contentType(ContentType.Application.Json)
            setBody(surveySubmission)
        }
    }

    override suspend fun postWeeklySurvey(surveySubmission: SurveySubmission): HttpResponse {
        return client.post {
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.LEMURS_API_URL
                path("/api/survey/weekly")
            }
            contentType(ContentType.Application.Json)
            setBody(surveySubmission)
        }
    }

    override suspend fun postDemographics(demographicsSubmission: DemographicsSubmission): HttpResponse {
        Logger.withTag("DemographicsViewModel").w("demographics submissions from impl: "+ demographicsSubmission.submission)
        return client.post {
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.LEMURS_API_URL
                path("/api/demographic")
            }
            contentType(ContentType.Application.Json)
            setBody(demographicsSubmission.submission)
        }
    }

    override suspend fun getDemographics(): List<Demographic> {
        val response = client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.LEMURS_API_URL
                path("/api/demographic")
            }
        }
        if (response.status.value !in 200..299) {
            Logger.withTag("MSAuth").e(
                "GET /api/demographic failed with HTTP ${response.status.value}; returning empty list"
            )
            return emptyList()
        }
        return try {
            val body = response.body<List<Demographic>>()
            Logger.withTag("MSAuth").w("demographics from impl: $body")
            body
        } catch (e: Exception) {
            Logger.withTag("MSAuth").e(
                "Failed to parse /api/demographic payload (${e.message}); body=${response.bodyAsText()}"
            )
            emptyList()
        }
    }

    override suspend fun getSurveyStatus(localDate: String, tzId: String): SurveyStatus {
        return client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.LEMURS_API_URL
                path("/api/survey/status")
                parameters.append("localDate", localDate)
                parameters.append("tzId", tzId)
            }
        }.body()
    }

    override suspend fun getProgress(): Progress {
        return client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.LEMURS_API_URL
                path("/api/progress")
            }
        }.body()
    }
}
