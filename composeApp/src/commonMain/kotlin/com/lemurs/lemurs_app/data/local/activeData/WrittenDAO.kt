package com.lemurs.lemurs_app.data.local.activeData

import com.lemurs.lemurs_app.data.local.BaseDAO
import com.lemurs.lemurs_app.data.local.BaseDAOImpl
import io.realm.kotlin.ext.query

interface WrittenDAO : BaseDAO<Written> {
    suspend fun getWrittenData(id: String): Written?
    suspend fun getWrittenID(date: String): Written?
    suspend fun getWrittenDataBySurveyResponseId(surveyResponseId: Int): Written?
}

class WrittenDAOImpl : BaseDAOImpl<Written>(Written::class), WrittenDAO {
    override suspend fun getWrittenData(id: String): Written? {
        return realm().query<Written>("ID == $0", id).first().find()
    }

    override suspend fun getWrittenID(date: String): Written? {
        return realm().query<Written>("date == $0", date).first().find()
    }

    override suspend fun getWrittenDataBySurveyResponseId(surveyResponseId: Int): Written? {
        return realm().query<Written>("surveyResponseId == $0", surveyResponseId).first().find()
    }
}
