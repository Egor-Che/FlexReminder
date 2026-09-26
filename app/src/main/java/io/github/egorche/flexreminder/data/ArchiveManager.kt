package io.github.egorche.flexreminder.data

import io.github.egorche.flexreminder.alarm.DateUtils

/**
 * Логика автоматической архивации напоминаний с истёкшим endDate.
 *
 * Вызывается:
 *  - при старте приложения (MainActivity);
 *  - при перезагрузке устройства (BootReceiver).
 *
 * Перед архивацией закрывает все PENDING-итерации архивных напоминаний
 * как SKIPPED (SYSTEM), чтобы в архиве не оставалось «висящих» записей.
 */
object ArchiveManager {

    /**
     * Архивирует все напоминания, у которых endDate < сегодня и archivedAt IS NULL.
     * Возвращает количество заархивированных напоминаний.
     */
    suspend fun archiveExpired(db: AppDatabase): Int {
        val reminderDao = db.reminderDao()
        val iterationDao = db.iterationDao()

        val now = System.currentTimeMillis()
        val todayMidnight = DateUtils.midnight(now)

        // 1. Находим ID напоминаний, которые подлежат архивации
        val idsToArchive = reminderDao.getIdsToArchive(todayMidnight)
        if (idsToArchive.isEmpty()) return 0

        // 2. Для каждого закрываем pending-итерации
        idsToArchive.forEach { id ->
            iterationDao.markPendingAsSkippedForReminder(id, now)
        }

        // 3. Архивируем сами напоминания
        return reminderDao.archiveExpired(now, todayMidnight)
    }
}