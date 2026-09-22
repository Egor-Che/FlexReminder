package com.example.flexreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.flexreminder.data.AppDatabase
import com.example.flexreminder.data.Iteration
import com.example.flexreminder.data.IterationStatus
import com.example.flexreminder.data.StatusSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ID, -1L)
        if (id < 0) return

        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "Напоминание"
        val notes = intent.getStringExtra(AlarmScheduler.EXTRA_NOTES).orEmpty()
        val silent = intent.getBooleanExtra(Notifications.EXTRA_SILENT, false)

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.get(context)
                val reminderDao = db.reminderDao()
                val iterationDao = db.iterationDao()

                val reminder = reminderDao.getById(id)
                if (reminder == null || !reminder.enabled) return@launch

                // Находим текущую итерацию (та, что только что сработала)
                val now = System.currentTimeMillis()
                val todayMidnight = DateUtils.midnight(now)

                val scheduledDate = findScheduledIterationDate(reminder, iterationDao, now)
                    ?: todayMidnight

                val existing = iterationDao.get(id, scheduledDate)

                // Проверяем статус: если пользователь заранее отметил итерацию —
                // уведомление не показываем, сразу планируем следующее
                val alreadyMarked = existing != null && (
                        existing.status == IterationStatus.COMPLETED ||
                                (existing.status == IterationStatus.SKIPPED &&
                                        existing.statusSource == StatusSource.USER)
                        )

                if (!alreadyMarked) {
                    // Записываем firedAt и обновляем/создаём запись итерации
                    val iteration = existing?.copy(firedAt = now)
                        ?: Iteration(
                            reminderId = id,
                            dateMillis = scheduledDate,
                            firedAt = now
                        )
                    iterationDao.upsertByDate(iteration)

                    // Показываем уведомление
                    Notifications.show(context, id, title, notes, silent)
                }

                // Перепланируем следующее срабатывание
                AlarmScheduler.schedule(context, reminder)
            } finally {
                pending.finish()
            }
        }
    }

    /**
     * Определяет, на какую дату было запланировано срабатывание.
     * Ищем ближайшую к now итерацию, которая уже должна была сработать,
     * но ещё не обработана. Окно поиска — ±2 дня от сегодня.
     */
    private suspend fun findScheduledIterationDate(
        reminder: com.example.flexreminder.data.Reminder,
        iterationDao: com.example.flexreminder.data.IterationDao,
        now: Long
    ): Long? {
        val todayMidnight = DateUtils.midnight(now)
        val from = DateUtils.addDays(todayMidnight, -2)
        val to = DateUtils.addDays(todayMidnight, 1)

        val dates = IterationLogic.generateIterationDates(reminder, from, to)
        if (dates.isEmpty()) return null

        // Берём последнюю дату, чей момент срабатывания (с учётом времени напоминания)
        // уже наступил, но не более 5 минут назад (защита от старых срабатываний)
        return dates
            .map { DateUtils.atTime(it, reminder.hour, reminder.minute) to it }
            .filter { (trigger, _) -> trigger <= now }
            .maxByOrNull { (trigger, _) -> trigger }
            ?.second
    }
}