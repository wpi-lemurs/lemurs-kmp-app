package com.lemurs.lemurs_app.data.local.passiveData

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

open class Sleep() : RealmObject {
    @PrimaryKey
    private var ID: ObjectId = ObjectId()

    fun getID(): String {
        return this.ID.toHexString()
    }
    var sleep: String = ""
    var startTimestamp: String = ""
    var endTimestamp: String = ""
    var appSource: String? = null

    constructor(
        sleep: String,
        startTimestamp: String,
        endTimestamp: String,
        appSource: String?
    ) : this() {
        this.sleep = sleep
        this.startTimestamp = startTimestamp
        this.endTimestamp = endTimestamp
        this.appSource = appSource
    }
}
