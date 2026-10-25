package io.github.egorche.flexreminder.data

import android.content.Context
import android.content.res.Configuration
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.runBlocking

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

enum class AppTheme {
    MONOCHROME,
    PALETTE,
    DARK
}

class SettingsRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val dataStore = appContext.settingsDataStore

    // ---------- Snooze интервал (FEAT #27) ----------

    val snoozeSmartMinutes: Flow<Int> = dataStore.data
        .onStart { ensureSnoozeSmartMigrated() }
        .map { prefs ->
            prefs[KEY_SNOOZE_SMART] ?: DEFAULT_SNOOZE_SMART
        }

    suspend fun setSnoozeSmart(minutes: Int) {
        val clamped = minutes.coerceIn(SNOOZE_MIN, SNOOZE_MAX)
        dataStore.edit { prefs ->
            prefs[KEY_SNOOZE_SMART] = clamped
        }
    }

    suspend fun getSnoozeSmart(): Int = snoozeSmartMinutes.first()
    fun getSnoozeSmartBlocking(): Int = runBlocking { snoozeSmartMinutes.first() }

    private suspend fun ensureSnoozeSmartMigrated() {
        val prefs = dataStore.data.first()
        if (prefs[KEY_SNOOZE_SMART] != null) return

        val short = prefs[KEY_SNOOZE_SHORT] ?: DEFAULT_SNOOZE_SHORT
        val long = prefs[KEY_SNOOZE_LONG] ?: DEFAULT_SNOOZE_LONG

        val candidate = when {
            short != DEFAULT_SNOOZE_SHORT -> short
            long != DEFAULT_SNOOZE_LONG -> long
            else -> DEFAULT_SNOOZE_SMART
        }.coerceIn(SNOOZE_MIN, SNOOZE_MAX)

        dataStore.edit { it[KEY_SNOOZE_SMART] = candidate }
    }

    // ---------- Тема ----------

    val appTheme: Flow<AppTheme> = dataStore.data
        .onStart { ensureThemeInitialized() }
        .map { prefs ->
            val raw = prefs[KEY_APP_THEME]
            try {
                AppTheme.valueOf(raw ?: DEFAULT_THEME.name)
            } catch (_: Exception) {
                DEFAULT_THEME
            }
        }

    suspend fun setAppTheme(theme: AppTheme) {
        dataStore.edit { prefs ->
            prefs[KEY_APP_THEME] = theme.name
        }
    }

    private suspend fun ensureThemeInitialized() {
        val prefs = dataStore.data.first()
        if (prefs[KEY_APP_THEME] != null) return
        val initial = if (isSystemInDarkMode()) AppTheme.DARK else AppTheme.PALETTE
        dataStore.edit { it[KEY_APP_THEME] = initial.name }
    }

    private fun isSystemInDarkMode(): Boolean {
        val uiMode = appContext.resources.configuration.uiMode
        return (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    // ---------- Звук по умолчанию ----------

    val defaultSoundUri: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_SOUND_URI]
    }

    suspend fun setDefaultSoundUri(uri: String?) {
        dataStore.edit { prefs ->
            if (uri == null) {
                prefs.remove(KEY_DEFAULT_SOUND_URI)
            } else {
                prefs[KEY_DEFAULT_SOUND_URI] = uri
            }
        }
    }

    suspend fun getDefaultSoundUri(): String? = defaultSoundUri.first()
    fun getDefaultSoundUriBlocking(): String? = runBlocking { defaultSoundUri.first() }

    // ---------- Настройки виджета (FEAT #32) ----------

    val widgetTheme: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_WIDGET_THEME] ?: DEFAULT_WIDGET_THEME
    }

    suspend fun setWidgetTheme(theme: String) {
        dataStore.edit { it[KEY_WIDGET_THEME] = theme }
    }

    val widgetTransparency: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_WIDGET_TRANSPARENCY] ?: DEFAULT_WIDGET_TRANSPARENCY
    }

    suspend fun setWidgetTransparency(value: Int) {
        val clamped = value.coerceIn(WIDGET_TRANSPARENCY_MIN, WIDGET_TRANSPARENCY_MAX)
        dataStore.edit { it[KEY_WIDGET_TRANSPARENCY] = clamped }
    }

    val widgetShowNotes: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_WIDGET_SHOW_NOTES] ?: DEFAULT_WIDGET_SHOW_NOTES
    }

    suspend fun setWidgetShowNotes(show: Boolean) {
        dataStore.edit { it[KEY_WIDGET_SHOW_NOTES] = show }
    }

    val widgetMaxLines: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_WIDGET_MAX_LINES] ?: DEFAULT_WIDGET_MAX_LINES
    }

    suspend fun setWidgetMaxLines(lines: Int) {
        val clamped = lines.coerceIn(WIDGET_MAX_LINES_MIN, WIDGET_MAX_LINES_MAX)
        dataStore.edit { it[KEY_WIDGET_MAX_LINES] = clamped }
    }

    companion object {
        // ---------- Snooze константы ----------
        const val DEFAULT_SNOOZE_SMART = 2
        const val SNOOZE_MIN = 1
        const val SNOOZE_MAX = 360

        // Legacy-дефолты для миграции
        const val DEFAULT_SNOOZE_SHORT = 2
        const val DEFAULT_SNOOZE_LONG = 30

        // ---------- Константы темы ----------
        val DEFAULT_THEME = AppTheme.PALETTE

        // ---------- Константы виджета ----------
        const val DEFAULT_WIDGET_THEME = "SYSTEM" // "SYSTEM" или "APP"
        const val DEFAULT_WIDGET_TRANSPARENCY = 100 // проценты
        const val WIDGET_TRANSPARENCY_MIN = 5
        const val WIDGET_TRANSPARENCY_MAX = 100
        const val DEFAULT_WIDGET_SHOW_NOTES = false
        const val DEFAULT_WIDGET_MAX_LINES = 1
        const val WIDGET_MAX_LINES_MIN = 1
        const val WIDGET_MAX_LINES_MAX = 2

        // ---------- Ключи DataStore ----------
        private val KEY_SNOOZE_SMART = intPreferencesKey("snooze_smart_minutes")
        private val KEY_SNOOZE_SHORT = intPreferencesKey("snooze_short_minutes")
        private val KEY_SNOOZE_LONG = intPreferencesKey("snooze_long_minutes")

        private val KEY_APP_THEME = stringPreferencesKey("app_theme")
        private val KEY_DEFAULT_SOUND_URI = stringPreferencesKey("default_sound_uri")

        private val KEY_WIDGET_THEME = stringPreferencesKey("widget_theme")
        private val KEY_WIDGET_TRANSPARENCY = intPreferencesKey("widget_transparency")
        private val KEY_WIDGET_SHOW_NOTES = booleanPreferencesKey("widget_show_notes")
        private val KEY_WIDGET_MAX_LINES = intPreferencesKey("widget_max_lines")

        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun get(context: Context): SettingsRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context).also { INSTANCE = it }
            }
    }
}