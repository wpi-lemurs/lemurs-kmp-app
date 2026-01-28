package com.lemurs.lemurs_app.data.local.passiveData

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

open class Bluetooth() : RealmObject {
    @PrimaryKey
    private var ID: ObjectId = ObjectId()

    fun getID(): String {
        return this.ID.toHexString()
    }
    var date: String = ""
    var dateOfCollection: String = ""
    var numberOfDevices: Int = 0
    var deviceList: String = ""

    constructor(
        date: String,
        dateOfCollection: String,
        numberOfDevices: Int,
        deviceList: String
    ) : this() {
        this.date = date
        this.dateOfCollection = dateOfCollection
        this.numberOfDevices = numberOfDevices
        this.deviceList = deviceList
    }
}

open class BluetoothDevice() : RealmObject {
    @PrimaryKey
    private var ID: ObjectId = ObjectId()

    fun getID(): String {
        return this.ID.toHexString()
    }
    var name: String = ""
    var address: String = ""

    constructor(nameString: String, addressString: String) : this() {
        this.name = nameString
        this.address = addressString
    }
}
