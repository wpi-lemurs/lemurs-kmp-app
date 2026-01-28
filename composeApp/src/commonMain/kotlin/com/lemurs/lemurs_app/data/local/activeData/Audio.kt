package com.lemurs.lemurs_app.data.local.activeData

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

open class Audio() : RealmObject {
    @PrimaryKey
    var ID: ObjectId = ObjectId()

    fun getID(): String {
        return this.ID.toHexString()
    }
    var surveyResponseId: Int = 0
    var date: String = ""
    var audioByte64: String = ""
    var questionId: Int = 0

    constructor(surveyResponseId: Int, date: String, audioByte64: String, questionId: Int) : this() {
        this.surveyResponseId = surveyResponseId
        this.date = date
        this.audioByte64 = audioByte64
        this.questionId = questionId
    }
}
