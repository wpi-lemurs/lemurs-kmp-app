package com.lemurs.lemurs_app.data.local.passiveData

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

open class TikTok() : RealmObject {
    @PrimaryKey
    private var ID: ObjectId = ObjectId()

    fun getID(): String {
        return this.ID.toHexString()
    }
    var date: String = ""
    var description: String = ""
    var datePublished: String = ""
    var title: String = ""

    constructor(
        date: String,
        description: String,
        datePublished: String,
        title: String
    ) : this() {
        this.date = date
        this.description = description
        this.datePublished = datePublished
        this.title = title
    }
}
