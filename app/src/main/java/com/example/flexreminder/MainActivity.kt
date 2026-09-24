package com.example.flexreminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.flexreminder.alarm.AlarmScheduler
import com.example.flexreminder.alarm.AutoSkipWorker
import com.example.flexreminder.alarm.NotificationChannels
import com.example.flexreminder.alarm.Notifications
import com.example.flexreminder.alarm.SnoozeRestorer
import com.example.flexreminder.alarm.SoundResolver
import com.example.flexreminder.data.AppDatabase
import com.example.flexreminder.data.AppTheme
import com.example.flexreminder.data.ArchiveManager
import com.example.flexreminder.data.SettingsRepository
import com.example.flexreminder.ui.ArchiveScreen
import com.example.flexreminder.ui.EditReminderScreen
import com.example.flexreminder.ui.MarkIterationsScreen
import com.example.flexreminder.ui.ReminderHistoryScreen
import com.example.flexreminder.ui.ReminderListScreen
import com.example.flexreminder.ui.ReminderViewModel
import com.example.flexreminder.ui.SettingsScreen
import com.example.flexreminder.ui.ViewReminderScreen
import com.example.flexreminder.ui.theme.FlexReminderTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notifications.ensureChannels(this)

        AutoSkipWorker.schedule(this)

        WorkManager.getInstance(this).enqueue(
            OneTimeWorkRequestBuilder<AutoSkipWorker>().build()
        )

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.get(this@MainActivity)

            // 1. Архивируем истёкшие напоминания (до всего остального)
            ArchiveManager.archiveExpired(db)

            // 2. Перепланирование активных будильников
            val reminderDao = db.reminderDao()
            val reminders = reminderDao.getEnabled()
            reminders.forEach { r ->
                AlarmScheduler.cancel(this@MainActivity, r.id)
                AlarmScheduler.schedule(this@MainActivity, r)
            }

            // 3. Сброс невалидных URI звуков
            val invalidReminders = reminders.filter {
                !it.soundUri.isNullOrBlank() &&
                        !SoundResolver.isUriValid(this@MainActivity, it.soundUri)
            }
            invalidReminders.forEach { r ->
                reminderDao.update(r.copy(soundUri = null))
            }

            // 4. Очистка «сиротских» каналов уведомлений
            cleanupChannels(
                reminders.mapNotNull { it.soundUri } +
                        listOfNotNull(
                            SettingsRepository.get(this@MainActivity)
                                .getDefaultSoundUriBlocking()
                        )
            )

            // 5. Восстановление активных переносов (snooze)
            SnoozeRestorer.restoreAll(this@MainActivity)
        }

        setContent {
            val settingsRepo = remember { SettingsRepository.get(this) }
            val appTheme by settingsRepo.appTheme.collectAsStateWithLifecycle(
                initialValue = AppTheme.PALETTE
            )

            FlexReminderTheme(theme = appTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNav()
                }
            }
        }
    }

    private fun cleanupChannels(activeUris: List<String>) {
        val activeChannelIds = activeUris
            .filter { it.isNotBlank() }
            .map { NotificationChannels.computeCustomChannelId(it) }
            .toSet() +
                activeUris
                    .filter { it.isNotBlank() }
                    .map { NotificationChannels.computeDefaultChannelId(it) }
                    .toSet()

        NotificationChannels.cleanupUnusedChannels(this, activeChannelIds)
    }
}

@Composable
fun AppNav() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = "list") {

        composable("list") {
            val vm: ReminderViewModel = viewModel()
            ReminderListScreen(
                vm = vm,
                onAdd = { nav.navigate("edit/-1") },
                onOpen = { id -> nav.navigate("view/$id") },
                onMark = { id -> nav.navigate("mark/$id") },
                onSettings = { nav.navigate("settings") },
                onArchive = { nav.navigate("archive") }
            )
        }

        composable("archive") {
            ArchiveScreen(
                onBack = { nav.navigateUp() },
                onOpenHistory = { id -> nav.navigate("archive/$id") }
            )
        }

        composable(
            route = "archive/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: -1L
            ReminderHistoryScreen(
                reminderId = id,
                onBack = { nav.navigateUp() },
                onCreateFromExample = { templateId ->
                    nav.navigate("edit-from-template/$templateId")
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                onBack = { nav.navigateUp() }
            )
        }

        composable(
            route = "view/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: -1L
            val vm: ReminderViewModel = viewModel()
            ViewReminderScreen(
                vm = vm,
                reminderId = id,
                onBack = { nav.navigateUp() },
                onEdit = { nav.navigate("edit/$id") }
            )
        }

        composable(
            route = "edit/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: -1L
            val vm: ReminderViewModel = viewModel()
            EditReminderScreen(
                vm = vm,
                reminderId = id,
                templateId = null,
                onDone = { nav.navigateUp() }
            )
        }

        composable(
            route = "edit-from-template/{templateId}",
            arguments = listOf(navArgument("templateId") { type = NavType.LongType })
        ) { entry ->
            val templateId = entry.arguments?.getLong("templateId") ?: -1L
            val vm: ReminderViewModel = viewModel()
            EditReminderScreen(
                vm = vm,
                reminderId = -1L,
                templateId = templateId,
                onDone = { nav.navigateUp() }
            )
        }

        composable(
            route = "mark/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: -1L
            MarkIterationsScreen(
                reminderId = id,
                onBack = { nav.navigateUp() }
            )
        }
    }
}