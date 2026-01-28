package com.lemurs.lemurs_app.health

import android.app.Application
import android.os.RemoteException
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.mutableStateOf
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.response.ChangesResponse
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.changes.DeletionChange
import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.data.repositories.AppRepository
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** View Model to Interact with the HealthConnect SDK */
class HealthConnectViewModel(
    private val context: Application,
    private val repository: AppRepository,
    private val stepDAO: com.lemurs.lemurs_app.data.local.passiveData.StepDAO? = null,
    private val calorieDAO: com.lemurs.lemurs_app.data.local.passiveData.CalorieDAO? = null,
    private val distanceDAO: com.lemurs.lemurs_app.data.local.passiveData.DistanceDAO? = null,
    private val speedDAO: com.lemurs.lemurs_app.data.local.passiveData.SpeedDAO? = null,
    private val weightDAO: com.lemurs.lemurs_app.data.local.passiveData.WeightDAO? = null,
    private val sleepDAO: com.lemurs.lemurs_app.data.local.passiveData.SleepDAO? = null,
    private val healthConnectTokensImpl: com.lemurs.lemurs_app.data.datastore.HealthConnectTokensImpl? = null,
) : ViewModel() {
    private val logger = Logger.withTag("HealthConnectViewModel")
    private val healthConnectClient by lazy { try{HealthConnectClient.getOrCreate(context) }catch(e: Exception) {
        logger.w("exception: "+ e)
        null}}
    private var _permissionsGranted = mutableStateOf(false)
    private var availability = mutableStateOf(HealthConnectAvailability.NOT_SUPPORTED)

    fun initialLoad() {
        checkAvailability()
    }

    val permissions =
        setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(PowerRecord::class),
            HealthPermission.getReadPermission(SpeedRecord::class),
            "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"
        )

    // Check permissions before running a get record function
    private suspend fun <T> tryWithPermissionsCheck(block: suspend () -> T): T? {
        logger.d("Checking permissions")
//        _permissionsGranted.value = hasAllPermissions()
        _permissionsGranted.value = hasAllPermissions(permissions)
        logger.d("Permissions granted: ${_permissionsGranted.value}")
        return try {
            if (_permissionsGranted.value) {
                block()
            } else {
                null
            }
        } catch (remoteException: RemoteException) {
            remoteException.printStackTrace()
            throw remoteException
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            throw ioException
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    // Check if HealthConnect SDK is available
    fun checkAvailability(): HealthConnectAvailability {
        logger.d("Checking availability")
        try {
            val current = HealthConnectClient.getSdkStatus(context)
            logger.d("Current status: $current")
            availability.value =
                when (current) {
                    HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.INSTALLED
                    HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.NOT_INSTALLED
                    else -> HealthConnectAvailability.NOT_SUPPORTED
                }
        } catch (e: Exception) {
            logger.e("Error checking availability", e)
            availability.value = HealthConnectAvailability.NOT_SUPPORTED
        }

        logger.d("Availability: ${availability.value}")
        return availability.value
    }

    // Check if all permissions needed for the app are granted
    suspend fun hasAllPermissions(permissions: Set<String>): Boolean {
        return try {
            val grantedPermissions =
                withContext(Dispatchers.IO) {
                    healthConnectClient?.permissionController?.getGrantedPermissions() ?: emptySet()
                }
            logger.w("permissions that are on: $grantedPermissions")
            val result = grantedPermissions.containsAll(permissions)
            logger.d("Permissions check result: $result")
            result
        } catch (e: Exception) {
            logger.e("Error checking permissions", e)
            false
        }
    }

    // Request permissions from the user
    fun requestPermissionsActivityContract(): ActivityResultContract<Set<String>, Set<String>> {
        logger.w("requesting health data permissions...")
        checkAvailability()
        return PermissionController.createRequestPermissionResultContract()
    }

    /**
     * Get or create a changes token for a specific record type.
     * Recommended to get separate tokens per data type to avoid exceptions if one permission is revoked.
     */
    private suspend inline fun <reified T : Record> getOrCreateChangesToken(): String? {
        return try {
            withContext(Dispatchers.IO) {
                tryWithPermissionsCheck {
                    val token = healthConnectClient!!.getChangesToken(
                        ChangesTokenRequest(recordTypes = setOf(T::class))
                    )
                    logger.d("Successfully obtained changes token for ${T::class.simpleName}")
                    token
                } ?: ""
            }
        } catch (e: Exception) {
            logger.e("Error getting changes token for ${T::class.simpleName}", e)
            null
        }
    }

    /**
     * Initialize changes tokens for all data types.
     * This should be called once after permissions are granted.
     */
    suspend fun initializeChangesTokens() {
        if (healthConnectTokensImpl == null) {
            logger.w("HealthConnectTokensImpl is null, cannot initialize changes tokens")
            return
        }

        logger.d("Initializing changes tokens for all data types")

        getOrCreateChangesToken<StepsRecord>()?.let {
            healthConnectTokensImpl.updateStepsToken(it)
            logger.d("Initialized steps token")
        }

        getOrCreateChangesToken<TotalCaloriesBurnedRecord>()?.let {
            healthConnectTokensImpl.updateCaloriesToken(it)
            logger.d("Initialized calories token")
        }

        getOrCreateChangesToken<DistanceRecord>()?.let {
            healthConnectTokensImpl.updateDistanceToken(it)
            logger.d("Initialized distance token")
        }

        getOrCreateChangesToken<SpeedRecord>()?.let {
            healthConnectTokensImpl.updateSpeedToken(it)
            logger.d("Initialized speed token")
        }

        getOrCreateChangesToken<SleepSessionRecord>()?.let {
            healthConnectTokensImpl.updateSleepToken(it)
            logger.d("Initialized sleep token")
        }

        getOrCreateChangesToken<HeartRateRecord>()?.let {
            healthConnectTokensImpl.updateHeartRateToken(it)
            logger.d("Initialized heart rate token")
        }

        getOrCreateChangesToken<ExerciseSessionRecord>()?.let {
            healthConnectTokensImpl.updateExerciseToken(it)
            logger.d("Initialized exercise token")
        }

        getOrCreateChangesToken<PowerRecord>()?.let {
            healthConnectTokensImpl.updatePowerToken(it)
            logger.d("Initialized power token")
        }
    }

    /**
     * Pull changes for a specific data type using its stored token.
     * This will get all changes since the last token and update the token for next time.
     */
    private suspend inline fun <reified T : Record> pullChangesForType(
        currentToken: String,
        crossinline onNewToken: suspend (String) -> Unit,
        crossinline processUpsert: suspend (T) -> Unit
    ) {
        if (currentToken.isEmpty()) {
            logger.w("Token is empty for ${T::class.simpleName}, cannot pull changes")
            return
        }

        try {
            withContext(Dispatchers.IO) {
                tryWithPermissionsCheck {
                    var token = currentToken
                    var hasMore = true

                    // Loop through all available changes
                    while (hasMore) {
                        val response: ChangesResponse = healthConnectClient!!.getChanges(token)

                        logger.d("Processing ${response.changes.size} changes for ${T::class.simpleName}")

                        // Process each change
                        response.changes.forEach { change ->
                            when (change) {
                                is UpsertionChange -> {
                                    // Only process if it's the correct type and not from our app
                                    if (change.record is T) {
                                        val record = change.record as T
                                        // Check if it's from another app (to avoid re-importing our own data)
                                        val packageName = context.packageName
                                        val recordMetadata = record.metadata

                                        if (recordMetadata.dataOrigin.packageName != packageName) {
                                            logger.d("Processing upsert for ${T::class.simpleName} from ${recordMetadata.dataOrigin.packageName}")
                                            processUpsert(record)
                                        } else {
                                            logger.d("Skipping upsert from own app")
                                        }
                                    }
                                }
                                is DeletionChange -> {
                                    // Handle deletions by removing from local database
                                    logger.d("Deletion detected for ${T::class.simpleName}: ${change.recordId}")

                                    // Remove from local database based on record type
                                    when (T::class) {
                                        StepsRecord::class -> {
                                            // Note: We can't easily delete by recordId since we store by timestamp
                                            // This is a limitation of the current database design
                                            logger.d("Steps record deleted from HealthConnect: ${change.recordId}")
                                        }
                                        TotalCaloriesBurnedRecord::class -> {
                                            logger.d("Calories record deleted from HealthConnect: ${change.recordId}")
                                        }
                                        DistanceRecord::class -> {
                                            logger.d("Distance record deleted from HealthConnect: ${change.recordId}")
                                        }
                                        SpeedRecord::class -> {
                                            logger.d("Speed record deleted from HealthConnect: ${change.recordId}")
                                        }
                                        else -> {
                                            logger.d("Deletion for ${T::class.simpleName} not implemented")
                                        }
                                    }
                                }
                            }
                        }

                        // Update token and check if there are more changes
                        token = response.nextChangesToken
                        hasMore = response.hasMore

                        logger.d("Has more changes: $hasMore")
                    }

                    // Save the new token for next time
                    onNewToken(token)
                    logger.d("Successfully pulled changes for ${T::class.simpleName}")
                }
            }
        } catch (e: Exception) {
            logger.e("Error pulling changes for ${T::class.simpleName}", e)
        }
    }

    /**
     * Pull all changes from Health Connect for all data types.
     * This should be called periodically (e.g., by a worker) to sync new data.
     */
    suspend fun pullAllChanges() {
        if (healthConnectTokensImpl == null) {
            logger.w("HealthConnectTokensImpl is null, cannot pull changes")
            return
        }

        logger.d("Pulling all changes from Health Connect")

        // Pull steps changes
        pullChangesForType<StepsRecord>(
            currentToken = healthConnectTokensImpl.getStepsToken(),
            onNewToken = { healthConnectTokensImpl.updateStepsToken(it) },
            processUpsert = { record ->
                val stepsData = record.toDto()
                try {
                    val success = repository.sendStepsData(stepsData)
                    if (!success && stepDAO != null) {
                        val startTimestampStr = stepsData.start_timestamp.toString()
                        val endTimestampStr = stepsData.end_timestamp.toString()
                        val startTimestamp = if (startTimestampStr.endsWith("Z")) startTimestampStr else "${startTimestampStr}Z"
                        val endTimestamp = if (endTimestampStr.endsWith("Z")) endTimestampStr else "${endTimestampStr}Z"

                        val existing = stepDAO.getStepsByTimestamps(startTimestamp, endTimestamp)
                        if (existing == null) {
                            stepDAO.insert(
                                com.lemurs.lemurs_app.data.local.passiveData.Step(
                                    steps = stepsData.steps.toInt(),
                                    startTimestamp = startTimestamp,
                                    endTimestamp = endTimestamp,
                                    appSource = stepsData.appSource
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    logger.e("Error processing steps change: $e")
                }
            }
        )

        // Pull calories changes
        pullChangesForType<TotalCaloriesBurnedRecord>(
            currentToken = healthConnectTokensImpl.getCaloriesToken(),
            onNewToken = { healthConnectTokensImpl.updateCaloriesToken(it) },
            processUpsert = processUpsert@{ record ->
                val caloriesData = record.toDto()

                try {
                    val success = repository.sendCaloriesData(caloriesData)
                    if (!success && calorieDAO != null) {
                        val startTimestampStr = caloriesData.start_timestamp.toString()
                        val endTimestampStr = caloriesData.end_timestamp.toString()
                        val startTimestamp = if (startTimestampStr.endsWith("Z")) startTimestampStr else "${startTimestampStr}Z"
                        val endTimestamp = if (endTimestampStr.endsWith("Z")) endTimestampStr else "${endTimestampStr}Z"

                        val existing = calorieDAO.getCaloriesByTimestamps(startTimestamp, endTimestamp)
                        if (existing == null) {
                            calorieDAO.insert(
                                com.lemurs.lemurs_app.data.local.passiveData.Calorie(
                                    calories = caloriesData.calories,
                                    startTimestamp = startTimestamp,
                                    endTimestamp = endTimestamp,
                                    appSource = caloriesData.appSource
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    logger.e("Error processing calories change: $e")
                }
            }
        )

        // Pull distance changes
        pullChangesForType<DistanceRecord>(
            currentToken = healthConnectTokensImpl.getDistanceToken(),
            onNewToken = { healthConnectTokensImpl.updateDistanceToken(it) },
            processUpsert = { record ->
                val distanceData = record.toDto()
                try {
                    val success = repository.sendDistanceData(distanceData)
                    if (!success && distanceDAO != null) {
                        val startTimestamp = if (distanceData.start_timestamp.toString().endsWith("Z"))
                            distanceData.start_timestamp.toString() else "${distanceData.start_timestamp}Z"
                        val endTimestamp = if (distanceData.end_timestamp.toString().endsWith("Z"))
                            distanceData.end_timestamp.toString() else "${distanceData.end_timestamp}Z"

                        val existing = distanceDAO.getDistanceByTimestamps(startTimestamp, endTimestamp)
                        if (existing == null) {
                            distanceDAO.insert(
                                com.lemurs.lemurs_app.data.local.passiveData.Distance(
                                    distance = distanceData.distance,
                                    startTimestamp = startTimestamp,
                                    endTimestamp = endTimestamp,
                                    appSource = distanceData.appSource
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    logger.e("Error processing distance change: $e")
                }
            }
        )

        // Pull speed changes
        pullChangesForType<SpeedRecord>(
            currentToken = healthConnectTokensImpl.getSpeedToken(),
            onNewToken = { healthConnectTokensImpl.updateSpeedToken(it) },
            processUpsert = { record ->
                val speedData = record.toDto()
                try {
                    val success = repository.sendSpeedData(speedData)
                    if (!success && speedDAO != null) {
                        val startTimestamp = if (speedData.start_timestamp.toString().endsWith("Z"))
                            speedData.start_timestamp.toString() else "${speedData.start_timestamp}Z"
                        val endTimestamp = if (speedData.end_timestamp.toString().endsWith("Z"))
                            speedData.end_timestamp.toString() else "${speedData.end_timestamp}Z"

                        val existing = speedDAO.getSpeedByTimestamps(startTimestamp, endTimestamp)
                        if (existing == null) {
                            speedDAO.insert(
                                com.lemurs.lemurs_app.data.local.passiveData.Speed(
                                    speed = speedData.speed.firstOrNull() ?: 0.0,
                                    startTimestamp = startTimestamp,
                                    endTimestamp = endTimestamp,
                                    appSource = speedData.appSource
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    logger.e("Error processing speed change: $e")
                }
            }
        )

        // You can add similar blocks for other data types (sleep, heart rate, exercise, power)

        logger.d("Completed pulling all changes")
    }
}

enum class HealthConnectAvailability {
    INSTALLED,
    NOT_INSTALLED,
    NOT_SUPPORTED,
}
