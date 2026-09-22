package com.example.flexreminder.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexreminder.alarm.AlarmScheduler
import com.example.flexreminder.alarm.SnoozeReceiver
import com.example.flexreminder.data.AppDatabase
import com.example.flexreminder.data.Iteration
import com.example.flexreminder.data.Reminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReminderViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).reminderDao()
    private val iterationDao = AppDatabase.get(app).iterationDao()

    val reminders: StateFlow<List<Reminder>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun observeById(id: Long): Flow<Reminder?> = dao.observeById(id)

    fun observeIterationsFor(reminderId: Long): Flow<List<Iteration>> =
        iterationDao.observeByReminder(reminderId)

    fun observeAllIterations(): Flow<List<Iteration>> = iterationDao.observeAll()

    fun save(reminder: Reminder, onDone: () -> Unit) = viewModelScope.launch {
        val id = if (reminder.id == 0L) {
            dao.insert(reminder)
        } else {
            dao.update(reminder)
            reminder.id
        }
        val saved = reminder.copy(id = id)
        AlarmScheduler.cancel(getApplication(), id)
        if (saved.enabled) {
            AlarmScheduler.schedule(getApplication(), saved)
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