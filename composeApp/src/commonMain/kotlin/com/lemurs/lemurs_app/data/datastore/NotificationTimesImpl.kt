package com.lemurs.lemurs_app.data.datastore

import androidx.datastore.core.DataStore
import com.lemurs.NotificationTimes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface NotificationTimesInterface {
    suspend fun updateMorningTime(morning: String)
    suspend fun updateAfternoonTime(afternoon: String)
    suspend fun updateDate(today: String)
    fun getMorningTime(): Flow<String>
    fun getAfternoonTime(): Flow<String>
    fun getDate(): Flow<String>
    suspend fun buildNotificationTimes(): NotificationTimesObject
}


class NotificationTimesImpl(private val dataStore: DataStore<NotificationTimes>) :
    NotificationTimesInterface {

    override suspend fun updateMorningTime(morning: String) {
        dataStore.updateData { currentNotificationTimes ->
            currentNotificationTimes.copy(morningTime = morning)
        }
    }

    override suspend fun updateAfternoonTime(afternoon: String) {
        dataStore.updateData { currentNotificationTimes ->
            currentNotificationTimes.copy(afternoonTime = afternoon)
        }
    }

    override suspend fun updateDate(today: String) {
        dataStore.updateData { currentNotificationTimes ->
            currentNotificationTimes.copy(date = today)
        }
    }

    override fun getMorningTime(): Flow<String> {
        return dataStore.data
            .map { notificationTimes ->
                notificationTimes.morningTime
            }
    }

    override fun getAfternoonTime(): Flow<String> {
        val c = dataStore.data
        return c
            .map { notificationTimes ->
                notificationTimes.afternoonTime
            }
    }

    override fun getDate(): Flow<String> {
        val c = dataStore.data
        return c
            .map { notificationTimes ->
                notificationTimes.date
            }
    }

    override suspend fun buildNotificationTimes(): NotificationTimesObject {
        return NotificationTimesObject(
            getMorningTime().first().toString(),
            getAfternoonTime().first().toString(),
            getDate().first().toString()
        )
    }
}
