package io.github.egorche.flexreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.egorche.flexreminder.data.AppDatabase
import io.github.egorche.flexreminder.data.Iteration
import io.github.egorche.flexreminder.data.IterationStatus
import io.github.egorche.flexreminder.data.SettingsRepository
import io.github.egorche.flexreminder.data.StatusSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ID, -1L)
        if (id < 0) return

        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "Напоминание"
        val notes = intent.getStringExtra(AlarmScheduler.EXTRA_NOTES).orEmpty()

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.get(context)
                val reminderDao = db.reminderDao()
                val iterationDao = db.iterationDao()

                val reminder = reminderDao.getById(id)
                if (reminder == null || !reminder.enabled) return@launch

                val now = System.currentTimeMillis()
                val scheduledDate = findScheduledIterationDate(reminder, now)
                    ?: DateUtils.midnight(now)

                val existing = iterationDao.get(id, scheduledDate)

                val alreadyMarked = existing != null && (
                        existing.status == IterationStatus.COMPLETED ||
                                (existing.status == IterationStatus.SKIPPED &&
                                        existing.statusSource == StatusSource.USER)
                        )

                if (!alreadyMarked) {
                    val iteration = existing?.copy(firedAt = now)
                        ?: Iteration(
                            reminderId = id,
                            dateMillis = scheduledDate,
                            firedAt = now
                        )
                    iterationDao.upsertByDate(iteration)

                    val snoozeCount = iteration.snoozeCount
                    val settings = SettingsRepository.get(context)
                    val snoozeShort = settings.getSnoozeShort()
                    val snoozeLong = settings.getSnoozeLong()

                    // Определяем канал: silent / custom / default / базовый
                    val channelId = resolveChannel(context, reminder)

                    val flags = Notifications.computeFlags(
                        reminder = reminder,
                        dateMillis = scheduledDate,
                        snoozeCount = snoozeCount,
                        snoozeShortMinutes = snoozeShort,
                        snoozeLongMinutes = snoozeLong
                    )

                    Notifications.show(
                        context = context,
                        id = id,
                        title = title,
                        text = notes,
                        channelId = channelId,
                        dateMillis = scheduledDate,
                        showSnoozeShort = flags.showSnoozeShort,
                        showSnoozeLong = flags.showSnoozeLong,
                        showSkip = flags.showSkip,
                        snoozeShortMinutes = snoozeShort,
                        snoozeLongMinutes = snoozeLong
                    )
                }

                AlarmScheduler.schedule(context, reminder)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun resolveChannel(
        context: Context,
        reminder: io.github.egorche.flexreminder.data.Reminder
    ): String {
        if (reminder.silent) return NotificationChannels.CHANNEL_SILENT

        val settings = SettingsRepository.get(context)
        val globalUri = settings.getDefaultSoundUri()

        val effectiveUri = SoundResolver.resolveSoundUri(context, reminder, globalUri)
        if (effectiveUri == null) {
            return NotificationChannels.CHANNEL_LOUD
        }

        // Если звук напоминания совпадает с глобальным → используем канал default
        val sameAsGlobal = !reminder.soundUri.isNullOrBlank() &&
                reminder.soundUri == globalUri
        val useGlobalChannel = reminder.soundUri.isNullOrBlank()

        return if (useGlobalChannel || sameAsGlobal) {
            NotificationChannels.getOrCreateDefaultChannel(context, effectiveUri)
        } else {
            NotificationChannels.getOrCreateCustomChannel(context, effectiveUri)
        }
    }

    private fun findScheduledIterationDate(
        reminder: io.github.egorche.flexreminder.data.Reminder,
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
    }
}