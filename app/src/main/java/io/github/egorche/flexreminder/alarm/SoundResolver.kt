package io.github.egorche.flexreminder.alarm

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import io.github.egorche.flexreminder.R
import io.github.egorche.flexreminder.data.Reminder

object SoundResolver {

    /**
     * URI встроенной мелодии приложения (res/raw/default_ringtone.ogg).
     *
     * Используем формат android.resource://, чтобы система корректно
     * находила файл в ресурсах приложения.
     */
    fun getBuiltinSoundUri(context: Context): String =
        "android.resource://${context.packageName}/${R.raw.default_ringtone}"

    fun isUriValid(context: Context, uri: String?): Boolean {
        if (uri.isNullOrBlank()) return false
        return try {
            context.contentResolver.openInputStream(Uri.parse(uri))?.use { true } ?: false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Возвращает URI звука для напоминания с учётом валидности.
     * null означает: использовать встроенную мелодию приложения.
     */
    fun resolveSoundUri(
        context: Context,
        reminder: Reminder,
        globalUri: String?
    ): String? {
        val reminderUri = reminder.soundUri
        if (!reminderUri.isNullOrBlank() && isUriValid(context, reminderUri)) {
            return reminderUri
        }
        if (!globalUri.isNullOrBlank() && isUriValid(context, globalUri)) {
            return globalUri
        }
        return null
    }

    /**
     * Человекочитаемое имя трека по URI.
     * Для null возвращает «Мелодия приложения».
     */
    fun getTrackName(context: Context, uri: String?): String {
        if (uri.isNullOrBlank()) return "Мелодия приложения"
        return try {
            val ringtone = RingtoneManager.getRingtone(context, Uri.parse(uri))
            ringtone?.getTitle(context) ?: "Мелодия приложения"
        } catch (_: Exception) {
            "Мелодия приложения"
        }
    }

    /**
     * Список системных звуков указанной категории.
     * Дедупликация по имени: RingtoneManager на многих устройствах возвращает
     * один и тот же звук дважды — из системной базы и из MediaStore.
     */
    fun listSystemSounds(context: Context, type: Int): List<SystemSound> {
        val result = mutableListOf<SystemSound>()
        try {
            val manager = RingtoneManager(context)
            manager.setType(type)
            val cursor = manager.cursor ?: return emptyList()

            val seenNames = mutableSetOf<String>()

            while (cursor.moveToNext()) {
                val uri = manager.getRingtoneUri(cursor.position) ?: continue
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX) ?: continue

                val normalized = title.trim().lowercase()
                if (!seenNames.add(normalized)) continue

                result.add(SystemSound(uri.toString(), title, type))
            }
        } catch (_: Exception) {
            // если не удалось получить список — возвращаем пустой
        }
        return result
    }

    data class SystemSound(
        val uri: String,
        val title: String,
        val type: Int
    )
}