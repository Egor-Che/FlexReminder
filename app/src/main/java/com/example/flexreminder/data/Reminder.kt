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
    /** Периодичность в днях: 1 = каждый день, 2 = через день, 30 ≈ раз в месяц. */
    val intervalDays: Int = 1,
    val hour: Int = 9,
    val minute: Int = 0,
    val enabled: Boolean = true,
    /** Если true — уведомление без звука и вибрации. */
    val silent: Boolean = false
)