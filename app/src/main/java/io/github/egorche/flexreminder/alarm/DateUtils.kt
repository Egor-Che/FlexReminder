package io.github.egorche.flexreminder.alarm

import java.util.Calendar

object DateUtils {
    const val DAY_MS = 24L * 60 * 60 * 1000

    fun midnight(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun endOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis

    fun atTime(dateMillis: Long, hour: Int, minute: Int): Long = Calendar.getInstance().apply {
        timeInMillis = dateMillis
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun addDays(millis: Long, delta: Int): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        add(Calendar.DAY_OF_YEAR, delta)
    }.timeInMillis

    fun daysBetween(fromMidnight: Long, toMidnight: Long): Int {
        return ((toMidnight - fromMidnight) / DAY_MS).toInt()
    }

    /** Сериализует набор дат (миллисекунды полуночи) в строку через запятую. */
    fun encodeDates(dates: Set<Long>): String = dates.sorted().joinToString(",")

    fun parseDates(s: String): Set<Long> {
        if (s.isBlank()) return emptySet()
        return s.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
    }
}