package com.lemurs.lemurs_app.data.local.passiveData

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

open class Screentime() : RealmObject {
    @PrimaryKey
    private var ID: ObjectId = ObjectId()

    fun getID(): String {
        return this.ID.toHexString()
    }
    var date: String = ""
    var startTime: String = ""
    var endTime: String = ""
    var appName: String = ""
    var totalTime: Long = 0
    var lastTimeUsed: String = ""

    constructor(
        date: String,
        startTime: String,
        endTime: String,
        appName: String,
        totalTime: Long,
        lastTimeUsed: String
    ) : this() {
        this.date = date
        this.startTime = startTime
        this.endTime = endTime
        this.appName = appName
        this.totalTime = totalTime
        this.lastTimeUsed = lastTimeUsed
    }
    constructor(
        id: Int,
        date: String,
        startTime: String,
        endTime: String,
        appName: String,
        totalTime: Long,
        lastTimeUsed: String
    ) : this() {
        this.ID = ObjectId(id.toString())
        this.date = date
        this.startTime = startTime
        this.endTime = endTime
        this.appName = appName
        this.totalTime = totalTime
        this.lastTimeUsed = lastTimeUsed
    }
}
