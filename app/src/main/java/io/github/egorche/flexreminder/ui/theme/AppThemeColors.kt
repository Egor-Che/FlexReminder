package io.github.egorche.flexreminder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.egorche.flexreminder.data.AppTheme

object AppThemeColors {

    // ---------- Статусные цвета: Палитра ----------

    val CompletedPalette = Color(0xFF5E9E62)
    val SkippedUserPalette = Color(0xFFD9B461)
    val SkippedSystemPalette = Color(0xFFC86A6A)

    // ---------- Статусные цвета: Монохром ----------

    val CompletedMonochrome = Color(0xFF7A947A)
    val SkippedUserMonochrome = Color(0xFFAD9A76)
    val SkippedSystemMonochrome = Color(0xFF9C7A7A)

    // ---------- Статусные цвета: Тёмная ----------
    // Чуть светлее/ярче, чтобы читались на тёмном фоне

    val CompletedDark = Color(0xFF7CB380)
    val SkippedUserDark = Color(0xFFE0C06E)
    val SkippedSystemDark = Color(0xFFD67A7A)

    // ---------- Монохром: серые ----------

    val MonoCardEven = Color(0xFFE8E8E8)
    val MonoCardOdd = Color(0xFFD8D8D8)
    val MonoAccentOff = Color(0xFFBFBFBF)
    val MonoAccentOn = Color(0xFF4A4A4A)
    val MonoScreenBg = Color(0xFFECECEC)
    val MonoCalendarHighlight = Color(0xFF6E6E6E)

    // ---------- Тёмная: серые ----------

    val DarkCardEven = Color(0xFF1E1E1E)
    val DarkCardOdd = Color(0xFF252525)
    val DarkAccent = Color(0xFF909090)
    val DarkScreenBg = Color(0xFF121212)
    val DarkCalendarHighlight = Color(0xFF6E6E6E)
    val DarkTextPrimary = Color(0xFFE0E0E0)
    val DarkTextSecondary = Color(0xFFA0A0A0)
    val DarkDivider = Color(0xFF404040)

    /** Нейтральный серый — используется как акцент, пока цвет не выбран. */
    val NeutralAccent = Color(0xFF6E6E6E)

    // ---------- Fallback по теме ----------

    @Composable
    fun completed(theme: AppTheme): Color = when (theme) {
        AppTheme.MONOCHROME -> CompletedMonochrome
        AppTheme.PALETTE -> CompletedPalette
        AppTheme.DARK -> CompletedDark
    }

    @Composable
    fun skippedUser(theme: AppTheme): Color = when (theme) {
        AppTheme.MONOCHROME -> SkippedUserMonochrome
        AppTheme.PALETTE -> SkippedUserPalette
        AppTheme.DARK -> SkippedUserDark
    }

    @Composable
    fun skippedSystem(theme: AppTheme): Color = when (theme) {
        AppTheme.MONOCHROME -> SkippedSystemMonochrome
        AppTheme.PALETTE -> SkippedSystemPalette
        AppTheme.DARK -> SkippedSystemDark
    }

    // ---------- Акцентный цвет ----------

    /**
     * Акцент для элементов напоминания (свитчи, карточка, кнопка «Отметить»).
     *  - Монохром → тёмно-серый.
     *  - Палитра: colorIndex задан → цвет напоминания, иначе → нейтральный серый.
     *  - Тёмная → светло-серый.
     */
    @Composable
    fun accent(
        theme: AppTheme,
        colorIndex: Int?
    ): Color = when (theme) {
        AppTheme.MONOCHROME -> MonoAccentOn
        AppTheme.PALETTE -> ReminderPalette.get(colorIndex)?.color ?: NeutralAccent
        AppTheme.DARK -> DarkAccent
    }

    /**
     * Акцент для редактора (Tab, кнопки, свитчи).
     */
    @Composable
    fun editorAccent(theme: AppTheme, colorIndex: Int?): Color = when (theme) {
        AppTheme.MONOCHROME -> MonoAccentOn
        AppTheme.PALETTE -> ReminderPalette.get(colorIndex)?.color ?: NeutralAccent
        AppTheme.DARK -> DarkAccent
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
        AppTheme.DARK -> if (itemIndex % 2 == 0) DarkCardEven else DarkCardOdd
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
        AppTheme.DARK -> DarkScreenBg
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
        AppTheme.DARK -> DarkCardEven
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
        AppTheme.DARK -> DarkCalendarHighlight
    }

    // ---------- Текст ----------

    @Composable
    fun cardContentColor(theme: AppTheme): Color = when (theme) {
        AppTheme.MONOCHROME -> Color(0xFF1A1A1A)
        AppTheme.PALETTE -> MaterialTheme.colorScheme.onSurface
        AppTheme.DARK -> DarkTextPrimary
    }

    @Composable
    fun cardContentColorSecondary(theme: AppTheme): Color = when (theme) {
        AppTheme.MONOCHROME -> Color(0xFF4D4D4D)
        AppTheme.PALETTE -> MaterialTheme.colorScheme.onSurfaceVariant
        AppTheme.DARK -> DarkTextSecondary
    }

    @Composable
    fun onAccentColor(): Color = Color.White

    @Composable
    fun dividerColor(theme: AppTheme): Color = when (theme) {
        AppTheme.MONOCHROME -> Color(0xFFBFBFBF)
        AppTheme.PALETTE -> MaterialTheme.colorScheme.outlineVariant
        AppTheme.DARK -> DarkDivider
    }
}