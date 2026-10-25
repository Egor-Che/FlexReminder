package io.github.egorche.flexreminder.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.egorche.flexreminder.alarm.AlarmScheduler
import io.github.egorche.flexreminder.alarm.DateUtils
import io.github.egorche.flexreminder.alarm.IterationLogic
import io.github.egorche.flexreminder.alarm.SnoozeReceiver
import io.github.egorche.flexreminder.data.AppDatabase
import io.github.egorche.flexreminder.data.Iteration
import io.github.egorche.flexreminder.data.IterationStatus
import io.github.egorche.flexreminder.data.Reminder
import io.github.egorche.flexreminder.data.StatusSource
import io.github.egorche.flexreminder.widget.ReminderWidgetProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SwipeResult(
    val success: Boolean,
    val previousIteration: Iteration?,
    val dateMillis: Long,
    val reminderId: Long
)

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

        if (saved.enabled && !shouldArchive) {
            AlarmScheduler.schedule(getApplication(), saved)
        }

        if (shouldArchive) {
            iterationDao.markPendingAsSkippedForReminder(id, System.currentTimeMillis())
        }

        onDone()
        ReminderWidgetProvider.updateAll(getApplication())
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
        ReminderWidgetProvider.updateAll(getApplication())
    }

    fun setSilent(reminder: Reminder, silent: Boolean) = viewModelScope.launch {
        val updated = reminder.copy(silent = silent)
        dao.update(updated)
        AlarmScheduler.cancel(getApplication(), reminder.id)
        if (updated.enabled) AlarmScheduler.schedule(getApplication(), updated)
        ReminderWidgetProvider.updateAll(getApplication())
    }

    fun deleteById(id: Long, onDone: () -> Unit = {}) = viewModelScope.launch {
        AlarmScheduler.cancel(getApplication(), id)
        SnoozeReceiver.cancelAllSnoozes(getApplication(), id)
        iterationDao.deleteByReminder(id)
        dao.deleteById(id)
        onDone()
        ReminderWidgetProvider.updateAll(getApplication())
    }

    fun markNearestCompleted(reminder: Reminder, onResult: (SwipeResult) -> Unit) = viewModelScope.launch {
        processSwipe(reminder, IterationStatus.COMPLETED, onResult)
    }

    fun markNearestSkipped(reminder: Reminder, onResult: (SwipeResult) -> Unit) = viewModelScope.launch {
        processSwipe(reminder, IterationStatus.SKIPPED, onResult)
    }

    private suspend fun processSwipe(
        reminder: Reminder,
        targetStatus: IterationStatus,
        onResult: (SwipeResult) -> Unit
    ) {
        val todayMidnight = DateUtils.midnight(System.currentTimeMillis())
        val iterations = iterationDao.getByReminder(reminder.id)
        val nearest = IterationLogic.getThreeIterations(reminder, todayMidnight, iterations).second

        if (nearest == null || nearest.dateMillis != todayMidnight) {
            onResult(SwipeResult(false, null, 0, reminder.id))
            return
        }

        val previous = if (nearest.id == 0L) null else nearest

        val isSameUserStatus = nearest.status == targetStatus && nearest.statusSource == StatusSource.USER

        val newStatus = if (isSameUserStatus) IterationStatus.PENDING else targetStatus
        val newSource = if (isSameUserStatus) null else StatusSource.USER

        val updated = nearest.copy(
            status = newStatus,
            statusSource = newSource,
            statusChangedAt = System.currentTimeMillis(),
            snoozeCount = 0,
            lastSnoozeAt = null,
            snoozeUntil = null
        )

        if (newStatus == IterationStatus.PENDING && updated.id != 0L) {
            iterationDao.delete(updated.reminderId, updated.dateMillis)
        } else if (newStatus != IterationStatus.PENDING) {
            iterationDao.upsertByDate(updated)
        }

        SnoozeReceiver.cancelAllSnoozes(getApplication(), reminder.id)
        AlarmScheduler.schedule(getApplication(), reminder)

        onResult(SwipeResult(true, previous, todayMidnight, reminder.id))
        ReminderWidgetProvider.updateAll(getApplication())
    }

    fun undoSwipeAction(reminder: Reminder, result: SwipeResult) = viewModelScope.launch {
        if (!result.success) return@launch

        if (result.previousIteration == null) {
            iterationDao.delete(result.reminderId, result.dateMillis)
        } else {
            iterationDao.upsertByDate(result.previousIteration)
        }

        AlarmScheduler.schedule(getApplication(), reminder)
        ReminderWidgetProvider.updateAll(getApplication())
    }
}