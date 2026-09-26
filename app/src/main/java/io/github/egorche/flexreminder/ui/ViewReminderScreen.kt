package io.github.egorche.flexreminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.egorche.flexreminder.R
import io.github.egorche.flexreminder.alarm.DateUtils
import io.github.egorche.flexreminder.alarm.ReminderLogic
import io.github.egorche.flexreminder.data.AppTheme
import io.github.egorche.flexreminder.data.Reminder
import io.github.egorche.flexreminder.data.ScheduleMode
import io.github.egorche.flexreminder.ui.theme.AppThemeColors
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

    val settingsVm: SettingsViewModel = viewModel()
    val appTheme by settingsVm.appTheme.collectAsStateWithLifecycle()

    val dateShortPattern = stringResource(R.string.format_date_short)
    val dateTimePattern = stringResource(R.string.format_date_time)

    val dfShort = remember(dateShortPattern) {
        SimpleDateFormat(dateShortPattern, Locale.getDefault())
    }
    val dfFull = remember(dateTimePattern) {
        SimpleDateFormat(dateTimePattern, Locale.getDefault())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_view_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                },
                actions = {
                    if (reminder != null) {
                        IconButton(onClick = {
                            vm.deleteById(reminderId) { onBack() }
                        }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.common_delete)
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
                stringResource(R.string.common_loading),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                style = MaterialTheme.typography.bodyLarge
            )
            return@Scaffold
        }

        val contentColor = AppThemeColors.cardContentColor(appTheme)
        val secondaryColor = AppThemeColors.cardContentColorSecondary(appTheme)
        val accentColor = AppThemeColors.accent(appTheme, r.colorIndex)
        val cardBg = AppThemeColors.viewCardBackground(appTheme, r.colorIndex)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AppThemeColors.screenBackground(appTheme, r.colorIndex))
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        r.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = contentColor
                    )

                    if (r.notes.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            r.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryColor
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(
                                R.string.pattern_time_label,
                                String.format(
                                    Locale.getDefault(), "%02d:%02d", r.hour, r.minute
                                )
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor
                        )
                        Spacer(Modifier.width(16.dp))
                        SoundLabel(
                            silent = r.silent,
                            enabled = r.enabled,
                            color = contentColor
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    val scheduleText = when (r.mode) {
                        ScheduleMode.INTERVAL -> formatPattern(r.daysOn, r.daysOff)
                        ScheduleMode.CUSTOM_DATES -> stringResource(
                            R.string.pattern_custom_dates_count,
                            r.customDates.size
                        )
                    }
                    Text(
                        text = scheduleText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = accentColor
                    )

                    if (r.mode == ScheduleMode.INTERVAL) {
                        Spacer(Modifier.height(4.dp))
                        val periodText = if (r.endDate != null) {
                            stringResource(
                                R.string.pattern_period_label_from_to,
                                dfShort.format(Date(r.startDate)),
                                dfShort.format(Date(r.endDate))
                            )
                        } else {
                            stringResource(
                                R.string.pattern_period_label_from,
                                dfShort.format(Date(r.startDate))
                            )
                        }
                        Text(
                            text = periodText,
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryColor
                        )
                    } else {
                        val sorted = r.customDates.sorted()
                        if (sorted.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(
                                    R.string.pattern_first_last_dates,
                                    dfShort.format(Date(sorted.first())),
                                    dfShort.format(Date(sorted.last()))
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = secondaryColor
                            )
                        }
                    }

                    val next = remember(r) { ReminderLogic.nextTriggerTime(r) }
                    if (r.enabled && next != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(
                                R.string.pattern_next_trigger,
                                dfFull.format(Date(next))
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = accentColor
                        )
                    } else if (r.enabled) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.view_period_finished),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppThemeColors.skippedSystem(appTheme)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Switch(
                            checked = r.enabled,
                            onCheckedChange = { vm.toggle(r) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = accentColor,
                                checkedTrackColor = accentColor.copy(alpha = 0.5f)
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                if (r.enabled) R.string.label_enabled
                                else R.string.label_disabled
                            ),
                            color = contentColor
                        )

                        Spacer(Modifier.width(24.dp))

                        Switch(
                            checked = if (r.enabled) !r.silent else false,
                            onCheckedChange = { soundOn -> vm.setSilent(r, !soundOn) },
                            enabled = r.enabled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = accentColor,
                                checkedTrackColor = accentColor.copy(alpha = 0.5f)
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        SoundLabel(
                            silent = r.silent,
                            enabled = r.enabled,
                            color = contentColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(
                    if (r.mode == ScheduleMode.INTERVAL) R.string.view_schedule_month
                    else R.string.view_selected_dates
                ),
                style = MaterialTheme.typography.titleMedium,
                color = contentColor
            )

            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(Modifier.padding(8.dp)) {
                    when (r.mode) {
                        ScheduleMode.CUSTOM_DATES -> {
                            ReminderCalendar(
                                selectedDates = r.customDates,
                                readOnly = true,
                                theme = appTheme,
                                colorIndex = r.colorIndex,
                                initialMonthMillis = r.customDates.minOrNull()
                                    ?: System.currentTimeMillis()
                            )
                        }
                        ScheduleMode.INTERVAL -> {
                            IntervalPreviewCalendar(r, appTheme)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_edit))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SoundLabel(
    silent: Boolean,
    enabled: Boolean,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (silent || !enabled)
                Icons.Default.VolumeOff
            else
                Icons.Default.VolumeUp,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(
                if (silent || !enabled) R.string.label_sound_off
                else R.string.label_sound_on
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}

@Composable
private fun IntervalPreviewCalendar(r: Reminder, theme: AppTheme) {
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
        theme = theme,
        colorIndex = r.colorIndex,
        initialMonthMillis = System.currentTimeMillis()
    )
}