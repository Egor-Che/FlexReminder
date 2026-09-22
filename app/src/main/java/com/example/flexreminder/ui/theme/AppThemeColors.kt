package com.example.flexreminder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.flexreminder.data.AppTheme

object AppThemeColors {

    // ---------- Статусные цвета: Палитра ----------

    val CompletedPalette = Color(0xFF5E9E62)
    val SkippedUserPalette = Color(0xFFD9B461)
    val SkippedSystemPalette = Color(0xFFC86A6A)

    // ---------- Статусные цвета: Монохром ----------

    val CompletedMonochrome = Color(0xFF7A947A)
    val SkippedUserMonochrome = Color(0xFFAD9A76)
    val SkippedSystemMonochrome = Color(0xFF9C7A7A)

    // ---------- Монохром: серые ----------

    val MonoCardEven = Color(0xFFE8E8E8)
    val MonoCardOdd = Color(0xFFD8D8D8)
    val MonoAccentOff = Color(0xFFBFBFBF)
    val MonoAccentOn = Color(0xFF4A4A4A)
    val MonoScreenBg = Color(0xFFECECEC)
    val MonoCalendarHighlight = Color(0xFF6E6E6E)

    /** Нейтральный серый — используется как акцент, пока цвет не выбран. */
    val NeutralAccent = Color(0xFF6E6E6E)

    // ---------- Fallback по теме ----------

    @Composable
    fun completed(theme: AppTheme): Color =
        if (theme == AppTheme.MONOCHROME) CompletedMonochrome else CompletedPalette

    @Composable
    fun skippedUser(theme: AppTheme): Color =
        if (theme == AppTheme.MONOCHROME) SkippedUserMonochrome else SkippedUserPalette

    @Composable
    fun skippedSystem(theme: AppTheme): Color =
        if (theme == AppTheme.MONOCHROME) SkippedSystemMonochrome else SkippedSystemPalette

    // ---------- Акцентный цвет ----------

    /**
     * Акцент для элементов напоминания (свитчи, карточка, кнопка «Отметить»).
     *  - Монохром → тёмно-серый.
     *  - Палитра: colorIndex задан → цвет напоминания, иначе → нейтральный серый.
     */
    @Composable
    fun accent(
        theme: AppTheme,
        colorIndex: Int?
    ): Color = when (theme) {
        AppTheme.MONOCHROME -> MonoAccentOn
        AppTheme.PALETTE -> ReminderPalette.get(colorIndex)?.color ?: NeutralAccent
    }

    /**
     * Акцент для редактора (Tab, кнопки, свитчи).
     * Пока пользователь не выбрал цвет — нейтральный серый, чтобы не было
     * дефолтного фиолетового Material.
     */
    @Composable
    fun editorAccent(theme: AppTheme, colorIndex: Int?): Color = when (theme) {
        AppTheme.MONOCHROME -> MonoAccentOn
        AppTheme.PALETTE -> ReminderPalette.get(colorIndex)?.color ?: NeutralAccent
    }

    // ---------- Фон карточки в списке ----------

    @Composable
    fun cardBackground(
        theme: AppTheme,
        colorIndex: Int?,
        itemIndex: Int
    ): Color = when (theme) {
        AppTheme.MONOCHROME -> if (itemIndex % 2 == 0) MonoCardEven else MonoCardOdd
        AppTheme.PALETTE -> {
            val base = ReminderPalette.get(colorIndex)?.color
            base?.copy(alpha = 0.28f) ?: MaterialTheme.colorScheme.surface
        }
    }

    // ---------- Фон экранов ----------

    @Composable
    fun screenBackground(
        theme: AppTheme,
        colorIndex: Int?
    ): Color = when (theme) {
        AppTheme.MONOCHROME -> MonoScreenBg
        AppTheme.PALETTE -> {
            val base = ReminderPalette.get(colorIndex)?.color
            base?.copy(alpha = 0.15f)
                ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        }
    }

    @Composable
    fun viewCardBackground(
        theme: AppTheme,
        colorIndex: Int?
    ): Color = when (theme) {
        AppTheme.MONOCHROME -> MonoCardEven
        AppTheme.PALETTE -> {
            val base = ReminderPalette.get(colorIndex)?.color
            base?.copy(alpha = 0.22f) ?: MaterialTheme.colorScheme.surface
        }
    }

    // ---------- Подсветка календаря ----------

    @Composable
    fun calendarHighlight(
        theme: AppTheme,
        colorIndex: Int?
    ): Color = when (theme) {
        AppTheme.MONOCHROME -> MonoCalendarHighlight
        AppTheme.PALETTE -> {
            val base = ReminderPalette.get(colorIndex)?.color
            base?.let {
                Color(
                    red = it.red * 0.7f,
                    green = it.green * 0.7f,
                    blue = it.blue * 0.7f,
                    alpha = 1f
                )
            } ?: NeutralAccent
        }
    }

    // ---------- Текст ----------

    @Composable
    fun cardContentColor(theme: AppTheme): Color = when (theme) {
        AppTheme.MONOCHROME -> Color(0xFF1A1A1A)
        AppTheme.PALETTE -> MaterialTheme.colorScheme.onSurface
    }

    @Composable
    fun cardContentColorSecondary(theme: AppTheme): Color = when (theme) {
        AppTheme.MONOCHROME -> Color(0xFF4D4D4D)
        AppTheme.PALETTE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    @Composable
    fun onAccentColor(): Color = Color.White

    @Composable
    fun dividerColor(theme: AppTheme): Color = when (theme) {
        AppTheme.MONOCHROME -> Color(0xFFBFBFBF)
        AppTheme.PALETTE -> MaterialTheme.colorScheme.outlineVariant
    }
}