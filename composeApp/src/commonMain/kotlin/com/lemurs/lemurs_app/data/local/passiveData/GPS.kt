package com.lemurs.lemurs_app.data.local.passiveData

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

open class GPS() : RealmObject {
    @PrimaryKey
    private var ID: ObjectId = ObjectId()

    fun getID(): String {
        return this.ID.toHexString()
    }
    var date: String = ""
    var longitude: Double = 0.0
    var latitude: Double = 0.0
    var altitude: Double = 0.0
    var speed: Int = 0 // assuming its a whole number?
    var timestamp: String = ""

    constructor(
        date: String,
        longitude: Double,
        latitude: Double,
        altitude: Double,
        speed: Int,
        timestamp: String
    ) : this() {
        this.date = date
        this.longitude = longitude
        this.latitude = latitude
        this.altitude = altitude
        this.speed = speed
        this.timestamp = timestamp
    }
}
