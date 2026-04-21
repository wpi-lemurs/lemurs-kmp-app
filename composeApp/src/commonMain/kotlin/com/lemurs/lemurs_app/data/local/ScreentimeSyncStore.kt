package com.lemurs.lemurs_app.data.local

interface ScreentimeSyncStore {
    suspend fun getLastRunTime(): Long
    suspend fun saveLastRunTime(time: Long)
}