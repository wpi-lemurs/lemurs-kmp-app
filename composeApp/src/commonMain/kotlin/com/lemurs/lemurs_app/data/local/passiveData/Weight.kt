package com.lemurs.lemurs_app.data.local.passiveData

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

open class Weight() : RealmObject {
    @PrimaryKey
    private var ID: ObjectId = ObjectId()

    fun getID(): String {
        return this.ID.toHexString()
    }
    var weight: Double = 0.0
    var timestamp: String = ""
    var appSource: String? = null

    constructor(
        weight: Double,
        timestamp: String,
        appSource: String?
    ) : this() {
        this.weight = weight
        this.timestamp = timestamp
        this.appSource = appSource
    }
}
