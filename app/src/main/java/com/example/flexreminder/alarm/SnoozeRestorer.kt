package com.example.flexreminder.alarm

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import com.example.flexreminder.data.AppDatabase
import com.example.flexreminder.data.Iteration
import com.example.flexreminder.data.IterationStatus
import com.example.flexreminder.data.Reminder
import com.example.flexreminder.data.SettingsRepository

/**
 * Восстанавливает активные snooze после перезагрузки устройства или
 * при запуске приложения.
 *
 * Логика:
 *  1. Находит все итерации с непустым `snoozeUntil`.
 *  2. Если `snoozeUntil > now` — планирует отложенный будильник на это время.
 *  3. Если `snoozeUntil <= now` — показывает уведомление немедленно
 *     (отложка уже давно должна была сработать).
 *
 * Итерации с финальным статусом (COMPLETED / SKIPPED) или
 * привязанные к выключенным/удалённым напоминаниям игнорируются,
 * их snoozeUntil сбрасывается.
 */
object SnoozeRestorer {

    suspend fun restoreAll(context: Context) {
        val db = AppDatabase.get(context)
        val iterationDao = db.iterationDao()
        val reminderDao = db.reminderDao()

        val now = System.currentTimeMillis()
        val iterations = iterationDao.getAllWithActiveSnooze()

        for (iteration in iterations) {
            val trigger = iteration.snoozeUntil ?: continue

            // Если итерация уже отмечена — не восстанавливаем
            if (iteration.status != IterationStatus.PENDING) {
                iterationDao.upsertByDate(iteration.copy(snoozeUntil = null))
                continue
            }

            val reminder = reminderDao.getById(iteration.reminderId)
            if (reminder == null || !reminder.enabled) {
                iterationDao.upsertByDate(iteration.copy(snoozeUntil = null))
                continue
            }

            if (trigger > now) {
                scheduleSnoozeFire(context, reminder, iteration, trigger)
            } else {
                showNow(context, reminder, iteration)
                iterationDao.upsertByDate(iteration.copy(snoozeUntil = null))
            }
        }
    }

    private fun scheduleSnoozeFire(
        context: Context,
        reminder: Reminder,
        iteration: Iteration,
        trigger: Long
    ) {
        val minutes = computeMinutes(iteration, trigger)
        val pi = SnoozeReceiver.buildSnoozeFirePI(
            context = context,
            reminderId = reminder.id,
            dateMillis = iteration.dateMillis,
            title = reminder.title,
            notes = reminder.notes,
            minutes = minutes
        )

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    am.canScheduleExactAlarms()
            if (exact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        }
    }

    /**
     * Восстанавливает `minutes`, с которыми была поставлена отложка.
     * Вычисляется по разнице между trigger и lastSnoozeAt.
     */
    private fun computeMinutes(iteration: Iteration, trigger: Long): Int {
        val lastSnooze = iteration.lastSnoozeAt ?: return DEFAULT_FALLBACK_MINUTES
        val diffMs = trigger - lastSnooze
        val minutes = (diffMs / 60_000L).toInt()
        return minutes.coerceAtLeast(1)
    }

    private suspend fun showNow(
        context: Context,
        reminder: Reminder,
        iteration: Iteration
    ) {
        val settings = SettingsRepository.get(context)
        val snoozeShort = settings.getSnoozeShort()
        val snoozeLong = settings.getSnoozeLong()

        val channelId = resolveChannel(context, reminder)

        val flags = Notifications.computeFlags(
            reminder = reminder,
            dateMillis = iteration.dateMillis,
            snoozeCount = iteration.snoozeCount,
            snoozeShortMinutes = snoozeShort,
            snoozeLongMinutes = snoozeLong
        )

        Notifications.show(
            context = context,
            id = reminder.id,
            title = reminder.title,
            text = reminder.notes,
            channelId = channelId,
            dateMillis = iteration.dateMillis,
            showSnoozeShort = flags.showSnoozeShort,
            showSnoozeLong = flags.showSnoozeLong,
            showSkip = flags.showSkip,
            snoozeShortMinutes = snoozeShort,
            snoozeLongMinutes = snoozeLong
        )
    }

    private suspend fun resolveChannel(
        context: Context,
        reminder: Reminder
    ): String {
        if (reminder.silent) return NotificationChannels.CHANNEL_SILENT

        val settings = SettingsRepository.get(context)
        val globalUri = settings.getDefaultSoundUri()

        val effectiveUri = SoundResolver.resolveSoundUri(context, reminder, globalUri)
        if (effectiveUri == null) {
            return NotificationChannels.CHANNEL_LOUD
        }

        val sameAsGlobal = !reminder.soundUri.isNullOrBlank() &&
                reminder.soundUri == globalUri
        val useGlobalChannel = reminder.soundUri.isNullOrBlank()

        return if (useGlobalChannel || sameAsGlobal) {
            NotificationChannels.getOrCreateDefaultChannel(context, effectiveUri)
        } else {
            NotificationChannels.getOrCreateCustomChannel(context, effectiveUri)
        }
    }

    private const val DEFAULT_FALLBACK_MINUTES = 30
}