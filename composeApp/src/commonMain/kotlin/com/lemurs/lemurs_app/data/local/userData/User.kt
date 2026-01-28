package com.lemurs.lemurs_app.data.local.userData

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

open class User() : RealmObject {
    @PrimaryKey
    private var ID: ObjectId = ObjectId()

    fun getID(): String {
        return this.ID.toHexString()
    }
    var userID: String = ""
    var date: String = ""

    constructor(userID: String, date: String) : this() {
        this.userID = userID
        this.date = date
    }
}
