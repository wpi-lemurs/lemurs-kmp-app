package com.lemurs.lemurs_app.data.datastore

import androidx.datastore.core.DataStore
import com.lemurs.NotificationTimes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface NotificationTimesInterface {
    /** Records when the notification for [windowName] was sent. */
    suspend fun updateWindowTime(windowName: String, firedAt: String)
    suspend fun updateDate(today: String)
    suspend fun updateCachedWindows(windowsJson: String)

    /** When [windowName]'s notification fired, or empty if it has not. */
    fun getWindowTime(windowName: String): Flow<String>
    fun getDate(): Flow<String>
    fun getCachedWindows(): Flow<String>
}

/**
 * Stores when each window's notification fired, so a submission can be timed against the nudge that
 * prompted it.
 *
 * Keyed by window name rather than fixed morning/afternoon fields: the set of windows comes from the
 * database and can change without an app release.
 */
class NotificationTimesImpl(private val dataStore: DataStore<NotificationTimes>) :
    NotificationTimesInterface {

    override suspend fun updateWindowTime(windowName: String, firedAt: String) {
        dataStore.updateData { current ->
            current.copy(windowTimes = current.windowTimes + (windowName to firedAt))
        }
    }

    override suspend fun updateDate(today: String) {
        dataStore.updateData { current -> current.copy(date = today) }
    }

    override suspend fun updateCachedWindows(windowsJson: String) {
        dataStore.updateData { current -> current.copy(cachedWindows = windowsJson) }
    }

    override fun getWindowTime(windowName: String): Flow<String> =
        dataStore.data.map { it.windowTimes[windowName] ?: "" }

    override fun getDate(): Flow<String> = dataStore.data.map { it.date }

    override fun getCachedWindows(): Flow<String> = dataStore.data.map { it.cachedWindows }
}
