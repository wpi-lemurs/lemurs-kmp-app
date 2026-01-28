package com.lemurs.lemurs_app.data.local.passiveData

import com.lemurs.lemurs_app.data.local.BaseDAO
import com.lemurs.lemurs_app.data.local.BaseDAOImpl
import io.realm.kotlin.ext.query

interface DistanceDAO : BaseDAO<Distance> {
    suspend fun getDistance(id: String): Distance?
    suspend fun getDistanceByTimestamps(startTimestamp: String, endTimestamp: String): Distance?
}

class DistanceDAOImpl : BaseDAOImpl<Distance>(Distance::class), DistanceDAO {
    override suspend fun getDistance(id: String): Distance? {
        return realm().query<Distance>("id == $0", id).first().find()
    }

    override suspend fun getDistanceByTimestamps(startTimestamp: String, endTimestamp: String): Distance? {
        return realm().query<Distance>("startTimestamp == $0 AND endTimestamp == $1", startTimestamp, endTimestamp).first().find()
    }
}
