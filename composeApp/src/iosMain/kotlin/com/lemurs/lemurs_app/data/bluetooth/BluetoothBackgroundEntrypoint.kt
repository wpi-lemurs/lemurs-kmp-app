package com.lemurs.lemurs_app.data.bluetooth

import com.lemurs.lemurs_app.data.local.passiveData.BluetoothDAO
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Swift can call this during BGAppRefreshTask / app relaunch.
 * It uses Koin to get the DAO and runs a short scan.
 */
object BluetoothBackgroundEntrypoint : KoinComponent {
    private val dao: BluetoothDAO by inject()

    fun runOnce(durationSeconds: Int = 15) {
        BluetoothScannerIos(dao).startScan(durationSeconds)
    }
}
fun runBluetoothBackgroundScan(durationSeconds: Int = 15) {
    BluetoothBackgroundEntrypoint.runOnce(durationSeconds)
}