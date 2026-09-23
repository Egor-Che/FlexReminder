package com.example.flexreminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ScheduleMode {
    /** Расписание «N дней вкл / M дней выкл». */
    INTERVAL,
    /** Конкретные даты, выбранные пользователем вручную. */
    CUSTOM_DATES
}

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val notes: String = "",

    val mode: ScheduleMode = ScheduleMode.INTERVAL,

    /** Полночь даты начала периода (актуально для режима INTERVAL). */
    val startDate: Long,
    /** Полночь последнего дня периода, либо null — бессрочно. */
    val endDate: Long? = null,

    /** Сколько дней подряд напоминание срабатывает в режиме INTERVAL (>= 1). */
    val daysOn: Int = 1,
    /** Сколько дней подряд пропускается в режиме INTERVAL (>= 0). */
    val daysOff: Int = 0,

    /**
     * Даты (в миллисекундах полуночи), выбранные вручную.
     * Актуально для режима CUSTOM_DATES.
     */
    val customDates: Set<Long> = emptySet(),

    val hour: Int = 9,
    val minute: Int = 0,
    val enabled: Boolean = true,
    val silent: Boolean = false,

    /**
     * Индекс цвета в палитре (0..15).
     * null = цвет не выбран (белый). Актуально только в теме «Палитра».
     */
    val colorIndex: Int? = null,

    /**
     * URI звука уведомления для этого напоминания.
     * null = использовать глобальный звук из настроек (или дефолт).
     */
    val soundUri: String? = null
)