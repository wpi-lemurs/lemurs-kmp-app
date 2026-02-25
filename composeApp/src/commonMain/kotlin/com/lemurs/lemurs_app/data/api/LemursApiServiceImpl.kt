package com.lemurs.lemurs_app.data.api

import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.util.Constants
import com.lemurs.lemurs_app.data.dtos.CaloriesDataDto
import com.lemurs.lemurs_app.data.dtos.DistanceDataDto
import com.lemurs.lemurs_app.data.dtos.SleepDataDto
import com.lemurs.lemurs_app.data.dtos.SpeedDataDto
import com.lemurs.lemurs_app.data.dtos.StepsDataDto
import com.lemurs.lemurs_app.data.dtos.WeightDataDto
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

class LemursApiServiceImpl(private val client: HttpClient) {
    val logger = Logger.withTag("API Logger")

    /**
     * for submitting data to API
     * use client with authorization and bearer token
     */
    suspend fun submitData(
        inputData: SurveyDataRequest,
        endpoint: String
    ): HttpResponse {
        val response: HttpResponse =
            client.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = Constants.LEMURS_API_URL
                    path("/api$endpoint")
                }
                contentType(ContentType.Application.Json)
                setBody(inputData)
            }
        return response
    }

    suspend fun submitAudioData(inputData: AudioDataRequest, endpoint: String): HttpResponse {
        // Use the authenticated client for audio submission since it requires authorization
        val webAuth = WebAPIAuthorizationService()
        val authenticatedClient = webAuth.getHttpClient()

        // Clean the Base64 string - remove any line breaks or whitespace that might cause validation issues
        val cleanedAudioData = inputData.audioByte64.replace("\n", "").replace("\r", "").trim()

        // Manually create JSON to ensure surveyResponseId is included and Base64 is properly formatted
        val manualJson = """
        {
            "timestamp": "${inputData.timestamp}",
            "audioQuestionId": ${inputData.audioQuestionId},
            "audioByte64": "$cleanedAudioData",
            "surveyResponseId": ${inputData.surveyResponseId}
        }
        """.trimIndent()

        val response: HttpResponse =
            authenticatedClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = Constants.LEMURS_API_URL
                    path("/api$endpoint")
                }
                contentType(ContentType.Application.Json)
                setBody(manualJson)
            }
        return response
    }
    suspend fun sendWeightData(weightRecord: WeightDataDto, endpoint: String): HttpResponse {
        val webAuth = WebAPIAuthorizationService()
        val authenticatedClient = webAuth.getHttpClient()

        val response: HttpResponse =
            authenticatedClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = Constants.LEMURS_API_URL
                    path("/api$endpoint")
                }
                contentType(ContentType.Application.Json)
                setBody(weightRecord)
            }
        return response
    }

    suspend fun sendStepsData(stepsData: StepsDataDto, endpoint: String): HttpResponse {
        val webAuth = WebAPIAuthorizationService()
        val authenticatedClient = webAuth.getHttpClient()

        val response: HttpResponse =
            authenticatedClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = Constants.LEMURS_API_URL
                    path("/api$endpoint")
                }
                contentType(ContentType.Application.Json)
                setBody(stepsData)
            }
        return response
    }

    suspend fun sendCaloriesData(caloriesData: CaloriesDataDto, endpoint: String): HttpResponse {
        val webAuth = WebAPIAuthorizationService()
        val authenticatedClient = webAuth.getHttpClient()

        val response: HttpResponse =
            authenticatedClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = Constants.LEMURS_API_URL
                    path("/api$endpoint")
                }
                contentType(ContentType.Application.Json)
                setBody(caloriesData)
            }
        return response
    }

    suspend fun sendDistanceData(distanceData: DistanceDataDto, endpoint: String): HttpResponse {
        val webAuth = WebAPIAuthorizationService()
        val authenticatedClient = webAuth.getHttpClient()

        val response: HttpResponse =
            authenticatedClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = Constants.LEMURS_API_URL
                    path("/api$endpoint")
                }
                contentType(ContentType.Application.Json)
                setBody(distanceData)
            }
        return response
    }

    suspend fun sendWritingData(url: String, inputData: WrittenResponseRequest): HttpResponse {
        val webAuth = WebAPIAuthorizationService()
        val authenticatedClient = webAuth.getHttpClient()

        // Properly escape the written data for JSON (handles newlines, quotes, etc.)
        val escapedWrittenData = inputData.writtenData
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

        val jsonb = """{"survey_response_id":"${inputData.surveyResponseId}","written_question_id":"${inputData.writtenQuestionId}","written_data":"$escapedWrittenData","timestamp":"${inputData.timestamp}"}"""

        // Remove leading slash from url if present to avoid double slashes
        val cleanUrl = url.trimStart('/')

        val response: HttpResponse =
            authenticatedClient.post("https://${Constants.LEMURS_API_URL}/api/$cleanUrl") {
                contentType(ContentType.Application.Json)
                setBody(jsonb)
            }
        return response
    }
        suspend fun sendSpeedData(speedData: SpeedDataDto, endpoint: String): HttpResponse {
            val webAuth = WebAPIAuthorizationService()
            val authenticatedClient = webAuth.getHttpClient()

            val response: HttpResponse =
                authenticatedClient.post {
                    url {
                        protocol = URLProtocol.HTTPS
                        host = Constants.LEMURS_API_URL
                        path("/api$endpoint")
                    }
                    contentType(ContentType.Application.Json)
                    setBody(speedData)
                }
            return response
        }

        suspend fun sendSleepSessionData(
            sleepSessionData: SleepDataDto,
            url: String
        ): HttpResponse {
            val response: HttpResponse =
                client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(sleepSessionData)
                }
            return response
        }

        /**
         * for submitting microsoft access token to our api to get lemurs access token
         */
        suspend fun submitAccessToken(accessToken: String, endpoint: String): HttpResponse {

            logger.i(
                "Sending access token to host '" + Constants.LEMURS_API_URL
                        + "' with endpoint '" + "/api$endpoint" + "'."
            )
            val response: HttpResponse =
                client.post {
                    url {
                        protocol = URLProtocol.HTTPS
                        host = Constants.LEMURS_API_URL
                        path("/api$endpoint")
                    }
                    contentType(ContentType.Application.Json)
                    setBody(AccessTokenRequest(accessToken))
                }
            return response
        }

        /**
         * for using refresh token to refresh lemurs access token when needed
         */
        suspend fun refreshAccessToken(refreshToken: String, endpoint: String): HttpResponse {

            val response: HttpResponse =
                client.post {
                    url {
                        protocol = URLProtocol.HTTPS
                        host = Constants.LEMURS_API_URL
                        path("/api$endpoint")
                    }
                    contentType(ContentType.Application.Json)
                    setBody(RefreshTokenRequest(refreshToken))
                }
            return response
        }


        @Serializable
        data class AudioDataRequest(
            @SerialName("timestamp") var timestamp: String, // Change from Instant to String to control format
            @SerialName("audioQuestionId") var audioQuestionId: Int,
            @SerialName("audioByte64") var audioByte64: String,
            @SerialName("surveyResponseId") var surveyResponseId: Int
        )

        @Serializable
        data class WrittenResponseRequest(
            @SerialName("survey_response_id") var surveyResponseId: Int,
            @SerialName("written_question_id") var writtenQuestionId: Int,
            @SerialName("written_data") var writtenData: String,
            @SerialName("timestamp") var timestamp: String,
        )

        @Serializable
        data class SurveyDataRequest(var type: String, var data: Map<String, String>)

        @Serializable
        data class AccessTokenRequest(var accessToken: String)

        @Serializable
        data class RefreshTokenRequest(var refreshToken: String)
    }
