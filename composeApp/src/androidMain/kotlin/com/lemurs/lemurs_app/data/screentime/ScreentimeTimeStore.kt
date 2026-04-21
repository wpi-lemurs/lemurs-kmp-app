package com.lemurs.lemurs_app.data.screentime

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.lemurs.lemurs_app.data.local.ScreentimeSyncStore
import kotlinx.coroutines.flow.first

// Using DataStore to persist the last run time of the screentime sync
private val Context.dataStore by preferencesDataStore(
    name = "screentime_sync.pb"
)

// Implementation of ScreentimeSyncStore using DataStore to persist the last run time of the screentime sync
class ScreentimeTimeStore(private val context: Context): ScreentimeSyncStore {
    private val LAST_RUN_TIME_KEY = longPreferencesKey("last_run_time")

    override suspend fun getLastRunTime(): Long {
        val preferences = context.dataStore.data.first()
        return preferences[LAST_RUN_TIME_KEY] ?: (System.currentTimeMillis() - 15 * 60 * 1000)
    }

    override suspend fun saveLastRunTime(time: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_RUN_TIME_KEY] = time
        }
    }
}




