package io.github.egorche.flexreminder.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.egorche.flexreminder.R
import io.github.egorche.flexreminder.data.AppTheme
import io.github.egorche.flexreminder.data.Reminder
import io.github.egorche.flexreminder.data.ScheduleMode
import io.github.egorche.flexreminder.ui.theme.AppThemeColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    onBack: () -> Unit,
    onOpenHistory: (Long) -> Unit
) {
    val vm: ArchiveViewModel = viewModel()
    val archived by vm.archived.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showClearDialog by remember { mutableStateOf(false) }

    val clearedMessage = stringResource(R.string.snackbar_archive_cleared)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_archive_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showClearDialog = true },
                        enabled = archived.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.CleaningServices,
                            contentDescription = stringResource(R.string.screen_archive_clear)
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (archived.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.screen_archive_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
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
                itemsIndexed(
                    items = archived,
                    key = { _, r -> r.id }
                ) { index, reminder ->
                    ArchiveCard(
                        reminder = reminder,
                        itemIndex = index,
                        onClick = { onOpenHistory(reminder.id) }
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.dialog_clear_archive_title)) },
            text = { Text(stringResource(R.string.dialog_clear_archive_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    vm.clearArchive {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = clearedMessage,
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }) {
                    Text(stringResource(R.string.dialog_clear_archive_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun ArchiveCard(
    reminder: Reminder,
    itemIndex: Int,
    onClick: () -> Unit
) {
    val cardBg = if (itemIndex % 2 == 0)
        AppThemeColors.MonoCardEven
    else
        AppThemeColors.MonoCardOdd

    val monoTheme = AppTheme.MONOCHROME
    val contentColor = AppThemeColors.cardContentColor(monoTheme)
    val secondaryColor = AppThemeColors.cardContentColorSecondary(monoTheme)

    val datePattern = stringResource(R.string.format_date_short)
    val df = remember(datePattern) {
        SimpleDateFormat(datePattern, Locale.getDefault())
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = reminder.title,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor
            )

            if (reminder.notes.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = reminder.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryColor,
                    maxLines = 2
                )
            }

            Spacer(Modifier.height(6.dp))

            val scheduleText = when (reminder.mode) {
                ScheduleMode.INTERVAL -> formatPattern(
                    reminder.daysOn, reminder.daysOff
                )
                ScheduleMode.CUSTOM_DATES -> stringResource(
                    R.string.pattern_custom_dates_count,
                    reminder.customDates.size
                )
            }
            Text(
                text = scheduleText,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryColor
            )

            Spacer(Modifier.height(2.dp))

            val periodText = buildString {
                val start = df.format(Date(reminder.startDate))
                val end = reminder.endDate?.let { df.format(Date(it)) }
                if (end != null) {
                    append(stringResource(R.string.pattern_period_from_to, start, end))
                } else {
                    append(stringResource(R.string.pattern_period_from, start))
                }
            }
            Text(
                text = periodText,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryColor
            )
        }
    }
}