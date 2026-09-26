package io.github.egorche.flexreminder.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import io.github.egorche.flexreminder.data.AppDatabase
import io.github.egorche.flexreminder.data.Reminder

object AlarmScheduler {

    const val EXTRA_ID = "extra_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_NOTES = "extra_notes"

    /** Следующий момент срабатывания без учёта отметок (для UI-превью). */
    fun nextTriggerTime(r: Reminder, from: Long = System.currentTimeMillis()): Long? =
        ReminderLogic.nextTriggerTime(r, from)

    /**
     * Планирует следующий будильник с учётом отметок итераций.
     * suspend, потому что читает итерации из БД.
     */
    suspend fun schedule(context: Context, r: Reminder) {
        if (!r.enabled) {
            cancel(context, r.id)
            return
        }
        val iterations = AppDatabase.get(context).iterationDao().getByReminder(r.id)
        val next = ReminderLogic.nextPendingTrigger(r, iterations)
            ?: run {
                cancel(context, r.id)
                return
            }
        setAlarm(context, r, next)
    }

    /**
     * Поставить будильник на конкретную дату (полночь) — используется экраном отметок,
     * когда пользователь снял статус с ближайшей итерации.
     */
    fun scheduleAt(context: Context, r: Reminder, dateMidnight: Long) {
        if (!r.enabled) {
            cancel(context, r.id)
            return
        }
        val trigger = DateUtils.atTime(dateMidnight, r.hour, r.minute)
        if (trigger <= System.currentTimeMillis()) return
        setAlarm(context, r, trigger)
    }

    private fun setAlarm(context: Context, r: Reminder, trigger: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, r.id, r.title, r.notes, r.silent)

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
            action = "io.github.egorche.flexreminder.ACTION_REMIND"
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