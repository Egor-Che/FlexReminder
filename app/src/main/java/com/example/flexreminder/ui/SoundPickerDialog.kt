package com.example.flexreminder.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SoundPickerDialog(
    initialUri: String?,
    onPicked: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val vm: SoundPickerViewModel = viewModel()

    LaunchedEffect(Unit) { vm.initialize(initialUri) }

    val currentUri by vm.currentUri.collectAsStateWithLifecycle()
    val currentName by vm.currentName.collectAsStateWithLifecycle()
    val playing by vm.playing.collectAsStateWithLifecycle()
    val activeCategory by vm.activeCategory.collectAsStateWithLifecycle()
    val soundsByCategory by vm.soundsByCategory.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }
        vm.stop()
        vm.select(uri.toString())
    }

    Dialog(onDismissRequest = {
        vm.stop()
        onDismiss()
    }) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Заголовок
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Выбрать звук",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        vm.stop()
                        onDismiss()
                    }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Закрыть"
                        )
                    }
                }

                // Текущее значение + Play/Stop
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Выбрано:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currentName ?: "Мелодия приложения",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { vm.togglePlay() }
                    ) {
                        Icon(
                            imageVector = if (playing) Icons.Default.Stop
                            else Icons.Default.PlayArrow,
                            contentDescription = if (playing) "Остановить" else "Прослушать"
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Табы категорий
                TabRow(
                    selectedTabIndex = activeCategory.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    SoundCategory.values().forEach { category ->
                        Tab(
                            selected = activeCategory == category,
                            onClick = { vm.setCategory(category) },
                            text = { Text(category.title) }
                        )
                    }
                }

                Divider()

                // Список звуков
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (loading) {
                        Box(
                            Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val sounds = soundsByCategory[activeCategory].orEmpty()
                        if (sounds.isEmpty()) {
                            Box(
                                Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Звуки не найдены",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(sounds, key = { it.uri }) { sound ->
                                    SoundRow(
                                        title = sound.title,
                                        selected = sound.uri == currentUri,
                                        onClick = {
                                            vm.stop()
                                            vm.select(sound.uri)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Divider()

                // Нижние кнопки
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                vm.stop()
                                safLauncher.launch(arrayOf("audio/*"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Свой файл")
                        }
                        OutlinedButton(
                            onClick = {
                                vm.stop()
                                vm.clear()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Сбросить")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            vm.stop()
                            onDismiss()
                        }) { Text("Отмена") }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            vm.stop()
                            onPicked(currentUri)
                        }) { Text("Сохранить") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}