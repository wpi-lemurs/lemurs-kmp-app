package com.lemurs.lemurs_app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import com.lemurs.JwtTokenResponse
import com.lemurs.NotificationTimes
import com.lemurs.HealthConnectTokens
import com.lemurs.lemurs_app.data.AndroidContextProvider
import okio.FileSystem
import okio.Path.Companion.toPath

actual fun getDataStore(): DataStore<JwtTokenResponse> {
    val content: Context = requireNotNull(org.koin.core.context.GlobalContext.get().get())
    val producePath = { content.filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath.toPath() }

    return createDataStore(fileSystem = FileSystem.SYSTEM, producePath = producePath)
}

actual fun getTimesDataStore(): DataStore<NotificationTimes> {
    val content: Context = requireNotNull(AndroidContextProvider.context)
    val producePath = { content.filesDir.resolve(DATA_STORE_TIMES_FILE_NAME).absolutePath.toPath() }

    return createTimeDataStore(fileSystem = FileSystem.SYSTEM, producePath = producePath)
}

actual fun getHealthConnectTokensDataStore(): DataStore<HealthConnectTokens> {
    val content: Context = requireNotNull(AndroidContextProvider.context)
    val producePath = { content.filesDir.resolve(DATA_STORE_HEALTH_TOKENS_FILE_NAME).absolutePath.toPath() }

    return createHealthConnectTokensDataStore(fileSystem = FileSystem.SYSTEM, producePath = producePath)
}
