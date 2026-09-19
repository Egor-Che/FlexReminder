package com.example.flexreminder.alarm

import com.example.flexreminder.data.Reminder
import com.example.flexreminder.data.ScheduleMode

object ReminderLogic {

    /**
     * Проверяет, попадает ли конкретный день (в миллисекундах полуночи)
     * в активное расписание напоминания.
     */
    fun isActiveOn(r: Reminder, dayMidnight: Long): Boolean {
        if (r.daysOn < 1 && r.customDates.isEmpty()) return false

        // Проверка окончания периода — общая для обоих режимов.
        // endDate трактуется как включительная граница (до конца дня).
        val endBoundary = r.endDate?.let { DateUtils.endOfDay(it) }
        if (endBoundary != null && dayMidnight > endBoundary) return false

        return when (r.mode) {
            ScheduleMode.CUSTOM_DATES -> {
                r.customDates.contains(DateUtils.midnight(dayMidnight))
            }
            ScheduleMode.INTERVAL -> {
                isActiveByInterval(r, dayMidnight)
            }
        }
    }

    private fun isActiveByInterval(r: Reminder, dayMidnight: Long): Boolean {
        val daysOff = r.daysOff.coerceAtLeast(0)
        val cycle = r.daysOn + daysOff
        if (cycle < 1) return false

        val startMidnight = DateUtils.midnight(r.startDate)
        val dayOffset = DateUtils.daysBetween(startMidnight, dayMidnight)
        if (dayOffset < 0) return false

        val posInCycle = ((dayOffset % cycle) + cycle) % cycle
        return posInCycle < r.daysOn
    }

    /**
     * Следующий момент срабатывания, начиная с from (не включая).
     * Возвращает null, если дальше ничего нет.
     */
    fun nextTriggerTime(r: Reminder, from: Long = System.currentTimeMillis()): Long? {
        if (!r.enabled) return null

        return when (r.mode) {
            ScheduleMode.CUSTOM_DATES -> nextTriggerCustomDates(r, from)
            ScheduleMode.INTERVAL -> nextTriggerInterval(r, from)
        }
    }

    private fun nextTriggerCustomDates(r: Reminder, from: Long): Long? {
        val endBoundary = r.endDate?.let { DateUtils.endOfDay(it) }
        val sorted = r.customDates.sorted()
        for (dateMillis in sorted) {
            val trigger = DateUtils.atTime(dateMillis, r.hour, r.minute)
            if (trigger <= from) continue
            if (endBoundary != null && trigger > endBoundary) return null
            return trigger
        }
        return null
    }

    private fun nextTriggerInterval(r: Reminder, from: Long): Long? {
        if (r.daysOn < 1) return null
        val daysOff = r.daysOff.coerceAtLeast(0)
        val cycle = r.daysOn + daysOff
        if (cycle < 1) return null

        val startMidnight = DateUtils.midnight(r.startDate)
        val todayMidnight = DateUtils.midnight(from)

        var dayOffset = DateUtils.daysBetween(startMidnight, todayMidnight)
        if (dayOffset < 0) dayOffset = 0

        val endBoundary = r.endDate?.let { DateUtils.endOfDay(it) }

        for (i in 0..cycle) {
            val currentOffset = dayOffset + i
            val posInCycle = ((currentOffset % cycle) + cycle) % cycle
            if (posInCycle < r.daysOn) {
                val dateMidnight = DateUtils.addDays(startMidnight, currentOffset)
                val trigger = DateUtils.atTime(dateMidnight, r.hour, r.minute)
                if (trigger > from) {
                    if (endBoundary != null && trigger > endBoundary) return null
                    return trigger
                }
            }
        }
        return null
    }
}