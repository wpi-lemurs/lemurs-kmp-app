package com.lemurs.lemurs_app.data.local.passiveData

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

open class Distance() : RealmObject {
    @PrimaryKey
    private var ID: ObjectId = ObjectId()

    fun getID(): String {
        return this.ID.toHexString()
    }
    var distance: Double = 0.0
    var startTimestamp: String = ""
    var endTimestamp: String = ""
    var appSource: String? = null

    constructor(
        distance: Double,
        startTimestamp: String,
        endTimestamp: String,
        appSource: String?
    ) : this() {
        this.distance = distance
        this.startTimestamp = startTimestamp
        this.endTimestamp = endTimestamp
        this.appSource = appSource
    }
}
