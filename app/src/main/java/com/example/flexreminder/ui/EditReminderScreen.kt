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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.example.flexreminder.data.AppDatabase
import com.example.flexreminder.data.Reminder
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
    var startDate by remember { mutableStateOf(todayMidnight()) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var daysOnText by remember { mutableStateOf("1") }
    var daysOffText by remember { mutableStateOf("0") }
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
                startDate = r.startDate
                endDate = r.endDate
                daysOnText = r.daysOn.toString()
                daysOffText = r.daysOff.toString()
                hour = r.hour
                minute = r.minute
                enabled = r.enabled
                silent = r.silent
            }
            loaded = true
        }
    }

    val df = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val daysOn = daysOnText.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val daysOff = daysOffText.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val canSave = title.isNotBlank() && loaded &&
            (endDate == null || endDate!! >= startDate)

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

            OutlinedButton(
                onClick = { showStartPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Начало: ${df.format(Date(startDate))}")
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = { showEndPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (endDate == null) "Окончание: бессрочно"
                    else "Окончание: ${df.format(Date(endDate!!))}"
                )
            }

            if (endDate != null) {
                TextButton(onClick = { endDate = null }) {
                    Text("Убрать дату окончания (бессрочно)")
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("Расписание", style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = daysOnText,
                    onValueChange = { s ->
                        daysOnText = s.filter { it.isDigit() }.take(3)
                    },
                    label = { Text("Дней подряд") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = daysOffText,
                    onValueChange = { s ->
                        daysOffText = s.filter { it.isDigit() }.take(3)
                    },
                    label = { Text("Дней перерыв") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Пример: «3 дня подряд + 5 перерыв» → напоминания три дня, потом пять дней тишины, потом снова три дня, и так по кругу.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Итого: ${formatPattern(daysOn, daysOff)} (цикл ${daysOn + daysOff} дн.)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

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
                Switch(checked = silent, onCheckedChange = { silent = it })
                Spacer(Modifier.width(8.dp))
                Text("Беззвучное уведомление")
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val r = Reminder(
                        id = if (isNew) 0L else reminderId,
                        title = title.trim(),
                        notes = notes.trim(),
                        startDate = startDate,
                        endDate = endDate,
                        daysOn = daysOn,
                        daysOff = daysOff,
                        hour = hour,
                        minute = minute,
                        enabled = enabled,
                        silent = silent
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

private fun todayMidnight(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis