package com.example.flexreminder.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexreminder.data.AppDatabase
import com.example.flexreminder.data.Reminder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ArchiveViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).reminderDao()

    val archived: StateFlow<List<Reminder>> = dao.observeArchived()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )
}