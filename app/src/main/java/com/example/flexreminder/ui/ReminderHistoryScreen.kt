package com.example.flexreminder.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flexreminder.data.AppDatabase
import com.example.flexreminder.data.AppTheme
import com.example.flexreminder.data.Iteration
import com.example.flexreminder.data.Reminder
import com.example.flexreminder.data.ScheduleMode
import com.example.flexreminder.ui.theme.AppThemeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderHistoryScreen(
    reminderId: Long,
    onBack: () -> Unit,
    onCreateFromExample: (Long) -> Unit
) {
    val vm: ReminderHistoryViewModel = viewModel()
    LaunchedEffect(reminderId) { vm.load(reminderId) }

    val reminder by vm.reminder.collectAsStateWithLifecycle()
    val iterations by vm.iterations.collectAsStateWithLifecycle()
    val activeDates by vm.activeDates.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val vm2: ReminderViewModel = viewModel()

    var selectedIterationDate by remember { mutableStateOf<Long?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val dfShort = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История напоминания") },
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
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Удалить"
                            )
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
                    .padding(padding)
                    .padding(16.dp),
                style = MaterialTheme.typography.bodyLarge
            )
            return@Scaffold
        }

        // Архив всегда в монохроме
        val monoTheme = AppTheme.MONOCHROME
        val contentColor = AppThemeColors.cardContentColor(monoTheme)
        val secondaryColor = AppThemeColors.cardContentColorSecondary(monoTheme)
        val cardBg = AppThemeColors.MonoCardEven

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Название
            Text(
                text = r.title,
                style = MaterialTheme.typography.headlineSmall,
                color = contentColor
            )

            // Заметка
            if (r.notes.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = r.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryColor
                )
            }

            Spacer(Modifier.height(16.dp))

            // Расписание
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = when (r.mode) {
                            ScheduleMode.INTERVAL -> formatPattern(r.daysOn, r.daysOff)
                            ScheduleMode.CUSTOM_DATES ->
                                "Конкретные даты: ${r.customDates.size} шт."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = contentColor
                    )

                    Spacer(Modifier.height(4.dp))

                    val periodText = buildString {
                        append("Период: с ")
                        append(dfShort.format(Date(r.startDate)))
                        r.endDate?.let {
                            append(" по ")
                            append(dfShort.format(Date(it)))
                        }
                    }
                    Text(
                        text = periodText,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryColor
                    )

                    Spacer(Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = String.format(
                                Locale.getDefault(), "%02d:%02d", r.hour, r.minute
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor
                        )
                        Spacer(Modifier.width(16.dp))
                        Icon(
                            imageVector = if (r.silent)
                                Icons.Default.VolumeOff
                            else
                                Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = secondaryColor,
                            modifier = Modifier.width(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (r.silent) "Без звука" else "Со звуком",
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Календарь с подсветкой активных дней
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(Modifier.padding(8.dp)) {
                    ReminderCalendar(
                        selectedDates = activeDates,
                        readOnly = true,
                        theme = monoTheme,
                        colorIndex = null,
                        onDateClick = { dateMillis ->
                            selectedIterationDate = dateMillis
                        },
                        initialMonthMillis = r.startDate
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Кнопка «Создать по примеру»
            Button(
                onClick = { onCreateFromExample(r.id) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppThemeColors.MonoAccentOn,
                    contentColor = AppThemeColors.onAccentColor()
                )
            ) {
                Text("Создать по примеру")
            }

            Spacer(Modifier.height(8.dp))

            // Кнопка «Удалить» — серая, без выделения
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Удалить",
                    color = secondaryColor
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // Модалка итерации
    val selectedDate = selectedIterationDate
    if (selectedDate != null && reminder != null) {
        val iter: Iteration? = iterations.find { it.dateMillis == selectedDate }
        IterationDetailsDialog(
            dateMillis = selectedDate,
            hour = reminder!!.hour,
            minute = reminder!!.minute,
            iteration = iter,
            onDismiss = { selectedIterationDate = null }
        )
    }

    // Диалог удаления
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить архивное напоминание?") },
            text = {
                Text("Всё, включая историю срабатываний, будет удалено безвозвратно.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    vm2.deleteById(reminderId) {
                        onBack()
                    }
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}