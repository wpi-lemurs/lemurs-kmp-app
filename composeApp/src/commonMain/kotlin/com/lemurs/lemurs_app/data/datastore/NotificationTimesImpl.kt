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

    /**
     * Records the time chosen for [windowName] on [localDate], as "HH:MM".
     *
     * Drawn once and reused, so re-opening the app does not move a notification that is already
     * pending.
     */
    suspend fun updatePlannedWindowTime(windowName: String, localDate: String, atLocalTime: String)

    /** The time chosen for [windowName] on [localDate], or empty if none has been drawn. */
    fun getPlannedWindowTime(windowName: String, localDate: String): Flow<String>

    /** Drops planned times for dates other than those given, so the map cannot grow forever. */
    suspend fun prunePlannedWindowTimes(keepDates: Set<String>)
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

    override suspend fun updatePlannedWindowTime(
        windowName: String,
        localDate: String,
        atLocalTime: String
    ) {
        dataStore.updateData { current ->
            current.copy(
                plannedWindowTimes =
                    current.plannedWindowTimes + (plannedKey(windowName, localDate) to atLocalTime)
            )
        }
    }

    override fun getPlannedWindowTime(windowName: String, localDate: String): Flow<String> =
        dataStore.data.map { it.plannedWindowTimes[plannedKey(windowName, localDate)] ?: "" }

    override suspend fun prunePlannedWindowTimes(keepDates: Set<String>) {
        dataStore.updateData { current ->
            current.copy(
                plannedWindowTimes = current.plannedWindowTimes.filterKeys { key ->
                    key.substringAfterLast('@', "") in keepDates
                }
            )
        }
    }

    private companion object {
        /** Window names cannot contain '@', so the date is recoverable from the key. */
        fun plannedKey(windowName: String, localDate: String) = "$windowName@$localDate"
    }
}
