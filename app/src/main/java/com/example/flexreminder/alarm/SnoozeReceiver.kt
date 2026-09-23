package com.example.flexreminder.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.example.flexreminder.data.AppDatabase
import com.example.flexreminder.data.Iteration
import com.example.flexreminder.data.IterationStatus
import com.example.flexreminder.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SnoozeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SNOOZE_BUTTON -> handleSnoozeButton(context, intent)
            ACTION_SNOOZE_FIRE -> handleSnoozeFire(context, intent)
        }
    }

    private fun handleSnoozeButton(context: Context, intent: Intent) {
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ID, -1L)
        if (id < 0) return

        val minutes = intent.getIntExtra(EXTRA_MINUTES, -1)
        if (minutes <= 0) return

        val dateMillis = intent.getLongExtra(EXTRA_DATE_MILLIS, -1L)
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "Напоминание"
        val notes = intent.getStringExtra(AlarmScheduler.EXTRA_NOTES).orEmpty()

        NotificationManagerCompat.from(context).cancel(id.toInt())

        val now = System.currentTimeMillis()
        val trigger = now + minutes * 60_000L
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = buildSnoozeFirePI(context, id, dateMillis, title, notes, minutes)

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

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (dateMillis > 0) {
                    val db = AppDatabase.get(context)
                    val dao = db.iterationDao()
                    val existing = dao.get(id, dateMillis)
                    val base = existing
                        ?: Iteration(reminderId = id, dateMillis = dateMillis)
                    dao.upsertByDate(
                        base.copy(
                            snoozeCount = base.snoozeCount + 1,
                            lastSnoozeAt = now,
                            snoozeUntil = trigger
                        )
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun handleSnoozeFire(context: Context, intent: Intent) {
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ID, -1L)
        if (id < 0) return

        val dateMillis = intent.getLongExtra(EXTRA_DATE_MILLIS, -1L)
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "Напоминание"
        val notes = intent.getStringExtra(AlarmScheduler.EXTRA_NOTES).orEmpty()

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.get(context)
                val reminderDao = db.reminderDao()
                val iterationDao = db.iterationDao()

                val reminder = reminderDao.getById(id) ?: return@launch

                val iteration = if (dateMillis > 0) {
                    iterationDao.get(id, dateMillis)
                } else null

                val alreadyMarked = iteration != null && (
                        iteration.status == IterationStatus.COMPLETED ||
                                iteration.status == IterationStatus.SKIPPED
                        )
                if (alreadyMarked) return@launch

                if (iteration != null) {
                    iterationDao.upsertByDate(iteration.copy(snoozeUntil = null))
                }

                val snoozeCount = iteration?.snoozeCount ?: 0

                val settings = SettingsRepository.get(context)
                val snoozeShort = settings.getSnoozeShort()
                val snoozeLong = settings.getSnoozeLong()

                val channelId = resolveChannel(context, reminder)

                val flags = Notifications.computeFlags(
                    reminder = reminder,
                    dateMillis = dateMillis,
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
                    dateMillis = dateMillis,
                    showSnoozeShort = flags.showSnoozeShort,
                    showSnoozeLong = flags.showSnoozeLong,
                    showSkip = flags.showSkip,
                    snoozeShortMinutes = snoozeShort,
                    snoozeLongMinutes = snoozeLong
                )
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun resolveChannel(
        context: Context,
        reminder: com.example.flexreminder.data.Reminder
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

    companion object {
        const val ACTION_SNOOZE_BUTTON = "com.example.flexreminder.SNOOZE_BUTTON"
        const val ACTION_SNOOZE_FIRE = "com.example.flexreminder.SNOOZE_FIRE"
        const val EXTRA_MINUTES = "extra_minutes"
        const val EXTRA_DATE_MILLIS = "extra_date_millis"

        /**
         * Собирает PendingIntent для срабатывания отложенного уведомления.
         * Public — используется из [SnoozeRestorer] при восстановлении после ребута.
         */
        fun buildSnoozeFirePI(
            context: Context,
            reminderId: Long,
            dateMillis: Long,
            title: String,
            notes: String,
            minutes: Int
        ): PendingIntent {
            val intent = Intent(context, SnoozeReceiver::class.java).apply {
                action = ACTION_SNOOZE_FIRE
                data = Uri.parse("flexreminder://snooze-fire/$reminderId/$dateMillis/$minutes")
                putExtra(AlarmScheduler.EXTRA_ID, reminderId)
                putExtra(EXTRA_DATE_MILLIS, dateMillis)
                putExtra(AlarmScheduler.EXTRA_TITLE, title)
                putExtra(AlarmScheduler.EXTRA_NOTES, notes)
                putExtra(EXTRA_MINUTES, minutes)
            }
            return PendingIntent.getBroadcast(
                context,
                fireRequestCode(reminderId, minutes),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun cancelAllSnoozes(context: Context, reminderId: Long) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val repository = SettingsRepository.get(context)
            val minutesList = listOf(
                repository.getSnoozeShortBlocking(),
                repository.getSnoozeLongBlocking()
            )
            for (minutes in minutesList) {
                val intent = Intent(context, SnoozeReceiver::class.java).apply {
                    action = ACTION_SNOOZE_FIRE
                    data = Uri.parse("flexreminder://snooze-fire/$reminderId/-1/$minutes")
                }
                val pi = PendingIntent.getBroadcast(
                    context,
                    fireRequestCode(reminderId, minutes),
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pi != null) {
                    am.cancel(pi)
                    pi.cancel()
                }
            }
            NotificationManagerCompat.from(context).cancel(reminderId.toInt())
        }

        fun fireRequestCode(id: Long, minutes: Int): Int =
            (id * 1000 + 100 + minutes).toInt()
    }
}