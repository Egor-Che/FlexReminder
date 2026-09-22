package com.example.flexreminder.alarm

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.flexreminder.data.AppDatabase
import com.example.flexreminder.data.Iteration
import com.example.flexreminder.data.IterationStatus
import com.example.flexreminder.data.StatusSource
import java.util.concurrent.TimeUnit

/**
 * Периодическая задача: раз в 3 часа проходит по прошедшим итерациям,
 * у которых прошло >3 часов после полуночи их даты, и ставит SKIPPED (SYSTEM).
 * COMPLETED и SKIPPED (USER) не затрагиваются.
 */
class AutoSkipWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val db = AppDatabase.get(context)
        val reminderDao = db.reminderDao()
        val iterationDao = db.iterationDao()

        val now = System.currentTimeMillis()
        val todayMidnight = DateUtils.midnight(now)
        val lookbackLimit = DateUtils.addDays(todayMidnight, -365)

        val activeReminders = reminderDao.getEnabled()

        for (reminder in activeReminders) {
            val from = maxOf(DateUtils.midnight(reminder.startDate), lookbackLimit)
            val to = minOf(
                reminder.endDate?.let { DateUtils.midnight(it) } ?: todayMidnight,
                todayMidnight
            )
            if (from > to) continue

            val dates = IterationLogic.generateIterationDates(reminder, from, to)
            if (dates.isEmpty()) continue

            val existing = iterationDao.getByReminder(reminder.id).associateBy { it.dateMillis }

            val toInsert = mutableListOf<Iteration>()

            for (dateMillis in dates) {
                // Граница автопропуска: полночь следующего дня + 3 часа
                val boundary = DateUtils.addDays(dateMillis, 1) + 3 * 60 * 60 * 1000L
                if (now < boundary) continue

                val current = existing[dateMillis]
                val isReplaceable = current == null ||
                        current.status == IterationStatus.PENDING ||
                        (current.status == IterationStatus.SKIPPED &&
                                current.statusSource == StatusSource.SYSTEM)
                if (!isReplaceable) continue

                val base = current ?: Iteration(
                    reminderId = reminder.id,
                    dateMillis = dateMillis
                )
                toInsert.add(
                    base.copy(
                        status = IterationStatus.SKIPPED,
                        statusSource = StatusSource.SYSTEM,
                        statusChangedAt = now
                    )
                )
            }

            if (toInsert.isNotEmpty()) {
                iterationDao.upsertAll(toInsert)
            }
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "auto_skip_worker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder().build()
            val request = PeriodicWorkRequestBuilder<AutoSkipWorker>(
                3, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}