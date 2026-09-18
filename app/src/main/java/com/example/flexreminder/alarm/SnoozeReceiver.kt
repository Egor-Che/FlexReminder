package com.example.flexreminder.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationManagerCompat

/**
 * Обрабатывает:
 *  1. ACTION_SNOOZE_BUTTON — нажатие «Отложить» в уведомлении.
 *  2. ACTION_SNOOZE_FIRE   — срабатывание отложенного будильника.
 */
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

        val minutes = intent.getIntExtra(EXTRA_MINUTES, 5)
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "Напоминание"
        val notes = intent.getStringExtra(AlarmScheduler.EXTRA_NOTES).orEmpty()
        val silent = intent.getBooleanExtra(Notifications.EXTRA_SILENT, false)

        // Скрываем текущее уведомление
        NotificationManagerCompat.from(context).cancel(id.toInt())

        val trigger = System.currentTimeMillis() + minutes * 60_000L
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = firePI(context, id, title, notes, silent, minutes)

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

    private fun handleSnoozeFire(context: Context, intent: Intent) {
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ID, -1L)
        if (id < 0) return

        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "Напоминание"
        val notes = intent.getStringExtra(AlarmScheduler.EXTRA_NOTES).orEmpty()
        val silent = intent.getBooleanExtra(Notifications.EXTRA_SILENT, false)

        Notifications.show(context, id, title, notes, silent)
    }

    private fun firePI(
        context: Context,
        id: Long,
        title: String,
        notes: String,
        silent: Boolean,
        minutes: Int
    ): PendingIntent {
        val intent = Intent(context, SnoozeReceiver::class.java).apply {
            action = ACTION_SNOOZE_FIRE
            data = Uri.parse("flexreminder://snooze-fire/$id/$minutes")
            putExtra(AlarmScheduler.EXTRA_ID, id)
            putExtra(AlarmScheduler.EXTRA_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_NOTES, notes)
            putExtra(Notifications.EXTRA_SILENT, silent)
            putExtra(EXTRA_MINUTES, minutes)
        }
        return PendingIntent.getBroadcast(
            context,
            (id * 100 + minutes + 50).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_SNOOZE_BUTTON = "com.example.flexreminder.SNOOZE_BUTTON"
        const val ACTION_SNOOZE_FIRE = "com.example.flexreminder.SNOOZE_FIRE"
        const val EXTRA_MINUTES = "extra_minutes"
    }
}