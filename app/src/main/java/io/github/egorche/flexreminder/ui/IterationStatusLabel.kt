package io.github.egorche.flexreminder.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.egorche.flexreminder.R
import io.github.egorche.flexreminder.data.AppTheme
import io.github.egorche.flexreminder.data.Iteration
import io.github.egorche.flexreminder.data.IterationStatus
import io.github.egorche.flexreminder.data.StatusSource
import io.github.egorche.flexreminder.ui.theme.AppThemeColors

@Composable
fun IterationStatusLabel(
    iteration: Iteration,
    theme: AppTheme,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodySmall,
    iconSize: Int = 14
) {
    val status = iteration.status
    val source = iteration.statusSource

    val data: Triple<String, Color, androidx.compose.ui.graphics.vector.ImageVector> =
        when {
            status == IterationStatus.COMPLETED -> Triple(
                stringResource(R.string.status_completed),
                AppThemeColors.completed(theme),
                Icons.Default.Check
            )
            status == IterationStatus.SKIPPED && source == StatusSource.USER -> Triple(
                stringResource(R.string.status_skipped_user),
                AppThemeColors.skippedUser(theme),
                Icons.Default.SkipNext
            )
            status == IterationStatus.SKIPPED && source == StatusSource.SYSTEM -> Triple(
                stringResource(R.string.status_skipped_system),
                AppThemeColors.skippedSystem(theme),
                Icons.Default.Close
            )
            else -> return
        }

    val (text, color, icon) = data

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(iconSize.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = textStyle,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun iterationStatusColor(
    iteration: Iteration,
    theme: AppTheme
): Color? = when {
    iteration.status == IterationStatus.COMPLETED ->
        AppThemeColors.completed(theme)
    iteration.status == IterationStatus.SKIPPED &&
            iteration.statusSource == StatusSource.USER ->
        AppThemeColors.skippedUser(theme)
    iteration.status == IterationStatus.SKIPPED &&
            iteration.statusSource == StatusSource.SYSTEM ->
        AppThemeColors.skippedSystem(theme)
    else -> null
}