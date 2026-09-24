package com.example.flexreminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.flexreminder.data.AppTheme
import com.example.flexreminder.ui.theme.AppThemeColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ReminderCalendar(
    selectedDates: Set<Long>,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    theme: AppTheme = AppTheme.PALETTE,
    colorIndex: Int? = null,
    onDateToggle: (Long) -> Unit = {},
    onDateClick: ((Long) -> Unit)? = null,
    initialMonthMillis: Long = System.currentTimeMillis()
) {
    var displayYear by remember {
        mutableIntStateOf(
            Calendar.getInstance().apply { timeInMillis = initialMonthMillis }
                .get(Calendar.YEAR)
        )
    }
    var displayMonth by remember {
        mutableIntStateOf(
            Calendar.getInstance().apply { timeInMillis = initialMonthMillis }
                .get(Calendar.MONTH)
        )
    }

    val monthFormat = remember { SimpleDateFormat("LLLL yyyy", Locale.getDefault()) }
    val monthTitle = remember(displayYear, displayMonth) {
        val raw = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayYear)
            set(Calendar.MONTH, displayMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }.time.let { monthFormat.format(it) }
        raw.replaceFirstChar { it.uppercase() }
    }

    val weekDays = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    val highlightColor = AppThemeColors.calendarHighlight(theme, colorIndex)
    val onHighlightColor = Color.White
    val todayColor = AppThemeColors.accent(theme, colorIndex)
    val textColor = AppThemeColors.cardContentColor(theme)
    val secondaryColor = AppThemeColors.cardContentColorSecondary(theme)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {
                val c = Calendar.getInstance().apply {
                    set(Calendar.YEAR, displayYear)
                    set(Calendar.MONTH, displayMonth)
                    set(Calendar.DAY_OF_MONTH, 1)
                    add(Calendar.MONTH, -1)
                }
                displayYear = c.get(Calendar.YEAR)
                displayMonth = c.get(Calendar.MONTH)
            }) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Предыдущий месяц",
                    tint = textColor
                )
            }
            Text(
                text = monthTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
            IconButton(onClick = {
                val c = Calendar.getInstance().apply {
                    set(Calendar.YEAR, displayYear)
                    set(Calendar.MONTH, displayMonth)
                    set(Calendar.DAY_OF_MONTH, 1)
                    add(Calendar.MONTH, 1)
                }
                displayYear = c.get(Calendar.YEAR)
                displayMonth = c.get(Calendar.MONTH)
            }) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Следующий месяц",
                    tint = textColor
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Row(Modifier.fillMaxWidth()) {
            weekDays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryColor
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        val cells = remember(displayYear, displayMonth) {
            buildMonthCells(displayYear, displayMonth)
        }

        val weeks = cells.chunked(7)
        weeks.forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { dayMillis ->
                    if (dayMillis == null) {
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    } else {
                        val isSelected = selectedDates.contains(dayMillis)
                        DayCell(
                            dayMillis = dayMillis,
                            isSelected = isSelected,
                            readOnly = readOnly,
                            highlightColor = highlightColor,
                            onHighlightColor = onHighlightColor,
                            todayColor = todayColor,
                            defaultTextColor = textColor,
                            onClick = {
                                if (readOnly) {
                                    onDateClick?.invoke(dayMillis)
                                } else {
                                    onDateToggle(dayMillis)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    dayMillis: Long,
    isSelected: Boolean,
    readOnly: Boolean,
    highlightColor: Color,
    onHighlightColor: Color,
    todayColor: Color,
    defaultTextColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cal = remember(dayMillis) {
        Calendar.getInstance().apply { timeInMillis = dayMillis }
    }
    val day = cal.get(Calendar.DAY_OF_MONTH)

    val today = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val isToday = dayMillis == today

    // В режиме readOnly клик разрешён только если подсвечен (активный день)
    val clickable = !readOnly || isSelected

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .then(if (clickable) Modifier.clickable { onClick() } else Modifier)
            .background(
                when {
                    isSelected -> highlightColor
                    else -> Color.Transparent
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            color = when {
                isSelected -> onHighlightColor
                isToday -> todayColor
                else -> defaultTextColor
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun buildMonthCells(year: Int, month: Int): List<Long?> {
    val firstDay = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val firstWeekDayRaw = firstDay.get(Calendar.DAY_OF_WEEK)
    val firstWeekDay = (firstWeekDayRaw + 5) % 7

    val daysInMonth = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH)

    val cells = mutableListOf<Long?>()
    repeat(firstWeekDay) { cells.add(null) }
    for (d in 1..daysInMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, d)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        cells.add(cal.timeInMillis)
    }
    while (cells.size < 42) {
        cells.add(null)
    }
    return cells
}