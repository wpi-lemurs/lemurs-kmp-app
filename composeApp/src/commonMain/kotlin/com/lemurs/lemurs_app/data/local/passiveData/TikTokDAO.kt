package com.lemurs.lemurs_app.data.local.passiveData

import com.lemurs.lemurs_app.data.local.BaseDAO
import com.lemurs.lemurs_app.data.local.BaseDAOImpl
import io.realm.kotlin.ext.query

interface TikTokDAO : BaseDAO<TikTok> {
    suspend fun getTikTokData(id: String): TikTok?
    suspend fun getTikTokID(date: String): TikTok?
}

class TikTokDAOImpl : BaseDAOImpl<TikTok>(TikTok::class), TikTokDAO {
    override suspend fun getTikTokData(id: String): TikTok? {
        return realm().query<TikTok>("ID == $0", id).first().find()
    }

    override suspend fun getTikTokID(date: String): TikTok? {
        return realm().query<TikTok>("date == $0", date).first().find()
    }
}
