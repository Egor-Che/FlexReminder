package com.example.flexreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.flexreminder.data.AppDatabase
import com.example.flexreminder.data.Iteration
import com.example.flexreminder.data.IterationStatus
import com.example.flexreminder.data.StatusSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Обрабатывает нажатие кнопки «Выполнено» в уведомлении.
 * Ставит статус COMPLETED (USER) для текущей итерации.
 */
class MarkCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MARK_COMPLETED) return

        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ID, -1L)
        if (id < 0) return

        // Сразу скрываем уведомление из шторки
        NotificationManagerCompat.from(context).cancel(id.toInt())

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.get(context)
                val reminderDao = db.reminderDao()
                val iterationDao = db.iterationDao()

                val reminder = reminderDao.getById(id) ?: return@launch

                val now = System.currentTimeMillis()
                val todayMidnight = DateUtils.midnight(now)

                // Определяем текущую итерацию
                val scheduledDate = findCurrentIterationDate(reminder, now) ?: todayMidnight

                val existing = iterationDao.get(id, scheduledDate)
                val iteration = (existing
                    ?: Iteration(reminderId = id, dateMillis = scheduledDate))
                    .copy(
                        status = IterationStatus.COMPLETED,
                        statusSource = StatusSource.USER,
                        statusChangedAt = now
                    )

                iterationDao.upsertByDate(iteration)

                // Пересчитываем следующий будильник (на случай, если пользователь
                // также заранее отметил и следующие итерации)
                AlarmScheduler.schedule(context, reminder)
            } finally {
                pending.finish()
            }
        }
    }

    private fun findCurrentIterationDate(
        reminder: com.example.flexreminder.data.Reminder,
        now: Long
    ): Long? {
        val todayMidnight = DateUtils.midnight(now)
        val from = DateUtils.addDays(todayMidnight, -2)
        val to = DateUtils.addDays(todayMidnight, 1)

        val dates = IterationLogic.generateIterationDates(reminder, from, to)
        if (dates.isEmpty()) return null

        return dates
            .map { DateUtils.atTime(it, reminder.hour, reminder.minute) to it }
            .filter { (trigger, _) -> trigger <= now }
            .maxByOrNull { (trigger, _) -> trigger }
            ?.second
            ?: todayMidnight
    }

    companion object {
        const val ACTION_MARK_COMPLETED = "com.example.flexreminder.MARK_COMPLETED"
    }
}