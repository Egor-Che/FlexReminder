package io.github.egorche.flexreminder.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.egorche.flexreminder.alarm.SoundResolver
import io.github.egorche.flexreminder.data.AppTheme
import io.github.egorche.flexreminder.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Поле настройки интервала отложки.
 *
 * До FEAT #27 было два поля (SHORT/LONG). Теперь одно — SMART.
 * Enum оставлен как каркас для диалога (min/max/presets).
 */
enum class SnoozeField(
    val minMinutes: Int,
    val maxMinutes: Int,
    val presets: List<Int>
) {
    SMART(
        minMinutes = SettingsRepository.SNOOZE_MIN,
        maxMinutes = SettingsRepository.SNOOZE_MAX,
        presets = listOf(1, 5, 10, 30, 60, 120, 180, 360)
    )
}

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SettingsRepository.get(app)

    // ---------- Snooze (единый интервал, FEAT #27) ----------

    val snoozeSmart: StateFlow<Int> = repo.snoozeSmartMinutes
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SettingsRepository.DEFAULT_SNOOZE_SMART
        )

    // ---------- Тема ----------

    val appTheme: StateFlow<AppTheme> = repo.appTheme
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SettingsRepository.DEFAULT_THEME
        )

    private val _themeDialogOpen = MutableStateFlow(false)
    val themeDialogOpen: StateFlow<Boolean> = _themeDialogOpen.asStateFlow()

    fun openThemeDialog() { _themeDialogOpen.value = true }
    fun closeThemeDialog() { _themeDialogOpen.value = false }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            repo.setAppTheme(theme)
            _themeDialogOpen.value = false
        }
    }

    // ---------- Звук по умолчанию ----------

    val defaultSoundUri: StateFlow<String?> = repo.defaultSoundUri
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null
        )

    val defaultSoundName: StateFlow<String?> = repo.defaultSoundUri
        .map { uri ->
            if (uri.isNullOrBlank()) null
            else SoundResolver.getTrackName(getApplication(), uri)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null
        )

    private val _soundDialogOpen = MutableStateFlow(false)
    val soundDialogOpen: StateFlow<Boolean> = _soundDialogOpen.asStateFlow()

    fun openSoundDialog() { _soundDialogOpen.value = true }
    fun closeSoundDialog() { _soundDialogOpen.value = false }

    fun setDefaultSoundUri(uri: String?) {
        viewModelScope.launch {
            repo.setDefaultSoundUri(uri)
            _soundDialogOpen.value = false
        }
    }

    // ---------- Диалог интервала отложки ----------

    private val _editingField = MutableStateFlow<SnoozeField?>(null)
    val editingField: StateFlow<SnoozeField?> = _editingField.asStateFlow()

    private val _editingValue = MutableStateFlow("")
    val editingValue: StateFlow<String> = _editingValue.asStateFlow()

    private val _snoozeError = MutableStateFlow<SnoozeField?>(null)
    val snoozeError: StateFlow<SnoozeField?> = _snoozeError.asStateFlow()

    fun openEditor(field: SnoozeField) {
        _editingField.value = field
        _editingValue.value = snoozeSmart.value.toString()
    }

    fun closeEditor() {
        _editingField.value = null
        _editingValue.value = ""
        _snoozeError.value = null
    }

    fun updateEditingValue(newValue: String) {
        // только цифры, до 3 символов (максимум 360)
        _editingValue.value = newValue.filter { it.isDigit() }.take(3)
    }

    fun saveEditor() {
        val field = _editingField.value ?: return
        val value = _editingValue.value.toIntOrNull() ?: return

        if (value !in field.minMinutes..field.maxMinutes) {
            _snoozeError.value = field
            return
        }

        viewModelScope.launch {
            repo.setSnoozeSmart(value)
            closeEditor()
        }
    }

    fun dismissSnoozeError() {
        _snoozeError.value = null
    }
}