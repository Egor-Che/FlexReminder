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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flexreminder.data.AppTheme
import com.example.flexreminder.data.SettingsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val vm: SettingsViewModel = viewModel()
    val snoozeShort by vm.snoozeShort.collectAsStateWithLifecycle()
    val snoozeLong by vm.snoozeLong.collectAsStateWithLifecycle()
    val appTheme by vm.appTheme.collectAsStateWithLifecycle()
    val editingField by vm.editingField.collectAsStateWithLifecycle()
    val editingValue by vm.editingValue.collectAsStateWithLifecycle()
    val themeDialogOpen by vm.themeDialogOpen.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
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
            SectionHeader("Snooze (отложить напоминание)")

            SettingRow(
                title = "Короткий интервал",
                subtitle = formatMinutes(snoozeShort),
                onClick = { vm.openEditor(SnoozeField.SHORT) }
            )
            SettingRow(
                title = "Длинный интервал",
                subtitle = formatMinutes(snoozeLong),
                onClick = { vm.openEditor(SnoozeField.LONG) }
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            SectionHeader("Внешний вид")

            SettingRow(
                title = "Тема",
                subtitle = themeLabel(appTheme),
                onClick = { vm.openThemeDialog() }
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            SectionHeader("Прочее")

            SettingRow(
                title = "Звук уведомления",
                subtitle = "Скоро",
                enabled = false,
                onClick = {}
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

    if (themeDialogOpen) {
        ThemeDialog(
            current = appTheme,
            onPick = vm::setTheme,
            onDismiss = vm::closeThemeDialog
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
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
        SnoozeField.SHORT -> "Короткий интервал"
        SnoozeField.LONG -> "Длинный интервал"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = { Text("Минут") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    "Быстрый выбор:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                PresetRow(
                    presets = SettingsRepository.PRESETS.take(4),
                    onPick = { onValueChange(it.toString()) }
                )
                Spacer(Modifier.height(6.dp))
                PresetRow(
                    presets = SettingsRepository.PRESETS.drop(4),
                    onPick = { onValueChange(it.toString()) }
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Диапазон: 1 — 720 минут",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = value.isNotBlank() && value.toIntOrNull() != null
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
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
        title = { Text("Тема оформления") },
        text = {
            Column {
                ThemeOption(
                    theme = AppTheme.PALETTE,
                    title = "Палитра",
                    subtitle = "16 пастельных цветов для напоминаний",
                    selected = current == AppTheme.PALETTE,
                    onClick = { onPick(AppTheme.PALETTE) }
                )
                Spacer(Modifier.height(8.dp))
                ThemeOption(
                    theme = AppTheme.MONOCHROME,
                    title = "Монохром",
                    subtitle = "Только оттенки серого, без цветных акцентов",
                    selected = current == AppTheme.MONOCHROME,
                    onClick = { onPick(AppTheme.MONOCHROME) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
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

private fun formatMinutes(minutes: Int): String = when {
    minutes < 60 -> "$minutes мин"
    minutes == 60 -> "1 час"
    minutes % 60 == 0 -> "${minutes / 60} ч"
    else -> "${minutes / 60} ч ${minutes % 60} мин"
}

private fun formatPreset(minutes: Int): String = when {
    minutes < 60 -> "$minutes"
    minutes == 60 -> "1ч"
    minutes == 120 -> "2ч"
    minutes == 240 -> "4ч"
    minutes == 720 -> "12ч"
    else -> "${minutes / 60}ч"
}

private fun themeLabel(theme: AppTheme): String = when (theme) {
    AppTheme.PALETTE -> "Палитра"
    AppTheme.MONOCHROME -> "Монохром"
}