package com.example.flexreminder.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class IterationStatus {
    /** Никакой отметки нет. */
    PENDING,
    /** Пользователь отметил, что действие выполнено. */
    COMPLETED,
    /** Пропущено — либо пользователем, либо системой. */
    SKIPPED
}

enum class StatusSource {
    /** Статус поставлен пользователем вручную. */
    USER,
    /** Статус поставлен системой автоматически (автопропуск). */
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
    val firedAt: Long? = null
)