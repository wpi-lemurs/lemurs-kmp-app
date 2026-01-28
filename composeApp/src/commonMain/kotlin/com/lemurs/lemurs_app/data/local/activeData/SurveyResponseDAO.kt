package com.lemurs.lemurs_app.data.local.activeData

import com.lemurs.lemurs_app.data.local.BaseDAO
import com.lemurs.lemurs_app.data.local.BaseDAOImpl
import io.realm.kotlin.ext.query

interface SurveyResponseDAO : BaseDAO<SurveyResponse> {
    suspend fun getById(id: String): SurveyResponse?
    suspend fun getByType(type: String): List<SurveyResponse>

    suspend fun modifySubmission(survey: SurveyResponse, newSurveyId: Int)
}

class SurveyResponseDAOImpl : BaseDAOImpl<SurveyResponse>(SurveyResponse::class), SurveyResponseDAO {
    override suspend fun getById(id: String): SurveyResponse? {
        return realm().query<SurveyResponse>("id == $0", id).first().find()
    }

    override suspend fun getByType(type: String): List<SurveyResponse> {
        return realm().query<SurveyResponse>("type == $0", type).find()
    }

    override suspend fun modifySubmission(survey: SurveyResponse, newSurveyId: Int) {
        realm().write {
            val modify = query<SurveyResponse>("ID == $0", survey.ID).first().find()
            modify?.let {
                it.surveyResponseId = newSurveyId
                it.submitted = true
            }
        }
    }
}
