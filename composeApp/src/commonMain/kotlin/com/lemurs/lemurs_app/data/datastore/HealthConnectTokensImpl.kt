package com.lemurs.lemurs_app.data.datastore

import androidx.datastore.core.DataStore
import com.lemurs.HealthConnectTokens
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface HealthConnectTokensInterface {
    suspend fun updateStepsToken(token: String)
    suspend fun updateCaloriesToken(token: String)
    suspend fun updateDistanceToken(token: String)
    suspend fun updateSpeedToken(token: String)
    suspend fun updateSleepToken(token: String)
    suspend fun updateHeartRateToken(token: String)
    suspend fun updateExerciseToken(token: String)
    suspend fun updatePowerToken(token: String)

    suspend fun getStepsToken(): String
    suspend fun getCaloriesToken(): String
    suspend fun getDistanceToken(): String
    suspend fun getSpeedToken(): String
    suspend fun getSleepToken(): String
    suspend fun getHeartRateToken(): String
    suspend fun getExerciseToken(): String
    suspend fun getPowerToken(): String
}

class HealthConnectTokensImpl(private val dataStore: DataStore<HealthConnectTokens>) :
    HealthConnectTokensInterface {

    override suspend fun updateStepsToken(token: String) {
        dataStore.updateData { current ->
            current.copy(stepsToken = token)
        }
    }

    override suspend fun updateCaloriesToken(token: String) {
        dataStore.updateData { current ->
            current.copy(caloriesToken = token)
        }
    }

    override suspend fun updateDistanceToken(token: String) {
        dataStore.updateData { current ->
            current.copy(distanceToken = token)
        }
    }

    override suspend fun updateSpeedToken(token: String) {
        dataStore.updateData { current ->
            current.copy(speedToken = token)
        }
    }

    override suspend fun updateSleepToken(token: String) {
        dataStore.updateData { current ->
            current.copy(sleepToken = token)
        }
    }

    override suspend fun updateHeartRateToken(token: String) {
        dataStore.updateData { current ->
            current.copy(heartRateToken = token)
        }
    }

    override suspend fun updateExerciseToken(token: String) {
        dataStore.updateData { current ->
            current.copy(exerciseToken = token)
        }
    }

    override suspend fun updatePowerToken(token: String) {
        dataStore.updateData { current ->
            current.copy(powerToken = token)
        }
    }

    override suspend fun getStepsToken(): String {
        return dataStore.data.map { it.stepsToken }.first()
    }

    override suspend fun getCaloriesToken(): String {
        return dataStore.data.map { it.caloriesToken }.first()
    }

    override suspend fun getDistanceToken(): String {
        return dataStore.data.map { it.distanceToken }.first()
    }

    override suspend fun getSpeedToken(): String {
        return dataStore.data.map { it.speedToken }.first()
    }

    override suspend fun getSleepToken(): String {
        return dataStore.data.map { it.sleepToken }.first()
    }

    override suspend fun getHeartRateToken(): String {
        return dataStore.data.map { it.heartRateToken }.first()
    }

    override suspend fun getExerciseToken(): String {
        return dataStore.data.map { it.exerciseToken }.first()
    }

    override suspend fun getPowerToken(): String {
        return dataStore.data.map { it.powerToken }.first()
    }
}
