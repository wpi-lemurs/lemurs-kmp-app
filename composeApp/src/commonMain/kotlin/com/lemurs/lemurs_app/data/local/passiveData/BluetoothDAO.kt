package com.lemurs.lemurs_app.data.local.passiveData

import com.lemurs.lemurs_app.data.local.BaseDAO
import com.lemurs.lemurs_app.data.local.BaseDAOImpl
import io.realm.kotlin.ext.query

interface BluetoothDAO : BaseDAO<Bluetooth> {
    suspend fun getBluetoothData(id: String): Bluetooth?
    suspend fun getBluetoothID(date: String): Bluetooth?
}

class BluetoothDAOImpl : BaseDAOImpl<Bluetooth>(Bluetooth::class), BluetoothDAO {
    override suspend fun getBluetoothData(id: String): Bluetooth? {
        return realm().query<Bluetooth>("ID == $0", id).first().find()
    }

    override suspend fun getBluetoothID(date: String): Bluetooth? {
        return realm().query<Bluetooth>("date == $0", date).first().find()
    }
}
