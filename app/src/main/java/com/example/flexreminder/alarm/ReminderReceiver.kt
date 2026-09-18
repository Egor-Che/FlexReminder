package com.example.flexreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.flexreminder.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ID, -1L)
        if (id < 0) return

        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "Напоминание"
        val notes = intent.getStringExtra(AlarmScheduler.EXTRA_NOTES).orEmpty()
        val silent = intent.getBooleanExtra(Notifications.EXTRA_SILENT, false)

        Notifications.show(context, id, title, notes, silent)

        // Перепланируем следующее срабатывание
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.get(context).reminderDao()
                val reminder = dao.getById(id)
                if (reminder != null && reminder.enabled) {
                    AlarmScheduler.schedule(context, reminder)
                }
            } finally {
                pending.finish()
            }
        }
    }
}