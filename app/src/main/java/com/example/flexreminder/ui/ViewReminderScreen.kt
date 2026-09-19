package com.example.flexreminder.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.flexreminder.alarm.DateUtils
import com.example.flexreminder.alarm.ReminderLogic
import com.example.flexreminder.data.Reminder
import com.example.flexreminder.data.ScheduleMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewReminderScreen(
    vm: ReminderViewModel,
    reminderId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    val reminderFlow = remember(reminderId) { vm.observeById(reminderId) }
    val reminder by reminderFlow.collectAsStateWithLifecycle(initialValue = null)

    val dfShort = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val dfFull = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Просмотр") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    if (reminder != null) {
                        IconButton(onClick = {
                            vm.deleteById(reminderId) { onBack() }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить")
                        }
                    }
                }
            )
        }
    ) { padding ->
        val r = reminder
        if (r == null) {
            Text(
                "Загрузка…",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                style = MaterialTheme.typography.bodyLarge
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(r.title, style = MaterialTheme.typography.headlineSmall)

            if (r.notes.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(r.notes, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Время: " + String.format(Locale.getDefault(), "%02d:%02d", r.hour, r.minute),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    if (r.silent) "🔇 без звука" else "🔔 со звуком",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = when (r.mode) {
                    ScheduleMode.INTERVAL -> formatPattern(r.daysOn, r.daysOff)
                    ScheduleMode.CUSTOM_DATES -> "Конкретные даты: ${r.customDates.size} шт."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            if (r.mode == ScheduleMode.INTERVAL) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Период: с ${dfShort.format(Date(r.startDate))}" +
                            (r.endDate?.let { " по ${dfShort.format(Date(it))}" } ?: " (бессрочно)"),
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                val sorted = r.customDates.sorted()
                if (sorted.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Первая: ${dfShort.format(Date(sorted.first()))}, " +
                                "последняя: ${dfShort.format(Date(sorted.last()))}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            val next = remember(r) { ReminderLogic.nextTriggerTime(r) }
            if (r.enabled && next != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Следующее: ${dfFull.format(Date(next))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (r.enabled) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Период завершён — отредактируйте расписание",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Switch(checked = r.enabled, onCheckedChange = { vm.toggle(r) })
                Spacer(Modifier.width(8.dp))
                Text("Включено")

                Spacer(Modifier.width(24.dp))

                Switch(
                    checked = if (r.enabled) !r.silent else false,
                    onCheckedChange = { soundOn -> vm.setSilent(r, !soundOn) },
                    enabled = r.enabled
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when {
                        !r.enabled -> "Звук"
                        r.silent -> "🔇 Без звука"
                        else -> "🔔 Со звуком"
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = if (r.mode == ScheduleMode.INTERVAL) "Расписание на месяц" else "Выбранные даты",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp)) {
                    when (r.mode) {
                        ScheduleMode.CUSTOM_DATES -> {
                            ReminderCalendar(
                                selectedDates = r.customDates,
                                readOnly = true,
                                initialMonthMillis = r.customDates.minOrNull()
                                    ?: System.currentTimeMillis()
                            )
                        }
                        ScheduleMode.INTERVAL -> {
                            IntervalPreviewCalendar(r)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Редактировать")
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun IntervalPreviewCalendar(r: Reminder) {
    val activeDates = remember(r) {
        val set = mutableSetOf<Long>()

        val startWindow = DateUtils.midnight(
            Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, -1)
            }.timeInMillis
        )
        val endWindow = DateUtils.midnight(
            Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, 2)
                add(Calendar.DAY_OF_YEAR, -1)
            }.timeInMillis
        )

        var cursor = startWindow
        var guard = 0
        while (cursor <= endWindow && guard < 400) {
            if (ReminderLogic.isActiveOn(r, cursor)) {
                set.add(cursor)
            }
            cursor = DateUtils.addDays(cursor, 1)
            guard++
        }
        set
    }

    ReminderCalendar(
        selectedDates = activeDates,
        readOnly = true,
        initialMonthMillis = System.currentTimeMillis()
    )
}