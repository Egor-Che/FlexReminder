package com.example.flexreminder.ui

import android.app.Application
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexreminder.R
import com.example.flexreminder.alarm.SoundResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SoundCategory(
    val ringtoneType: Int,
    @StringRes val titleRes: Int
) {
    RINGTONE(
        RingtoneManager.TYPE_RINGTONE,
        R.string.sound_category_ringtones
    ),
    NOTIFICATION(
        RingtoneManager.TYPE_NOTIFICATION,
        R.string.sound_category_notifications
    ),
    ALARM(
        RingtoneManager.TYPE_ALARM,
        R.string.sound_category_alarms
    )
}

class SoundPickerViewModel(app: Application) : AndroidViewModel(app) {

    private val _currentUri = MutableStateFlow<String?>(null)
    val currentUri: StateFlow<String?> = _currentUri.asStateFlow()

    private val _currentName = MutableStateFlow<String?>(null)
    val currentName: StateFlow<String?> = _currentName.asStateFlow()

    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing.asStateFlow()

    private val _activeCategory = MutableStateFlow(SoundCategory.RINGTONE)
    val activeCategory: StateFlow<SoundCategory> = _activeCategory.asStateFlow()

    private val _soundsByCategory =
        MutableStateFlow<Map<SoundCategory, List<SoundResolver.SystemSound>>>(emptyMap())
    val soundsByCategory:
            StateFlow<Map<SoundCategory, List<SoundResolver.SystemSound>>> =
        _soundsByCategory.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private var ringtone: Ringtone? = null

    fun initialize(uri: String?) {
        _currentUri.value = uri
        refreshCurrentName()
        if (_soundsByCategory.value.isEmpty()) {
            loadAllCategories()
        }
    }

    private fun refreshCurrentName() {
        val uri = _currentUri.value
        if (uri.isNullOrBlank()) {
            _currentName.value = null
            return
        }
        viewModelScope.launch {
            _currentName.value = withContext(Dispatchers.IO) {
                SoundResolver.getTrackName(getApplication(), uri)
            }
        }
    }

    private fun loadAllCategories() {
        viewModelScope.launch {
            _loading.value = true
            val map = withContext(Dispatchers.IO) {
                SoundCategory.values().associateWith { category ->
                    SoundResolver.listSystemSounds(getApplication(), category.ringtoneType)
                }
            }
            _soundsByCategory.value = map
            _loading.value = false
        }
    }

    fun setCategory(category: SoundCategory) {
        _activeCategory.value = category
    }

    fun select(uri: String) {
        _currentUri.value = uri
        refreshCurrentName()
    }

    fun clear() {
        _currentUri.value = null
        _currentName.value = null
    }

    fun togglePlay() {
        if (_playing.value) stop() else play()
    }

    private fun play() {
        val uri = _currentUri.value
        val effectiveUri = uri ?: SoundResolver.getBuiltinSoundUri(getApplication())
        try {
            val r = RingtoneManager.getRingtone(
                getApplication(),
                Uri.parse(effectiveUri)
            )
            r?.play()
            ringtone = r
            _playing.value = true
        } catch (_: Exception) {
            _playing.value = false
        }
    }

    fun stop() {
        try {
            ringtone?.stop()
        } catch (_: Exception) {
        }
        ringtone = null
        _playing.value = false
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}