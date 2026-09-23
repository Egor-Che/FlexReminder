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

class MarkCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MARK_COMPLETED) return

        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ID, -1L)
        if (id < 0) return

        val dateMillis = intent.getLongExtra(SnoozeReceiver.EXTRA_DATE_MILLIS, -1L)

        NotificationManagerCompat.from(context).cancel(id.toInt())

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.get(context)
                val reminderDao = db.reminderDao()
                val iterationDao = db.iterationDao()

                val reminder = reminderDao.getById(id) ?: return@launch

                val targetDate = if (dateMillis > 0) {
                    dateMillis
                } else {
                    findCurrentIterationDate(reminder, System.currentTimeMillis())
                        ?: DateUtils.midnight(System.currentTimeMillis())
                }

                val now = System.currentTimeMillis()
                val existing = iterationDao.get(id, targetDate)

                val iteration = (existing
                    ?: Iteration(reminderId = id, dateMillis = targetDate))
                    .copy(
                        status = IterationStatus.COMPLETED,
                        statusSource = StatusSource.USER,
                        statusChangedAt = now,
                        snoozeCount = 0,
                        lastSnoozeAt = null,
                        snoozeUntil = null
                    )

                iterationDao.upsertByDate(iteration)

                // Отменяем возможный отложенный snooze
                SnoozeReceiver.cancelAllSnoozes(context, id)

                // Пересчитываем следующий будильник
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