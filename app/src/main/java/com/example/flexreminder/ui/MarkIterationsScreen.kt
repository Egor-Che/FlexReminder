package com.example.flexreminder.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flexreminder.alarm.DateUtils
import com.example.flexreminder.data.Iteration
import com.example.flexreminder.data.IterationStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkIterationsScreen(
    reminderId: Long,
    onBack: () -> Unit
) {
    val vm: IterationsViewModel = viewModel()
    LaunchedEffect(reminderId) { vm.load(reminderId) }

    val reminder by vm.reminder.collectAsStateWithLifecycle()
    val iterations by vm.iterations.collectAsStateWithLifecycle()
    val hasChanges by vm.hasChanges.collectAsStateWithLifecycle()

    var showOnlyMarked by remember { mutableStateOf(false) }
    var showCompleteAllDialog by remember { mutableStateOf(false) }
    var showSkipAllDialog by remember { mutableStateOf(false) }
    var showRollbackDialog by remember { mutableStateOf(false) }

    val visibleIterations = remember(iterations, showOnlyMarked) {
        if (showOnlyMarked) iterations.filter { it.status != IterationStatus.PENDING }
        else iterations
    }

    val todayMidnight = remember { DateUtils.midnight(System.currentTimeMillis()) }
    val initialIndex = remember(visibleIterations) {
        visibleIterations.indexOfFirst { it.dateMillis >= todayMidnight }
            .let { if (it < 0) (visibleIterations.size - 1).coerceAtLeast(0) else it }
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    val r = reminder
    if (r == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Отметки") },
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
            Text(
                "Загрузка…",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            )
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Отметки: ${r.title}") },
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
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            // Шапка с массовыми действиями
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showCompleteAllDialog = true },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Text("✅ Выполнить всё\nпрошедшее", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = { showSkipAllDialog = true },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Text("⏭️ Пропустить всё\nбудущее", style = MaterialTheme.typography.labelSmall)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = showOnlyMarked,
                    onCheckedChange = { showOnlyMarked = it }
                )
                Spacer(Modifier.width(8.dp))
                Text("Показать только с отметками", style = MaterialTheme.typography.bodyMedium)
            }

            HorizontalDivider()

            if (visibleIterations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (showOnlyMarked) "Нет отмеченных итераций"
                        else "Нет итераций в этом диапазоне",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(visibleIterations, key = { _, it -> it.dateMillis }) { _, iteration ->
                        val isNearest = iteration.dateMillis >= todayMidnight &&
                                visibleIterations
                                    .firstOrNull { it.dateMillis >= todayMidnight }
                                    ?.dateMillis == iteration.dateMillis

                        IterationRow(
                            iteration = iteration,
                            hour = r.hour,
                            minute = r.minute,
                            isNearest = isNearest,
                            onClickComplete = {
                                vm.toggleStatus(iteration.dateMillis, IterationStatus.COMPLETED)
                            },
                            onClickSkip = {
                                vm.toggleStatus(iteration.dateMillis, IterationStatus.SKIPPED)
                            }
                        )
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { showRollbackDialog = true },
                    enabled = hasChanges,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Отменить все изменения сессии")
                }
            }
        }
    }

    // Диалоги
    if (showCompleteAllDialog) {
        val count = vm.countCompletablePast()
        val range = vm.pastRange()
        AlertDialog(
            onDismissRequest = { showCompleteAllDialog = false },
            title = { Text("Отметить прошедшее выполненным?") },
            text = {
                Text(
                    buildString {
                        append("Будет отмечено: $count итераций\n")
                        if (range != null) {
                            append("Диапазон: ")
                            append(formatDialogDate(range.first))
                            append(" — ")
                            append(formatDialogDate(range.second))
                        }
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showCompleteAllDialog = false
                    vm.completeAllPast()
                }) { Text("Отметить") }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteAllDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showSkipAllDialog) {
        val count = vm.countSkippableFuture()
        val range = vm.futureRange()
        AlertDialog(
            onDismissRequest = { showSkipAllDialog = false },
            title = { Text("Отметить будущее пропущенным?") },
            text = {
                Text(
                    buildString {
                        append("Будет отмечено: $count итераций\n")
                        if (range != null) {
                            append("Диапазон: ")
                            append(formatDialogDate(range.first))
                            append(" — ")
                            append(formatDialogDate(range.second))
                        }
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSkipAllDialog = false
                    vm.skipAllFuture()
                }) { Text("Пропустить") }
            },
            dismissButton = {
                TextButton(onClick = { showSkipAllDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showRollbackDialog) {
        AlertDialog(
            onDismissRequest = { showRollbackDialog = false },
            title = { Text("Отменить все изменения сессии?") },
            text = { Text("Все статусы, изменённые за эту сессию, будут возвращены к исходному состоянию.") },
            confirmButton = {
                TextButton(onClick = {
                    showRollbackDialog = false
                    vm.rollbackSession()
                }) { Text("Отменить изменения") }
            },
            dismissButton = {
                TextButton(onClick = { showRollbackDialog = false }) { Text("Отмена") }
            }
        )
    }
}

private val dialogDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

private fun formatDialogDate(millis: Long): String =
    dialogDateFormat.format(Date(millis))