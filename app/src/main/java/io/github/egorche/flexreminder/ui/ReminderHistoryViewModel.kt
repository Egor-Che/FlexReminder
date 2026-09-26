package io.github.egorche.flexreminder.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.egorche.flexreminder.alarm.IterationLogic
import io.github.egorche.flexreminder.data.AppDatabase
import io.github.egorche.flexreminder.data.Iteration
import io.github.egorche.flexreminder.data.Reminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Один перенос: время (мс) и длительность в минутах.
 */
data class SnoozeEntry(
    val timestamp: Long,
    val minutes: Int
)

/**
 * Парсит snoozeHistory из строки формата "ts1:min1,ts2:min2,...".
 * Возвращает пустой список при пустой или невалидной строке.
 */
fun parseSnoozeHistory(raw: String): List<SnoozeEntry> {
    if (raw.isBlank()) return emptyList()
    return raw.split(",").mapNotNull { entry ->
        val parts = entry.split(":")
        if (parts.size != 2) return@mapNotNull null
        val ts = parts[0].trim().toLongOrNull() ?: return@mapNotNull null
        val min = parts[1].trim().toIntOrNull() ?: return@mapNotNull null
        if (min <= 0) null else SnoozeEntry(ts, min)
    }
}

class ReminderHistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val reminderDao = db.reminderDao()
    private val iterationDao = db.iterationDao()

    private val _reminder = MutableStateFlow<Reminder?>(null)
    val reminder: StateFlow<Reminder?> = _reminder.asStateFlow()

    private val _iterations = MutableStateFlow<List<Iteration>>(emptyList())
    val iterations: StateFlow<List<Iteration>> = _iterations.asStateFlow()

    /**
     * Все активные дни за весь период напоминания.
     * Используются для подсветки в календаре.
     */
    private val _activeDates = MutableStateFlow<Set<Long>>(emptySet())
    val activeDates: StateFlow<Set<Long>> = _activeDates.asStateFlow()

    fun load(id: Long) {
        viewModelScope.launch {
            val r = reminderDao.getById(id) ?: return@launch
            _reminder.value = r

            val iters = iterationDao.getByReminder(id)
            _iterations.value = iters

            // Генерируем все активные дни за период
            val from = r.startDate
            val to = r.endDate ?: System.currentTimeMillis()
            val allDates = IterationLogic.generateIterationDates(r, from, to).toSet()
            _activeDates.value = allDates
        }
    }
}