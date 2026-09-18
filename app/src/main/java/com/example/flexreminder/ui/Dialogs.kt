package com.example.flexreminder.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar

@Composable
fun DatePickerDialogWrapper(
    show: Boolean,
    initialMillis: Long,
    onDismiss: () -> Unit,
    onPicked: (Long) -> Unit
) {
    if (!show) return
    val ctx = LocalContext.current
    DisposableEffect(show, initialMillis) {
        val cal = Calendar.getInstance().apply { timeInMillis = initialMillis }
        val dlg = DatePickerDialog(
            ctx,
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
    onDismiss: () -> Unit,
    onPicked: (Int, Int) -> Unit
) {
    if (!show) return
    val ctx = LocalContext.current
    DisposableEffect(show, hour, minute) {
        val dlg = TimePickerDialog(
            ctx,
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