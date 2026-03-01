package com.lemurs.lemurs_app.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.data.local.UseCaseResult
import com.lemurs.lemurs_app.data.local.passiveData.Bluetooth
import com.lemurs.lemurs_app.data.local.passiveData.BluetoothDAO
import com.lemurs.lemurs_app.data.local.passiveData.BluetoothDataUseCase
import com.lemurs.lemurs_app.data.local.passiveData.BluetoothDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val REQUEST_ENABLE_BT = 1
private const val SCAN_PERIOD: Long = 10_000 // 10 seconds

class BluetoothFunction(private val context: Activity, private val bluetoothDAO: BluetoothDAO) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    private val logger = Logger.withTag("BluetoothFunction")

    private var scanning = false
//  private val handler = Handler()

    // holds the list of scanned devices.
    private val leDeviceListAdapter = LeDeviceListAdapter()
    init {
        checkBLESupport()
        ensureBluetoothEnabled()
    }


    /** Scans for Bluetooth devices and stores the results in the database. */
    fun permissionsForScan() {
        logger.w("beginning bluetooth permissions scan")
        // for Android 12 and higher app will need BLUETOOTH_SCAN and BLUETOOTH_CONNECT permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            logger.w("Android 12+, requesting Bluetooth SCAN and CONNECT permissions")
            val hasScanPermission =
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
            val hasConnectPermission =
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)

            if (hasScanPermission != PackageManager.PERMISSION_GRANTED ||
                hasConnectPermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    context,
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ),
                    REQUEST_ENABLE_BT,
                )
                logger.w("Requesting Bluetooth SCAN and CONNECT permissions")
                return
            }
        } else {
            // Android < 12 requires location permissions which are not being requested
            // Bluetooth scanning will not work on these versions
            logger.w("Android version < 12 (API < 31) - Bluetooth scanning not supported without location permissions")
            return
        }
        logger.w("Bluetooth permissions already granted")
    }

    fun checkBluetoothPermissions(): Boolean {
        logger.w("checking bluetooth permissions... ")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasScanPermission =
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
            val hasConnectPermission =
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)

            logger.w { "BLUETOOTH_SCAN permission status: $hasScanPermission (${if (hasScanPermission == PackageManager.PERMISSION_GRANTED) "GRANTED" else "DENIED"})" }
            logger.w { "BLUETOOTH_CONNECT permission status: $hasConnectPermission (${if (hasConnectPermission == PackageManager.PERMISSION_GRANTED) "GRANTED" else "DENIED"})" }

            val bluetoothPermissions = hasScanPermission == PackageManager.PERMISSION_GRANTED
                    && hasConnectPermission == PackageManager.PERMISSION_GRANTED

            logger.w("Bluetooth permissions: $bluetoothPermissions")
            return bluetoothPermissions
        } else {
            // Android < 12 requires location permissions which are not being requested
            // Bluetooth scanning will not work on these versions
            logger.w("Android version < 12 (API < 31) - Bluetooth scanning not supported without location permissions")
            return false
        }
    }

    // helper functions to make the code cleaner
    private fun checkBLESupport() {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
//      Toast.makeText(context, "Bluetooth LE is not supported on this device", Toast.LENGTH_SHORT)
//        .show()
            logger.w("Bluetooth LE is not supported on this device")
        }
        if (bluetoothAdapter == null) {
//      Toast.makeText(context, "Bluetooth not available on this device", Toast.LENGTH_SHORT).show()
            logger.w("Bluetooth not available on this device")
        }
    }

    private fun ensureBluetoothEnabled() {
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            ActivityCompat.startActivityForResult(context, enableBtIntent, REQUEST_ENABLE_BT, null)
        }
    }

    /** Scans for [SCAN_PERIOD] ms */
    @SuppressLint("MissingPermission")
    suspend fun scan(): UseCaseResult<Any> = withContext(Dispatchers.IO){
        logger.w("=== Bluetooth Scan Request Started ===")

        if (!checkBluetoothPermissions()) {
            logger.w("bluetooth permissions not granted, cannot scan")
            return@withContext UseCaseResult.Failure()
        }

        // Check Bluetooth adapter state
        if (bluetoothAdapter == null) {
            logger.e("Bluetooth adapter is null!")
            return@withContext UseCaseResult.Failure()
        }

        if (!bluetoothAdapter.isEnabled) {
            logger.e("Bluetooth is not enabled!")
            return@withContext UseCaseResult.Failure()
        }

        if (bluetoothLeScanner == null) {
            logger.e("Bluetooth LE scanner is null!")
            return@withContext UseCaseResult.Failure()
        }

        logger.w("Bluetooth adapter state: enabled=${bluetoothAdapter.isEnabled}")

        if (scanning) {
            scanning = false
            bluetoothLeScanner.stopScan(leScanCallback)
            logger.w("Stopped scanning (was already scanning)")
            return@withContext UseCaseResult.Failure()
        }

        logger.w("Starting BLE scan...")
        leDeviceListAdapter.clear()
        logger.w("Device list cleared, starting fresh scan")
        scanning = true

        try {
            bluetoothLeScanner.startScan(leScanCallback)
            logger.w("BLE scan started successfully, waiting for ${SCAN_PERIOD}ms")
        } catch (e: Exception) {
            logger.e("Exception starting BLE scan: ${e.message}", e)
            scanning = false
            return@withContext UseCaseResult.Failure()
        }

        Handler(Looper.getMainLooper()).postDelayed(
            {
                logger.w("=== Scan timeout reached after ${SCAN_PERIOD}ms ===")
                scanning = false

                try {
                    bluetoothLeScanner.stopScan(leScanCallback)
                    logger.w("BLE scan stopped successfully")
                } catch (e: Exception) {
                    logger.e("Exception stopping BLE scan: ${e.message}", e)
                }

                val deviceCount = leDeviceListAdapter.getDevices().size
                logger.w("Scan completed. Total devices found: $deviceCount")

                if (deviceCount == 0) {
                    logger.w("WARNING: No Bluetooth devices were detected during the scan!")
                    logger.w("This could mean:")
                    logger.w("  - No BLE devices are nearby")
                    logger.w("  - Location services are disabled (required on Android < 12)")
                    logger.w("  - Bluetooth is not functioning properly")
                }

                // Log each device found
                leDeviceListAdapter.getDevices().forEachIndexed { index, device ->
                    val btDevice = device as? android.bluetooth.BluetoothDevice
                    if (btDevice != null) {
                        logger.w("Device ${index + 1}: Name=${btDevice.name ?: "Unknown"}, Address=${btDevice.address}")
                    }
                }

                // insert the data into the database
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val deviceList = leDeviceListAdapter.getDevices()
                            .mapNotNull { it as? android.bluetooth.BluetoothDevice }
                            .map { makeBluetoothDevice(it) }
                            .joinToString(separator = ",") { it.address }

                        logger.w("Inserting Bluetooth data into database:")
                        logger.w("  - Number of devices: $deviceCount")
                        logger.w("  - Device addresses: $deviceList")

                        bluetoothDAO.insert(
                            Bluetooth(
                                date = System.currentTimeMillis().toString(),
                                dateOfCollection = kotlinx.datetime.Clock.System.now().toString(),
                                numberOfDevices = deviceCount,
                                deviceList = deviceList
                            )
                        )
                        logger.w("Bluetooth data inserted into database successfully")

                        try {
                            val bluetoothDataUseCase = BluetoothDataUseCase(bluetoothDAO)
                            val result = bluetoothDataUseCase.call()
                            when (result) {
                                is UseCaseResult.Success -> logger.w("Bluetooth data submitted to API successfully")
                                is UseCaseResult.Failure -> logger.w("Bluetooth data submission to API failed")
                            }
                        } catch (e: java.nio.channels.UnresolvedAddressException) {
                            logger.e("Network error: Cannot resolve API host. Check network connectivity and DNS.", e)
                        } catch (e: java.net.UnknownHostException) {
                            logger.e("Network error: Unknown host. Check network connectivity.", e)
                        } catch (e: Exception) {
                            logger.e("Error submitting Bluetooth data to API: ${e.message}", e)
                        }
                    } catch (e: Exception) {
                        logger.e("Error inserting Bluetooth data into remote database: ${e.message}", e)
                    }
                }
            },
            SCAN_PERIOD,
        )

        return@withContext UseCaseResult.Success(Any())
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun makeBluetoothDevice(device: android.bluetooth.BluetoothDevice): BluetoothDevice {
        val name = device.name ?: ""
        val address = device.address ?: ""
        return BluetoothDevice(name, address)
    }

    // Location permissions are not requested per project requirements
    // This function is no longer used
    /*
    private fun promptEnableLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        logger.w("isGpsEnabled: $isGpsEnabled, isNetworkEnabled: $isNetworkEnabled")

        if (!isGpsEnabled && !isNetworkEnabled) {
            logger.w("requesting location")
            AlertDialog.Builder(context)
                .setTitle("Enable Location")
                .setMessage(
                    "Location services are off. Lemurs needs location services to work properly. Would you like to enable them?"
                )
                .setPositiveButton("Yes") { dialog, _ ->
                    dialog.dismiss()
                    // opens location settings automatically
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                .show()
        }
        logger.w("done location prompt")
        return
    }
    */

    private val leScanCallback: ScanCallback =
        object : ScanCallback() {
            @SuppressLint("MissingPermission")
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                try {
                    logger.w("SCAN RESULT RECEIVED!")
                    val deviceName = result.device.name ?: "Unknown"
                    val deviceAddress = result.device.address ?: "No Address"
                    logger.w("Found device: Name=$deviceName, Address=$deviceAddress, RSSI=${result.rssi}")
                    leDeviceListAdapter.addDevice(result.device)
                    logger.w("Device added to list. Total devices: ${leDeviceListAdapter.getDevices().size}")
                } catch (e: Exception) {
                    logger.e("Error processing scan result: ${e.message}", e)
                }
            }

            @SuppressLint("MissingPermission")
            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                super.onBatchScanResults(results)
                logger.w("Batch scan results received: ${results?.size ?: 0} devices")
                results?.forEach { result ->
                    try {
                        val deviceName = result.device.name ?: "Unknown"
                        val deviceAddress = result.device.address ?: "No Address"
                        logger.w("Batch device: Name=$deviceName, Address=$deviceAddress")
                        leDeviceListAdapter.addDevice(result.device)
                    } catch (e: Exception) {
                        logger.e("Error processing batch scan result: ${e.message}", e)
                    }
                }
                logger.w("Total devices after batch: ${leDeviceListAdapter.getDevices().size}")
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                val errorMessage = when (errorCode) {
                    SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
                    SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "Application registration failed"
                    SCAN_FAILED_FEATURE_UNSUPPORTED -> "Feature unsupported"
                    SCAN_FAILED_INTERNAL_ERROR -> "Internal error"
                    else -> "Unknown error code: $errorCode"
                }
                logger.e("BLE Scan failed: $errorMessage")
                scanning = false
            }
        }
}

class LeDeviceListAdapter {
    private val mLeDevices = mutableSetOf<Any>()

    fun addDevice(device: Any) {
        mLeDevices.add(device)
    }

    fun getDevices(): Set<Any> {
        return mLeDevices
    }

    fun clear() {
        mLeDevices.clear()
    }
}

// Code guideline from
// https://developer.android.com/develop/connectivity/bluetooth/ble/find-ble-devices [any reason why number of devices refuses to work]