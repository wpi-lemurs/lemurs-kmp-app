package com.lemurs.lemurs_app.data.datastore

import androidx.datastore.core.DataStore
import com.lemurs.JwtTokenResponse
import com.lemurs.NotificationTimes
import com.lemurs.HealthConnectTokens

actual fun getDataStore(): DataStore<JwtTokenResponse> {
    TODO("Not yet implemented")
}

actual fun getTimesDataStore(): DataStore<NotificationTimes> {
    TODO("Not yet implemented")
}

actual fun getHealthConnectTokensDataStore(): DataStore<HealthConnectTokens> {
    TODO("Not yet implemented")
}
