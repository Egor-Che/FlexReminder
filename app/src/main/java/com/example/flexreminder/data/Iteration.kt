package com.example.flexreminder.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class IterationStatus {
    PENDING,
    COMPLETED,
    SKIPPED
}

enum class StatusSource {
    USER,
    SYSTEM
}

@Entity(
    tableName = "iterations",
    indices = [Index(value = ["reminderId", "dateMillis"], unique = true)]
)
data class Iteration(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val reminderId: Long,
    /** Полночь конкретного дня. */
    val dateMillis: Long,
    val status: IterationStatus = IterationStatus.PENDING,
    /** null если PENDING. */
    val statusSource: StatusSource? = null,
    /** Время установки статуса (для истории). */
    val statusChangedAt: Long? = null,
    /** Время реального срабатывания уведомления (для истории). */
    val firedAt: Long? = null,
    /** Сколько раз пользователь перенёс уведомление для этой итерации. */
    val snoozeCount: Int = 0,
    /** Время последнего переноса (для расчёта автопропуска). */
    val lastSnoozeAt: Long? = null,
    /**
     * Время, когда должно сработать перенесённое уведомление.
     * null = нет активного переноса.
     */
    val snoozeUntil: Long? = null,
    /**
     * История переносов в формате "timestamp1:minutes1,timestamp2:minutes2,...".
     * Пустая строка — переносов не было.
     * Максимум 5 записей (лимит переносов на итерацию).
     */
    val snoozeHistory: String = ""
)