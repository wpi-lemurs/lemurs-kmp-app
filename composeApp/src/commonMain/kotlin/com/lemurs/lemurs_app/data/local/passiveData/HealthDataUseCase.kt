package com.lemurs.lemurs_app.data.local.passiveData

import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.data.local.SendDataUseCase
import com.lemurs.lemurs_app.data.local.UseCaseResult
import com.lemurs.lemurs_app.data.repositories.AppRepository
import com.lemurs.lemurs_app.data.dtos.StepsDataDto
import com.lemurs.lemurs_app.data.dtos.CaloriesDataDto
import com.lemurs.lemurs_app.data.dtos.DistanceDataDto
import com.lemurs.lemurs_app.data.dtos.SpeedDataDto
import kotlinx.datetime.Instant
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Contains work for sending health data */
class HealthDataUseCase(
    private val stepDAO: StepDAO,
    private val calorieDAO: CalorieDAO,
    private val distanceDAO: DistanceDAO,
    private val speedDAO: SpeedDAO,
    private val weightDAO: WeightDAO,
    private val sleepDAO: SleepDAO
) : SendDataUseCase, KoinComponent {

    val logger = Logger.withTag("Health Work Manager")
    private val appRepository: AppRepository by inject()

    /**
     * Sends data from all health DAOs
     */
    override suspend fun call(): UseCaseResult<Any> {
        logger.w("health data worker called")

        var overallSuccess = true

        // Submit data from each DAO
        if (!submitStepsData()) overallSuccess = false
        if (!submitCaloriesData()) overallSuccess = false
        if (!submitDistanceData()) overallSuccess = false
        if (!submitSpeedData()) overallSuccess = false
        if (!submitWeightData()) overallSuccess = false
        if (!submitSleepData()) overallSuccess = false

        return if (overallSuccess) {
            UseCaseResult.Success(Any())
        } else {
            UseCaseResult.Failure()
        }
    }


    /** Submit steps data from local database to API */
    private suspend fun submitStepsData(): Boolean {
        logger.w("getting all steps data...")
        val data: List<Step> = stepDAO.getAll()
        logger.w("got ${data.size} steps records")

        if (data.isEmpty()) {
            logger.w("no steps data found")
            return true
        }

        var overallSuccess = true

        for (d in data) {
            try {
                logger.w("processing steps data: ID=${d.getID()}, start=${d.startTimestamp}, end=${d.endTimestamp}, steps=${d.steps}")

                // Append 'Z' if not present to ensure proper ISO 8601 format
                val startTimestamp = if (d.startTimestamp.endsWith("Z")) d.startTimestamp else "${d.startTimestamp}Z"
                val endTimestamp = if (d.endTimestamp.endsWith("Z")) d.endTimestamp else "${d.endTimestamp}Z"

                val startInstant = Instant.parse(startTimestamp)
                val endInstant = Instant.parse(endTimestamp)
                val startDateTime = startInstant.toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
                val endDateTime = endInstant.toLocalDateTime(kotlinx.datetime.TimeZone.UTC)

                val stepsDto = StepsDataDto(
                    userId = "",
                    steps = d.steps.toLong(),
                    start_timestamp = startDateTime,
                    end_timestamp = endDateTime,
                    appSource = d.appSource ?: "HealthConnect"
                )

                val success = appRepository.sendStepsData(stepsDto)
                if (success) {
                    logger.w("Step data submitted successfully for record ${d.getID()}, deleting from local storage")
                    stepDAO.delete(d)
                } else {
                    logger.w("Failed to submit steps data for record ${d.getID()}")
                    overallSuccess = false
                }
            } catch (e: Exception) {
                logger.w("Error submitting steps data for record ${d.getID()}: $e")
                overallSuccess = false
            }
        }

        return overallSuccess
    }

    /** Submit calories data from local database to API */
    private suspend fun submitCaloriesData(): Boolean {
        logger.w("getting all calories data...")
        val data: List<Calorie> = calorieDAO.getAll()
        logger.w("got ${data.size} calories records")

        if (data.isEmpty()) {
            logger.w("no calories data found")
            return true
        }

        var overallSuccess = true

        for (d in data) {
            try {
                logger.w("processing calories data: ID=${d.getID()}, start=${d.startTimestamp}, end=${d.endTimestamp}, calories=${d.calories}")

                // Append 'Z' if not present to ensure proper ISO 8601 format
                val startTimestamp = if (d.startTimestamp.endsWith("Z")) d.startTimestamp else "${d.startTimestamp}Z"
                val endTimestamp = if (d.endTimestamp.endsWith("Z")) d.endTimestamp else "${d.endTimestamp}Z"

                val startInstant = Instant.parse(startTimestamp)
                val endInstant = Instant.parse(endTimestamp)
                val startDateTime = startInstant.toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
                val endDateTime = endInstant.toLocalDateTime(kotlinx.datetime.TimeZone.UTC)

                val caloriesDto = CaloriesDataDto(
                    userId = "",
                    calories = d.calories,
                    start_timestamp = startDateTime,
                    end_timestamp = endDateTime,
                    appSource = d.appSource ?: "HealthConnect"
                )

                val success = appRepository.sendCaloriesData(caloriesDto)
                if (success) {
                    logger.w("Calorie data submitted successfully for record ${d.getID()}, deleting from local storage")
                    calorieDAO.delete(d)
                } else {
                    logger.w("Failed to submit calories data for record ${d.getID()}")
                    overallSuccess = false
                }
            } catch (e: Exception) {
                logger.w("Error submitting calories data for record ${d.getID()}: $e")
                overallSuccess = false
            }
        }

        return overallSuccess
    }

    /** Submit distance data from local database to API */
    private suspend fun submitDistanceData(): Boolean {
        logger.w("getting all distance data...")
        val data: List<Distance> = distanceDAO.getAll()
        logger.w("got ${data.size} distance records")

        if (data.isEmpty()) {
            logger.w("no distance data found")
            return true
        }

        var overallSuccess = true

        for (d in data) {
            try {
                logger.w("processing distance data: ID=${d.getID()}, start=${d.startTimestamp}, end=${d.endTimestamp}, distance=${d.distance}")

                // Append 'Z' if not present to ensure proper ISO 8601 format
                val startTimestamp = if (d.startTimestamp.endsWith("Z")) d.startTimestamp else "${d.startTimestamp}Z"
                val endTimestamp = if (d.endTimestamp.endsWith("Z")) d.endTimestamp else "${d.endTimestamp}Z"

                val startInstant = Instant.parse(startTimestamp)
                val endInstant = Instant.parse(endTimestamp)
                val startDateTime = startInstant.toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
                val endDateTime = endInstant.toLocalDateTime(kotlinx.datetime.TimeZone.UTC)

                val distanceDto = DistanceDataDto(
                    userId = "",
                    distance = d.distance,
                    start_timestamp = startDateTime,
                    end_timestamp = endDateTime,
                    appSource = d.appSource ?: "HealthConnect"
                )

                val success = appRepository.sendDistanceData(distanceDto)
                if (success) {
                    logger.w("Distance data submitted successfully for record ${d.getID()}, deleting from local storage")
                    distanceDAO.delete(d)
                } else {
                    logger.w("Failed to submit distance data for record ${d.getID()}")
                    overallSuccess = false
                }
            } catch (e: Exception) {
                logger.w("Error submitting distance data for record ${d.getID()}: $e")
                overallSuccess = false
            }
        }

        return overallSuccess
    }

    /** Submit speed data from local database to API */
    private suspend fun submitSpeedData(): Boolean {
        logger.w("getting all speed data...")
        val data: List<Speed> = speedDAO.getAll()
        logger.w("got ${data.size} speed records")

        if (data.isEmpty()) {
            logger.w("no speed data found")
            return true
        }

        var overallSuccess = true

        for (d in data) {
            try {
                logger.w("processing speed data: ID=${d.getID()}, start=${d.startTimestamp}, end=${d.endTimestamp}, speed=${d.speed}")

                // Append 'Z' if not present to ensure proper ISO 8601 format
                val startTimestamp = if (d.startTimestamp.endsWith("Z")) d.startTimestamp else "${d.startTimestamp}Z"
                val endTimestamp = if (d.endTimestamp.endsWith("Z")) d.endTimestamp else "${d.endTimestamp}Z"

                val startInstant = Instant.parse(startTimestamp)
                val endInstant = Instant.parse(endTimestamp)
                val startDateTime = startInstant.toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
                val endDateTime = endInstant.toLocalDateTime(kotlinx.datetime.TimeZone.UTC)

                // Speed DTO expects a list, so wrap the single value
                val speedDto = SpeedDataDto(
                    userId = "",
                    speed = listOf(d.speed),
                    start_timestamp = startDateTime,
                    end_timestamp = endDateTime,
                    appSource = d.appSource ?: "HealthConnect"
                )

                val success = appRepository.sendSpeedData(speedDto)
                if (success) {
                    logger.w("Speed data submitted successfully for record ${d.getID()}, deleting from local storage")
                    speedDAO.delete(d)
                } else {
                    logger.w("Failed to submit speed data for record ${d.getID()}")
                    overallSuccess = false
                }
            } catch (e: Exception) {
                logger.w("Error submitting speed data for record ${d.getID()}: $e")
                overallSuccess = false
            }
        }

        return overallSuccess
    }

    /** Submit weight data from local database to API */
    private suspend fun submitWeightData(): Boolean {
        logger.w("getting all weight data...")
        val data: List<Weight> = weightDAO.getAll()
        logger.w("got ${data.size} weight records")

        if (data.isEmpty()) {
            logger.w("no weight data found")
            return true
        }

        var overallSuccess = true

        for (d in data) {
            try {
                logger.w("processing weight data: ID=${d.getID()}, timestamp=${d.timestamp}, weight=${d.weight}")

                // TODO: Create WeightDataDto and add sendWeightData method to AppRepository
                // For now, just log and consider it successful
                logger.w("Weight data submission not yet implemented, skipping record ${d.getID()}")
                // When implemented:
                // val instant = Instant.parse(d.timestamp)
                // val localDateTime = instant.toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
                // val weightDto = WeightDataDto(...)
                // val success = appRepository.sendWeightData(weightDto)
                // if (success) weightDAO.deleteWeightByID(d.ID)

            } catch (e: Exception) {
                logger.w("Error submitting weight data for record ${d.getID()}: $e")
                overallSuccess = false
            }
        }

        return overallSuccess
    }

    /** Submit sleep data from local database to API */
    private suspend fun submitSleepData(): Boolean {
        logger.w("getting all sleep data...")
        val data: List<Sleep> = sleepDAO.getAll()
        logger.w("got ${data.size} sleep records")

        if (data.isEmpty()) {
            logger.w("no sleep data found")
            return true
        }

        var overallSuccess = true

        for (d in data) {
            try {
                logger.w("processing sleep data: ID=${d.getID()}, start=${d.startTimestamp}, end=${d.endTimestamp}, sleep=${d.sleep}")

                // TODO: Create SleepDataDto and add sendSleepData method to AppRepository
                // For now, just log and consider it successful
                logger.w("Sleep data submission not yet implemented, skipping record ${d.getID()}")
                // When implemented:
                // val startInstant = Instant.parse(d.startTimestamp)
                // val endInstant = Instant.parse(d.endTimestamp)
                // val startDateTime = startInstant.toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
                // val endDateTime = endInstant.toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
                // val sleepDto = SleepDataDto(...)
                // val success = appRepository.sendSleepData(sleepDto)
                // if (success) sleepDAO.deleteSleepByID(d.ID)

            } catch (e: Exception) {
                logger.w("Error submitting sleep data for record ${d.getID()}: $e")
                overallSuccess = false
            }
        }

        return overallSuccess
    }
}

