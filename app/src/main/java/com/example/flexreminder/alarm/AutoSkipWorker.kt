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
        val lookforwardLimit = DateUtils.addDays(todayMidnight, 365)

        val activeReminders = reminderDao.getEnabled()

        for (reminder in activeReminders) {
            val from = maxOf(DateUtils.midnight(reminder.startDate), lookbackLimit)
            val to = minOf(
                reminder.endDate?.let { DateUtils.midnight(it) } ?: Long.MAX_VALUE,
                todayMidnight
            )
            if (from > to) continue

            val datesToCheck = IterationLogic.generateIterationDates(reminder, from, to)
            if (datesToCheck.isEmpty()) continue

            val allDates = IterationLogic.generateIterationDates(reminder, from, lookforwardLimit)

            val existing = iterationDao.getByReminder(reminder.id)
                .associateBy { it.dateMillis }

            val toInsert = mutableListOf<Iteration>()

            for (dateMillis in datesToCheck) {
                val current = existing[dateMillis]

                val isReplaceable = current == null ||
                        current.status == IterationStatus.PENDING ||
                        (current.status == IterationStatus.SKIPPED &&
                                current.statusSource == StatusSource.SYSTEM)
                if (!isReplaceable) continue

                val scheduledTime = DateUtils.atTime(
                    dateMillis, reminder.hour, reminder.minute
                )
                val candidates = mutableListOf(scheduledTime)
                current?.firedAt?.let { candidates.add(it) }
                current?.lastSnoozeAt?.let { candidates.add(it) }
                val lastActivity = candidates.max()

                val autoSkipBoundary = lastActivity + AUTO_SKIP_DELAY_MS

                val nextDateMillis = allDates.firstOrNull { it > dateMillis }
                val nextIterationTime = nextDateMillis?.let {
                    DateUtils.atTime(it, reminder.hour, reminder.minute)
                }
                val effectiveBoundary = if (nextIterationTime != null) {
                    minOf(autoSkipBoundary, nextIterationTime)
                } else {
                    autoSkipBoundary
                }

                if (now < effectiveBoundary) continue

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

        // ============================================================
        // TODO: перед релизом вернуть AUTO_SKIP_DELAY_HOURS = 3.
        //       Для тестирования стоит уменьшенное значение.
        // ============================================================
        const val AUTO_SKIP_DELAY_HOURS = 1

        /** Периодичность запуска воркера (как часто проверять). */
        const val AUTO_SKIP_CHECK_INTERVAL_HOURS = 1L

        private val AUTO_SKIP_DELAY_MS =
            AUTO_SKIP_DELAY_HOURS * 60 * 60 * 1000L

        fun schedule(context: Context) {
            val constraints = Constraints.Builder().build()
            val request = PeriodicWorkRequestBuilder<AutoSkipWorker>(
                AUTO_SKIP_CHECK_INTERVAL_HOURS, TimeUnit.HOURS
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