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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SkipNext
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flexreminder.alarm.DateUtils
import com.example.flexreminder.data.IterationStatus
import com.example.flexreminder.ui.theme.AppThemeColors
import kotlinx.coroutines.launch
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

    val settingsVm: SettingsViewModel = viewModel()
    val appTheme by settingsVm.appTheme.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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

    val listState = rememberLazyListState()
    var initialScrollDone by remember(reminderId) { mutableStateOf(false) }

    val r = reminder

    LaunchedEffect(visibleIterations, r) {
        if (!initialScrollDone && r != null && visibleIterations.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val idx = visibleIterations.indexOfFirst {
                DateUtils.atTime(it.dateMillis, r.hour, r.minute) > now
            }.let { if (it < 0) (visibleIterations.size - 1).coerceAtLeast(0) else it }
            listState.scrollToItem(idx)
            initialScrollDone = true
        }
    }

    val showSystemHint: () -> Unit = {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = "Чтобы изменить, нажмите на галочку",
                duration = SnackbarDuration.Short
            )
        }
    }

    if (r == null) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
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

    val accent = AppThemeColors.accent(appTheme, r.colorIndex)
    val completeColor = AppThemeColors.completed(appTheme)
    val skipColor = AppThemeColors.skippedUser(appTheme)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                .background(AppThemeColors.screenBackground(appTheme, r.colorIndex))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BulkActionButton(
                    icon = Icons.Default.Check,
                    iconTint = completeColor,
                    line1 = "Выполнить всё",
                    line2 = "прошедшее",
                    onClick = { showCompleteAllDialog = true },
                    modifier = Modifier.weight(1f)
                )
                BulkActionButton(
                    icon = Icons.Default.SkipNext,
                    iconTint = skipColor,
                    line1 = "Пропустить всё",
                    line2 = "будущее",
                    onClick = { showSkipAllDialog = true },
                    modifier = Modifier.weight(1f)
                )
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
                    itemsIndexed(
                        visibleIterations,
                        key = { _, it -> it.dateMillis }
                    ) { _, iteration ->
                        val now = System.currentTimeMillis()
                        val isNearest = DateUtils.atTime(
                            iteration.dateMillis, r.hour, r.minute
                        ) > now &&
                                visibleIterations.firstOrNull {
                                    DateUtils.atTime(it.dateMillis, r.hour, r.minute) > now
                                }?.dateMillis == iteration.dateMillis

                        IterationRow(
                            iteration = iteration,
                            hour = r.hour,
                            minute = r.minute,
                            isNearest = isNearest,
                            theme = appTheme,
                            colorIndex = r.colorIndex,
                            onClickComplete = {
                                vm.toggleStatus(iteration.dateMillis, IterationStatus.COMPLETED)
                            },
                            onClickSkip = {
                                vm.toggleStatus(iteration.dateMillis, IterationStatus.SKIPPED)
                            },
                            onBlockedBySystem = showSystemHint
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

@Composable
private fun BulkActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    line1: String,
    line2: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = line1,
                    style = MaterialTheme.typography.labelMedium,
                    lineHeight = MaterialTheme.typography.labelMedium.lineHeight
                )
                Text(
                    text = line2,
                    style = MaterialTheme.typography.labelMedium,
                    lineHeight = MaterialTheme.typography.labelMedium.lineHeight
                )
            }
        }
    }
}

private val dialogDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

private fun formatDialogDate(millis: Long): String =
    dialogDateFormat.format(Date(millis))