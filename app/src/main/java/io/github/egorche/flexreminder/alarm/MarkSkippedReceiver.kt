package io.github.egorche.flexreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import io.github.egorche.flexreminder.data.AppDatabase
import io.github.egorche.flexreminder.data.Iteration
import io.github.egorche.flexreminder.data.IterationStatus
import io.github.egorche.flexreminder.data.StatusSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MarkSkippedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MARK_SKIPPED) return

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
                    DateUtils.midnight(System.currentTimeMillis())
                }

                val now = System.currentTimeMillis()
                val isPast = DateUtils.endOfDay(targetDate) < now

                val existing = iterationDao.get(id, targetDate)
                val iteration = (existing
                    ?: Iteration(reminderId = id, dateMillis = targetDate))
                    .copy(
                        status = IterationStatus.SKIPPED,
                        statusSource = if (isPast) StatusSource.SYSTEM else StatusSource.USER,
                        statusChangedAt = now,
                        snoozeCount = 0,
                        lastSnoozeAt = null,
                        snoozeUntil = null
                    )

                iterationDao.upsertByDate(iteration)

                SnoozeReceiver.cancelAllSnoozes(context, id)
                AlarmScheduler.schedule(context, reminder)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_MARK_SKIPPED = "io.github.egorche.flexreminder.MARK_SKIPPED"
    }
}