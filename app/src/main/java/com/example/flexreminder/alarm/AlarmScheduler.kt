package com.example.flexreminder.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.example.flexreminder.data.Reminder
import java.util.Calendar

object AlarmScheduler {

    private const val DAY_MS = 24L * 60 * 60 * 1000

    const val EXTRA_ID = "extra_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_NOTES = "extra_notes"

    /** Следующий момент срабатывания или null, если период закончился. */
    fun nextTriggerTime(r: Reminder, from: Long = System.currentTimeMillis()): Long? {
        if (r.daysOn < 1) return null
        val daysOff = r.daysOff.coerceAtLeast(0)
        val cycle = r.daysOn + daysOff
        if (cycle < 1) return null

        // Полночь даты старта
        val startMidnight = Calendar.getInstance().apply {
            timeInMillis = r.startDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Полночь сегодняшнего дня
        val todayMidnight = Calendar.getInstance().apply {
            timeInMillis = from
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Сколько дней прошло от startDate до сегодня
        var dayOffset = ((todayMidnight - startMidnight) / DAY_MS).toInt()
        if (dayOffset < 0) dayOffset = 0

        // Ищем в пределах одного цикла вперёд — этого всегда достаточно
        for (i in 0..cycle) {
            val currentOffset = dayOffset + i
            val posInCycle = ((currentOffset % cycle) + cycle) % cycle
            if (posInCycle < r.daysOn) {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = startMidnight
                    add(Calendar.DAY_OF_YEAR, currentOffset)
                    set(Calendar.HOUR_OF_DAY, r.hour)
                    set(Calendar.MINUTE, r.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val trigger = cal.timeInMillis
                if (trigger > from) {
                    val endBoundary = r.endDate?.let { endOfDay(it) }
                    if (endBoundary != null && trigger > endBoundary) return null
                    return trigger
                }
            }
        }
        return null
    }

    private fun endOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis

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