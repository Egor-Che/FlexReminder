package com.example.flexreminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.flexreminder.ui.theme.ReminderPalette

/**
 * Сетка 2×8 со скруглёнными квадратиками — палитра из 16 цветов напоминания.
 *
 * Порядок слева направо, сверху вниз — по спектру.
 * Активный цвет обведён рамкой + галочка внутри.
 */
@Composable
fun ColorPalettePicker(
    selectedIndex: Int?,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ReminderPalette.colors
    val firstRow = colors.take(8)
    val secondRow = colors.drop(8)

    Column(modifier = modifier.fillMaxWidth()) {
        PaletteRow(
            items = firstRow,
            startIndex = 0,
            selectedIndex = selectedIndex,
            onPick = onPick
        )
        Spacer(Modifier.height(8.dp))
        PaletteRow(
            items = secondRow,
            startIndex = 8,
            selectedIndex = selectedIndex,
            onPick = onPick
        )
    }
}

@Composable
private fun PaletteRow(
    items: List<com.example.flexreminder.ui.theme.ReminderColor>,
    startIndex: Int,
    selectedIndex: Int?,
    onPick: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        items.forEachIndexed { localIndex, item ->
            val globalIndex = startIndex + localIndex
            ColorSwatch(
                color = item.color,
                selected = selectedIndex == globalIndex,
                onClick = { onPick(globalIndex) }
            )
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val size = 36.dp
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }
    val borderWidth = if (selected) 2.dp else 1.dp

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}