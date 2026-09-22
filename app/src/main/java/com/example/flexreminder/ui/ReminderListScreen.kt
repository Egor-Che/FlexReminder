package com.example.flexreminder.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.flexreminder.alarm.DateUtils
import com.example.flexreminder.alarm.IterationLogic
import com.example.flexreminder.data.Iteration
import com.example.flexreminder.data.IterationStatus
import com.example.flexreminder.data.Reminder
import com.example.flexreminder.data.ScheduleMode
import com.example.flexreminder.data.StatusSource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    vm: ReminderViewModel,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onMark: (Long) -> Unit,
    onSettings: () -> Unit
) {
    val list by vm.reminders.collectAsStateWithLifecycle()
    val allIterations by vm.observeAllIterations()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val iterationsByReminder = remember(allIterations) {
        allIterations.groupBy { it.reminderId }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои напоминания") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Настройки"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        }
    ) { padding ->
        if (list.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Нет напоминаний.\nНажмите + чтобы создать.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(list, key = { it.id }) { r ->
                    ReminderCard(
                        reminder = r,
                        iterations = iterationsByReminder[r.id].orEmpty(),
                        onClick = { onOpen(r.id) },
                        onToggle = { vm.toggle(r) },
                        onToggleSilent = {
                            if (r.enabled) vm.setSilent(r, !r.silent)
                        },
                        onMark = { onMark(r.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: Reminder,
    iterations: List<Iteration>,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onToggleSilent: () -> Unit,
    onMark: () -> Unit
) {
    val todayMidnight = remember { DateUtils.midnight(System.currentTimeMillis()) }
    val triple = remember(reminder, iterations) {
        IterationLogic.getThreeIterations(reminder, todayMidnight, iterations)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(reminder.title, style = MaterialTheme.typography.titleMedium)

                    if (reminder.notes.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(reminder.notes, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = buildString {
                            when (reminder.mode) {
                                ScheduleMode.INTERVAL -> {
                                    append(formatPattern(reminder.daysOn, reminder.daysOff))
                                }
                                ScheduleMode.CUSTOM_DATES -> {
                                    append("Конкретные даты: ")
                                    append(reminder.customDates.size)
                                    append(" шт.")
                                }
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Switch(
                        checked = reminder.enabled,
                        onCheckedChange = { onToggle() }
                    )
                    IconButton(
                        onClick = onToggleSilent,
                        enabled = reminder.enabled
                    ) {
                        val silentEffective = !reminder.enabled || reminder.silent
                        Icon(
                            imageVector = if (silentEffective)
                                Icons.Default.VolumeOff
                            else
                                Icons.Default.VolumeUp,
                            contentDescription = when {
                                !reminder.enabled -> "Событие выключено"
                                reminder.silent -> "Включить звук"
                                else -> "Выключить звук"
                            },
                            tint = when {
                                !reminder.enabled ->
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                reminder.silent ->
                                    MaterialTheme.colorScheme.primary
                                else ->
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (reminder.enabled) {
                IterationSummaryRow(
                    label = "⬆️ Прошлая",
                    iteration = triple.first,
                    hour = reminder.hour,
                    minute = reminder.minute
                )
                Spacer(Modifier.height(2.dp))
                IterationSummaryRow(
                    label = "▶️ Ближайшая",
                    iteration = triple.second,
                    hour = reminder.hour,
                    minute = reminder.minute
                )
                Spacer(Modifier.height(2.dp))
                IterationSummaryRow(
                    label = "⏭️ Следующая",
                    iteration = triple.third,
                    hour = reminder.hour,
                    minute = reminder.minute
                )
            } else {
                Text(
                    "Выключено",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onMark,
                modifier = Modifier.fillMaxWidth(),
                enabled = reminder.enabled
            ) {
                Text("Отметить")
            }
        }
    }
}

@Composable
private fun IterationSummaryRow(
    label: String,
    iteration: Iteration?,
    hour: Int,
    minute: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )

        if (iteration == null) {
            Text(
                "—",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "${shortDate(iteration.dateMillis)} " +
                        String.format(Locale.getDefault(), "%02d:%02d", hour, minute),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )

            val statusText = iterationStatusText(iteration)
            if (statusText != null) {
                Text(
                    text = statusText.text,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = statusText.color
                )
            }
        }
    }
}

private val shortDateFormat = SimpleDateFormat("dd.MM, EEE", Locale("ru"))

private fun shortDate(millis: Long): String = shortDateFormat.format(Date(millis))

private data class StatusText(val text: String, val color: Color)

private fun iterationStatusText(iteration: Iteration): StatusText? = when {
    iteration.status == IterationStatus.COMPLETED ->
        StatusText("✅ Выполнено", CompletedColor)
    iteration.status == IterationStatus.SKIPPED &&
            iteration.statusSource == StatusSource.USER ->
        StatusText("⏭️ Пропущено", SkippedUserColor)
    iteration.status == IterationStatus.SKIPPED &&
            iteration.statusSource == StatusSource.SYSTEM ->
        StatusText("❌ Пропущено", SkippedSystemColor)
    else -> null
}

fun formatPattern(daysOn: Int, daysOff: Int): String = when {
    daysOff <= 0 -> "Каждый день"
    daysOn == 1 && daysOff == 1 -> "Через день"
    daysOn == 1 && daysOff == 6 -> "Раз в неделю"
    daysOn == 1 && daysOff == 29 -> "Раз в 30 дней"
    daysOn == 1 -> "Раз в ${daysOff + 1} дн."
    daysOff == 1 -> "$daysOn дн. подряд, потом 1 дн. пауза"
    else -> "$daysOn дн. подряд, потом $daysOff дн. пауза"
}