package com.lemurs.lemurs_app.data.local.passiveData

import com.lemurs.lemurs_app.data.local.BaseDAO
import com.lemurs.lemurs_app.data.local.BaseDAOImpl
import io.realm.kotlin.ext.query

interface WeightDAO : BaseDAO<Weight> {
    suspend fun getWeight(id: String): Weight?
}

class WeightDAOImpl : BaseDAOImpl<Weight>(Weight::class), WeightDAO {
    override suspend fun getWeight(id: String): Weight? {
        return realm().query<Weight>("id == $0", id).first().find()
    }
}
