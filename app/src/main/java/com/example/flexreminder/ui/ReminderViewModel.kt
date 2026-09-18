package com.example.flexreminder.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexreminder.alarm.AlarmScheduler
import com.example.flexreminder.data.AppDatabase
import com.example.flexreminder.data.Reminder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReminderViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).reminderDao()

    val reminders: StateFlow<List<Reminder>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(reminder: Reminder, onDone: () -> Unit) = viewModelScope.launch {
        val id = if (reminder.id == 0L) {
            dao.insert(reminder)
        } else {
            dao.update(reminder)
            reminder.id
        }
        val saved = reminder.copy(id = id)
        AlarmScheduler.cancel(getApplication(), id)
        if (saved.enabled) AlarmScheduler.schedule(getApplication(), saved)
        onDone()
    }

    fun toggle(reminder: Reminder) = viewModelScope.launch {
        val updated = reminder.copy(enabled = !reminder.enabled)
        dao.update(updated)
        AlarmScheduler.cancel(getApplication(), reminder.id)
        if (updated.enabled) AlarmScheduler.schedule(getApplication(), updated)
    }

    fun deleteById(id: Long, onDone: () -> Unit = {}) = viewModelScope.launch {
        AlarmScheduler.cancel(getApplication(), id)
        dao.deleteById(id)
        onDone()
    }
}