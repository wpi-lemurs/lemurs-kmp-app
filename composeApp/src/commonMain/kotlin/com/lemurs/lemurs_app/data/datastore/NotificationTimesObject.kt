package com.lemurs.lemurs_app.data.datastore

class NotificationTimesObject (private val morning: String, private val afternoon: String, private val d: String) {
    var morningTime: String = morning
    var afternoonTime: String = afternoon
    var date: String = d
}
