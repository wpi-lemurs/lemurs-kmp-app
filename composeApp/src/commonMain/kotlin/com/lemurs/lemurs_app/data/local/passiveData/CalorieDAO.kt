package com.lemurs.lemurs_app.data.local.passiveData

import com.lemurs.lemurs_app.data.local.BaseDAO
import com.lemurs.lemurs_app.data.local.BaseDAOImpl
import io.realm.kotlin.ext.query

interface CalorieDAO : BaseDAO<Calorie> {
    suspend fun getCalories(id: String): Calorie?
    suspend fun getCaloriesByTimestamps(startTimestamp: String, endTimestamp: String): Calorie?
}

class CalorieDAOImpl : BaseDAOImpl<Calorie>(Calorie::class), CalorieDAO {
    override suspend fun getCalories(id: String): Calorie? {
        return realm().query<Calorie>("id == $0", id).first().find()
    }

    override suspend fun getCaloriesByTimestamps(startTimestamp: String, endTimestamp: String): Calorie? {
        return realm().query<Calorie>("startTimestamp == $0 AND endTimestamp == $1", startTimestamp, endTimestamp).first().find()
    }
}
