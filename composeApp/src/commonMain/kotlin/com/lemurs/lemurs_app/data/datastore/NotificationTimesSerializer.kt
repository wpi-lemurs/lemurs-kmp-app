package com.lemurs.lemurs_app.data.datastore

import androidx.datastore.core.okio.OkioSerializer
import com.lemurs.NotificationTimes
import kotlinx.io.IOException
import okio.BufferedSink
import okio.BufferedSource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

object NotificationTimesSerializer : OkioSerializer<NotificationTimes>, KoinComponent {
    val notificationTimes: NotificationTimes by inject(qualifier = named("notificationTimes"))
    override val defaultValue: NotificationTimes
        get() = NotificationTimes()

    override suspend fun readFrom(source: BufferedSource): NotificationTimes {
        try {
            val adapter = NotificationTimes.ADAPTER
            val decodedSource = adapter.decode(source)
            return decodedSource
        } catch (exception: IOException) {
            throw Exception(exception.message ?: "Serialization Exception")
        }
    }

    override suspend fun writeTo(t: NotificationTimes, sink: BufferedSink) {
        sink.write(t.encode())
    }
}
