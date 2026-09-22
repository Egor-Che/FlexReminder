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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flexreminder.data.Iteration
import com.example.flexreminder.data.IterationStatus
import com.example.flexreminder.data.StatusSource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormatRu = SimpleDateFormat("dd.MM.yyyy, EEE", Locale("ru"))
private val timeFormatRu = SimpleDateFormat("HH:mm", Locale.getDefault())

fun formatIterationDate(millis: Long): String = dateFormatRu.format(Date(millis))

@Composable
fun IterationRow(
    iteration: Iteration,
    hour: Int,
    minute: Int,
    isNearest: Boolean,
    onClickComplete: () -> Unit,
    onClickSkip: () -> Unit
) {
    val status = iteration.status
    val source = iteration.statusSource

    val rowBg = if (isNearest)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    else
        MaterialTheme.colorScheme.surface

    val borderMod = if (isNearest) {
        Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
    } else Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(rowBg)
            .then(borderMod)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatIterationDate(iteration.dateMillis),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isNearest) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = "${String.format(Locale.getDefault(), "%02d:%02d", hour, minute)}" +
                        statusLabelSuffix(status, source),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        StatusIconButton(
            isActive = status == IterationStatus.COMPLETED,
            color = CompletedColor,
            contentDescription = "Выполнено",
            icon = { Icon(Icons.Default.Check, contentDescription = null, tint = Color.White) },
            onClick = onClickComplete
        )
        Spacer(Modifier.width(8.dp))
        StatusIconButton(
            isActive = status == IterationStatus.SKIPPED,
            color = skippedColor(source),
            contentDescription = "Пропустить",
            icon = { Icon(Icons.Default.Close, contentDescription = null, tint = Color.White) },
            onClick = onClickSkip
        )
    }
}

private fun statusLabelSuffix(status: IterationStatus, source: StatusSource?): String = when {
    status == IterationStatus.COMPLETED -> "  ✅ Выполнено"
    status == IterationStatus.SKIPPED && source == StatusSource.USER -> "  ⏭️ Пропущено (вами)"
    status == IterationStatus.SKIPPED && source == StatusSource.SYSTEM -> "  ❌ Пропущено"
    else -> ""
}

val CompletedColor = Color(0xFF2E7D32)          // Насыщенный зелёный
val SkippedUserColor = Color(0xFFE6A100)        // Насыщенный янтарный
val SkippedSystemColor = Color(0xFFC62828)      // Насыщенный красный

fun skippedColor(source: StatusSource?): Color =
    if (source == StatusSource.SYSTEM) SkippedSystemColor else SkippedUserColor

@Composable
private fun StatusIconButton(
    isActive: Boolean,
    color: Color,
    contentDescription: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (isActive) color else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (isActive) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isActive) {
            icon()
        }
    }
}