package com.lemurs.lemurs_app.data.datastore

import androidx.datastore.core.okio.OkioSerializer
import com.lemurs.HealthConnectTokens
import kotlinx.io.IOException
import okio.BufferedSink
import okio.BufferedSource
import org.koin.core.component.KoinComponent

object HealthConnectTokensSerializer : OkioSerializer<HealthConnectTokens>, KoinComponent {
    override val defaultValue: HealthConnectTokens
        get() = HealthConnectTokens()

    override suspend fun readFrom(source: BufferedSource): HealthConnectTokens {
        try {
            val adapter = HealthConnectTokens.ADAPTER
            val decodedSource = adapter.decode(source)
            return decodedSource
        } catch (exception: IOException) {
            throw Exception(exception.message ?: "Serialization Exception")
        }
    }

    override suspend fun writeTo(t: HealthConnectTokens, sink: BufferedSink) {
        sink.write(t.encode())
    }
}
