package com.lemurs.lemurs_app.data.local.passiveData


import com.lemurs.lemurs_app.data.api.ModalitiesApiServiceImpl
import com.lemurs.lemurs_app.data.api.ScreentimeData
import com.lemurs.lemurs_app.data.api.ScreentimeRequest
import com.lemurs.lemurs_app.data.api.WebAPIAuthorizationService
import com.lemurs.lemurs_app.data.local.ScreentimeSyncStore
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
            val sorted = data.sortedBy { it.startTime } // sorting the data by start time so API receives data in chronological order.
            val chunks = sorted.chunked(chunkSize)

            for (chunk in chunks) {
                val inputDataRequest = createInputDataRequest(chunk)
                val webAPIAuthorizationService = WebAPIAuthorizationService()
                val modalitiesApiServiceImpl = ModalitiesApiServiceImpl(webAPIAuthorizationService.getHttpClient())
                val response = modalitiesApiServiceImpl.submitScreentimeData(inputDataRequest)
                if ((response.status.value/100)%10 != 2) {
                    throw Exception("Error sending screen time data")
                }
                // deleting only the records that weren't successfully sent
                dao.deleteBatch(chunk)
            }
            return true
        }

        return true
    }


    /*
    * Function takes in a chunk of data of type: List<Screentime>
    * Proceses a chunk of screentime data to create a ScreentimeRequest object that can be sent to the API.
    * chunk -> an arbitrary collection of screentime sessions (can be multiple 15 min sessions)
    * A chunk is a single flat list of all app entries over one or multiple 15 minute intervals.
    * It creates a ScreentimeRequest object that contains the start time, end time, and a list of ScreentimeData objects
    * We get screentime data -> extract certain fields
    */
    fun createInputDataRequest(screentimeEntries: List<Screentime>): ScreentimeRequest {
        val requestItems: MutableList<ScreentimeData> = mutableListOf()

        // This code avoids startTime and endTime recording the
        // start and end time for the previous chunk of data
        // Ensures we are collecting times for the current chunk of data being sent to the API

        // firstOrNull() returns the first element of the list.
        // if list is empty -> return null
        // ?:startTime: If the object isn't null, return startTime. If its null, return null.

        val sortedEntries = screentimeEntries.sortedBy{ it.startTime }

        val startTime = sortedEntries.firstOrNull()?.startTime ?: ""
        val endTime = sortedEntries.lastOrNull()?.endTime ?: ""

        // d is a screentime object in the list of screentime data.
        // We create a screentime data object for each screentime object.
        // We add each screentime data object to the list of screentime data objects "requestItems" that will be sent to the API.
        for (entry in sortedEntries) {

            // requestItems -> for ("screentime_app" table)
            requestItems.add(
                ScreentimeData(entry.appName, entry.totalTime, entry.lastTimeUsed)
            )
        }

        // ScreentimeRequest -> for ("screentime" table)
        return ScreentimeRequest(startTime, endTime, requestItems)
    }


}