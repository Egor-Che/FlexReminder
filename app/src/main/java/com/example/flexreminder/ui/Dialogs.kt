package com.example.flexreminder.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.example.flexreminder.R
import com.example.flexreminder.data.AppTheme
import java.util.Calendar

@Composable
fun DatePickerDialogWrapper(
    show: Boolean,
    initialMillis: Long,
    theme: AppTheme,
    colorIndex: Int?,
    onDismiss: () -> Unit,
    onPicked: (Long) -> Unit
) {
    if (!show) return
    val baseCtx = LocalContext.current
    val themedCtx = ContextThemeWrapper(baseCtx, themeRes(theme, colorIndex))
    DisposableEffect(show, initialMillis, theme, colorIndex) {
        val cal = Calendar.getInstance().apply { timeInMillis = initialMillis }
        val dlg = DatePickerDialog(
            themedCtx,
            { _, y, m, d ->
                val c = Calendar.getInstance().apply {
                    set(Calendar.YEAR, y)
                    set(Calendar.MONTH, m)
                    set(Calendar.DAY_OF_MONTH, d)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onPicked(c.timeInMillis)
                onDismiss()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        dlg.setOnCancelListener { onDismiss() }
        dlg.show()
        onDispose { dlg.dismiss() }
    }
}

@Composable
fun TimePickerDialogWrapper(
    show: Boolean,
    hour: Int,
    minute: Int,
    theme: AppTheme,
    colorIndex: Int?,
    onDismiss: () -> Unit,
    onPicked: (Int, Int) -> Unit
) {
    if (!show) return
    val baseCtx = LocalContext.current
    val themedCtx = ContextThemeWrapper(baseCtx, themeRes(theme, colorIndex))
    DisposableEffect(show, hour, minute, theme, colorIndex) {
        val dlg = TimePickerDialog(
            themedCtx,
            { _, h, m ->
                onPicked(h, m)
                onDismiss()
            },
            hour,
            minute,
            true
        )
        dlg.setOnCancelListener { onDismiss() }
        dlg.show()
        onDispose { dlg.dismiss() }
    }
}

private fun themeRes(theme: AppTheme, colorIndex: Int?): Int = when (theme) {
    AppTheme.MONOCHROME -> R.style.DialogThemeMonochrome
    AppTheme.PALETTE -> when (colorIndex) {
        null -> R.style.DialogThemeNeutral
        0 -> R.style.DialogThemeColor1
        1 -> R.style.DialogThemeColor2
        2 -> R.style.DialogThemeColor3
        3 -> R.style.DialogThemeColor4
        4 -> R.style.DialogThemeColor5
        5 -> R.style.DialogThemeColor6
        6 -> R.style.DialogThemeColor7
        7 -> R.style.DialogThemeColor8
        8 -> R.style.DialogThemeColor9
        9 -> R.style.DialogThemeColor10
        10 -> R.style.DialogThemeColor11
        11 -> R.style.DialogThemeColor12
        12 -> R.style.DialogThemeColor13
        13 -> R.style.DialogThemeColor14
        14 -> R.style.DialogThemeColor15
        15 -> R.style.DialogThemeColor16
        else -> R.style.DialogThemeNeutral
    }
}