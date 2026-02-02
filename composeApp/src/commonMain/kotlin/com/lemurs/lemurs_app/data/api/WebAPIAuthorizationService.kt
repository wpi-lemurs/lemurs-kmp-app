package com.lemurs.lemurs_app.data.api

import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.data.datastore.JwtTokenResponseImpl
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json.Default.parseToJsonElement
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WebAPIAuthorizationService : KoinComponent {

    val logger = Logger.withTag("MSAuth")

    //client and service for api call without authorization
    private val httpClient = createPlatformHttpClient {
        install(ContentNegotiation) {
            json()
        }
    }

    private val jwtTokenResponseData: JwtTokenResponseImpl by inject()

    private var lemursApiService = LemursApiServiceImpl(httpClient)

    /**
    uses microsoft access token to get lemurs access token and refresh token
    returns json containing lemurs access token and refresh token
     */
    suspend fun accessWebApi(
        microsoftAccessToken: String,
    ): JsonElement {
        val LOGIN_ENDPOINT = "/auth/login"
        val response = lemursApiService.submitAccessToken(microsoftAccessToken, LOGIN_ENDPOINT)
        logger.w("apiResponse body: " + response.bodyAsText())
        var jwtTokenResponseJson: JsonElement =
            parseToJsonElement("{\"accessToken\": \"\", \"refreshToken\": \"\"}")
        try {
            jwtTokenResponseJson = parseToJsonElement(response.bodyAsText())
        } catch (e: Exception) {
            logger.w("json exception: " + e.toString())
        }
        return jwtTokenResponseJson
    }


    /**
     * function to get HttpClient with Authorization set up
     * use for every api call that is submitting data
     */
    fun getHttpClient() : HttpClient {
        logger.w("Getting Http Client")
        lateinit var jwtTokenResponse : JwtTokenResponseObject
        runBlocking {
            jwtTokenResponse = jwtTokenResponseData.buildJwtTokenResponse()
        }
        try {
            lateinit var refreshedAccessToken: String
            lateinit var refreshedRefreshToken: String
            runBlocking {
                refreshedAccessToken = (parseToJsonElement(
                    lemursApiService.refreshAccessToken(
                        jwtTokenResponse.refreshToken,
                        "/auth/refresh"
                    ).bodyAsText()
                )).jsonObject.get("accessToken")!!.jsonPrimitive.content
                refreshedRefreshToken = (parseToJsonElement(
                    lemursApiService.refreshAccessToken(
                        jwtTokenResponse.refreshToken,
                        "/auth/refresh"
                    ).bodyAsText()
                )).jsonObject.get("refreshToken")!!.jsonPrimitive.content
            }
            return createPlatformHttpClient {
                install(HttpTimeout) {
                    requestTimeoutMillis = 60000 // 60 seconds timeout for requests
                    connectTimeoutMillis = 15000 // 15 seconds for connection
                }
                install(ContentNegotiation) {
                    json()
                }
                install(Logging) {
                    logger = io.ktor.client.plugins.logging.Logger.SIMPLE
                    level = LogLevel.ALL
                }
                install(Auth) {
                    bearer {
                        loadTokens {
                            // Load tokens from a local storage and return them as the 'BearerTokens' instance
                            BearerTokens(
                                jwtTokenResponse.lemursAccessToken,
                                jwtTokenResponse.refreshToken
                            )
                        }
                        refreshTokens {
                            BearerTokens(refreshedAccessToken, refreshedRefreshToken)
                        }
                    }
                }
            }
        } catch (e: NullPointerException) {
            logger.w("exception: " + e.toString())
            return createPlatformHttpClient {
                install(HttpTimeout) {
                    requestTimeoutMillis = 60000 // 60 seconds timeout for requests
                    connectTimeoutMillis = 15000 // 15 seconds for connection
                }
                install(ContentNegotiation) {
                    json()
                }
            }
        }
    }


    /**
     * function to send test data to api (with send data button)
     */
    suspend fun sendData(endpoint: String) {
        logger.w("requesting to send data...")
        val map = mapOf("key" to "value")
        val surveyDataRequest = LemursApiServiceImpl.SurveyDataRequest("survey", map)
        lemursApiService = LemursApiServiceImpl(getHttpClient())
        val list = ArrayList<Any>();
        list.add(lemursApiService.submitData(surveyDataRequest, endpoint))

    }

}

class JwtTokenResponseObject(private val access: String, private val refresh: String) {
    var lemursAccessToken: String = access
    var refreshToken: String = refresh
}
