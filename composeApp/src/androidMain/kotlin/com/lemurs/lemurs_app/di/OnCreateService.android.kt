package com.lemurs.lemurs_app.di

import android.Manifest
import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity.APP_OPS_SERVICE
import android.app.AppOpsManager
import android.app.AppOpsManager.MODE_ALLOWED
import android.app.AppOpsManager.OPSTR_GET_USAGE_STATS
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.WorkManager
import androidx.core.content.edit
import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.bluetooth.BluetoothFunction
import com.lemurs.lemurs_app.bluetooth.BluetoothScheduler
import com.lemurs.lemurs_app.data.AndroidActivityLauncherProvider
import com.lemurs.lemurs_app.data.AndroidActivityProvider
import com.lemurs.lemurs_app.data.AndroidContextProvider
import com.lemurs.lemurs_app.data.local.SendDataScheduler
import com.lemurs.lemurs_app.data.repositories.AppRepository
import com.lemurs.lemurs_app.data.local.passiveData.StepDAO
import com.lemurs.lemurs_app.data.local.passiveData.DistanceDAO
import com.lemurs.lemurs_app.data.local.passiveData.CalorieDAO
import com.lemurs.lemurs_app.data.local.passiveData.SpeedDAO
import com.lemurs.lemurs_app.data.local.passiveData.BluetoothDAO
import com.lemurs.lemurs_app.data.screentime.ScreentimeScheduler
import com.lemurs.lemurs_app.health.HealthConnectAvailability
import com.lemurs.lemurs_app.health.HealthConnectViewModel
import com.lemurs.lemurs_app.health.HealthDataScheduler
import com.lemurs.lemurs_app.util.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class OnCreateService(private val context: Context, private val app: Application) : KoinComponent {
    val logger = Logger.withTag("OnCreateService")

    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var usageStatsPermissionLauncher: ActivityResultLauncher<Intent>
    private lateinit var healthDataPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var bluetoothPermissionLauncher: ActivityResultLauncher<Array<String>>
    
    // Separate flags for each scheduler type
    private var hasScheduledNotifications = false
    private var hasScheduledHealth = false
    private var hasScheduledBluetooth = false
    private var hasScheduledScreentime = false
    private var hasScheduledSurveys = false

    private val appRepository: AppRepository by inject()
    private val stepsDAO: StepDAO by inject()
    private val caloriesDAO: CalorieDAO by inject()
    private val distanceDAO: DistanceDAO by inject()
    private val speedDAO: SpeedDAO by inject()
    private val bluetoothDAO: BluetoothDAO by inject()
    val healthConnectTokensImpl: com.lemurs.lemurs_app.data.datastore.HealthConnectTokensImpl by inject()
    private val health = HealthConnectViewModel(
        app,
        appRepository,
        stepsDAO,
        caloriesDAO,
        distanceDAO,
        speedDAO,
        null,
        null,
        healthConnectTokensImpl
    )
    private lateinit var appOpsManager: AppOpsManager


    @RequiresApi(Build.VERSION_CODES.Q)
    fun setupPermissionLaunchers(activity: ComponentActivity) {
        // Request Notification Permission (callback to request screenime)
        notificationPermissionLauncher =
            activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) {
                    logger.w("notification permission granted, now scheduling notification workers")
                    scheduleNotificationWorkers()
                    logger.w("notification permission granted, now requesting usage stats")
                    requestUsageStatsPermission()
                } else {
                    logger.w("notification permission denied, continuing to usage stats")
                    requestUsageStatsPermission()
                }
            }

        // Request Usage Stats Permission (call back to request health data)
        usageStatsPermissionLauncher =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                logger.w("usage stats permission callback, checking if granted")
                if (checkScreentimePermissions()) {
                    logger.w("usage stats permission granted, scheduling screentime workers")
                    scheduleScreentimeWorkers()
                } else {
                    logger.w("usage stats permission still not granted")
                }
                logger.w("now requesting health data")
                requestHealthDataPermission()
            }

        // Request Health Data Permission (call back to request bluetooth)
        healthDataPermissionLauncher =
            activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                val healthPermissionsGranted = permissions.values.all { it }
                logger.w("Health data permissions result: $permissions, all granted: $healthPermissionsGranted")

                if (healthPermissionsGranted && checkHealthDataAvailable(health)) {
                    logger.w("health permissions granted, scheduling health workers")
                    scheduleHealthWorkers()
                } else {
                    logger.w("health permissions not fully granted or health data not available")
                }

                logger.w("health data permission request completed, now requesting bluetooth")
                requestBluetoothPermission()
            }

        // Request Bluetooth Permissions
        bluetoothPermissionLauncher =
            activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                val allGranted = permissions.all { it.value }
                if (allGranted) {
                    logger.w("bluetooth permissions granted, scheduling bluetooth workers")
                    scheduleBluetoothWorkers()
                } else {
                    logger.w("not all bluetooth permissions granted")
                }
                logger.w("bluetooth permission callback completed, scheduling remaining workers")
                scheduleRemainingWorkers()
            }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun onCreate(activity: Activity) {
        AndroidContextProvider.setContext(context)
        AndroidActivityProvider.setActivity(activity)
        AndroidActivityLauncherProvider.setActivityMultiple(activity as ComponentActivity, context)
        appOpsManager = context.getSystemService(APP_OPS_SERVICE)!! as AppOpsManager

        //start chain with notifications:
        requestNotificationPermission()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!checkNotificationPermissions()) {
                logger.w("launching notification permission request")
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // If notifications are already enabled, schedule workers immediately
                logger.w("Notifications already enabled, scheduling workers")
                scheduleNotificationWorkers()
                requestUsageStatsPermission()
            }
        } else {
            // For older Android versions, notifications are enabled by default
            logger.w("Android < 13, notifications enabled by default, scheduling workers")
            scheduleNotificationWorkers()
            requestUsageStatsPermission()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestUsageStatsPermission() {
        if (!checkScreentimePermissions()) {
            logger.w("launching screentime permission request")
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            usageStatsPermissionLauncher.launch(intent)
        } else {
            logger.w("Screentime permissions already granted, continuing to health data")
            scheduleScreentimeWorkers()
            requestHealthDataPermission()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestHealthDataPermission() {
        CoroutineScope(Dispatchers.Main).launch {
            if (checkHealthDataAvailable(health)) {
                if (!checkHealthDataPermissions(health)) {
                    logger.w("launching health permission request (from OnCreateService)")
                    val permissionsArray: Array<String> = health.permissions.toTypedArray()
                    healthDataPermissionLauncher.launch(permissionsArray)
                } else {
                    logger.w("Health permissions already granted (from OnCreateService check), continuing to bluetooth")
                    scheduleHealthWorkers()
                    requestBluetoothPermission()
                }
            } else {
                logger.w("Health data not available (from OnCreateService), continuing to bluetooth")
                requestBluetoothPermission()
            }
        }
    }

    //dana's phone api is too low
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestBluetoothPermission() {
        logger.w("launching bluetooth permission request")
//            bluetoothPermissionLauncher.launch(
//                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
//            )
        val activity = requireNotNull(AndroidActivityProvider.activity)
        val bluetoothFunction = BluetoothFunction(activity, bluetoothDAO)
        if (!checkBluetoothPermissions()) {
            logger.w("requesting bluetooth permissions")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                bluetoothPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                    )
                )
            } else {
                bluetoothFunction.permissionsForScan()
            }
        } else {
            scheduleBluetoothWorkers()
            scheduleRemainingWorkers()
        }
        logger.w("done bluetooth permission request")
    }

    //returns true if permissions allowed
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkScreentimePermissions(): Boolean {
        val appOpsManager = context.getSystemService(APP_OPS_SERVICE)!! as AppOpsManager
        var mode =
            appOpsManager.unsafeCheckOpNoThrow(OPSTR_GET_USAGE_STATS, Process.myUid(), "com.lemurs")
        if (mode == 3) {
            val permissions =
                context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
            mode = if (permissions) 0 else 1
        }
        logger.w("screentime permissions mode: " + mode)
        return mode == MODE_ALLOWED
    }

    //returns true if permissions allowed
    private fun checkNotificationPermissions(): Boolean {
        val notificationPermissions =
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        logger.w("notification permissions: " + notificationPermissions)
        return notificationPermissions
    }

    //returns true if permissions allowed
    private fun checkBluetoothPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasScanPermission =
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
            val hasConnectPermission =
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)

            val bluetoothPermissions =
                hasScanPermission == PackageManager.PERMISSION_GRANTED &&
                        hasConnectPermission == PackageManager.PERMISSION_GRANTED

            logger.w("Bluetooth permissions (Android 12+): $bluetoothPermissions")
            logger.w("  SCAN: ${if (hasScanPermission == PackageManager.PERMISSION_GRANTED) "GRANTED" else "DENIED"}")
            logger.w("  CONNECT: ${if (hasConnectPermission == PackageManager.PERMISSION_GRANTED) "GRANTED" else "DENIED"}")
            return bluetoothPermissions
        } else {
            // Android < 12 requires location permissions which are not being requested
            // Bluetooth scanning will not work on these versions
            logger.w("Android version < 12 (API < 31) - Bluetooth scanning not supported without location permissions")
            return false
        }
    }

    //returns true if permissions allowed
    private suspend fun checkHealthDataPermissions(health: HealthConnectViewModel): Boolean {
        return withContext(Dispatchers.IO) {
            health.checkAvailability()
            val result = health.hasAllPermissions(health.permissions)
            logger.w("OnCreateHealth Data permissions: $result")
            result
        }
    }

    private fun checkHealthDataAvailable(health: HealthConnectViewModel): Boolean {
        val availability = health.checkAvailability()
        logger.w("Health Data availability: " + availability)
        val res = (availability == HealthConnectAvailability.INSTALLED)
        return res
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun scheduleNotificationWorkers() {
        logger.w("Public entry point for scheduling notification workers...")

        // Check if notification system has already been set up (one-time setup)
        val prefs = context.getSharedPreferences("lemurs_notification_prefs", Context.MODE_PRIVATE)
        val isSetup = prefs.getBoolean("notification_system_setup", false)

        if (!isSetup) {
            logger.w("First time setup - scheduling persistent notification alarms")

            scheduleNotificationWorkersInternal()

            // Mark as setup so it doesn't run again
            prefs.edit { putBoolean("notification_system_setup", true) }
            logger.w("Notification system setup completed and marked as configured")
        } else {
            logger.w("Notification system already configured, skipping setup")
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun scheduleNotificationWorkersInternal() {
        if (hasScheduledNotifications) return

        logger.w("Scheduling notification workers...")
        if (!checkNotificationPermissions()) {
            logger.w("Notification permissions not granted")
            return
        }

        // Check if context is properly set before proceeding
        if (AndroidContextProvider.context == null) {
            logger.w("Context not yet available, delaying notification scheduling")
            Handler(Looper.getMainLooper()).postDelayed({
                logger.w("Retrying notification scheduling after delay")
                scheduleNotificationWorkersInternal()
            }, 1000)
            return
        }

        try {
            val workManager = WorkManager.getInstance(context)
            val notificationScheduler = NotificationScheduler("")

            notificationScheduler.scheduleDailyNotificationSetup()
            notificationScheduler.scheduleWeeklySurveyNotification()

            // Disable hourly random reminders to ensure exactly 3 notifications per window
            workManager.cancelUniqueWork("random notification schedule")
            workManager.pruneWork()

            hasScheduledNotifications = true
            logger.w("Notification scheduling completed")
        } catch (e: Exception) {
            logger.e("Failed to schedule notifications: ${e.message}", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun scheduleHealthWorkers() {
        if (hasScheduledHealth) return

        if (!checkHealthDataAvailable(health)) {
            logger.w("Health data not available")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val workManager = WorkManager.getInstance(context)
                val healthScheduler = HealthDataScheduler()

                if (checkHealthDataPermissions(health)) {
                    logger.w("Scheduling health data with permissions granted")
                    health.initializeChangesTokens()
                } else {
                    logger.w("Not all permissions on for health - will retry in health data scheduler")
                }

                workManager.pruneWork()
                healthScheduler.scheduleHealth()

                hasScheduledHealth = true
                logger.w("Health scheduling completed")

                // Add small delay to allow permissions to be processed
                delay(500)
            } catch (e: Exception) {
                logger.e("Failed to schedule health workers: ${e.message}", e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun scheduleBluetoothWorkers() {
        if (hasScheduledBluetooth) return

        try {
            val bluetoothScheduler = BluetoothScheduler()
            bluetoothScheduler.schedule()

            hasScheduledBluetooth = true
            logger.w("Bluetooth scheduling completed")
        } catch (e: Exception) {
            logger.e("Failed to schedule bluetooth workers: ${e.message}", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun scheduleScreentimeWorkers() {
        if (hasScheduledScreentime) return

        if (!checkScreentimePermissions()) {
            logger.w("Screentime permissions not granted")
            return
        }

        try {
            val workManager = WorkManager.getInstance(context)
            workManager.pruneWork()

            val screentimeScheduler = ScreentimeScheduler()
            val sendDataScheduler = SendDataScheduler()

            screentimeScheduler.schedule()
            sendDataScheduler.scheduleScreentime()

            hasScheduledScreentime = true
            logger.w("Screentime scheduling completed")
        } catch (e: Exception) {
            logger.e("Failed to schedule screentime workers: ${e.message}", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun scheduleSurveyWorkers() {
        if (hasScheduledSurveys) return

        CoroutineScope(Dispatchers.Main).launch {
            try {
                logger.w("Attempting to handle any unsent survey responses...")
                val allSent = appRepository.handleSurveyResponse()
                if (allSent) {
                    logger.w("All pending survey responses were successfully sent.")
                } else {
                    logger.w("Some survey responses could not be sent. They will be retried later.")
                    SendDataScheduler().scheduleSurveyResponse()
                }

                hasScheduledSurveys = true
                logger.w("Survey scheduling completed")
            } catch (e: Exception) {
                logger.e("Failed to schedule survey workers: ${e.message}", e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun scheduleRemainingWorkers() {
        logger.w("Scheduling remaining workers...")
        // Schedule workers that don't require special permissions
        scheduleSurveyWorkers()
    }


}