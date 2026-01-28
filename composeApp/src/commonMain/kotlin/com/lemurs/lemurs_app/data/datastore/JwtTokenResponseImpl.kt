package com.lemurs.lemurs_app.data.datastore

import androidx.datastore.core.DataStore
import com.lemurs.JwtTokenResponse
import com.lemurs.lemurs_app.data.api.JwtTokenResponseObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface JwtTokenResponseInterface {
    suspend fun updateLemursAccessToken(accessToken: String)
    suspend fun updateRefreshToken(lemursRefreshToken: String)
    fun getLemursAccessToken(): Flow<String>
    fun getRefreshToken(): Flow<String>
    suspend fun buildJwtTokenResponse(): JwtTokenResponseObject
}


class JwtTokenResponseImpl(private val dataStore: DataStore<JwtTokenResponse>) :
    JwtTokenResponseInterface {

    override suspend fun updateLemursAccessToken(accessToken: String) {
        dataStore.updateData { currentJwtTokenResponse ->
            currentJwtTokenResponse.copy(lemursAccessToken = accessToken)
        }
    }

    override suspend fun updateRefreshToken(lemursRefreshToken: String) {
        dataStore.updateData { currentJwtTokenResponse ->
            currentJwtTokenResponse.copy(refreshToken = lemursRefreshToken)
        }
    }

    override fun getLemursAccessToken(): Flow<String> {
        return dataStore.data
            .map { jwtTokenResponse ->
                jwtTokenResponse.lemursAccessToken
            }
    }

    override fun getRefreshToken(): Flow<String> {
        val c = dataStore.data
        return c
            .map { jwtTokenResponse ->
                jwtTokenResponse.refreshToken
            }
    }

    override suspend fun buildJwtTokenResponse(): JwtTokenResponseObject {
        return JwtTokenResponseObject(
            getLemursAccessToken().first().toString(),
            getRefreshToken().first().toString()
        )
    }
}
