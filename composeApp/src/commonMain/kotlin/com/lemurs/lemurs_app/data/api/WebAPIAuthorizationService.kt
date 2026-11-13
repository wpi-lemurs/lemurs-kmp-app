package com.lemurs.lemurs_app.data.api

import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.data.datastore.JwtTokenResponseImpl
import com.lemurs.lemurs_app.util.DemoMode
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.Json.Default.parseToJsonElement
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WebAPIAuthorizationService(
    unauthenticatedClient: HttpClient? = null
) : KoinComponent {

    val logger = Logger.withTag("MSAuth")

    //client and service for api call without authorization
    private val httpClient = unauthenticatedClient ?: createPlatformHttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    private val jwtTokenResponseData: JwtTokenResponseImpl by inject()

    private var lemursApiService = LemursApiServiceImpl(httpClient)

    /**
     * Result of exchanging a Microsoft access token for LEMURS backend JWT tokens.
     */
    sealed interface BackendLoginResult {
        data class Success(val accessToken: String, val refreshToken: String) : BackendLoginResult
        data class AuthRejected(val statusCode: Int, val body: String) : BackendLoginResult
        data class RetryableFailure(val statusCode: Int? = null, val reason: String) : BackendLoginResult
    }

    /**
     * Uses a Microsoft access token to obtain LEMURS access and refresh tokens.
     * Distinguishes auth rejections (401/403) from retryable network/5xx failures so callers
     * do not discard valid cached sessions on transient outages.
     */
    suspend fun loginWithMicrosoftToken(microsoftAccessToken: String): BackendLoginResult {
        val LOGIN_ENDPOINT = "/auth/login"
        return try {
            val response = lemursApiService.submitAccessToken(microsoftAccessToken, LOGIN_ENDPOINT)
            val bodyText = response.bodyAsText()
            logger.w("POST $LOGIN_ENDPOINT status=${response.status.value}, body=$bodyText")
            val status = response.status.value
            if (status == 401 || status == 403) {
                logger.e("Backend $LOGIN_ENDPOINT rejected Microsoft token with HTTP $status")
                return BackendLoginResult.AuthRejected(status, bodyText)
            }
            if (status !in 200..299) {
                logger.e("Backend $LOGIN_ENDPOINT failed with retryable HTTP $status")
                return BackendLoginResult.RetryableFailure(statusCode = status, reason = "HTTP $status")
            }
            val json = parseToJsonElement(bodyText).jsonObject
            val accessToken = json["accessToken"]?.jsonPrimitive?.content.orEmpty()
            val refreshToken = json["refreshToken"]?.jsonPrimitive?.content.orEmpty()
            if (accessToken.isBlank() || refreshToken.isBlank()) {
                logger.e("Backend $LOGIN_ENDPOINT returned 2xx with blank tokens")
                return BackendLoginResult.RetryableFailure(statusCode = status, reason = "Blank tokens in 2xx response")
            }
            BackendLoginResult.Success(accessToken = accessToken, refreshToken = refreshToken)
        } catch (e: Exception) {
            logger.e("Failed to authenticate with backend $LOGIN_ENDPOINT: $e")
            BackendLoginResult.RetryableFailure(statusCode = null, reason = e.message ?: e.toString())
        }
    }

    /**
    uses microsoft access token to get lemurs access token and refresh token
    returns json containing lemurs access token and refresh token
     */
    suspend fun accessWebApi(
        microsoftAccessToken: String,
    ): JsonElement {
        return when (val result = loginWithMicrosoftToken(microsoftAccessToken)) {
            is BackendLoginResult.Success ->
                parseToJsonElement("{\"accessToken\": \"${result.accessToken}\", \"refreshToken\": \"${result.refreshToken}\"}")
            is BackendLoginResult.AuthRejected,
            is BackendLoginResult.RetryableFailure ->
                parseToJsonElement("{\"accessToken\": \"\", \"refreshToken\": \"\"}")
        }
    }


    /**
     * function to get HttpClient with Authorization set up
     * use for every api call that is submitting data
     */
    fun getHttpClient() : HttpClient {
        logger.w("Getting Http Client")

        // In demo mode, return a basic client without auth (for App Review)
        if (DemoMode.isActive) {
            logger.w("Demo mode active - returning client without authentication")
            return createPlatformHttpClient {
                install(HttpTimeout) {
                    requestTimeoutMillis = 60000
                    connectTimeoutMillis = 15000
                }
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                    })
                }
                install(Logging) {
                    logger = io.ktor.client.plugins.logging.Logger.SIMPLE
                    level = LogLevel.ALL
                }
            }
        }

        lateinit var jwtTokenResponse : JwtTokenResponseObject
        runBlocking {
            jwtTokenResponse = jwtTokenResponseData.buildJwtTokenResponse()
        }
        if (jwtTokenResponse.refreshToken.isBlank()) {
            logger.w("No refresh token available - returning unauthenticated client")
            return createPlatformHttpClient {
                install(HttpTimeout) {
                    requestTimeoutMillis = 60000
                    connectTimeoutMillis = 15000
                }
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                    })
                }
            }
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
                    json(Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                    })
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
        } catch (e: Exception) {
            logger.w("HttpClient creation exception: " + e.toString())
            return createPlatformHttpClient {
                install(HttpTimeout) {
                    requestTimeoutMillis = 60000 // 60 seconds timeout for requests
                    connectTimeoutMillis = 15000 // 15 seconds for connection
                }
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                    })
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
