package com.lemurs.lemurs_app.data.bluetooth

interface IOSBluetoothBridge {
    /**
     * Start a BLE scan for `durationSeconds` and return discovered devices as display strings.
     * Example item: "Polar H10 (A1B2-C3...)"
     */
    fun scan(
        durationSeconds: Int,
        onSuccess: (List<String>) -> Unit,
        onError: (String) -> Unit
    )
}

object IOSBluetoothBridgeProvider {
    var bridge: IOSBluetoothBridge? = null
}