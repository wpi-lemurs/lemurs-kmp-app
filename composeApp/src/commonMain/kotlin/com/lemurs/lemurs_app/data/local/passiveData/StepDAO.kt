package com.lemurs.lemurs_app.data.local.passiveData

import com.lemurs.lemurs_app.data.local.BaseDAO
import com.lemurs.lemurs_app.data.local.BaseDAOImpl
import io.realm.kotlin.ext.query

interface StepDAO : BaseDAO<Step> {
    suspend fun getSteps(id: String): Step?
    suspend fun getStepsByTimestamps(startTimestamp: String, endTimestamp: String): Step?
}

class StepDAOImpl : BaseDAOImpl<Step>(Step::class), StepDAO {
    override suspend fun getSteps(id: String): Step? {
        return realm().query<Step>("id == $0", id).first().find()
    }

    override suspend fun getStepsByTimestamps(startTimestamp: String, endTimestamp: String): Step? {
        return realm().query<Step>("startTimestamp == $0 AND endTimestamp == $1", startTimestamp, endTimestamp).first().find()
    }
}
