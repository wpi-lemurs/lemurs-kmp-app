package com.lemurs.lemurs_app.data.bluetooth

import com.lemurs.lemurs_app.data.local.passiveData.Bluetooth
import com.lemurs.lemurs_app.data.local.passiveData.BluetoothDAO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class BluetoothScannerIos(
    private val bluetoothDAO: BluetoothDAO,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
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
                scope.launch { bluetoothDAO.insert(bluetooth) }
            },
            onError = { err ->
                println("❌ iOS BLE scan error: $err")
            }
        )
    }
}