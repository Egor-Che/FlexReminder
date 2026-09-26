package io.github.egorche.flexreminder.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.egorche.flexreminder.alarm.AlarmScheduler
import io.github.egorche.flexreminder.alarm.SnoozeReceiver
import io.github.egorche.flexreminder.data.AppDatabase
import io.github.egorche.flexreminder.data.Reminder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArchiveViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val reminderDao = db.reminderDao()
    private val iterationDao = db.iterationDao()

    val archived: StateFlow<List<Reminder>> = reminderDao.observeArchived()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    /**
     * Полностью очищает архив: удаляет все архивные напоминания вместе
     * с их историями итераций.
     *
     * @param onDone вызывается после завершения удаления
     */
    fun clearArchive(onDone: () -> Unit = {}) = viewModelScope.launch {
        val ids = reminderDao.getArchivedIds()
        ids.forEach { id ->
            // Будильников быть не должно (архивные), но на всякий случай
            AlarmScheduler.cancel(getApplication(), id)
            SnoozeReceiver.cancelAllSnoozes(getApplication(), id)
            iterationDao.deleteByReminder(id)
        }
        reminderDao.deleteAllArchived()
        onDone()
    }
}