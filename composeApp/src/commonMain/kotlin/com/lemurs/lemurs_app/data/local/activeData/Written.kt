package com.lemurs.lemurs_app.data.local.activeData

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

open class Written() : RealmObject {
    @PrimaryKey
    private var ID: ObjectId = ObjectId()

    fun getID(): String {
        return this.ID.toHexString()
    }
    var surveyResponseId: Int = 0
    var date: String = ""
    var questionNumber: Int = 0
    var response: String = ""

    constructor(
        surveyResponseId: Int,
        date: String,
        questionNumber: Int,
        response: String
    ) : this() {
        this.surveyResponseId = surveyResponseId
        this.date = date
        this.questionNumber = questionNumber
        this.response = response
    }
}
