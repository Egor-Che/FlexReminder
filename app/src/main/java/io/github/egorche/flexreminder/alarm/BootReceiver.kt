package io.github.egorche.flexreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.egorche.flexreminder.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.get(context).reminderDao()
                dao.getEnabled().forEach { AlarmScheduler.schedule(context, it) }

                // Восстанавливаем активные отложки (snooze)
                SnoozeRestorer.restoreAll(context)
            } finally {
                pending.finish()
            }
        }
    }
}