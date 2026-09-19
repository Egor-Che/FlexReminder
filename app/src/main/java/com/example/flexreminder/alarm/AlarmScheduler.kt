package com.example.flexreminder.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.example.flexreminder.data.Reminder

object AlarmScheduler {

    const val EXTRA_ID = "extra_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_NOTES = "extra_notes"

    /** Следующий момент срабатывания или null, если дальше ничего нет. */
    fun nextTriggerTime(r: Reminder, from: Long = System.currentTimeMillis()): Long? =
        ReminderLogic.nextTriggerTime(r, from)

    fun schedule(context: Context, r: Reminder) {
        if (!r.enabled) {
            cancel(context, r.id)
            return
        }

        val next = nextTriggerTime(r) ?: run {
            cancel(context, r.id)
            return
        }

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, r.id, r.title, r.notes, r.silent)

        try {
            val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
            if (exact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
        }
    }

    fun cancel(context: Context, id: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context, id, "", "", false))
    }

    private fun pendingIntent(
        context: Context,
        id: Long,
        title: String,
        notes: String,
        silent: Boolean
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.example.flexreminder.ACTION_REMIND"
            data = Uri.parse("flexreminder://reminder/$id")
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_NOTES, notes)
            putExtra(Notifications.EXTRA_SILENT, silent)
        }
        return PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}