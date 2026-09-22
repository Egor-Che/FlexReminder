package com.example.flexreminder.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexreminder.data.AppTheme
import com.example.flexreminder.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SnoozeField { SHORT, LONG }

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SettingsRepository.get(app)

    // ---------- Snooze ----------

    val snoozeShort: StateFlow<Int> = repo.snoozeShortMinutes
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SettingsRepository.DEFAULT_SNOOZE_SHORT
        )

    val snoozeLong: StateFlow<Int> = repo.snoozeLongMinutes
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SettingsRepository.DEFAULT_SNOOZE_LONG
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

    fun openThemeDialog() {
        _themeDialogOpen.value = true
    }

    fun closeThemeDialog() {
        _themeDialogOpen.value = false
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            repo.setAppTheme(theme)
            _themeDialogOpen.value = false
        }
    }

    // ---------- Диалог snooze ----------

    private val _editingField = MutableStateFlow<SnoozeField?>(null)
    val editingField: StateFlow<SnoozeField?> = _editingField.asStateFlow()

    private val _editingValue = MutableStateFlow("")
    val editingValue: StateFlow<String> = _editingValue.asStateFlow()

    fun openEditor(field: SnoozeField) {
        _editingField.value = field
        _editingValue.value = when (field) {
            SnoozeField.SHORT -> snoozeShort.value.toString()
            SnoozeField.LONG -> snoozeLong.value.toString()
        }
    }

    fun closeEditor() {
        _editingField.value = null
        _editingValue.value = ""
    }

    fun updateEditingValue(newValue: String) {
        _editingValue.value = newValue.filter { it.isDigit() }.take(3)
    }

    fun saveEditor() {
        val field = _editingField.value ?: return
        val value = _editingValue.value.toIntOrNull() ?: return
        val clamped = value.coerceIn(
            SettingsRepository.MIN_SNOOZE_MINUTES,
            SettingsRepository.MAX_SNOOZE_MINUTES
        )

        viewModelScope.launch {
            val currentShort = snoozeShort.value
            val currentLong = snoozeLong.value
            when (field) {
                SnoozeField.SHORT -> repo.setSnoozeIntervals(clamped, currentLong)
                SnoozeField.LONG -> repo.setSnoozeIntervals(currentShort, clamped)
            }
            closeEditor()
        }
    }
}