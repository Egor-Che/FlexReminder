package com.example.flexreminder.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flexreminder.R
import com.example.flexreminder.data.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val vm: SettingsViewModel = viewModel()

    val snoozeShort by vm.snoozeShort.collectAsStateWithLifecycle()
    val snoozeLong by vm.snoozeLong.collectAsStateWithLifecycle()
    val appTheme by vm.appTheme.collectAsStateWithLifecycle()
    val defaultSoundUri by vm.defaultSoundUri.collectAsStateWithLifecycle()
    val defaultSoundName by vm.defaultSoundName.collectAsStateWithLifecycle()
    val editingField by vm.editingField.collectAsStateWithLifecycle()
    val editingValue by vm.editingValue.collectAsStateWithLifecycle()
    val themeDialogOpen by vm.themeDialogOpen.collectAsStateWithLifecycle()
    val soundDialogOpen by vm.soundDialogOpen.collectAsStateWithLifecycle()
    val snoozeError by vm.snoozeError.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
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
        ) {
            SectionHeader(stringResource(R.string.section_snooze))

            SettingRow(
                title = stringResource(R.string.label_snooze_short),
                subtitle = formatMinutes(snoozeShort),
                onClick = { vm.openEditor(SnoozeField.SHORT) }
            )
            SettingRow(
                title = stringResource(R.string.label_snooze_long),
                subtitle = formatMinutes(snoozeLong),
                onClick = { vm.openEditor(SnoozeField.LONG) }
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            SectionHeader(stringResource(R.string.section_notifications))

            SettingRow(
                title = stringResource(R.string.label_default_sound),
                subtitle = defaultSoundName ?: stringResource(R.string.sound_builtin),
                onClick = { vm.openSoundDialog() }
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            SectionHeader(stringResource(R.string.section_appearance))

            SettingRow(
                title = stringResource(R.string.label_theme),
                subtitle = themeLabel(appTheme),
                onClick = { vm.openThemeDialog() }
            )
        }
    }

    if (editingField != null) {
        SnoozeEditorDialog(
            field = editingField!!,
            value = editingValue,
            onValueChange = vm::updateEditingValue,
            onSave = vm::saveEditor,
            onDismiss = vm::closeEditor
        )
    }

    if (snoozeError != null) {
        SnoozeErrorDialog(
            field = snoozeError!!,
            onDismiss = vm::dismissSnoozeError
        )
    }

    if (themeDialogOpen) {
        ThemeDialog(
            current = appTheme,
            onPick = vm::setTheme,
            onDismiss = vm::closeThemeDialog
        )
    }

    if (soundDialogOpen) {
        SoundPickerDialog(
            initialUri = defaultSoundUri,
            onPicked = { vm.setDefaultSoundUri(it) },
            onDismiss = { vm.closeSoundDialog() }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val textColor = if (enabled)
        MaterialTheme.colorScheme.onSurface
    else
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                maxLines = 2
            )
        }
        if (enabled) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SnoozeEditorDialog(
    field: SnoozeField,
    value: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val title = when (field) {
        SnoozeField.SHORT -> stringResource(R.string.dialog_snooze_short_title)
        SnoozeField.LONG -> stringResource(R.string.dialog_snooze_long_title)
    }
    val rangeLabel = when (field) {
        SnoozeField.SHORT -> stringResource(R.string.label_snooze_range_short)
        SnoozeField.LONG -> stringResource(R.string.label_snooze_range_long)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = { Text(rangeLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    stringResource(R.string.label_snooze_presets),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                PresetRow(
                    presets = field.presets.take(4),
                    onPick = { onValueChange(it.toString()) }
                )
                Spacer(Modifier.height(6.dp))
                PresetRow(
                    presets = field.presets.drop(4),
                    onPick = { onValueChange(it.toString()) }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = value.isNotBlank() && value.toIntOrNull() != null
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
private fun SnoozeErrorDialog(
    field: SnoozeField,
    onDismiss: () -> Unit
) {
    val messageRes = when (field) {
        SnoozeField.SHORT -> R.string.dialog_snooze_error_short
        SnoozeField.LONG -> R.string.dialog_snooze_error_long
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_snooze_error_title)) },
        text = { Text(stringResource(messageRes)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_ok))
            }
        }
    )
}

@Composable
private fun PresetRow(
    presets: List<Int>,
    onPick: (Int) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        presets.forEachIndexed { index, minutes ->
            if (index > 0) Spacer(Modifier.width(6.dp))
            TextButton(
                onClick = { onPick(minutes) },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = formatPreset(minutes),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun ThemeDialog(
    current: AppTheme,
    onPick: (AppTheme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_theme_title)) },
        text = {
            Column {
                ThemeOption(
                    theme = AppTheme.PALETTE,
                    title = stringResource(R.string.theme_palette),
                    subtitle = stringResource(R.string.dialog_theme_palette_desc),
                    selected = current == AppTheme.PALETTE,
                    onClick = { onPick(AppTheme.PALETTE) }
                )
                Spacer(Modifier.height(8.dp))
                ThemeOption(
                    theme = AppTheme.MONOCHROME,
                    title = stringResource(R.string.theme_monochrome),
                    subtitle = stringResource(R.string.dialog_theme_monochrome_desc),
                    selected = current == AppTheme.MONOCHROME,
                    onClick = { onPick(AppTheme.MONOCHROME) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
private fun ThemeOption(
    theme: AppTheme,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun formatMinutes(minutes: Int): String = when {
    minutes < 60 -> stringResource(R.string.pattern_minutes_value, minutes)
    minutes == 60 -> stringResource(R.string.pattern_minutes_one_hour)
    minutes % 60 == 0 -> stringResource(R.string.pattern_hours_value, minutes / 60)
    else -> stringResource(
        R.string.pattern_hours_minutes_value,
        minutes / 60,
        minutes % 60
    )
}

@Composable
private fun formatPreset(minutes: Int): String = when {
    minutes < 60 -> "$minutes${stringResource(R.string.unit_minutes_compact)}"
    minutes % 60 == 0 -> "${minutes / 60}${stringResource(R.string.unit_hours_short)}"
    else -> "$minutes${stringResource(R.string.unit_minutes_compact)}"
}

@Composable
private fun themeLabel(theme: AppTheme): String = when (theme) {
    AppTheme.PALETTE -> stringResource(R.string.theme_palette)
    AppTheme.MONOCHROME -> stringResource(R.string.theme_monochrome)
}