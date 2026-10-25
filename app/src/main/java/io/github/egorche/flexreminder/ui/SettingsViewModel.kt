package io.github.egorche.flexreminder.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.egorche.flexreminder.alarm.SoundResolver
import io.github.egorche.flexreminder.data.AppTheme
import io.github.egorche.flexreminder.data.SettingsRepository
import io.github.egorche.flexreminder.widget.ReminderWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    val snoozeSmart: StateFlow<Int> = repo.snoozeSmartMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_SNOOZE_SMART)

    val appTheme: StateFlow<AppTheme> = repo.appTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_THEME)

    private val _themeDialogOpen = MutableStateFlow(false)
    val themeDialogOpen: StateFlow<Boolean> = _themeDialogOpen.asStateFlow()

    fun openThemeDialog() { _themeDialogOpen.value = true }
    fun closeThemeDialog() { _themeDialogOpen.value = false }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            repo.setAppTheme(theme)
            _themeDialogOpen.value = false
            // Обновляем виджет, если он следует за темой приложения
            ReminderWidgetProvider.updateAll(getApplication())
        }
    }

    val defaultSoundUri: StateFlow<String?> = repo.defaultSoundUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val defaultSoundName: StateFlow<String?> = repo.defaultSoundUri
        .map { uri ->
            if (uri.isNullOrBlank()) null
            else SoundResolver.getTrackName(getApplication(), uri)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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

    // --- Настройки виджета (FEAT #32) ---

    val widgetTheme: StateFlow<String> = repo.widgetTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_WIDGET_THEME)

    private val _widgetThemeDialogOpen = MutableStateFlow(false)
    val widgetThemeDialogOpen: StateFlow<Boolean> = _widgetThemeDialogOpen.asStateFlow()

    fun openWidgetThemeDialog() { _widgetThemeDialogOpen.value = true }
    fun closeWidgetThemeDialog() { _widgetThemeDialogOpen.value = false }

    fun setWidgetTheme(theme: String) {
        viewModelScope.launch {
            repo.setWidgetTheme(theme)
            _widgetThemeDialogOpen.value = false
            ReminderWidgetProvider.updateAll(getApplication())
        }
    }

    val widgetShowNotes: StateFlow<Boolean> = repo.widgetShowNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_WIDGET_SHOW_NOTES)

    fun setWidgetShowNotes(show: Boolean) {
        viewModelScope.launch {
            repo.setWidgetShowNotes(show)
            ReminderWidgetProvider.updateAll(getApplication())
        }
    }
}