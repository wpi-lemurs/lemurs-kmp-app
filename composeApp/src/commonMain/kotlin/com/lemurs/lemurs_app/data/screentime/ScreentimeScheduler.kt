package com.lemurs.lemurs_app.data.screentime

expect class ScreentimeScheduler {
    fun schedule()
    //for testing so we don't have to wait 15 min:
    fun scheduleQuick()
}