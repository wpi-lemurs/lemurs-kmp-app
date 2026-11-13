package com.lemurs.lemurs_app.survey

import com.lemurs.lemurs_app.data.api.WebAPIAuthorizationService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Reproduces the production crash (`NoTransformationFoundException` at `SurveysApiImpl.getDemographics`)
 * when `/api/auth/login` or `/api/demographic` returns HTTP 401 with an empty body.
 */
class AuthLoginDemographicsCrashTest {

    private fun createMockClient(
        handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(io.ktor.client.request.HttpRequestData) -> io.ktor.client.request.HttpResponseData
    ): HttpClient {
        return HttpClient(MockEngine(handler)) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }
        }
    }

    @Test
    fun `accessWebApi returns blank tokens and does not throw when auth login returns 401 empty body`() = runTest {
        val mockClient = createMockClient {
            respond(
                content = "",
                status = HttpStatusCode.Unauthorized
            )
        }

        val webAuth = WebAPIAuthorizationService(unauthenticatedClient = mockClient)
        val result = webAuth.accessWebApi("expired-or-invalid-ms-token")

        assertEquals("", result.jsonObject["accessToken"]?.jsonPrimitive?.content)
        assertEquals("", result.jsonObject["refreshToken"]?.jsonPrimitive?.content)
    }

    @Test
    fun `getDemographics returns empty list instead of throwing NoTransformationFoundException on 401 empty body`() = runTest {
        val mockClient = createMockClient {
            respond(
                content = "",
                status = HttpStatusCode.Unauthorized
            )
        }

        val api = SurveysApiImpl(client = mockClient)
        val demographics = api.getDemographics()

        assertTrue(demographics.isEmpty())
    }

    @Test
    fun `getDemographics parses valid 200 JSON payload`() = runTest {
        val mockClient = createMockClient {
            respond(
                content = """[{"keyword":"age","value":"20"}]""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api = SurveysApiImpl(client = mockClient)
        val demographics = api.getDemographics()

        assertEquals(1, demographics.size)
        assertEquals("age", demographics.first().keyword)
        assertEquals("20", demographics.first().value)
    }

    @Test
    fun `loginWithMicrosoftToken distinguishes 401 auth rejection from 503 retryable failure`() = runTest {
        val rejectedClient = createMockClient {
            respond(content = "", status = HttpStatusCode.Unauthorized)
        }
        val rejectedResult = WebAPIAuthorizationService(unauthenticatedClient = rejectedClient)
            .loginWithMicrosoftToken("bad-token")
        assertIs<WebAPIAuthorizationService.BackendLoginResult.AuthRejected>(rejectedResult)
        assertEquals(401, rejectedResult.statusCode)

        val outageClient = createMockClient {
            respond(content = "Service Unavailable", status = HttpStatusCode.ServiceUnavailable)
        }
        val outageResult = WebAPIAuthorizationService(unauthenticatedClient = outageClient)
            .loginWithMicrosoftToken("valid-token")
        assertIs<WebAPIAuthorizationService.BackendLoginResult.RetryableFailure>(outageResult)
        assertEquals(503, outageResult.statusCode)

        val throwingClient = createMockClient {
            throw RuntimeException("timeout")
        }
        val networkResult = WebAPIAuthorizationService(unauthenticatedClient = throwingClient)
            .loginWithMicrosoftToken("valid-token")
        assertIs<WebAPIAuthorizationService.BackendLoginResult.RetryableFailure>(networkResult)
        assertEquals(null, networkResult.statusCode)

        val nullTokenClient = createMockClient {
            respond(
                content = """{"accessToken":null,"refreshToken":null}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val nullTokenResult = WebAPIAuthorizationService(unauthenticatedClient = nullTokenClient)
            .loginWithMicrosoftToken("valid-token")
        assertIs<WebAPIAuthorizationService.BackendLoginResult.RetryableFailure>(nullTokenResult)
    }

    @Test
    fun `loginWithMicrosoftToken returns Success on valid 200 JSON payload`() = runTest {
        val okClient = createMockClient {
            respond(
                content = """{"accessToken":"jwt-access","refreshToken":"jwt-refresh"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val result = WebAPIAuthorizationService(unauthenticatedClient = okClient)
            .loginWithMicrosoftToken("valid-ms-token")
        val success = assertIs<WebAPIAuthorizationService.BackendLoginResult.Success>(result)
        assertEquals("jwt-access", success.accessToken)
        assertEquals("jwt-refresh", success.refreshToken)
    }

    @Test
    fun `getDemographicsResult distinguishes HTTP or parse failures from confirmed empty 200 list`() = runTest {
        val outageApi = SurveysApiImpl(client = createMockClient {
            respond(content = "", status = HttpStatusCode.ServiceUnavailable)
        })
        val outageResult = outageApi.getDemographicsResult()
        val outageFailure = assertIs<SurveysApiImpl.DemographicsLookupResult.Failure>(outageResult)
        assertEquals(503, outageFailure.statusCode)

        val malformedApi = SurveysApiImpl(client = createMockClient {
            respond(
                content = "not-json",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        })
        assertIs<SurveysApiImpl.DemographicsLookupResult.Failure>(malformedApi.getDemographicsResult())

        val emptyOkApi = SurveysApiImpl(client = createMockClient {
            respond(
                content = "[]",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        })
        val emptySuccess = assertIs<SurveysApiImpl.DemographicsLookupResult.Success>(emptyOkApi.getDemographicsResult())
        assertTrue(emptySuccess.demographics.isEmpty())
    }
}
