package com.lemurs.lemurs_app.data.local.passiveData

import com.lemurs.lemurs_app.data.local.BaseDAO
import com.lemurs.lemurs_app.data.local.BaseDAOImpl
import io.realm.kotlin.ext.query

interface SleepDAO : BaseDAO<Sleep> {
    suspend fun getSleep(id: String): Sleep?
}

class SleepDAOImpl : BaseDAOImpl<Sleep>(Sleep::class), SleepDAO {
    override suspend fun getSleep(id: String): Sleep? {
        return realm().query<Sleep>("id == $0", id).first().find()
    }
}
