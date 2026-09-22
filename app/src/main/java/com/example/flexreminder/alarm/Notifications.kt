package com.example.flexreminder.alarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.flexreminder.MainActivity
import com.example.flexreminder.data.Reminder

object Notifications {

    private const val CHANNEL_LOUD = "reminders_loud"
    private const val CHANNEL_SILENT = "reminders_silent"

    const val EXTRA_SILENT = "extra_silent"

    /** Максимальное количество snooze на одну итерацию. */
    const val MAX_SNOOZE_COUNT = 5

    data class ActionFlags(
        val showSnoozeShort: Boolean,
        val showSnoozeLong: Boolean,
        val showSkip: Boolean
    )

    /**
     * Вычисляет, какие кнопки показывать в уведомлении.
     *
     *  - snooze Short — если (сейчас + short) < время следующей итерации
     *    И snoozeCount < MAX_SNOOZE_COUNT
     *  - snooze Long  — если (сейчас + long) < время следующей итерации
     *    И snoozeCount < MAX_SNOOZE_COUNT
     *  - skip — если snoozeCount >= MAX_SNOOZE_COUNT
     *
     * Значения short и long берутся из настроек пользователя.
     */
    fun computeFlags(
        reminder: Reminder,
        dateMillis: Long,
        snoozeCount: Int,
        snoozeShortMinutes: Int,
        snoozeLongMinutes: Int
    ): ActionFlags {
        val now = System.currentTimeMillis()
        val nextIterationTime = findNextIterationTime(reminder, dateMillis)

        val canSnooze = snoozeCount < MAX_SNOOZE_COUNT
        val showSnoozeShort = canSnooze &&
                (nextIterationTime == null ||
                        now + snoozeShortMinutes * 60_000L < nextIterationTime)
        val showSnoozeLong = canSnooze &&
                (nextIterationTime == null ||
                        now + snoozeLongMinutes * 60_000L < nextIterationTime)
        val showSkip = !canSnooze

        return ActionFlags(showSnoozeShort, showSnoozeLong, showSkip)
    }

    private fun findNextIterationTime(reminder: Reminder, currentDateMillis: Long): Long? {
        if (currentDateMillis <= 0) return null
        return try {
            val currentTrigger = DateUtils.atTime(
                currentDateMillis, reminder.hour, reminder.minute
            )
            ReminderLogic.nextTriggerTime(reminder, currentTrigger + 1)
        } catch (_: Exception) {
            null
        }
    }

