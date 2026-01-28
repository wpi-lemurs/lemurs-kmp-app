package com.lemurs.lemurs_app.data.datastore

import androidx.datastore.core.okio.OkioSerializer
import com.lemurs.JwtTokenResponse
import kotlinx.io.IOException
import okio.BufferedSink
import okio.BufferedSource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

object JwtTokenResponseSerializer : OkioSerializer<JwtTokenResponse>, KoinComponent {
    val jwtTokenResponse: JwtTokenResponse by inject(qualifier = named("jwtTokenResponse"))
    override val defaultValue: JwtTokenResponse
        get() = JwtTokenResponse()

    override suspend fun readFrom(source: BufferedSource): JwtTokenResponse {
        try {
            val adapter = JwtTokenResponse.ADAPTER
            val decodedSource = adapter.decode(source)
            return decodedSource
        } catch (exception: IOException) {
            throw Exception(exception.message ?: "Serialization Exception")
        }
    }

    override suspend fun writeTo(t: JwtTokenResponse, sink: BufferedSink) {
        sink.write(t.encode())
    }
}
