package com.lemurs.lemurs_app.data.local.activeData

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

open class SurveyResponse() : RealmObject {
    @PrimaryKey
    var ID: ObjectId = ObjectId()

    var localID: Int = 0
    fun getID(): Int {
        return this.localID
    }
    var answers: String = "" // JSON string representing List<AnswerDto>
    var timestamp: String = ""
    var notificationTime: String = ""
    var type: Int = 0 // 0: morning, 1: afternoon, 2: weekly
    var submitted: Boolean = false
    var surveyResponseId: Int = -1

    constructor(answers: String, timestamp: String, notificationTime: String, type: Int) : this() {
        this.answers = answers
        this.timestamp = timestamp
        this.notificationTime = notificationTime
        this.type = type
    }
    constructor(id: Int, answers: String, timestamp: String, notificationTime: String, type: Int) : this() {
        this.localID = id
        this.answers = answers
        this.timestamp = timestamp
        this.notificationTime = notificationTime
        this.type = type
    }
}

