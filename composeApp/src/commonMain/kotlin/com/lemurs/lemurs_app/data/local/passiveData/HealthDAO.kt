package com.lemurs.lemurs_app.data.local.passiveData

import com.lemurs.lemurs_app.data.local.BaseDAO
import com.lemurs.lemurs_app.data.local.BaseDAOImpl
import io.realm.kotlin.ext.query

interface HealthDAO : BaseDAO<Health> {
    suspend fun getHealthData(id: String): Health?
    suspend fun getHealthID(date: String): Health?
}

class HealthDAOImpl : BaseDAOImpl<Health>(Health::class), HealthDAO {
    override suspend fun getHealthData(id: String): Health? {
        return realm().query<Health>("ID == $0", id).first().find()
    }

    override suspend fun getHealthID(date: String): Health? {
        return realm().query<Health>("date == $0", date).first().find()
    }
}
