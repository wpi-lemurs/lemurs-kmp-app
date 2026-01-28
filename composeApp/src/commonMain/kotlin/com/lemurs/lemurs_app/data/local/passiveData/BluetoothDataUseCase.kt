package com.lemurs.lemurs_app.data.local.passiveData

import co.touchlab.kermit.Logger
import com.lemurs.lemurs_app.data.api.BluetoothData
import com.lemurs.lemurs_app.data.api.BluetoothRequest
import com.lemurs.lemurs_app.data.api.ModalitiesApiServiceImpl
import com.lemurs.lemurs_app.data.api.WebAPIAuthorizationService
import com.lemurs.lemurs_app.data.local.SendDataUseCase
import com.lemurs.lemurs_app.data.local.UseCaseResult
import kotlinx.datetime.Instant
import kotlinx.datetime.toLocalDateTime

/** contains work to for sending active data */
class BluetoothDataUseCase(
    private val bluetoothDAO: BluetoothDAO
) : SendDataUseCase {

    val logger = Logger.withTag("Work Manager")

    /**
     * sends data from all daos
     */
    override suspend fun call(): UseCaseResult<Any> {
        logger.w("bluetooth data worker called")
        val bluetoothResult = sendData(bluetoothDAO)
        if(bluetoothResult){
            return UseCaseResult.Success(Any())
        }else{
            return UseCaseResult.Failure()
        }
    }

    /** function for sending data for a specific dao to API */
    suspend fun sendData(dao : BluetoothDAO): Boolean {
        logger.w("getting all bluetooth data...")
        val data: List<Bluetooth> = dao.getAll()
        logger.w("got bluetooth data: ")
        for(d in data){
            logger.w("bluetooth data: "+d.getID() +" "+d.date+"  "+d.dateOfCollection+" "+d.numberOfDevices)
        }
        if (data.isNotEmpty()) {
            val inputDataRequest: BluetoothRequest = createInputDataRequest(data)
            var status = true
            var i = 0
            for (d in data) {
                val inputData = inputDataRequest.data[i]
                val webAPIAuthorizationService= WebAPIAuthorizationService()
                val modalitiesApiServiceImpl = ModalitiesApiServiceImpl(webAPIAuthorizationService.getHttpClient())
                val response = modalitiesApiServiceImpl.submitBluetoothData(inputData)
                if ((response.status.value/100)%10 == 2) {
                    logger.w("bluetooth data submitted, now deleting from local storage")
                    // only deletes from room if successfully sent to API

                    dao.delete(d)

                } else {
                    logger.w("bluetooth data failed to submit")
                    status = false
                }
            }
            return status

        } else {
            logger.w("no bluetooth data found")
            return true
        }
    }

    fun createInputDataRequest(data: List<Bluetooth>): BluetoothRequest {
        val bluetoothData: MutableList<BluetoothData> = mutableListOf()
        for (d in data) {
            val instant = try {
                Instant.parse(d.date)
            } catch (e: Exception) {
                Instant.fromEpochMilliseconds(d.date.toLong())
            }
            val bluetooth = BluetoothData(instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()), d.numberOfDevices)
            bluetoothData.add(bluetooth)
        }
        return BluetoothRequest(bluetoothData as List<BluetoothData>)
    }
}
