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

object Notifications {

    private const val CHANNEL_LOUD = "reminders_loud"
    private const val CHANNEL_SILENT = "reminders_silent"

    const val EXTRA_SILENT = "extra_silent"

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
        silent: Boolean = false
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

        val markCompleted = buildMarkCompletedPI(context, id, title, text, silent)
        val snooze5 = buildSnoozePI(context, id, title, text, silent, 5)
        val snooze10 = buildSnoozePI(context, id, title, text, silent, 10)

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
            .addAction(
                android.R.drawable.ic_menu_recent_history,
                "Отложить 5 мин",
                snooze5
            )
            .addAction(
                android.R.drawable.ic_menu_recent_history,
                "Отложить 10 мин",
                snooze10
            )

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
        title: String,
        notes: String,
        silent: Boolean
    ): PendingIntent {
        val intent = Intent(context, MarkCompletedReceiver::class.java).apply {
            action = MarkCompletedReceiver.ACTION_MARK_COMPLETED
            data = Uri.parse("flexreminder://mark-completed/$id")
            putExtra(AlarmScheduler.EXTRA_ID, id)
            putExtra(AlarmScheduler.EXTRA_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_NOTES, notes)
            putExtra(EXTRA_SILENT, silent)
        }
        return PendingIntent.getBroadcast(
            context,
            (id * 100 + 90).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildSnoozePI(
        context: Context,
        id: Long,
        title: String,
        notes: String,
        silent: Boolean,
        minutes: Int
    ): PendingIntent {
        val intent = Intent(context, SnoozeReceiver::class.java).apply {
            action = SnoozeReceiver.ACTION_SNOOZE_BUTTON
            data = Uri.parse("flexreminder://snooze/$id/$minutes")
            putExtra(AlarmScheduler.EXTRA_ID, id)
            putExtra(AlarmScheduler.EXTRA_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_NOTES, notes)
            putExtra(EXTRA_SILENT, silent)
            putExtra(SnoozeReceiver.EXTRA_MINUTES, minutes)
        }
        return PendingIntent.getBroadcast(
            context,
            (id * 100 + minutes).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}