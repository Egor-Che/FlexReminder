package io.github.egorche.flexreminder

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import io.github.egorche.flexreminder.alarm.AlarmScheduler
import io.github.egorche.flexreminder.alarm.AutoSkipWorker
import io.github.egorche.flexreminder.alarm.NotificationChannels
import io.github.egorche.flexreminder.alarm.Notifications
import io.github.egorche.flexreminder.alarm.SnoozeRestorer
import io.github.egorche.flexreminder.alarm.SoundResolver
import io.github.egorche.flexreminder.data.AppDatabase
import io.github.egorche.flexreminder.data.ArchiveManager
import io.github.egorche.flexreminder.data.SettingsRepository
import io.github.egorche.flexreminder.ui.ArchiveScreen
import io.github.egorche.flexreminder.ui.EditReminderScreen
import io.github.egorche.flexreminder.ui.MarkIterationsScreen
import io.github.egorche.flexreminder.ui.ReminderHistoryScreen
import io.github.egorche.flexreminder.ui.ReminderListScreen
import io.github.egorche.flexreminder.ui.ReminderViewModel
import io.github.egorche.flexreminder.ui.SettingsScreen
import io.github.egorche.flexreminder.ui.ViewReminderScreen
import io.github.egorche.flexreminder.ui.theme.FlexReminderTheme
import io.github.egorche.flexreminder.widget.ReminderWidgetProvider // ДОБАВЛЕН ИМПОРТ
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Notifications.ensureChannels(this)
        AutoSkipWorker.schedule(this)
        WorkManager.getInstance(this).enqueue(
            OneTimeWorkRequestBuilder<androidx.work.Worker>().build()
        )

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.get(this@MainActivity)
            ArchiveManager.archiveExpired(db)
            val reminderDao = db.reminderDao()
            val reminders = reminderDao.getEnabled()
            reminders.forEach { r ->
                AlarmScheduler.cancel(this@MainActivity, r.id)
                AlarmScheduler.schedule(this@MainActivity, r)
            }
            val invalidReminders = reminders.filter {
                !it.soundUri.isNullOrBlank() && !SoundResolver.isUriValid(this@MainActivity, it.soundUri)
            }
            invalidReminders.forEach { r ->
                reminderDao.update(r.copy(soundUri = null))
            }
            cleanupChannels(
                reminders.mapNotNull { it.soundUri } + listOfNotNull(
                    SettingsRepository.get(this@MainActivity).getDefaultSoundUriBlocking()
                )
            )
            SnoozeRestorer.restoreAll(this@MainActivity)
        }

        setContent {
            val settingsVm: io.github.egorche.flexreminder.ui.SettingsViewModel = viewModel()
            val appTheme by settingsVm.appTheme.collectAsStateWithLifecycle()

            FlexReminderTheme(theme = appTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNav(
                        onNavControllerReady = { navController = it }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // ПРИНУДИТЕЛЬНО ОБНОВЛЯЕМ ВИДЖЕТ ПРИ КАЖДОМ ОТКРЫТИИ ПРИЛОЖЕНИЯ
        ReminderWidgetProvider.updateAll(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Жесткий сброс стека навигации на главный экран.
        navController?.navigate("list") {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    private fun cleanupChannels(activeUris: List<String>) {
        val activeChannelIds = activeUris.map {
            NotificationChannels.computeDefaultChannelId(it)
        }.toSet()
        NotificationChannels.cleanupUnusedChannels(this, activeChannelIds)
    }
}

@Composable
fun AppNav(
    onNavControllerReady: (NavHostController) -> Unit
) {
    val nav = rememberNavController()

    LaunchedEffect(nav) {
        onNavControllerReady(nav)
    }

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
        composable("view/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: return@composable
            val vm: ReminderViewModel = viewModel()
            ViewReminderScreen(
                vm = vm,
                reminderId = id,
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate("edit/$id") }
            )
        }
        composable("edit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: return@composable
            val vm: ReminderViewModel = viewModel()
            EditReminderScreen(
                vm = vm,
                reminderId = id,
                templateId = null,
                onDone = { nav.popBackStack() }
            )
        }
        composable("mark/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: return@composable
            MarkIterationsScreen(
                reminderId = id,
                onBack = { nav.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
        composable("archive") {
            ArchiveScreen(
                onBack = { nav.popBackStack() },
                onOpenHistory = { id -> nav.navigate("history/$id") }
            )
        }
        composable("history/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: return@composable
            ReminderHistoryScreen(
                reminderId = id,
                onBack = { nav.popBackStack() },
                onCreateFromExample = { templateId -> nav.navigate("edit/-1?templateId=$templateId") }
            )
        }
        composable(
            "edit/-1?templateId={templateId}",
            arguments = listOf(navArgument("templateId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getLong("templateId")?.takeIf { it > 0 }
            val vm: ReminderViewModel = viewModel()
            EditReminderScreen(
                vm = vm,
                reminderId = -1L,
                templateId = templateId,
                onDone = { nav.popBackStack() }
            )
        }
    }
}