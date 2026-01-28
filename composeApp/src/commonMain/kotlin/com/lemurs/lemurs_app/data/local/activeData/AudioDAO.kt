package com.lemurs.lemurs_app.data.local.activeData

import com.lemurs.lemurs_app.data.local.BaseDAO
import com.lemurs.lemurs_app.data.local.BaseDAOImpl
import io.realm.kotlin.ext.query

interface AudioDAO : BaseDAO<Audio> {
    suspend fun getAudioData(id: String): Audio?
    suspend fun getAudioID(date: String): Audio?
    suspend fun getAudioDataBySurveyResponseId(surveyResponseId: Int): Audio?
}

class AudioDAOImpl : BaseDAOImpl<Audio>(Audio::class), AudioDAO {
    override suspend fun getAudioData(id: String): Audio? {
        return realm().query<Audio>("ID == $0", id).first().find()
    }

    override suspend fun getAudioID(date: String): Audio? {
        return realm().query<Audio>("date == $0", date).first().find()
    }

    override suspend fun getAudioDataBySurveyResponseId(surveyResponseId: Int): Audio? {
        return realm().query<Audio>("surveyResponseId == $0", surveyResponseId).first().find()
    }
}
