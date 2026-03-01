package com.lemurs.lemurs_app.data.local.passiveData

import com.lemurs.lemurs_app.data.api.ModalitiesApiServiceImpl
import com.lemurs.lemurs_app.data.api.ScreentimeData
import com.lemurs.lemurs_app.data.api.ScreentimeRequest
import com.lemurs.lemurs_app.data.api.WebAPIAuthorizationService
import com.lemurs.lemurs_app.data.local.SendDataUseCase
import com.lemurs.lemurs_app.data.local.UseCaseResult
import com.lemurs.lemurs_app.util.retryIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/** contains work to for sending screentime data to api */
class ScreentimeDataUseCase(
    private val screentimeDAO: ScreentimeDAO
) : SendDataUseCase {

    /**
     * sends data from all daos
     */
    override suspend fun call(): UseCaseResult<Any> {
        val screentimeResult = retryIO(
            { sendData(screentimeDAO) },
            shouldRetry = { true }
        )
        if(screentimeResult){
            return UseCaseResult.Success(Any())
        }else{
            return UseCaseResult.Failure()
        }
    }

    /** function for sending data for a specific dao to API */
    suspend fun sendData(dao : ScreentimeDAO): Boolean {
        var data: List<Screentime> = dao.getAll()
        if (data.isNotEmpty()) {
            // Log heartbeat entries for debugging
            val heartbeatCount = data.count { it.appName == "com.lemurs.no_screentime_detected" }
            if (heartbeatCount > 0) {
                co.touchlab.kermit.Logger.withTag("Screentime").w("Sending $heartbeatCount heartbeat entries (no screentime detected)")
            }

            val chunkSize = 100 // cutting the screentime data into chuncks of 100 so that the app doesn't run out of memory
            val chunks = data.chunked(chunkSize)
            for (chunk in chunks) {
                val inputDataRequest = createInputDataRequest(chunk)
                val webAPIAuthorizationService = WebAPIAuthorizationService()
                val modalitiesApiServiceImpl = ModalitiesApiServiceImpl(webAPIAuthorizationService.getHttpClient())
                val response = modalitiesApiServiceImpl.submitScreentimeData(inputDataRequest)
                if ((response.status.value/100)%10 != 2) {
                    throw Exception("Error sending screen time data")
                }
                // deleting only the records that weren't successfully sent
                chunk.forEach { d ->
                    dao.delete(d)
                }
            }
            return true
        }

        return true
    }

    fun createInputDataRequest(data: List<Screentime>): ScreentimeRequest {
        val screentimeData: MutableList<ScreentimeData> = mutableListOf()
        var startTime: String = ""
        var endTime: String = ""
        for (d in data) {
            startTime = d.startTime
            endTime = d.endTime
            val screentime = ScreentimeData(d.appName, d.totalTime, d.lastTimeUsed)
            screentimeData.add(screentime)
        }
        return ScreentimeRequest(startTime, endTime, screentimeData as List<ScreentimeData>)
    }


}