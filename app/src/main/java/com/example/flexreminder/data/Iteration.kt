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
    /** Время реального срабатывания уведомления (для будущей истории). */
    val firedAt: Long? = null,
    /** Сколько раз пользователь отложил уведомление для этой итерации. */
    val snoozeCount: Int = 0,
    /** Время последнего snooze (для расчёта автопропуска). null = snooze не было. */
    val lastSnoozeAt: Long? = null
)