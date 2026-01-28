package com.lemurs.lemurs_app.survey

import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.data.api.WebAPIAuthorizationService
import com.lemurs.lemurs_app.util.Constants
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path

class SurveysApiImpl() : SurveysApi {
    val client = WebAPIAuthorizationService().getHttpClient()

    override suspend fun getDailySurvey(): List<Surveys> {
        return client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.LEMURS_API_URL
                path("/api/survey/daily")
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
        val response =  client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.LEMURS_API_URL
                path("/api/demographic")
            }
        }
        val body = response.body<List<Demographic>>()
        Logger.withTag("MSAuth").w("demographics from impl: " + body.toString())
        return body
    }

    override suspend fun getSurveyAvailability(): List<SurveyAvailability> {
        return client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.LEMURS_API_URL
                path("/api/survey/available")
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
