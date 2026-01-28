package com.lemurs.lemurs_app.data.local.passiveData

import com.lemurs.lemurs_app.data.api.GpsData
import com.lemurs.lemurs_app.data.api.GpsRequest
import com.lemurs.lemurs_app.data.api.ModalitiesApiServiceImpl
import com.lemurs.lemurs_app.data.api.WebAPIAuthorizationService
import com.lemurs.lemurs_app.data.local.SendDataUseCase
import com.lemurs.lemurs_app.data.local.UseCaseResult

/** contains work to for sending active data */
class GPSDataUseCase(
    private val gpsDAO: GPSDAO
) : SendDataUseCase {

    /**
     * sends data from all daos
     */
    override suspend fun call(): UseCaseResult<Any> {
        val gpsResult = sendData(gpsDAO)
        if(gpsResult){
            return UseCaseResult.Success(Any())
        }else{
            return UseCaseResult.Failure()
        }
    }

    /** function for sending data for a specific dao to API */
    suspend fun sendData(dao : GPSDAO): Boolean {
        val data: List<GPS> = dao.getAll()
        if (data.isNotEmpty()) {
            val inputDataRequest: GpsRequest = createInputDataRequest(data)
            val webAPIAuthorizationService= WebAPIAuthorizationService()
            val modalitiesApiServiceImpl = ModalitiesApiServiceImpl(webAPIAuthorizationService.getHttpClient())
            val response = modalitiesApiServiceImpl.submitGpsData(inputDataRequest)
            if ((response.status.value/100)%10 == 2) {
                // only deletes from room if successfully sent to API
                for (d in data) {
                    dao.delete(d)
                }
                return true
            } else {
                return false
            }

        } else {
            return true
        }
    }

    fun createInputDataRequest(data: List<GPS>): GpsRequest {
        val gpsData: MutableList<GpsData> = mutableListOf()
        for (d in data) {
            val gps = GpsData(d.date, d.longitude, d.latitude, d.altitude, d.speed, d.timestamp)
            gpsData.add(gps)
        }
        return GpsRequest(gpsData as List<GpsData>)
    }
}
