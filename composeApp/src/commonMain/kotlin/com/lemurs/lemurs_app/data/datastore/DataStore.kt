package com.lemurs.lemurs_app.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import com.lemurs.JwtTokenResponse
import com.lemurs.NotificationTimes
import com.lemurs.HealthConnectTokens
import okio.FileSystem
import okio.Path


internal const val DATA_STORE_FILE_NAME = "proto_datastore.preferences_pb"

expect fun getDataStore(): DataStore<JwtTokenResponse>

fun createDataStore(
    fileSystem: FileSystem,
    producePath: () -> Path
): DataStore<JwtTokenResponse> =
    DataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = fileSystem,
            producePath = producePath,
            serializer = JwtTokenResponseSerializer
        ),
    )

internal const val DATA_STORE_TIMES_FILE_NAME = "proto_datastore.preferences2_pb"

expect fun getTimesDataStore(): DataStore<NotificationTimes>

fun createTimeDataStore(
    fileSystem: FileSystem,
    producePath: () -> Path
): DataStore<NotificationTimes> =
    DataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = fileSystem,
            producePath = producePath,
            serializer = NotificationTimesSerializer
        ),
    )

internal const val DATA_STORE_HEALTH_TOKENS_FILE_NAME = "proto_datastore.health_tokens_pb"

expect fun getHealthConnectTokensDataStore(): DataStore<HealthConnectTokens>

fun createHealthConnectTokensDataStore(
    fileSystem: FileSystem,
    producePath: () -> Path
): DataStore<HealthConnectTokens> =
    DataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = fileSystem,
            producePath = producePath,
            serializer = HealthConnectTokensSerializer
        ),
    )
