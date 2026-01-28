package com.lemurs.lemurs_app.data.local


/**
 * Class to schedule work every two hours for syncing passive data to API
 */
expect class SendDataScheduler(){
    fun scheduleBluetooth()
    fun scheduleScreentime()
    fun scheduleSurveyResponse()
}