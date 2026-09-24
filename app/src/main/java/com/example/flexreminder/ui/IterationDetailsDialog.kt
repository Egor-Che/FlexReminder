package com.example.flexreminder.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.flexreminder.data.Iteration
import com.example.flexreminder.data.IterationStatus
import com.example.flexreminder.data.StatusSource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IterationDetailsDialog(
    dateMillis: Long,
    hour: Int,
    minute: Int,
    iteration: Iteration?,
    onDismiss: () -> Unit
) {
    val dateFormatFull = SimpleDateFormat("dd.MM.yyyy, EEEE", Locale("ru"))
    val dateTimeFormat = SimpleDateFormat("HH:mm dd.MM.yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    val hasRecord = iteration != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(dateFormatFull.format(Date(dateMillis)))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                val scheduledTime = String.format(
                    Locale.getDefault(), "%02d:%02d", hour, minute
                )
                DetailRow("Назначено:", scheduledTime)

                if (hasRecord) {
                    iteration!!.firedAt?.let { firedAt ->
                        DetailRow("Сработало:", dateTimeFormat.format(Date(firedAt)))
                    }

                    val snoozes = parseSnoozeHistory(iteration.snoozeHistory)
                    if (snoozes.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Переносы:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        snoozes.forEach { snooze ->
                            Text(
                                text = "  • ${timeFormat.format(Date(snooze.timestamp))} " +
                                        "→ на ${formatMinutesLabel(snooze.minutes)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    if (iteration.status != IterationStatus.PENDING) {
                        Spacer(Modifier.height(10.dp))
                        val statusText = when {
                            iteration.status == IterationStatus.COMPLETED -> "✅ Выполнено"
                            iteration.status == IterationStatus.SKIPPED &&
                                    iteration.statusSource == StatusSource.USER -> "⏭️ Пропущено (вами)"
                            iteration.status == IterationStatus.SKIPPED &&
                                    iteration.statusSource == StatusSource.SYSTEM -> "❌ Пропущено"
                            else -> ""
                        }
                        DetailRow("Статус:", statusText)
                        iteration.statusChangedAt?.let { ts ->
                            DetailRow("Время:", dateTimeFormat.format(Date(ts)))
                        }
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Действий не зафиксировано.\nВозможно, устройство было отключено.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Text(
        text = "$label $value",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

private fun formatMinutesLabel(minutes: Int): String = when {
    minutes < 60 -> "$minutes мин"
    minutes == 60 -> "1 час"
    minutes % 60 == 0 -> "${minutes / 60} ч"
    else -> "${minutes / 60} ч ${minutes % 60} мин"
}