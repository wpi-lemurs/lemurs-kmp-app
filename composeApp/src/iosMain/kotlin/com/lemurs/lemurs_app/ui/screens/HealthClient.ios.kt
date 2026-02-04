package com.lemurs.lemurs_app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.data.health.IOSHealthKitBridgeProvider
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * iOS implementation of HealthScreen.
 *
 * This uses HealthKit (Apple's health data API) instead of Health Connect (Google's).
 *
 * HealthKit on iOS requires:
 * 1. HealthKit capability added to the app in Xcode
 * 2. Usage descriptions in Info.plist (NSHealthShareUsageDescription, NSHealthUpdateUsageDescription)
 * 3. Authorization request at runtime
 *
 * Note: HealthKit is only available on iOS devices (not simulators for some features)
 */
@Composable
actual fun HealthScreen(onNavigateTo: (String) -> Unit) {
    val logger = remember { Logger.withTag("HealthKit-iOS") }
    val scope = rememberCoroutineScope()

    var healthKitAvailable by remember { mutableStateOf<HealthKitAvailability>(HealthKitAvailability.CHECKING) }
    var permissionsGranted by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Checking HealthKit availability...") }

    // Check HealthKit availability on launch
    LaunchedEffect(Unit) {
        scope.launch {
            healthKitAvailable = checkHealthKitAvailability()
            when (healthKitAvailable) {
                HealthKitAvailability.AVAILABLE -> {
                    statusMessage = "HealthKit is available"
                    // Check if permissions are already granted
                    permissionsGranted = checkHealthKitPermissions()
                    if (permissionsGranted) {
                        statusMessage = "HealthKit permissions granted"
                        // Initialize data collection
                        initializeHealthKitDataCollection()
                    } else {
                        statusMessage = "HealthKit permissions not yet granted"
                    }
                }
                HealthKitAvailability.NOT_AVAILABLE -> {
                    statusMessage = "HealthKit is not available on this device"
                }
                HealthKitAvailability.CHECKING -> {
                    statusMessage = "Checking HealthKit availability..."
                }
            }
            logger.i(statusMessage)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Health Data",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (healthKitAvailable) {
                    HealthKitAvailability.AVAILABLE ->
                        if (permissionsGranted) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.secondaryContainer
                    HealthKitAvailability.NOT_AVAILABLE -> MaterialTheme.colorScheme.errorContainer
                    HealthKitAvailability.CHECKING -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (healthKitAvailable) {
                        HealthKitAvailability.AVAILABLE ->
                            if (permissionsGranted) "✅ HealthKit Connected"
                            else "⚠️ HealthKit Available"
                        HealthKitAvailability.NOT_AVAILABLE -> "❌ HealthKit Unavailable"
                        HealthKitAvailability.CHECKING -> "⏳ Checking..."
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Request Permissions Button
        if (healthKitAvailable == HealthKitAvailability.AVAILABLE && !permissionsGranted) {
            Button(
                onClick = {
                    scope.launch {
                        logger.i("Requesting HealthKit permissions")
                        val granted = requestHealthKitPermissions()
                        permissionsGranted = granted
                        statusMessage = if (granted) {
                            "HealthKit permissions granted"
                        } else {
                            "HealthKit permissions denied or partially granted"
                        }

                        if (granted) {
                            initializeHealthKitDataCollection()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Health Permissions")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "This app needs access to your health data to track your progress. " +
                       "You can manage these permissions in Settings > Privacy > Health.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Info for unavailable HealthKit
        if (healthKitAvailable == HealthKitAvailability.NOT_AVAILABLE) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "HealthKit is only available on iPhone and iPad. " +
                       "Some features may not work on the iOS Simulator.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Show connected data types if permissions granted
        if (permissionsGranted) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Connected Health Data",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // List of health data types being tracked
            val healthDataTypes = listOf(
                "Steps" to "Daily step count",
                "Calories" to "Total calories burned",
                "Distance" to "Walking/running distance",
                "Sleep" to "Sleep analysis",
                "Heart Rate" to "Heart rate measurements"
            )

            healthDataTypes.forEach { (name, description) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * HealthKit availability status
 */
enum class HealthKitAvailability {
    CHECKING,
    AVAILABLE,
    NOT_AVAILABLE
}

/**
 * Check if HealthKit is available on this device.
 * Uses the Swift bridge to call HKHealthStore.isHealthDataAvailable()
 */
private fun checkHealthKitAvailability(): HealthKitAvailability {
    val bridge = IOSHealthKitBridgeProvider.bridge
    return if (bridge != null && bridge.isHealthKitAvailable()) {
        HealthKitAvailability.AVAILABLE
    } else if (bridge == null) {
        // Bridge not registered yet, assume checking
        HealthKitAvailability.CHECKING
    } else {
        HealthKitAvailability.NOT_AVAILABLE
    }
}

/**
 * Check if HealthKit permissions have been granted.
 * Uses the Swift bridge to check authorization status.
 *
 * Note: On iOS, we can only check if we've requested authorization,
 * not whether the user actually granted it (for privacy reasons).
 */
private suspend fun checkHealthKitPermissions(): Boolean = suspendCoroutine { continuation ->
    val bridge = IOSHealthKitBridgeProvider.bridge
    if (bridge == null) {
        continuation.resume(false)
        return@suspendCoroutine
    }

    bridge.checkAuthorizationStatus(
        onResult = { isAuthorized ->
            continuation.resume(isAuthorized)
        },
        onError = { _ ->
            continuation.resume(false)
        }
    )
}

/**
 * Request HealthKit permissions from the user.
 * Uses the Swift bridge to call HKHealthStore.requestAuthorization()
 * This will show the iOS Health permissions dialog.
 *
 * Data types requested:
 * - HKQuantityType.quantityType(forIdentifier: .stepCount)
 * - HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned)
 * - HKQuantityType.quantityType(forIdentifier: .distanceWalkingRunning)
 * - HKQuantityType.quantityType(forIdentifier: .heartRate)
 * - HKCategoryType.categoryType(forIdentifier: .sleepAnalysis)
 */
private suspend fun requestHealthKitPermissions(): Boolean = suspendCoroutine { continuation ->
    val bridge = IOSHealthKitBridgeProvider.bridge
    if (bridge == null) {
        continuation.resume(false)
        return@suspendCoroutine
    }

    bridge.requestAuthorization(
        onSuccess = { granted ->
            continuation.resume(granted)
        },
        onError = { _ ->
            continuation.resume(false)
        }
    )
}

/**
 * Initialize HealthKit data collection and background delivery.
 * Uses the Swift bridge to enable background delivery for health data types.
 */
private suspend fun initializeHealthKitDataCollection(): Unit = suspendCoroutine { continuation ->
    val bridge = IOSHealthKitBridgeProvider.bridge
    if (bridge == null) {
        continuation.resume(Unit)
        return@suspendCoroutine
    }

    bridge.enableBackgroundDelivery(
        onSuccess = {
            continuation.resume(Unit)
        },
        onError = { _ ->
            // Log error but don't fail - background delivery is optional
            continuation.resume(Unit)
        }
    )
}

