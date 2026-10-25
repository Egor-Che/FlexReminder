package io.github.egorche.flexreminder.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import io.github.egorche.flexreminder.MainActivity
import io.github.egorche.flexreminder.R
import io.github.egorche.flexreminder.alarm.AlarmScheduler
import io.github.egorche.flexreminder.alarm.DateUtils
import io.github.egorche.flexreminder.alarm.IterationLogic
import io.github.egorche.flexreminder.alarm.SnoozeReceiver
import io.github.egorche.flexreminder.data.AppDatabase
import io.github.egorche.flexreminder.data.AppTheme
import io.github.egorche.flexreminder.data.Iteration
import io.github.egorche.flexreminder.data.IterationStatus
import io.github.egorche.flexreminder.data.Reminder
import io.github.egorche.flexreminder.data.SettingsRepository
import io.github.egorche.flexreminder.data.StatusSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    updateWidget(context, appWidgetManager, appWidgetId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val action = intent.action ?: return
        if (action == ACTION_COMPLETE || action == ACTION_SKIP) {
            val id = intent.getLongExtra(EXTRA_ID, -1L)
            val dateMillis = intent.getLongExtra(EXTRA_DATE, -1L)
            if (id < 0 || dateMillis < 0) return

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val targetStatus = if (action == ACTION_COMPLETE) IterationStatus.COMPLETED else IterationStatus.SKIPPED
                    processAction(context, id, dateMillis, targetStatus)

                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val componentName = ComponentName(context, ReminderWidgetProvider::class.java)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                    for (appWidgetId in appWidgetIds) {
                        updateWidget(context, appWidgetManager, appWidgetId)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private suspend fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val settings = SettingsRepository.get(context)
        val themePref = settings.widgetTheme.first()
        val showNotes = settings.widgetShowNotes.first()
        val appTheme = settings.appTheme.first()

        val isSystemDark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val resolvedTheme = if (themePref == "SYSTEM") {
            if (isSystemDark) AppTheme.DARK else AppTheme.MONOCHROME
        } else {
            appTheme
        }

        val db = AppDatabase.get(context)
        val reminderDao = db.reminderDao()
        val iterationDao = db.iterationDao()
        
        val activeReminders = reminderDao.getEnabled()
        val allIterations = iterationDao.observeAll().first()
        val todayMidnight = DateUtils.midnight(System.currentTimeMillis())

        val upcoming = activeReminders.mapNotNull { reminder ->
            val iterations = allIterations.filter { it.reminderId == reminder.id }
            val triple = IterationLogic.getThreeIterations(reminder, todayMidnight, iterations)
            val nearest = triple.second
            if (nearest != null) WidgetReminderData(reminder, nearest) else null
        }.sortedBy { it.reminder.hour * 60 + it.reminder.minute }.take(10)

        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        
        val primaryText = getPrimaryTextColor(resolvedTheme)
        val secondaryText = getSecondaryTextColor(resolvedTheme)
        val completedColor = getCompletedColor(resolvedTheme)
        val skippedColor = getSkippedColor(resolvedTheme)

        val bgRes = when (resolvedTheme) {
            AppTheme.DARK -> R.drawable.widget_bg_dark
            AppTheme.MONOCHROME -> R.drawable.widget_bg_light
            AppTheme.PALETTE -> R.drawable.widget_bg_palette
        }
        views.setInt(R.id.widget_root, "setBackgroundResource", bgRes)

        if (upcoming.isEmpty()) {
            views.setViewVisibility(R.id.widget_empty_text, View.VISIBLE)
            views.setViewVisibility(R.id.widget_items_container, View.GONE)
            views.setTextColor(R.id.widget_empty_text, primaryText)
        } else {
            views.setViewVisibility(R.id.widget_empty_text, View.GONE)
            views.setViewVisibility(R.id.widget_items_container, View.VISIBLE)
            views.removeAllViews(R.id.widget_items_container)

            val currentLocale = Locale.forLanguageTag("ru")

            upcoming.forEach { item ->
                val itemView = RemoteViews(context.packageName, R.layout.widget_item)
                
                itemView.setTextViewText(R.id.widget_item_title, item.reminder.title)
                itemView.setTextColor(R.id.widget_item_title, primaryText)

                val triggerTime = DateUtils.atTime(item.iteration.dateMillis, item.reminder.hour, item.reminder.minute)
                val dateFormat = SimpleDateFormat("d MMM, EEE, HH:mm", currentLocale)
                val dateStr = dateFormat.format(Date(triggerTime))
                
                itemView.setTextViewText(R.id.widget_item_date, dateStr)
                itemView.setTextColor(R.id.widget_item_date, secondaryText)

                if (showNotes && item.reminder.notes.isNotBlank()) {
                    itemView.setViewVisibility(R.id.widget_item_notes, View.VISIBLE)
                    itemView.setTextViewText(R.id.widget_item_notes, item.reminder.notes)
                    itemView.setTextColor(R.id.widget_item_notes, secondaryText)
                } else {
                    itemView.setViewVisibility(R.id.widget_item_notes, View.GONE)
                }

                val isCompleted = item.iteration.status == IterationStatus.COMPLETED
                val isSkipped = item.iteration.status == IterationStatus.SKIPPED

                itemView.setTextColor(R.id.widget_item_btn_complete, if (isCompleted) completedColor else secondaryText)
                itemView.setTextColor(R.id.widget_item_btn_skip, if (isSkipped) skippedColor else secondaryText)

                val completeIntent = Intent(context, ReminderWidgetProvider::class.java).apply {
                    action = ACTION_COMPLETE
                    putExtra(EXTRA_ID, item.reminder.id)
                    putExtra(EXTRA_DATE, item.iteration.dateMillis)
                }
                val completePi = PendingIntent.getBroadcast(
                    context, (item.reminder.id * 1000 + 1).toInt(), completeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                itemView.setOnClickPendingIntent(R.id.widget_item_btn_complete, completePi)

                val skipIntent = Intent(context, ReminderWidgetProvider::class.java).apply {
                    action = ACTION_SKIP
                    putExtra(EXTRA_ID, item.reminder.id)
                    putExtra(EXTRA_DATE, item.iteration.dateMillis)
                }
                val skipPi = PendingIntent.getBroadcast(
                    context, (item.reminder.id * 1000 + 2).toInt(), skipIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                itemView.setOnClickPendingIntent(R.id.widget_item_btn_skip, skipPi)

                // ОБЫЧНЫЙ INTENT ВМЕСТО DEEP LINK
                val openAppIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val openAppPi = PendingIntent.getActivity(
                    context, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                itemView.setOnClickPendingIntent(R.id.widget_item_title, openAppPi)
                itemView.setOnClickPendingIntent(R.id.widget_item_date, openAppPi)

                views.addView(R.id.widget_items_container, itemView)
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private suspend fun processAction(
        context: Context,
        id: Long,
        dateMillis: Long,
        targetStatus: IterationStatus
    ) {
        val db = AppDatabase.get(context)
        val iterationDao = db.iterationDao()
        val reminderDao = db.reminderDao()

        val reminder = reminderDao.getById(id) ?: return
        val existing = iterationDao.get(id, dateMillis)

        val currentStatus = existing?.status ?: IterationStatus.PENDING
        val currentSource = existing?.statusSource
        val isSameUserStatus = currentStatus == targetStatus && currentSource == StatusSource.USER

        val newStatus = if (isSameUserStatus) IterationStatus.PENDING else targetStatus
        val newSource = if (isSameUserStatus) null else StatusSource.USER

        val updated = (existing ?: Iteration(reminderId = id, dateMillis = dateMillis)).copy(
            status = newStatus,
            statusSource = newSource,
            statusChangedAt = System.currentTimeMillis(),
            snoozeCount = 0,
            lastSnoozeAt = null,
            snoozeUntil = null
        )

        if (newStatus == IterationStatus.PENDING && updated.id != 0L) {
            iterationDao.delete(updated.reminderId, updated.dateMillis)
        } else if (newStatus != IterationStatus.PENDING) {
            iterationDao.upsertByDate(updated)
        }

        SnoozeReceiver.cancelAllSnoozes(context, id)
        AlarmScheduler.schedule(context, reminder)
    }

    private fun getPrimaryTextColor(theme: AppTheme): Int = when (theme) {
        AppTheme.DARK -> 0xFFE0E0E0.toInt()
        AppTheme.MONOCHROME -> 0xFF1A1A1A.toInt()
        AppTheme.PALETTE -> 0xFF1A1A1A.toInt()
    }

    private fun getSecondaryTextColor(theme: AppTheme): Int = when (theme) {
        AppTheme.DARK -> 0xFFA0A0A0.toInt()
        AppTheme.MONOCHROME -> 0xFF4D4D4D.toInt()
        AppTheme.PALETTE -> 0xFF4D4D4D.toInt()
    }

    private fun getCompletedColor(theme: AppTheme): Int = when (theme) {
        AppTheme.DARK -> 0xFF7CB380.toInt()
        AppTheme.MONOCHROME -> 0xFF7A947A.toInt()
        AppTheme.PALETTE -> 0xFF5E9E62.toInt()
    }

    private fun getSkippedColor(theme: AppTheme): Int = when (theme) {
        AppTheme.DARK -> 0xFFE0C06E.toInt()
        AppTheme.MONOCHROME -> 0xFFAD9A76.toInt()
        AppTheme.PALETTE -> 0xFFD9B461.toInt()
    }

    companion object {
        private const val ACTION_COMPLETE = "io.github.egorche.flexreminder.widget.ACTION_COMPLETE"
        private const val ACTION_SKIP = "io.github.egorche.flexreminder.widget.ACTION_SKIP"
        private const val EXTRA_ID = "extra_id"
        private const val EXTRA_DATE = "extra_date"

        fun updateAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, ReminderWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            val intent = Intent(context, ReminderWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            context.sendBroadcast(intent)
        }
    }
}

data class WidgetReminderData(
    val reminder: Reminder,
    val iteration: Iteration
)