package io.github.egorche.flexreminder.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Один цвет напоминания.
 *
 * @param hex цвет в ARGB-формате
 * @param name человекочитаемое название (для content description / подсказок)
 */
data class ReminderColor(
    val hex: Long,
    val name: String
) {
    val color: Color get() = Color(hex)
}

/**
 * Фиксированная палитра из 16 пастельных оттенков.
 *
 * Требования:
 *  - Каждый канал RGB в диапазоне [64; 191].
 *  - Порядок в списке — по спектру (от тёплых к холодным).
 *  - Без чёрного, белого, серого, коричневого.
 */
object ReminderPalette {

    val colors: List<ReminderColor> = listOf(
        ReminderColor(0xFFB46E5A, "Терракотовый"),
        ReminderColor(0xFFB97D69, "Коралловый"),
        ReminderColor(0xFFBE8C6E, "Персиковый"),
        ReminderColor(0xFFB9A073, "Песочный"),
        ReminderColor(0xFFB9AF78, "Пшеничный"),
        ReminderColor(0xFFAAB473, "Салатовый"),
        ReminderColor(0xFF91B478, "Фисташковый"),
        ReminderColor(0xFF78B48C, "Мятный"),
        ReminderColor(0xFF73B4AA, "Бирюзовый"),
        ReminderColor(0xFF78AAB9, "Небесный"),
        ReminderColor(0xFF7D9BBE, "Лазурный"),
        ReminderColor(0xFF878CB9, "Дымчато-синий"),
        ReminderColor(0xFF9B8CB9, "Лавандовый"),
        ReminderColor(0xFFAF8CB9, "Сиреневый"),
        ReminderColor(0xFFB98CA5, "Пудрово-розовый"),
        ReminderColor(0xFFBE8C96, "Розовый")
    )

    val size: Int get() = colors.size

    /** Возвращает цвет по индексу или null, если индекс некорректен / null. */
    fun get(index: Int?): ReminderColor? {
        if (index == null) return null
        return colors.getOrNull(index)
    }

    /**
     * Возвращает базовый цвет напоминания или стандартный Material-цвет,
     * если у напоминания нет выбранного цвета.
     */
    fun getOrDefault(index: Int?, default: Color): Color =
        get(index)?.color ?: default
}