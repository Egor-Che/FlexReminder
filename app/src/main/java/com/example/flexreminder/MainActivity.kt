package com.example.flexreminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.flexreminder.alarm.AlarmScheduler
import com.example.flexreminder.alarm.Notifications
import com.example.flexreminder.data.AppDatabase
import com.example.flexreminder.ui.EditReminderScreen
import com.example.flexreminder.ui.ReminderListScreen
import com.example.flexreminder.ui.ReminderViewModel
import com.example.flexreminder.ui.ViewReminderScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notifications.ensureChannels(this)

        // При запуске приложения перепланируем все активные будильники
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.get(this@MainActivity).reminderDao()
            dao.getEnabled().forEach { r ->
                AlarmScheduler.cancel(this@MainActivity, r.id)
                AlarmScheduler.schedule(this@MainActivity, r)
            }
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNav()
                }
            }
        }
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
                onOpen = { id -> nav.navigate("view/$id") }
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
                onBack = { nav.popBackStack() },
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
                onDone = { nav.popBackStack() }
            )
        }
    }
}