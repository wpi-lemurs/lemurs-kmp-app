package com.lemurs.lemurs_app.data.local.passiveData

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

open class Step() : RealmObject {
    @PrimaryKey
    private var ID: ObjectId = ObjectId()

    fun getID(): String {
        return this.ID.toHexString()
    }
    var steps: Int = 0
    var startTimestamp: String = ""
    var endTimestamp: String = ""
    var appSource: String? = null

    constructor(
        steps: Int,
        startTimestamp: String,
        endTimestamp: String,
        appSource: String?
    ) : this() {
        this.steps = steps
        this.startTimestamp = startTimestamp
        this.endTimestamp = endTimestamp
        this.appSource = appSource
    }
}
