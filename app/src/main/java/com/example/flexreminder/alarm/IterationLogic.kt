package com.example.flexreminder.alarm

import com.example.flexreminder.data.Iteration
import com.example.flexreminder.data.Reminder
import com.example.flexreminder.data.ScheduleMode

object IterationLogic {

    /**
     * Максимальное количество итераций, которые генерируются за один вызов.
     * Защита от бесконечных циклов при неверных данных.
     */
    private const val MAX_ITERATIONS = 5000

    /**
     * Генерирует все даты итераций напоминания в диапазоне [fromMidnight; toMidnight].
     * Учитывает startDate и endDate самого напоминания.
     *
     * Возвращает отсортированный список миллисекунд полуночи.
     */
    fun generateIterationDates(
        reminder: Reminder,
        fromMidnight: Long,
        toMidnight: Long
    ): List<Long> {
        val startLimit = maxOf(DateUtils.midnight(reminder.startDate), fromMidnight)
        val endLimit = minOf(
            reminder.endDate?.let { DateUtils.midnight(it) } ?: Long.MAX_VALUE,
            toMidnight
        )
        if (startLimit > endLimit) return emptyList()

        return when (reminder.mode) {
            ScheduleMode.CUSTOM_DATES -> reminder.customDates
                .filter { it in startLimit..endLimit }
                .sorted()

            ScheduleMode.INTERVAL -> generateIntervalDates(reminder, startLimit, endLimit)
        }
    }

    private fun generateIntervalDates(
        reminder: Reminder,
        fromMidnight: Long,
        toMidnight: Long
    ): List<Long> {
        if (reminder.daysOn < 1) return emptyList()
        val daysOff = reminder.daysOff.coerceAtLeast(0)
        val cycle = reminder.daysOn + daysOff
        if (cycle < 1) return emptyList()

        val startMidnight = DateUtils.midnight(reminder.startDate)
        val result = mutableListOf<Long>()
        var cursor = fromMidnight
        var guard = 0

        while (cursor <= toMidnight && guard < MAX_ITERATIONS) {
            val dayOffset = DateUtils.daysBetween(startMidnight, cursor)
            if (dayOffset >= 0) {
                val posInCycle = ((dayOffset % cycle) + cycle) % cycle
                if (posInCycle < reminder.daysOn) {
                    result.add(cursor)
                }
            }
            cursor = DateUtils.addDays(cursor, 1)
            guard++
        }
        return result
    }

    /**
     * Мержит сгенерированные даты с записями из БД.
     * Возвращает полный список итераций для отображения.
     */
    fun mergeWithDb(
        reminder: Reminder,
        fromMidnight: Long,
        toMidnight: Long,
        dbIterations: List<Iteration>
    ): List<Iteration> {
        val dates = generateIterationDates(reminder, fromMidnight, toMidnight)
        val dbMap = dbIterations.associateBy { it.dateMillis }

        return dates.map { dateMillis ->
            dbMap[dateMillis] ?: Iteration(
                reminderId = reminder.id,
                dateMillis = dateMillis
            )
        }
    }

    /**
     * Возвращает три итерации для карточки списка:
     * прошлую (последняя строго до сегодня), ближайшую (сегодня или позже),
     * следующую (после ближайшей).
     *
     * Окно поиска — ±60 дней, что с запасом покрывает большинство сценариев.
     */
    fun getThreeIterations(
        reminder: Reminder,
        todayMidnight: Long,
        dbIterations: List<Iteration>
    ): Triple<Iteration?, Iteration?, Iteration?> {
        val from = DateUtils.addDays(todayMidnight, -60)
        val to = DateUtils.addDays(todayMidnight, 60)

        val dates = generateIterationDates(reminder, from, to)
        val dbMap = dbIterations.associateBy { it.dateMillis }

        val past = dates.lastOrNull { it < todayMidnight }
        val future = dates.filter { it >= todayMidnight }
        val nearest = future.getOrNull(0)
        val next = future.getOrNull(1)

        fun wrap(dateMillis: Long?): Iteration? = dateMillis?.let {
            dbMap[it] ?: Iteration(reminderId = reminder.id, dateMillis = it)
        }

        return Triple(wrap(past), wrap(nearest), wrap(next))
    }
}