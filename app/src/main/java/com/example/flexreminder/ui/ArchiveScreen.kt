package com.example.flexreminder.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flexreminder.data.Reminder
import com.example.flexreminder.data.ScheduleMode
import com.example.flexreminder.ui.theme.AppThemeColors
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Архив напоминаний") },
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
        if (archived.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Архив пуст\n\nЗдесь будут напоминания, которые закончились — " +
                            "после наступления даты окончания.",
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
}

@Composable
private fun ArchiveCard(
    reminder: Reminder,
    itemIndex: Int,
    onClick: () -> Unit
) {
    // Архив — всегда серая зебра, независимо от активной темы
    val cardBg = if (itemIndex % 2 == 0)
        AppThemeColors.MonoCardEven
    else
        AppThemeColors.MonoCardOdd

    val contentColor = AppThemeColors.cardContentColor(
        com.example.flexreminder.data.AppTheme.MONOCHROME
    )
    val secondaryColor = AppThemeColors.cardContentColorSecondary(
        com.example.flexreminder.data.AppTheme.MONOCHROME
    )

    val df = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

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

            Text(
                text = buildString {
                    when (reminder.mode) {
                        ScheduleMode.INTERVAL -> {
                            append(formatPattern(reminder.daysOn, reminder.daysOff))
                        }
                        ScheduleMode.CUSTOM_DATES -> {
                            append("Конкретные даты: ")
                            append(reminder.customDates.size)
                            append(" шт.")
                        }
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = secondaryColor
            )

            Spacer(Modifier.height(2.dp))

            val periodText = buildString {
                append("С ")
                append(df.format(Date(reminder.startDate)))
                reminder.endDate?.let {
                    append(" по ")
                    append(df.format(Date(it)))
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