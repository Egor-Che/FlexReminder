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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.flexreminder.alarm.DateUtils
import com.example.flexreminder.data.AppDatabase
import com.example.flexreminder.data.Reminder
import com.example.flexreminder.data.ScheduleMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReminderScreen(
    vm: ReminderViewModel,
    reminderId: Long,
    onDone: () -> Unit
) {
    val ctx = LocalContext.current
    val isNew = reminderId <= 0L

    var loaded by remember { mutableStateOf(isNew) }
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var mode by remember { mutableStateOf(ScheduleMode.INTERVAL) }

    var startDate by remember { mutableStateOf(todayMidnight()) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var daysOnText by remember { mutableStateOf("1") }
    var daysOffText by remember { mutableStateOf("0") }

    var customDates by remember { mutableStateOf<Set<Long>>(emptySet()) }

    var lastGeneratedKey by remember { mutableStateOf<String?>(null) }

    var hour by remember { mutableStateOf(9) }
    var minute by remember { mutableStateOf(0) }
    var enabled by remember { mutableStateOf(true) }
    var silent by remember { mutableStateOf(false) }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(reminderId) {
        if (!isNew && !loaded) {
            val r = withContext(Dispatchers.IO) {
                AppDatabase.get(ctx).reminderDao().getById(reminderId)
            }
            if (r != null) {
                title = r.title
                notes = r.notes
                mode = r.mode
                startDate = r.startDate
                endDate = r.endDate
                daysOnText = r.daysOn.toString()
                daysOffText = r.daysOff.toString()
                customDates = r.customDates
                hour = r.hour
                minute = r.minute
                enabled = r.enabled
                silent = r.silent
                if (r.mode == ScheduleMode.INTERVAL) {
                    lastGeneratedKey = intervalKey(
                        r.daysOn, r.daysOff, r.startDate, r.endDate
                    )
                }
            }
            loaded = true
        }
    }

    val df = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val daysOn = daysOnText.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val daysOff = daysOffText.toIntOrNull()?.coerceAtLeast(0) ?: 0

    val canSave = title.isNotBlank() && loaded && when (mode) {
        ScheduleMode.INTERVAL -> endDate == null || endDate!! >= startDate
        ScheduleMode.CUSTOM_DATES -> customDates.isNotEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Новое напоминание" else "Редактирование") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = {
                            vm.deleteById(reminderId) { onDone() }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Заметка (необязательно)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Text("Тип расписания", style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(8.dp))

            TabRow(selectedTabIndex = mode.ordinal) {
                Tab(
                    selected = mode == ScheduleMode.INTERVAL,
                    onClick = {
                        if (mode != ScheduleMode.INTERVAL) {
                            if (customDates.isNotEmpty()) {
                                val detected = detectPattern(customDates)
                                if (detected != null) {
                                    daysOnText = detected.first.toString()
                                    daysOffText = detected.second.toString()
                                    startDate = customDates.minOrNull() ?: startDate
                                    endDate = customDates.maxOrNull()
                                    lastGeneratedKey = intervalKey(
                                        detected.first,
                                        detected.second,
                                        startDate,
                                        endDate
                                    )
                                } else {
                                    daysOnText = ""
                                    daysOffText = ""
                                    lastGeneratedKey = intervalKey(
                                        1, 0, startDate, endDate
                                    )
                                }
                            }
                            mode = ScheduleMode.INTERVAL
                        }
                    },
                    text = { Text("Интервалы") }
                )
                Tab(
                    selected = mode == ScheduleMode.CUSTOM_DATES,
                    onClick = {
                        if (mode != ScheduleMode.CUSTOM_DATES) {
                            val key = intervalKey(daysOn, daysOff, startDate, endDate)
                            if (customDates.isEmpty() || key != lastGeneratedKey) {
                                customDates = generateDatesFromInterval(
                                    startDate, endDate, daysOn, daysOff
                                )
                                lastGeneratedKey = key
                            }
                            mode = ScheduleMode.CUSTOM_DATES
                        }
                    },
                    text = { Text("Конкретные даты") }
                )
            }

            Spacer(Modifier.height(16.dp))

            when (mode) {
                ScheduleMode.INTERVAL -> {
                    IntervalForm(
                        startDate = startDate,
                        endDate = endDate,
                        daysOnText = daysOnText,
                        daysOffText = daysOffText,
                        df = df,
                        onShowStartPicker = { showStartPicker = true },
                        onShowEndPicker = { showEndPicker = true },
                        onClearEnd = { endDate = null },
                        onDaysOnChange = { s -> daysOnText = s.filter { it.isDigit() }.take(3) },
                        onDaysOffChange = { s -> daysOffText = s.filter { it.isDigit() }.take(3) },
                        daysOn = daysOn,
                        daysOff = daysOff
                    )
                }
                ScheduleMode.CUSTOM_DATES -> {
                    CustomDatesForm(
                        selectedDates = customDates,
                        onToggle = { d ->
                            customDates = if (customDates.contains(d)) {
                                customDates - d
                            } else {
                                customDates + d
                            }
                            lastGeneratedKey = null
                        },
                        onClearAll = {
                            customDates = emptySet()
                            lastGeneratedKey = null
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = { showTimePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(String.format(Locale.getDefault(), "Время: %02d:%02d", hour, minute))
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = enabled, onCheckedChange = { enabled = it })
                Spacer(Modifier.width(8.dp))
                Text("Включено")
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = if (enabled) !silent else false,
                    onCheckedChange = { soundOn -> silent = !soundOn },
                    enabled = enabled
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when {
                        !enabled -> "Звук"
                        silent -> "🔇 Без звука"
                        else -> "🔔 Со звуком"
                    }
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val r = Reminder(
                        id = if (isNew) 0L else reminderId,
                        title = title.trim(),
                        notes = notes.trim(),
                        mode = mode,
                        startDate = when (mode) {
                            ScheduleMode.INTERVAL -> startDate
                            ScheduleMode.CUSTOM_DATES ->
                                customDates.minOrNull() ?: todayMidnight()
                        },
                        endDate = when (mode) {
                            ScheduleMode.INTERVAL -> endDate
                            ScheduleMode.CUSTOM_DATES -> customDates.maxOrNull()
                        },
                        daysOn = if (mode == ScheduleMode.INTERVAL) daysOn else 1,
                        daysOff = if (mode == ScheduleMode.INTERVAL) daysOff else 0,
                        customDates = if (mode == ScheduleMode.CUSTOM_DATES) customDates else emptySet(),
                        hour = hour,
                        minute = minute,
                        enabled = enabled,
                        silent = if (enabled) silent else false
                    )
                    vm.save(r) { onDone() }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить")
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    DatePickerDialogWrapper(
        show = showStartPicker,
        initialMillis = startDate,
        onDismiss = { showStartPicker = false },
        onPicked = { startDate = it }
    )

    DatePickerDialogWrapper(
        show = showEndPicker,
        initialMillis = endDate ?: startDate,
        onDismiss = { showEndPicker = false },
        onPicked = { endDate = it }
    )

    TimePickerDialogWrapper(
        show = showTimePicker,
        hour = hour,
        minute = minute,
        onDismiss = { showTimePicker = false },
        onPicked = { h, m ->
            hour = h
            minute = m
        }
    )
}

@Composable
private fun IntervalForm(
    startDate: Long,
    endDate: Long?,
    daysOnText: String,
    daysOffText: String,
    df: SimpleDateFormat,
    onShowStartPicker: () -> Unit,
    onShowEndPicker: () -> Unit,
    onClearEnd: () -> Unit,
    onDaysOnChange: (String) -> Unit,
    onDaysOffChange: (String) -> Unit,
    daysOn: Int,
    daysOff: Int
) {
    Column {
        OutlinedButton(
            onClick = onShowStartPicker,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Начало: ${df.format(Date(startDate))}")
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onShowEndPicker,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (endDate == null) "Окончание: бессрочно"
                else "Окончание: ${df.format(Date(endDate))}"
            )
        }

        if (endDate != null) {
            TextButton(onClick = onClearEnd) {
                Text("Убрать дату окончания (бессрочно)")
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = daysOnText,
                onValueChange = onDaysOnChange,
                label = { Text("Дней подряд") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = daysOffText,
                onValueChange = onDaysOffChange,
                label = { Text("Дней перерыв") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Итого: ${formatPattern(daysOn, daysOff)} (цикл ${daysOn + daysOff} дн.)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun CustomDatesForm(
    selectedDates: Set<Long>,
    onToggle: (Long) -> Unit,
    onClearAll: () -> Unit
) {
    Column {
        Text(
            text = "Выбрано: ${selectedDates.size} шт.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Нажмите на дату, чтобы добавить или убрать её. Можно переключаться между месяцами — уже выбранные даты сохраняются.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(8.dp)) {
                ReminderCalendar(
                    selectedDates = selectedDates,
                    readOnly = false,
                    onDateToggle = onToggle,
                    initialMonthMillis = selectedDates.minOrNull()
                        ?: System.currentTimeMillis()
                )
            }
        }

        if (selectedDates.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onClearAll) {
                Text("Очистить все даты")
            }
        }
    }
}

private fun todayMidnight(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun intervalKey(daysOn: Int, daysOff: Int, start: Long, end: Long?): String =
    "$daysOn|$daysOff|$start|${end ?: 0L}"

private fun generateDatesFromInterval(
    startDate: Long,
    endDate: Long?,
    daysOn: Int,
    daysOff: Int
): Set<Long> {
    if (daysOn < 1) return emptySet()
    val off = daysOff.coerceAtLeast(0)
    val cycle = daysOn + off
    if (cycle < 1) return emptySet()

    val startMidnight = DateUtils.midnight(startDate)
    val endLimit = endDate?.let { DateUtils.midnight(it) }
        ?: DateUtils.addDays(startMidnight, 365)

    val result = mutableSetOf<Long>()
    var cursor = startMidnight
    var daysCount = 0
    val dayCap = 1000

    while (cursor <= endLimit && daysCount < dayCap) {
        val dayOffset = DateUtils.daysBetween(startMidnight, cursor)
        val posInCycle = ((dayOffset % cycle) + cycle) % cycle
        if (posInCycle < daysOn) result.add(cursor)
        cursor = DateUtils.addDays(cursor, 1)
        daysCount++
    }
    return result
}

private fun detectPattern(dates: Set<Long>): Pair<Int, Int>? {
    if (dates.size < 2) return null
    val sorted = dates.sorted()
    val diffs = sorted.zipWithNext { a, b ->
        ((b - a) / DateUtils.DAY_MS).toInt()
    }
    if (diffs.any { it < 1 }) return null

    if (diffs.all { it == 1 }) return 1 to 0

    for (period in 1..diffs.size) {
        var offDiff = -1
        var ok = true
        for (i in diffs.indices) {
            val posInCycle = i % period
            val isOffPos = posInCycle == period - 1
            if (isOffPos) {
                if (diffs[i] < 2) { ok = false; break }
                if (offDiff == -1) offDiff = diffs[i]
                else if (diffs[i] != offDiff) { ok = false; break }
            } else {
                if (diffs[i] != 1) { ok = false; break }
            }
        }
        if (ok && offDiff > 0) {
            return period to (offDiff - 1)
        }
    }
    return null
}