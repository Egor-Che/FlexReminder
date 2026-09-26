package io.github.egorche.flexreminder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.egorche.flexreminder.data.AppTheme

/**
 * Общая тема приложения. В зависимости от выбранной пользователем темы
 * подменяет всю палитру Material 3:
 *
 *  - Монохром → полностью серая палитра.
 *  - Палитра → нейтрально-серая база. Цвет напоминания применяется
 *    точечно через AppThemeColors.accent / editorAccent.
 *
 * Никаких фиолетовых оттенков ни в одной из тем.
 */
@Composable
fun FlexReminderTheme(
    theme: AppTheme,
    content: @Composable () -> Unit
) {
    val colorScheme = when (theme) {
        AppTheme.MONOCHROME -> monochromeScheme
        AppTheme.PALETTE -> paletteScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

private val monochromeScheme = lightColorScheme(
    primary = Color(0xFF4A4A4A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8D8D8),
    onPrimaryContainer = Color(0xFF1A1A1A),

    secondary = Color(0xFF6E6E6E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E0E0),
    onSecondaryContainer = Color(0xFF1A1A1A),

    tertiary = Color(0xFF7A7A7A),
    onTertiary = Color.White,

    background = Color(0xFFECECEC),
    onBackground = Color(0xFF1A1A1A),

    surface = Color(0xFFF5F5F5),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFF4D4D4D),

    outline = Color(0xFF9A9A9A),
    outlineVariant = Color(0xFFBFBFBF),

    error = Color(0xFF9C7A7A),
    onError = Color.White,
    errorContainer = Color(0xFFE0D2D2),
    onErrorContainer = Color(0xFF4D2E2E)
)

private val paletteScheme = lightColorScheme(
    primary = Color(0xFF6E6E6E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E0E0),
    onPrimaryContainer = Color(0xFF1A1A1A),

    secondary = Color(0xFF7D7D7D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECECEC),
    onSecondaryContainer = Color(0xFF1A1A1A),

    tertiary = Color(0xFF878787),
    onTertiary = Color.White,

    background = Color(0xFFF8F8F8),
    onBackground = Color(0xFF1A1A1A),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF4D4D4D),

    outline = Color(0xFF9A9A9A),
    outlineVariant = Color(0xFFD0D0D0),

    error = Color(0xFFC86A6A),
    onError = Color.White,
    errorContainer = Color(0xFFF0D2D2),
    onErrorContainer = Color(0xFF4D2E2E)
)