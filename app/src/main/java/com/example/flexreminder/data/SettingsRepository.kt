package com.example.flexreminder.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

class SettingsRepository private constructor(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    val snoozeShortMinutes: Flow<Int> = dataStore.data.map {
        it[KEY_SNOOZE_SHORT] ?: DEFAULT_SNOOZE_SHORT
    }

    val snoozeLongMinutes: Flow<Int> = dataStore.data.map {
        it[KEY_SNOOZE_LONG] ?: DEFAULT_SNOOZE_LONG
    }

    suspend fun setSnoozeIntervals(short: Int, long: Int) {
        val a = short.coerceIn(MIN_SNOOZE_MINUTES, MAX_SNOOZE_MINUTES)
        val b = long.coerceIn(MIN_SNOOZE_MINUTES, MAX_SNOOZE_MINUTES)
        val min = minOf(a, b)
        val max = maxOf(a, b)
        dataStore.edit { prefs ->
            prefs[KEY_SNOOZE_SHORT] = min
            prefs[KEY_SNOOZE_LONG] = max
        }
    }

    suspend fun getSnoozeShort(): Int = snoozeShortMinutes.first()

    suspend fun getSnoozeLong(): Int = snoozeLongMinutes.first()

    /** Только для вызова из фоновых потоков (BroadcastReceiver на IO). */
    fun getSnoozeShortBlocking(): Int = runBlocking { snoozeShortMinutes.first() }

    fun getSnoozeLongBlocking(): Int = runBlocking { snoozeLongMinutes.first() }

    companion object {
        const val MIN_SNOOZE_MINUTES = 1
        const val MAX_SNOOZE_MINUTES = 720

        const val DEFAULT_SNOOZE_SHORT = 5
        const val DEFAULT_SNOOZE_LONG = 60

        val PRESETS = listOf(5, 10, 15, 30, 60, 120, 240, 720)

        private val KEY_SNOOZE_SHORT = intPreferencesKey("snooze_short_minutes")
        private val KEY_SNOOZE_LONG = intPreferencesKey("snooze_long_minutes")

        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun get(context: Context): SettingsRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context).also { INSTANCE = it }
            }
    }
}