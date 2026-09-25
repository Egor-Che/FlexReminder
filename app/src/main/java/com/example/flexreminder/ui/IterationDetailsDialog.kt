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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.flexreminder.R
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
    val dateFullPattern = stringResource(R.string.format_date_full_with_weekday)
    val dateTimePattern = stringResource(R.string.format_date_time)
    val timePattern = stringResource(R.string.format_time)

    val dateFormatFull = remember(dateFullPattern) {
        SimpleDateFormat(dateFullPattern, Locale.getDefault())
    }
    val dateTimeFormat = remember(dateTimePattern) {
        SimpleDateFormat(dateTimePattern, Locale.getDefault())
    }
    val timeFormat = remember(timePattern) {
        SimpleDateFormat(timePattern, Locale.getDefault())
    }

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
                DetailRow(
                    label = stringResource(R.string.dialog_iteration_scheduled),
                    value = scheduledTime
                )

                if (hasRecord) {
                    iteration!!.firedAt?.let { firedAt ->
                        DetailRow(
                            label = stringResource(R.string.dialog_iteration_fired),
                            value = dateTimeFormat.format(Date(firedAt))
                        )
                    }

                    val snoozes = parseSnoozeHistory(iteration.snoozeHistory)
                    if (snoozes.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.dialog_iteration_snoozes),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        snoozes.forEach { snooze ->
                            Text(
                                text = stringResource(
                                    R.string.pattern_snooze_entry,
                                    timeFormat.format(Date(snooze.timestamp)),
                                    formatMinutesLabel(snooze.minutes)
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    if (iteration.status != IterationStatus.PENDING) {
                        Spacer(Modifier.height(10.dp))
                        val statusText = when {
                            iteration.status == IterationStatus.COMPLETED ->
                                stringResource(R.string.status_completed)
                            iteration.status == IterationStatus.SKIPPED &&
                                    iteration.statusSource == StatusSource.USER ->
                                stringResource(R.string.status_skipped_user)
                            iteration.status == IterationStatus.SKIPPED &&
                                    iteration.statusSource == StatusSource.SYSTEM ->
                                stringResource(R.string.status_skipped_system)
                            else -> ""
                        }
                        DetailRow(
                            label = stringResource(R.string.dialog_iteration_status),
                            value = statusText
                        )
                        iteration.statusChangedAt?.let { ts ->
                            DetailRow(
                                label = stringResource(R.string.dialog_iteration_time),
                                value = dateTimeFormat.format(Date(ts))
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.dialog_iteration_no_actions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close))
            }
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

@Composable
private fun formatMinutesLabel(minutes: Int): String = when {
    minutes < 60 -> "$minutes ${stringResource(R.string.unit_minutes_short)}"
    minutes == 60 -> "1 ${stringResource(R.string.unit_hours_short)}"
    minutes % 60 == 0 -> "${minutes / 60} ${stringResource(R.string.unit_hours_short)}"
    else -> "${minutes / 60} ${stringResource(R.string.unit_hours_short)} " +
            "${minutes % 60} ${stringResource(R.string.unit_minutes_short)}"
}