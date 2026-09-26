package io.github.egorche.flexreminder.alarm

import io.github.egorche.flexreminder.data.Iteration
import io.github.egorche.flexreminder.data.IterationStatus
import io.github.egorche.flexreminder.data.Reminder
import io.github.egorche.flexreminder.data.ScheduleMode
import io.github.egorche.flexreminder.data.StatusSource

object ReminderLogic {

    /**
     * Проверяет, попадает ли конкретный день в активное расписание напоминания.
     * Учитывает endDate.
     */
    fun isActiveOn(r: Reminder, dayMidnight: Long): Boolean {
        if (r.daysOn < 1 && r.customDates.isEmpty()) return false

        val endBoundary = r.endDate?.let { DateUtils.endOfDay(it) }
        if (endBoundary != null && dayMidnight > endBoundary) return false

        return when (r.mode) {
            ScheduleMode.CUSTOM_DATES -> r.customDates.contains(DateUtils.midnight(dayMidnight))
            ScheduleMode.INTERVAL -> isActiveByInterval(r, dayMidnight)
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
     * Следующий момент срабатывания по расписанию, без учёта отметок.
     * Используется для расчёта базового расписания.
     */
    fun nextTriggerTime(r: Reminder, from: Long = System.currentTimeMillis()): Long? {
        if (!r.enabled) return null

        return when (r.mode) {
            ScheduleMode.CUSTOM_DATES -> nextTriggerCustomDates(r, from)
            ScheduleMode.INTERVAL -> nextTriggerInterval(r, from)
        }
    }

    /**
     * Следующий момент срабатывания с учётом отметок итераций.
     * Итерации со статусом COMPLETED или SKIPPED (USER) пропускаются.
     */
    fun nextPendingTrigger(
        reminder: Reminder,
        iterations: List<Iteration>,
        from: Long = System.currentTimeMillis()
    ): Long? {
        if (iterations.isEmpty()) return nextTriggerTime(reminder, from)

        val iterationMap = iterations.associateBy { it.dateMillis }
        var cursor = from
        var guard = 0
        val maxIterations = 500

        while (guard < maxIterations) {
            val next = nextTriggerTime(reminder, cursor) ?: return null
            val dateMillis = DateUtils.midnight(next)
            val iteration = iterationMap[dateMillis]
            val isMarked = iteration != null && (
                    iteration.status == IterationStatus.COMPLETED ||
                            (iteration.status == IterationStatus.SKIPPED &&
                                    iteration.statusSource == StatusSource.USER)
                    )
            if (!isMarked) return next
            cursor = next + 1
            guard++
        }
        return null
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