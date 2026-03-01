package com.lemurs.lemurs_app.data.bluetooth

import com.lemurs.lemurs_app.data.local.passiveData.Bluetooth
import com.lemurs.lemurs_app.data.local.passiveData.BluetoothDAO
import com.lemurs.lemurs_app.data.local.passiveData.BluetoothDataUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock

class BluetoothScannerIos(
    private val bluetoothDAO: BluetoothDAO,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    fun startScan(durationSeconds: Int = 15) {
        val bridge = IOSBluetoothBridgeProvider.bridge
        if (bridge == null) {
            // If you want, log/throw. I’m just no-oping with a message.
            println("❌ IOSBluetoothBridge is not registered")
            return
        }

        bridge.scan(
            durationSeconds = durationSeconds,
            onSuccess = { devices ->
                val now = Clock.System.now().toString()
                val bluetooth = Bluetooth(
                    date = now,
                    dateOfCollection = now,
                    numberOfDevices = devices.size,
                    deviceList = devices.joinToString(";")
                )
                scope.launch {
                    try {
                        bluetoothDAO.insert(bluetooth)
                        println("✅ Inserted BLE row")
                        val bluetoothDataUseCase = BluetoothDataUseCase(bluetoothDAO)
                        bluetoothDataUseCase.call()
                    } catch (e: Exception) {
                        println("❌ Insert failed: ${e.message}")
                    }
                }
                /* Brute force, for Debugging
                runBlocking {
                    println("🟦 [BLE][DB] inserting...")
                    bluetoothDAO.insert(bluetooth)
                    println("✅ [BLE][DB] insert finished")
                    val bluetoothDataUseCase = BluetoothDataUseCase(bluetoothDAO)
                    bluetoothDataUseCase.call()
                    val all = bluetoothDAO.getAll()
                    println("🟩 [BLE][DB] total rows after insert = ${all.size}")
                }
                */
            },
            onError = { err ->
                println("❌ iOS BLE scan error: $err")
            }
        )
    }
}