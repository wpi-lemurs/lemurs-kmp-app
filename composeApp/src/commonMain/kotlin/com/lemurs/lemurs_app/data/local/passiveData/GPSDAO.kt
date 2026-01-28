package com.lemurs.lemurs_app.data.local.passiveData

import com.lemurs.lemurs_app.data.local.BaseDAO
import com.lemurs.lemurs_app.data.local.BaseDAOImpl
import io.realm.kotlin.ext.query

interface GPSDAO : BaseDAO<GPS> {
    suspend fun getGPSData(id: String): GPS?
    suspend fun getGPSID(date: String): GPS?
}

class GPSDAOImpl : BaseDAOImpl<GPS>(GPS::class), GPSDAO {
    override suspend fun getGPSData(id: String): GPS? {
        return realm().query<GPS>("ID == $0", id).first().find()
    }

    override suspend fun getGPSID(date: String): GPS? {
        return realm().query<GPS>("date == $0", date).first().find()
    }
}
