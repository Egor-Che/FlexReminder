package com.example.flexreminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val notes: String = "",
    /** Полночь даты начала периода. */
    val startDate: Long,
    /** Полночь последнего дня периода, либо null — бессрочно. */
    val endDate: Long? = null,
    /** Сколько дней подряд напоминание срабатывает (>= 1). */
    val daysOn: Int = 1,
    /** Сколько дней подряд пропускается после активного блока (>= 0). */
    val daysOff: Int = 0,
    val hour: Int = 9,
    val minute: Int = 0,
    val enabled: Boolean = true,
    /** Если true — уведомление без звука и вибрации. */
    val silent: Boolean = false
)