package com.lemurs.lemurs_app.data.datasource.health

import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.data.api.LemursApiServiceImpl.AudioDataRequest
import com.lemurs.lemurs_app.data.api.LemursApiServiceImpl
import com.lemurs.lemurs_app.data.api.LemursApiServiceImpl.SurveyDataRequest
import com.lemurs.lemurs_app.data.api.LemursApiServiceImpl.WrittenResponseRequest
import com.lemurs.lemurs_app.data.dtos.CaloriesDataDto
import com.lemurs.lemurs_app.data.dtos.DistanceDataDto
import com.lemurs.lemurs_app.data.dtos.SpeedDataDto
import com.lemurs.lemurs_app.data.dtos.StepsDataDto
import com.lemurs.lemurs_app.data.dtos.WeightDataDto
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class HealthRemoteDataSource(private val apiClient: LemursApiServiceImpl) {
    suspend fun submitData(inputDataRequest: SurveyDataRequest): Result<Unit> {
        val endpoint = "/data"

        return try {
            withContext(Dispatchers.IO) {
                val response = apiClient.submitData(inputDataRequest, endpoint)
                Logger.w("HealthRemoteDataSource") { "Response: $response" }
                if (response.status.isSuccess()) {
                    Result.success(Unit)
                } else {
                    Logger.w("HealthRemoteDataSource") { "Failed to submit data: $response" }
                    Result.failure(
                        Exception(
                            "Failed to submit data: ${
                                response.bodyAsText()
                            }"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Logger.w("HealthRemoteDataSource") { "Failed to submit data with exception: ${e.message}" }
            Result.failure(e)
        }
    }


    suspend fun submitAudioData(input: AudioDataRequest): Result<Unit> {
        val endpoint = "/data/audio"

        return try {
            withContext(Dispatchers.IO) {
                val response = apiClient.submitAudioData(input, endpoint)
                Logger.w("HealthRemoteDataSource") { "Response: $response" }
                if (response.status.isSuccess()) {
                    Result.success(Unit)
                } else {
                    Logger.w("HealthRemoteDataSource") { "Failed to submit data: $response" }
                    Result.failure(
                        Exception(
                            "Failed to submit data: ${
                                response.bodyAsText()
                            }"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Logger.w("HealthRemoteDataSource") { "Failed to submit data with exception: ${e.message}" }
            Result.failure(e)
        }
    }

//    suspend fun getWritingData(): {
//        // API endpoint for getting writing data
//        val endpoint = "/data/text"
//        val url = "http://10.0.2.2:8080$endpoint"
//
//        return try {
//            withContext(Dispatchers.IO) {
//                val response = apiClient.sendWritingData(endpoint, text)
//                Logger.w("HealthRemoteDataSource") { "Response: $response" }
//
//                if (response.status.isSuccess()) {
//                    Result.success(Unit)
//                } else {
//                    Logger.w("HealthRemoteDataSource") { "Failed to submit data: $response" }
//                    Result.failure(
//                        Exception("Failed to submit data: ${response.bodyAsText()}")
//                    )
//                }
//            }
//        } catch (e: Exception) {
//            Logger.w("HealthRemoteDataSource") { "Failed to submit data with exception: ${e.message}" }
//            Result.failure(e)
//        }
//    }

    suspend fun saveWriting(input: WrittenResponseRequest): Result<Unit> {
        val endpoint = "/data/text"
        return try {
            withContext(Dispatchers.IO) {
                val response = apiClient.sendWritingData(endpoint, input)
                Logger.w("HealthRemoteDataSource") { "Response: $response" }

                if (response.status.isSuccess()) {
                    Result.success(Unit)
                } else {
                    Logger.w("HealthRemoteDataSource") { "Failed to submit data: $response" }
                    Result.failure(
                        Exception("Failed to submit data: ${response.bodyAsText()}")
                    )
                }
            }
        } catch (e: Exception) {
            Logger.w("HealthRemoteDataSource") { "Failed to submit data with exception: ${e.message}" }
            Result.failure(e)
        }
    }


    suspend fun sendWeightData(weightRecord: WeightDataDto): Result<Unit> {
        val endpoint = "/data/weight"
        return try {
            withContext(Dispatchers.IO) {
                val response = apiClient.sendWeightData(weightRecord, endpoint)
                Logger.w("HealthRemoteDataSource") { "Response: $response" }
                if (response.status.isSuccess()) {
                    Result.success(Unit)
                } else {
                    Logger.w("HealthRemoteDataSource") { "Failed to submit data: $response" }
                    Result.failure(
                        Exception(
                            "Failed to submit data: ${
                                response.bodyAsText()
                            }"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Logger.w("HealthRemoteDataSource") { "Failed to submit data with exception: ${e.message}" }
            Result.failure(e)
        }
    }

    suspend fun sendStepsData(stepsData: StepsDataDto): Result<Unit> {
        val endpoint = "/data/steps"
        return try {
            withContext(Dispatchers.IO) {
                val response = apiClient.sendStepsData(stepsData, endpoint)
                Logger.w("HealthRemoteDataSource") { "Response: $response" }
                if (response.status.isSuccess()) {
                    Result.success(Unit)
                } else {
                    Logger.w("HealthRemoteDataSource") { "Failed to submit data: $response" }
                    Result.failure(
                        Exception(
                            "Failed to submit data: ${
                                response.bodyAsText()
                            }"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Logger.w("HealthRemoteDataSource") { "Failed to submit data with exception: ${e.message}" }
            Result.failure(e)
        }

    }

    suspend fun sendDistanceData(distanceData: DistanceDataDto): Result<Unit>{
        val endpoint = "/data/distance"
        return try {
            withContext(Dispatchers.IO) {
                val response = apiClient.sendDistanceData(distanceData, endpoint)
                Logger.w("HealthRemoteDataSource") { "Response: $response" }
                if (response.status.isSuccess()) {
                    Result.success(Unit)
                } else {
                    Logger.w("HealthRemoteDataSource") { "Failed to submit data: $response" }
                    Result.failure(
                        Exception(
                            "Failed to submit data: ${
                                response.bodyAsText()
                            }"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Logger.w("HealthRemoteDataSource") { "Failed to submit data with exception: ${e.message}" }
            Result.failure(e)
        }

    }

    suspend fun sendCaloriesData(caloriesData: CaloriesDataDto): Result<Unit> {
        val endpoint = "/data/calories"
        return try {
            withContext(Dispatchers.IO) {
                val response = apiClient.sendCaloriesData(caloriesData, endpoint)
                Logger.w("HealthRemoteDataSource") { "Response: $response" }
                if (response.status.isSuccess()) {
                    Result.success(Unit)
                } else {
                    Logger.w("HealthRemoteDataSource") { "Failed to submit data: $response" }
                    Result.failure(
                        Exception(
                            "Failed to submit data: ${
                                response.bodyAsText()
                            }"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Logger.w("HealthRemoteDataSource") { "Failed to submit data with exception: ${e.message}" }
            Result.failure(e)
        }

    }

    suspend fun sendSpeedData(speedData: SpeedDataDto): Result<Unit>{
        val endpoint = "/data/speed"
        return try {
            withContext(Dispatchers.IO) {
                val response = apiClient.sendSpeedData(speedData, endpoint)
                Logger.w("HealthRemoteDataSource") { "Response: $response" }
                if (response.status.isSuccess()) {
                    Result.success(Unit)
                } else {
                    Logger.w("HealthRemoteDataSource") { "Failed to submit data: $response" }
                    Result.failure(
                        Exception(
                            "Failed to submit data: ${
                                response.bodyAsText()
                            }"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Logger.w("HealthRemoteDataSource") { "Failed to submit data with exception: ${e.message}" }
            Result.failure(e)
        }

    }


}
