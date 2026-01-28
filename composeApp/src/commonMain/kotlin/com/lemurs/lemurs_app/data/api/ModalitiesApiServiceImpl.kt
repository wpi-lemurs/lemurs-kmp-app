package com.lemurs.lemurs_app.data.api

import com.lemurs.lemurs_app.util.Constants
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

class ModalitiesApiServiceImpl(private val client: HttpClient) {
    /**
     * for submitting data to API
     * use client with authorization and bearer token
     */
    suspend fun submitScreentimeData(
        inputData: ScreentimeRequest
    ): HttpResponse {
        val endpoint = "/screentime"
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

    suspend fun submitBluetoothData(
        inputData: BluetoothData
    ): HttpResponse {
        val endpoint = "/data/proximity"
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

    suspend fun submitGpsData(
        inputData: GpsRequest
    ): HttpResponse {
        val endpoint = "/gps"
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
}

@Serializable
data class ScreentimeRequest(
    val startTime: String,
    val endTime: String,
    val usageData: List<ScreentimeData>
)

@Serializable
data class ScreentimeData(
    val appName: String,
    val totalTime: Long,
    val lastTimeUsed: String
)

@Serializable
data class BluetoothRequest(var data: List<BluetoothData>)

@Serializable
data class BluetoothData(
    val timestamp: @Contextual LocalDateTime,
    val numberOfDevices: Int
)

@Serializable
data class GpsRequest(var data: List<GpsData>)

@Serializable
data class GpsData(
    val date: String,
    val longitude: Double,
    val latitude: Double,
    val altitude: Double,
    val speed: Int,
    val timestamp: String
)
