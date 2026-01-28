package com.lemurs.lemurs_app.data.local.passiveData

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

open class Calorie() : RealmObject {
    @PrimaryKey
    private var ID: ObjectId = ObjectId()

    fun getID(): String {
        return this.ID.toHexString()
    }
    var calories: Int = 0
    var startTimestamp: String = ""
    var endTimestamp: String = ""
    var appSource: String? = null

    constructor(
        calories: Int,
        startTimestamp: String,
        endTimestamp: String,
        appSource: String?
    ) : this() {
        this.calories = calories
        this.startTimestamp = startTimestamp
        this.endTimestamp = endTimestamp
        this.appSource = appSource
    }
}
