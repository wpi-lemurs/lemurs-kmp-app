package com.lemurs.lemurs_app.data.local.passiveData

import com.lemurs.lemurs_app.data.local.BaseDAO
import com.lemurs.lemurs_app.data.local.BaseDAOImpl
import io.realm.kotlin.ext.query

interface SpeedDAO : BaseDAO<Speed> {
    suspend fun getSpeed(id: String): Speed?
    suspend fun getSpeedByTimestamps(startTimestamp: String, endTimestamp: String): Speed?
}

class SpeedDAOImpl : BaseDAOImpl<Speed>(Speed::class), SpeedDAO {
    override suspend fun getSpeed(id: String): Speed? {
        return realm().query<Speed>("id == $0", id).first().find()
    }

    override suspend fun getSpeedByTimestamps(startTimestamp: String, endTimestamp: String): Speed? {
        return realm().query<Speed>("startTimestamp == $0 AND endTimestamp == $1", startTimestamp, endTimestamp).first().find()
    }
}