    fun formatSnoozeLabel(minutes: Int): String = when {
        minutes < 60 -> "Отложить $minutes мин"
        minutes == 60 -> "Отложить 1 час"
        minutes % 60 == 0 -> "Отложить ${minutes / 60} ч"
        else -> "Отложить ${minutes / 60} ч ${minutes % 60} мин"
    }

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java)

            if (mgr.getNotificationChannel(CHANNEL_LOUD) == null) {
                val c = NotificationChannel(
                    CHANNEL_LOUD,
                    "Напоминания (со звуком)",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Напоминания со звуком и вибрацией" }
                mgr.createNotificationChannel(c)
            }

            if (mgr.getNotificationChannel(CHANNEL_SILENT) == null) {
                val c = NotificationChannel(
                    CHANNEL_SILENT,
                    "Напоминания (беззвучные)",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Тихие напоминания — только в шторке"
                    setSound(null, null)
                    enableVibration(false)
                    enableLights(false)
                }
                mgr.createNotificationChannel(c)
            }
        }
    }

    fun show(
        context: Context,
        id: Long,
        title: String,
        text: String,
        silent: Boolean = false,
        dateMillis: Long = -1L,
        showSnoozeShort: Boolean = true,
        showSnoozeLong: Boolean = true,
        showSkip: Boolean = false,
        snoozeShortMinutes: Int,
        snoozeLongMinutes: Int
    ) {
        ensureChannels(context)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val channelId = if (silent) CHANNEL_SILENT else CHANNEL_LOUD

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPi = PendingIntent.getActivity(
            context,
            id.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markCompleted = buildMarkCompletedPI(context, id, dateMillis, title, text, silent)
        val markSkipped = buildMarkSkippedPI(context, id, dateMillis, title, text, silent)
        val snoozeShortPi = buildSnoozePI(
            context, id, dateMillis, title, text, silent, snoozeShortMinutes
        )
        val snoozeLongPi = buildSnoozePI(
            context, id, dateMillis, title, text, silent, snoozeLongMinutes
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(
                if (silent) NotificationCompat.PRIORITY_LOW
                else NotificationCompat.PRIORITY_HIGH
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .addAction(
                android.R.drawable.checkbox_on_background,
                "Выполнено",
                markCompleted
            )

        if (showSnoozeShort) {
            builder.addAction(
                android.R.drawable.ic_menu_recent_history,
                formatSnoozeLabel(snoozeShortMinutes),
                snoozeShortPi
            )
        }
        if (showSnoozeLong) {
            builder.addAction(
                android.R.drawable.ic_menu_recent_history,
                formatSnoozeLabel(snoozeLongMinutes),
                snoozeLongPi
            )
        }
        if (showSkip) {
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Пропустить",
                markSkipped
            )
        }

        if (silent) builder.setSilent(true)

        try {
            NotificationManagerCompat.from(context).notify(id.toInt(), builder.build())
        } catch (_: SecurityException) {
            // нет разрешения — молча выходим
        }
    }

    private fun buildMarkCompletedPI(
        context: Context,
        id: Long,
        dateMillis: Long,
        title: String,
        notes: String,
        silent: Boolean
    ): PendingIntent {
        val intent = Intent(context, MarkCompletedReceiver::class.java).apply {
            action = MarkCompletedReceiver.ACTION_MARK_COMPLETED
            data = Uri.parse("flexreminder://mark-completed/$id/$dateMillis")
            putExtra(AlarmScheduler.EXTRA_ID, id)
            putExtra(SnoozeReceiver.EXTRA_DATE_MILLIS, dateMillis)
            putExtra(AlarmScheduler.EXTRA_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_NOTES, notes)
            putExtra(EXTRA_SILENT, silent)
        }
        return PendingIntent.getBroadcast(
            context,
            (id * 1000 + 1).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildMarkSkippedPI(
        context: Context,
        id: Long,
        dateMillis: Long,
        title: String,
        notes: String,
        silent: Boolean
    ): PendingIntent {
        val intent = Intent(context, MarkSkippedReceiver::class.java).apply {
            action = MarkSkippedReceiver.ACTION_MARK_SKIPPED
            data = Uri.parse("flexreminder://mark-skipped/$id/$dateMillis")
            putExtra(AlarmScheduler.EXTRA_ID, id)
            putExtra(SnoozeReceiver.EXTRA_DATE_MILLIS, dateMillis)
            putExtra(AlarmScheduler.EXTRA_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_NOTES, notes)
            putExtra(EXTRA_SILENT, silent)
        }
        return PendingIntent.getBroadcast(
            context,
            (id * 1000 + 2).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildSnoozePI(
        context: Context,
        id: Long,
        dateMillis: Long,
        title: String,
        notes: String,
        silent: Boolean,
        minutes: Int
    ): PendingIntent {
        val intent = Intent(context, SnoozeReceiver::class.java).apply {
            action = SnoozeReceiver.ACTION_SNOOZE_BUTTON
            data = Uri.parse("flexreminder://snooze/$id/$dateMillis/$minutes")
            putExtra(AlarmScheduler.EXTRA_ID, id)
            putExtra(SnoozeReceiver.EXTRA_DATE_MILLIS, dateMillis)
            putExtra(AlarmScheduler.EXTRA_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_NOTES, notes)
            putExtra(EXTRA_SILENT, silent)
            putExtra(SnoozeReceiver.EXTRA_MINUTES, minutes)
        }
        return PendingIntent.getBroadcast(
            context,
            (id * 1000 + 3 + minutes).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}