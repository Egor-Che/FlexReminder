package com.example.flexreminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flexreminder.R
import com.example.flexreminder.data.AppTheme
import com.example.flexreminder.data.Iteration
import com.example.flexreminder.data.IterationStatus
import com.example.flexreminder.data.StatusSource
import com.example.flexreminder.ui.theme.AppThemeColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IterationRow(
    iteration: Iteration,
    hour: Int,
    minute: Int,
    isNearest: Boolean,
    theme: AppTheme,
    colorIndex: Int?,
    onClickComplete: () -> Unit,
    onClickSkip: () -> Unit,
    onBlockedBySystem: () -> Unit
) {
    val status = iteration.status
    val source = iteration.statusSource
    val isSystemStatus = status == IterationStatus.SKIPPED && source == StatusSource.SYSTEM

    val datePattern = stringResource(R.string.format_iteration_date)
    val dateFormat = remember(datePattern) {
        SimpleDateFormat(datePattern, Locale.getDefault())
    }
    val dateText = remember(iteration.dateMillis, dateFormat) {
        dateFormat.format(Date(iteration.dateMillis))
    }

    val rowShape = MaterialTheme.shapes.medium

    val rowBg = if (isNearest)
        AppThemeColors.accent(theme, colorIndex).copy(alpha = 0.15f)
    else
        MaterialTheme.colorScheme.surface

    val borderMod = if (isNearest) {
        Modifier.border(
            width = 1.dp,
            color = AppThemeColors.accent(theme, colorIndex),
            shape = rowShape
        )
    } else Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(rowShape)
            .background(rowBg)
            .then(borderMod)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dateText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isNearest) FontWeight.Bold else FontWeight.Normal
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d", hour, minute),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (status != IterationStatus.PENDING) {
                    Spacer(Modifier.width(8.dp))
                    IterationStatusLabel(
                        iteration = iteration,
                        theme = theme
                    )
                }
            }
        }

        StatusIconButton(
            isActive = status == IterationStatus.COMPLETED,
            activeColor = AppThemeColors.completed(theme),
            isCheck = true,
            contentDescription = stringResource(R.string.action_mark_completed),
            onClick = onClickComplete
        )
        Spacer(Modifier.width(8.dp))
        StatusIconButton(
            isActive = status == IterationStatus.SKIPPED,
            activeColor = if (source == StatusSource.SYSTEM)
                AppThemeColors.skippedSystem(theme)
            else
                AppThemeColors.skippedUser(theme),
            isCheck = false,
            contentDescription = stringResource(R.string.action_mark_skipped),
            onClick = {
                if (isSystemStatus) onBlockedBySystem() else onClickSkip()
            }
        )
    }
}

@Composable
private fun StatusIconButton(
    isActive: Boolean,
    activeColor: Color,
    isCheck: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    val icon = if (isCheck) Icons.Default.Check else Icons.Default.Close
    val inactiveColor = MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (isActive) activeColor else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (isActive) activeColor
                else inactiveColor.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) Color.White else inactiveColor.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
    }
}