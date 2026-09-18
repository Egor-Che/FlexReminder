package com.example.flexreminder.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.flexreminder.alarm.AlarmScheduler
import com.example.flexreminder.data.Reminder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    vm: ReminderViewModel,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val list by vm.reminders.collectAsStateWithLifecycle()

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* результат не важен */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Напоминания") }) },
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
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(list, key = { it.id }) { r ->
                    ReminderCard(
                        reminder = r,
                        onClick = { onEdit(r.id) },
                        onToggle = { vm.toggle(r) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: Reminder,
    onClick: () -> Unit,
    onToggle: () -> Unit
) {
    val dfShort = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val dfFull = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    val next = remember(reminder) { AlarmScheduler.nextTriggerTime(reminder) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(reminder.title, style = MaterialTheme.typography.titleMedium)

                if (reminder.notes.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(reminder.notes, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(4.dp))

                val period = buildString {
                    append("Каждые ")
                    append(reminder.intervalDays)
                    append(" дн. · ")
                    append(dfShort.format(Date(reminder.startDate)))
                    reminder.endDate?.let { append(" — ${dfShort.format(Date(it))}") }
                    if (reminder.silent) append(" · без звука")
                }
                Text(period, style = MaterialTheme.typography.bodySmall)

                when {
                    reminder.enabled && next != null -> {
                        Text(
                            "Следующее: ${dfFull.format(Date(next))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    reminder.enabled -> {
                        Text(
                            "Период завершён — откройте, чтобы продлить",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {
                        Text(
                            "Выключено",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            Switch(checked = reminder.enabled, onCheckedChange = { onToggle() })
        }
    }
}