package io.github.egorche.flexreminder.data

import android.content.Context
import android.content.res.Configuration
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
    //
    // До FEAT #27 было две настройки: snooze_short_minutes и snooze_long_minutes.
    // Теперь одна — snooze_smart_minutes.
    //
    // Миграция (первое чтение, если snooze_smart_minutes ещё не задан):
    //   1. Если short != DEFAULT_SNOOZE_SHORT → берём short.
    //   2. Иначе, если long != DEFAULT_SNOOZE_LONG → берём long.
    //   3. Иначе → DEFAULT_SNOOZE_SMART.
    //   Затем обрезаем в [SNOOZE_MIN, SNOOZE_MAX].
    //
    // Старые ключи НЕ удаляем — на случай откатов или будущих миграций.

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

    /**
     * Одноразовая миграция со старых ключей (short/long) на новый
     * snooze_smart_minutes. Идемпотентна: если ключ уже установлен — сразу
     * выходим, ничего не меняя.
     */
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

    /**
     * При первом запуске (ключа нет) — фиксируем тему по системе:
     *  - если в системе включена тёмная тема → DARK;
     *  - иначе → PALETTE (дефолт).
     * После этого системные изменения больше не влияют на выбор темы.
     */
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

    companion object {
        // ---------- Snooze константы ----------

        /** Дефолт единого интервала отложки (минуты). Совпадает со старым DEFAULT_SNOOZE_SHORT. */
        const val DEFAULT_SNOOZE_SMART = 2

        /** Минимум интервала отложки (минуты). */
        const val SNOOZE_MIN = 1

        /** Максимум интервала отложки (минуты) — 6 часов. */
        const val SNOOZE_MAX = 360

        // Legacy-дефолты. Нужны только для миграции. Не использовать в новом коде.
        const val DEFAULT_SNOOZE_SHORT = 2
        const val DEFAULT_SNOOZE_LONG = 30

        /** Дефолт темы, если по каким-то причинам не удалось определить системную. */
        val DEFAULT_THEME = AppTheme.PALETTE

        // ---------- Ключи DataStore ----------

        private val KEY_SNOOZE_SMART = intPreferencesKey("snooze_smart_minutes")

        // Legacy-ключи. Не удаляем — оставлены для обратной совместимости
        // и возможного возврата к двум интервалам в будущих волнах.
        private val KEY_SNOOZE_SHORT = intPreferencesKey("snooze_short_minutes")
        private val KEY_SNOOZE_LONG = intPreferencesKey("snooze_long_minutes")

        private val KEY_APP_THEME = stringPreferencesKey("app_theme")
        private val KEY_DEFAULT_SOUND_URI = stringPreferencesKey("default_sound_uri")

        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun get(context: Context): SettingsRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context).also { INSTANCE = it }
            }
    }
}