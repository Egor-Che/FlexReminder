package io.github.egorche.flexreminder.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.egorche.flexreminder.alarm.AlarmScheduler
import io.github.egorche.flexreminder.alarm.DateUtils
import io.github.egorche.flexreminder.alarm.SnoozeReceiver
import io.github.egorche.flexreminder.data.AppDatabase
import io.github.egorche.flexreminder.data.Iteration
import io.github.egorche.flexreminder.data.Reminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReminderViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val dao = db.reminderDao()
    private val iterationDao = db.iterationDao()

    val reminders: StateFlow<List<Reminder>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun observeById(id: Long): Flow<Reminder?> = dao.observeById(id)

    fun observeIterationsFor(reminderId: Long): Flow<List<Iteration>> =
        iterationDao.observeByReminder(reminderId)

    fun observeAllIterations(): Flow<List<Iteration>> = iterationDao.observeAll()

    fun save(reminder: Reminder, onDone: () -> Unit) = viewModelScope.launch {
        // Проверяем: если endDate в прошлом — сразу архивируем
        val todayMidnight = DateUtils.midnight(System.currentTimeMillis())
        val shouldArchive = reminder.endDate != null && reminder.endDate < todayMidnight

        val prepared = if (shouldArchive) {
            reminder.copy(archivedAt = System.currentTimeMillis())
        } else {
            reminder
        }

        val id = if (prepared.id == 0L) {
            dao.insert(prepared)
        } else {
            dao.update(prepared)
            prepared.id
        }
        val saved = prepared.copy(id = id)
        AlarmScheduler.cancel(getApplication(), id)

        // Если напоминание заархивировано — не планируем будильник
        if (saved.enabled && !shouldArchive) {
            AlarmScheduler.schedule(getApplication(), saved)
        }

        // Если архивируем — закрываем pending-итерации
        if (shouldArchive) {
            iterationDao.markPendingAsSkippedForReminder(id, System.currentTimeMillis())
        }

        onDone()
    }

    fun toggle(reminder: Reminder) = viewModelScope.launch {
        val updated = reminder.copy(enabled = !reminder.enabled)
        dao.update(updated)
        AlarmScheduler.cancel(getApplication(), reminder.id)
        if (updated.enabled) {
            AlarmScheduler.schedule(getApplication(), updated)
        } else {
            SnoozeReceiver.cancelAllSnoozes(getApplication(), reminder.id)
        }
    }

    fun setSilent(reminder: Reminder, silent: Boolean) = viewModelScope.launch {
        val updated = reminder.copy(silent = silent)
        dao.update(updated)
        AlarmScheduler.cancel(getApplication(), reminder.id)
        if (updated.enabled) AlarmScheduler.schedule(getApplication(), updated)
    }

    fun deleteById(id: Long, onDone: () -> Unit = {}) = viewModelScope.launch {
        AlarmScheduler.cancel(getApplication(), id)
        SnoozeReceiver.cancelAllSnoozes(getApplication(), id)
        iterationDao.deleteByReminder(id)
        dao.deleteById(id)
        onDone()
    }
}