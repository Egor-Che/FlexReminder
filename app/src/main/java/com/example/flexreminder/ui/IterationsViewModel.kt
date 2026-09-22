package com.example.flexreminder.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexreminder.alarm.AlarmScheduler
import com.example.flexreminder.alarm.DateUtils
import com.example.flexreminder.alarm.IterationLogic
import com.example.flexreminder.data.AppDatabase
import com.example.flexreminder.data.Iteration
import com.example.flexreminder.data.IterationStatus
import com.example.flexreminder.data.Reminder
import com.example.flexreminder.data.StatusSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ToggleResult {
    CHANGED,
    BLOCKED_BY_SYSTEM
}

class IterationsViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val reminderDao = db.reminderDao()
    private val iterationDao = db.iterationDao()

    private var reminderId: Long = -1L

    private val _reminder = MutableStateFlow<Reminder?>(null)
    val reminder: StateFlow<Reminder?> = _reminder.asStateFlow()

    private val _iterations = MutableStateFlow<List<Iteration>>(emptyList())
    val iterations: StateFlow<List<Iteration>> = _iterations.asStateFlow()

    private val _hasChanges = MutableStateFlow(false)
    val hasChanges: StateFlow<Boolean> = _hasChanges.asStateFlow()

    private val snapshot = mutableMapOf<Long, Iteration?>()

    private val todayMidnight: Long = DateUtils.midnight(System.currentTimeMillis())

    fun load(id: Long) {
        if (reminderId == id && _reminder.value != null) return
        reminderId = id
        snapshot.clear()
        _hasChanges.value = false

        viewModelScope.launch {
            val r = reminderDao.getById(id) ?: return@launch
            _reminder.value = r
            refresh()
        }
    }

    private suspend fun refresh() {
        val r = _reminder.value ?: return
        val dbIterations = iterationDao.getByReminder(r.id)

        val from = maxOf(
            DateUtils.midnight(r.startDate),
            DateUtils.addDays(todayMidnight, -365 * 3)
        )
        val to = minOf(
            r.endDate?.let { DateUtils.midnight(it) } ?: Long.MAX_VALUE,
            DateUtils.addDays(todayMidnight, 365 * 3)
        )

        val merged = if (from > to) emptyList() else IterationLogic.mergeWithDb(
            reminder = r,
            fromMidnight = from,
            toMidnight = to,
            dbIterations = dbIterations
        )
        _iterations.value = merged
    }

    /**
     * Тап по иконке статуса.
     *
     * Матрица переходов:
     *  Будущие/сегодняшние итерации:
     *    - PENDING → COMPLETED (USER) / SKIPPED (USER)
     *    - COMPLETED (USER) → повторный тап по ✅ = PENDING
     *    - COMPLETED (USER) → тап по ⏭️ = SKIPPED (USER)
     *    - SKIPPED (USER) → повторный тап по ⏭️ = PENDING
     *    - SKIPPED (USER) → тап по ✅ = COMPLETED (USER)
     *
     *  Прошедшие итерации:
     *    - SKIPPED (SYSTEM) → тап по ✅ = COMPLETED (USER), тап по ❌ = блок
     *    - COMPLETED (USER) → повторный тап по ✅ = SKIPPED (SYSTEM)
     *    - COMPLETED (USER) → тап по ⏭️ = SKIPPED (SYSTEM) (не USER!)
     *    - PENDING → тап по ⏭️ = SKIPPED (SYSTEM) (не USER!)
     */
    fun toggleStatus(dateMillis: Long, target: IterationStatus): ToggleResult {
        val current = _iterations.value.find { it.dateMillis == dateMillis }
        val currentStatus = current?.status ?: IterationStatus.PENDING
        val currentSource = current?.statusSource

        val isSystemSkipped = currentStatus == IterationStatus.SKIPPED &&
                currentSource == StatusSource.SYSTEM

        if (isSystemSkipped && target != IterationStatus.COMPLETED) {
            return ToggleResult.BLOCKED_BY_SYSTEM
        }

        viewModelScope.launch {
            saveSnapshotFor(dateMillis)
            val now = System.currentTimeMillis()
            val isPast = DateUtils.endOfDay(dateMillis) < now

            val newIteration: Iteration = when {
                // Снятие USER-статуса: повторный тап по активной иконке
                currentStatus == target && currentSource == StatusSource.USER -> {
                    val base = current ?: Iteration(
                        reminderId = reminderId, dateMillis = dateMillis
                    )
                    if (isPast) {
                        // Прошедшая итерация не может стать PENDING —
                        // возвращаем системный пропуск
                        base.copy(
                            status = IterationStatus.SKIPPED,
                            statusSource = StatusSource.SYSTEM,
                            statusChangedAt = now
                        )
                    } else {
                        base.copy(
                            status = IterationStatus.PENDING,
                            statusSource = null,
                            statusChangedAt = now
                        )
                    }
                }
                // Замена системного SKIPPED на COMPLETED
                isSystemSkipped && target == IterationStatus.COMPLETED -> {
                    current!!.copy(
                        status = IterationStatus.COMPLETED,
                        statusSource = StatusSource.USER,
                        statusChangedAt = now
                    )
                }
                // Обычная замена/установка
                else -> {
                    val base = current ?: Iteration(
                        reminderId = reminderId, dateMillis = dateMillis
                    )
                    // Для прошедшей итерации SKIPPED — всегда SYSTEM
                    val effectiveSource = if (target == IterationStatus.SKIPPED && isPast) {
                        StatusSource.SYSTEM
                    } else {
                        StatusSource.USER
                    }
                    base.copy(
                        status = target,
                        statusSource = effectiveSource,
                        statusChangedAt = now
                    )
                }
            }

            if (newIteration.status == IterationStatus.PENDING && newIteration.id != 0L) {
                iterationDao.delete(newIteration.reminderId, newIteration.dateMillis)
            } else if (newIteration.status != IterationStatus.PENDING) {
                iterationDao.upsertByDate(newIteration)
            }

            updateAlarmForReminder()
            _hasChanges.value = true
            refresh()
        }
        return ToggleResult.CHANGED
    }

    fun countCompletablePast(): Int =
        _iterations.value.count { it.dateMillis < todayMidnight && it.isReplaceable() }

    fun countSkippableFuture(): Int {
        val limit = futureLimit()
        return _iterations.value.count {
            it.dateMillis >= todayMidnight && it.dateMillis <= limit && it.isReplaceable()
        }
    }

    fun pastRange(): Pair<Long, Long>? {
        val past = _iterations.value.filter {
            it.dateMillis < todayMidnight && it.isReplaceable()
        }
        if (past.isEmpty()) return null
        return past.first().dateMillis to past.last().dateMillis
    }

    fun futureRange(): Pair<Long, Long>? {
        val limit = futureLimit()
        val future = _iterations.value.filter {
            it.dateMillis >= todayMidnight && it.dateMillis <= limit && it.isReplaceable()
        }
        if (future.isEmpty()) return null
        return future.first().dateMillis to future.last().dateMillis
    }

    fun completeAllPast(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val targets = _iterations.value.filter {
                it.dateMillis < todayMidnight && it.isReplaceable()
            }
            applyMass(targets, IterationStatus.COMPLETED)
            onDone()
        }
    }

    fun skipAllFuture(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val limit = futureLimit()
            val targets = _iterations.value.filter {
                it.dateMillis >= todayMidnight &&
                        it.dateMillis <= limit &&
                        it.isReplaceable()
            }
            applyMass(targets, IterationStatus.SKIPPED)
            onDone()
        }
    }

    private suspend fun applyMass(targets: List<Iteration>, newStatus: IterationStatus) {
        if (targets.isEmpty()) return
        val now = System.currentTimeMillis()
        val toUpdate = targets.map { it ->
            saveSnapshotFor(it.dateMillis)
            val isPast = DateUtils.endOfDay(it.dateMillis) < now
            val effectiveSource = if (newStatus == IterationStatus.SKIPPED && isPast) {
                StatusSource.SYSTEM
            } else {
                StatusSource.USER
            }
            it.copy(
                status = newStatus,
                statusSource = effectiveSource,
                statusChangedAt = now
            )
        }
        iterationDao.upsertAll(toUpdate)
        updateAlarmForReminder()
        _hasChanges.value = true
        refresh()
    }

    fun rollbackSession(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            for ((dateMillis, snapshotValue) in snapshot) {
                if (snapshotValue == null) {
                    iterationDao.delete(reminderId, dateMillis)
                } else {
                    iterationDao.upsertByDate(snapshotValue)
                }
            }
            snapshot.clear()
            _hasChanges.value = false
            updateAlarmForReminder()
            refresh()
            onDone()
        }
    }

    private fun saveSnapshotFor(dateMillis: Long) {
        if (snapshot.containsKey(dateMillis)) return
        val current = _iterations.value.find { it.dateMillis == dateMillis }
        snapshot[dateMillis] = current?.takeIf { it.status != IterationStatus.PENDING }
    }

    private suspend fun updateAlarmForReminder() {
        val r = _reminder.value ?: return
        if (!r.enabled) return

        AlarmScheduler.cancel(getApplication(), r.id)

        val now = System.currentTimeMillis()
        val dbIterations = iterationDao.getByReminder(r.id)

        val futureFrom = DateUtils.midnight(now)
        val futureTo = DateUtils.addDays(futureFrom, 365 * 3)
        val merged = IterationLogic.mergeWithDb(r, futureFrom, futureTo, dbIterations)

        val next = merged.firstOrNull {
            it.status == IterationStatus.PENDING &&
                    DateUtils.atTime(it.dateMillis, r.hour, r.minute) > now
        }

        if (next != null) {
            AlarmScheduler.scheduleAt(getApplication(), r, next.dateMillis)
        }
    }

    private fun Iteration.isReplaceable(): Boolean {
        return status == IterationStatus.PENDING ||
                (status == IterationStatus.SKIPPED && statusSource == StatusSource.SYSTEM)
    }

    private fun futureLimit(): Long {
        val r = _reminder.value ?: return todayMidnight
        val oneYear = DateUtils.addDays(todayMidnight, 365)
        val end = r.endDate?.let { DateUtils.midnight(it) } ?: Long.MAX_VALUE
        return minOf(oneYear, end)
    }
}