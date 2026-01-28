package com.lemurs.lemurs_app.data.local.passiveData

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

open class Health() : RealmObject {
    @PrimaryKey
    private var ID: ObjectId = ObjectId()

    fun getID(): String {
        return this.ID.toHexString()
    }
    var date: String = ""
    var totalSteps: Int? = null
    var totalCalories: Double? = null
    var totalDistance: Double? = null
    var averageSpeed: Double? = null

    constructor(
        date: String,
        totalSteps: Int?,
        totalCalories: Double?,
        totalDistance: Double?,
        averageSpeed: Double?
    ) : this() {
        this.date = date
        this.totalSteps = totalSteps
        this.totalCalories = totalCalories
        this.totalDistance = totalDistance
        this.averageSpeed = averageSpeed
    }
}
