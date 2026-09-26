package io.github.egorche.flexreminder.ui

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.egorche.flexreminder.R
import io.github.egorche.flexreminder.alarm.DateUtils
import io.github.egorche.flexreminder.alarm.IterationLogic
import io.github.egorche.flexreminder.data.AppTheme
import io.github.egorche.flexreminder.data.Iteration
import io.github.egorche.flexreminder.data.IterationStatus
import io.github.egorche.flexreminder.data.Reminder
import io.github.egorche.flexreminder.data.ScheduleMode
import io.github.egorche.flexreminder.ui.theme.AppThemeColors
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
    onSettings: () -> Unit,
    onArchive: () -> Unit
) {
    val list by vm.reminders.collectAsStateWithLifecycle()
    val allIterations by vm.observeAllIterations()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val settingsVm: SettingsViewModel = viewModel()
    val appTheme by settingsVm.appTheme.collectAsStateWithLifecycle()

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
                title = { Text(stringResource(R.string.screen_list_title)) },
                actions = {
                    IconButton(onClick = onArchive) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = stringResource(R.string.screen_list_archive)
                        )
                    }
                    IconButton(onClick = onSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.screen_list_settings)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.screen_list_add)
                )
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
                    stringResource(R.string.screen_list_empty),
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
                itemsIndexed(
                    items = list,
                    key = { _, r -> r.id }
                ) { index, r ->
                    ReminderCard(
                        reminder = r,
                        iterations = iterationsByReminder[r.id].orEmpty(),
                        theme = appTheme,
                        itemIndex = index,
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
    theme: AppTheme,
    itemIndex: Int,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onToggleSilent: () -> Unit,
    onMark: () -> Unit
) {
    val todayMidnight = remember { DateUtils.midnight(System.currentTimeMillis()) }
    val triple = remember(reminder, iterations) {
        IterationLogic.getThreeIterations(reminder, todayMidnight, iterations)
    }

    val cardBg = AppThemeColors.cardBackground(theme, reminder.colorIndex, itemIndex)
    val contentColor = AppThemeColors.cardContentColor(theme)
    val secondaryColor = AppThemeColors.cardContentColorSecondary(theme)
    val accentColor = AppThemeColors.accent(theme, reminder.colorIndex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        reminder.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor
                    )

                    if (reminder.notes.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            reminder.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryColor,
                            maxLines = 2
                        )
                    }

                    Spacer(Modifier.height(4.dp))

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
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Switch(
                        checked = reminder.enabled,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AppThemeColors.onAccentColor(),
                            checkedTrackColor = accentColor
                        )
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
                                !reminder.enabled ->
                                    stringResource(R.string.sound_event_disabled)
                                reminder.silent ->
                                    stringResource(R.string.sound_enable)
                                else ->
                                    stringResource(R.string.sound_disable)
                            },
                            tint = when {
                                !reminder.enabled -> secondaryColor.copy(alpha = 0.4f)
                                reminder.silent -> accentColor
                                else -> secondaryColor
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (reminder.enabled) {
                IterationSummaryRow(
                    icon = Icons.Default.ArrowUpward,
                    label = stringResource(R.string.label_iteration_past),
                    iteration = triple.first,
                    hour = reminder.hour,
                    minute = reminder.minute,
                    theme = theme,
                    contentColor = contentColor,
                    secondaryColor = secondaryColor
                )
                Spacer(Modifier.height(2.dp))
                IterationSummaryRow(
                    icon = Icons.Default.PlayArrow,
                    label = stringResource(R.string.label_iteration_nearest),
                    iteration = triple.second,
                    hour = reminder.hour,
                    minute = reminder.minute,
                    theme = theme,
                    contentColor = contentColor,
                    secondaryColor = secondaryColor
                )
                Spacer(Modifier.height(2.dp))
                IterationSummaryRow(
                    icon = Icons.Default.SkipNext,
                    label = stringResource(R.string.label_iteration_next),
                    iteration = triple.third,
                    hour = reminder.hour,
                    minute = reminder.minute,
                    theme = theme,
                    contentColor = contentColor,
                    secondaryColor = secondaryColor
                )
            } else {
                Text(
                    stringResource(R.string.label_disabled),
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryColor
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onMark,
                modifier = Modifier.fillMaxWidth(),
                enabled = reminder.enabled
            ) {
                Text(
                    stringResource(R.string.action_mark),
                    color = if (reminder.enabled) accentColor else secondaryColor
                )
            }
        }
    }
}

@Composable
private fun IterationSummaryRow(
    icon: ImageVector,
    label: String,
    iteration: Iteration?,
    hour: Int,
    minute: Int,
    theme: AppTheme,
    contentColor: Color,
    secondaryColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(secondaryColor.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = secondaryColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = secondaryColor,
            modifier = Modifier.width(90.dp)
        )

        if (iteration == null) {
            Text(
                stringResource(R.string.label_no_value),
                style = MaterialTheme.typography.bodySmall,
                color = secondaryColor
            )
        } else {
            val datePattern = stringResource(R.string.format_date_short_weekday)
            val dateFormat = remember(datePattern) {
                SimpleDateFormat(datePattern, Locale.getDefault())
            }
            val dateText = remember(iteration.dateMillis, dateFormat) {
                dateFormat.format(Date(iteration.dateMillis))
            }

            Text(
                text = "$dateText " +
                        String.format(Locale.getDefault(), "%02d:%02d", hour, minute),
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )

            if (iteration.status != IterationStatus.PENDING) {
                IterationStatusLabel(
                    iteration = iteration,
                    theme = theme
                )
            }
        }
    }
}

@Composable
fun formatPattern(daysOn: Int, daysOff: Int): String = when {
    daysOff <= 0 -> stringResource(R.string.pattern_every_day)
    daysOn == 1 && daysOff == 1 -> stringResource(R.string.pattern_every_other_day)
    daysOn == 1 && daysOff == 6 -> stringResource(R.string.pattern_once_a_week)
    daysOn == 1 && daysOff == 29 -> stringResource(R.string.pattern_once_in_30_days)
    daysOn == 1 -> stringResource(R.string.pattern_once_in_n_days, daysOff + 1)
    daysOff == 1 -> stringResource(R.string.pattern_days_on_off_one, daysOn)
    else -> stringResource(R.string.pattern_days_on_off, daysOn, daysOff)
}