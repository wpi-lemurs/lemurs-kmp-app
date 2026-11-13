package com.lemurs.lemurs_app.bluetooth

import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import com.lemurs.lemurs_app.data.local.UseCaseResult
import com.lemurs.lemurs_app.data.local.passiveData.Bluetooth
import com.lemurs.lemurs_app.data.local.passiveData.BluetoothDAO
import io.realm.kotlin.notifications.ResultsChange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class BluetoothFunctionTest {

    private class FakeBluetoothDAO : BluetoothDAO {
        override suspend fun getBluetoothData(id: String): Bluetooth? = null
        override suspend fun getBluetoothID(date: String): Bluetooth? = null
        override suspend fun insert(item: Bluetooth) {}
        override suspend fun getAll(): List<Bluetooth> = emptyList()
        override fun getAllAsFlow(): Flow<ResultsChange<Bluetooth>> = emptyFlow()
        override suspend fun delete(item: Bluetooth): Boolean = true
        override suspend fun deleteAll() {}
    }

    private class FakeContext(
        private val permissionResult: Int = PackageManager.PERMISSION_DENIED
    ) : ContextWrapper(null) {
        override fun getSystemService(name: String): Any? = null
        override fun getSystemServiceName(serviceClass: Class<*>): String? = null
        override fun getPackageManager(): PackageManager? = null
        override fun checkPermission(permission: String, pid: Int, uid: Int): Int = permissionResult
    }

    private class FakeActivity(
        private val permissionResult: Int = PackageManager.PERMISSION_DENIED
    ) : Activity() {
        var startedActivitiesCount = 0

        override fun getSystemService(name: String): Any? = null
        override fun getSystemServiceName(serviceClass: Class<*>): String? = null
        override fun getPackageManager(): PackageManager? = null
        override fun checkPermission(permission: String, pid: Int, uid: Int): Int = permissionResult

        override fun startActivityForResult(intent: Intent?, requestCode: Int, options: Bundle?) {
            startedActivitiesCount++
            if (permissionResult != PackageManager.PERMISSION_GRANTED) {
                throw SecurityException("Need android.permission.BLUETOOTH_CONNECT permission")
            }
        }
    }

    @Test
    fun `initializing BluetoothFunction with Bluetooth off and permissions denied does not crash or start enable intent`() {
        val activity = FakeActivity(permissionResult = PackageManager.PERMISSION_DENIED)
        val bluetoothFunction = BluetoothFunction(activity, FakeBluetoothDAO())

        assertEquals(0, activity.startedActivitiesCount)
        assertFalse(bluetoothFunction.checkBluetoothPermissions())
    }

    @Test
    fun `ensureBluetoothEnabled does not launch enable intent when permissions are denied`() {
        val activity = FakeActivity(permissionResult = PackageManager.PERMISSION_DENIED)
        val bluetoothFunction = BluetoothFunction(activity, FakeBluetoothDAO())

        bluetoothFunction.ensureBluetoothEnabled(activity)

        assertEquals(0, activity.startedActivitiesCount)
    }

    @Test
    fun `scan returns Failure cleanly when permissions are denied or Bluetooth is off`() = runTest {
        val context = FakeContext(permissionResult = PackageManager.PERMISSION_DENIED)
        val bluetoothFunction = BluetoothFunction(context, FakeBluetoothDAO())

        val result = bluetoothFunction.scan()

        assertIs<UseCaseResult.Failure<*>>(result)
    }
}

