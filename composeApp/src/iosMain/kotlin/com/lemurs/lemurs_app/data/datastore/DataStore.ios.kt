package com.lemurs.lemurs_app.data.datastore

import androidx.datastore.core.DataStore
import com.lemurs.JwtTokenResponse
import com.lemurs.NotificationTimes
import com.lemurs.HealthConnectTokens
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.*

private fun documentsDir(): String {
    val paths = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask,
        true
    )
    return paths.first() as String
}

actual fun getDataStore(): DataStore<JwtTokenResponse> {
    val producePath = {
        "${documentsDir()}/$DATA_STORE_FILE_NAME".toPath()
    }

    return createDataStore(FileSystem.SYSTEM, producePath)
}

actual fun getTimesDataStore(): DataStore<NotificationTimes> {
    val producePath = {
        "${documentsDir()}/$DATA_STORE_TIMES_FILE_NAME".toPath()
    }

    return createTimeDataStore(FileSystem.SYSTEM, producePath)
}

actual fun getHealthConnectTokensDataStore(): DataStore<HealthConnectTokens> {
    val producePath = {
        "${documentsDir()}/$DATA_STORE_HEALTH_TOKENS_FILE_NAME".toPath()
    }

    return createHealthConnectTokensDataStore(FileSystem.SYSTEM, producePath)
}
