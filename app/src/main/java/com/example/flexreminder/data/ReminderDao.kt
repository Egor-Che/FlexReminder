package com.example.flexreminder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    /**
     * Активные напоминания (не в архиве). Используется главным экраном.
     */
    @Query(
        "SELECT * FROM reminders " +
                "WHERE archivedAt IS NULL " +
                "ORDER BY startDate ASC, id ASC"
    )
    fun observeAll(): Flow<List<Reminder>>

    /**
     * Архивные напоминания. Сортировка по убыванию archivedAt —
     * недавно заархивированные вверху.
     */
    @Query(
        "SELECT * FROM reminders " +
                "WHERE archivedAt IS NOT NULL " +
                "ORDER BY archivedAt DESC, id DESC"
    )
    fun observeArchived(): Flow<List<Reminder>>

    /**
     * Все активные (не в архиве) напоминания, у которых enabled = true.
     * Используется для перепланирования будильников.
     */
    @Query("SELECT * FROM reminders WHERE enabled = 1 AND archivedAt IS NULL")
    suspend fun getEnabled(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Reminder?

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<Reminder?>

    /**
     * Архивирует все напоминания, у которых endDate истёк и которые
     * ещё не в архиве. Возвращает количество заархивированных.
     */
    @Query(
        "UPDATE reminders " +
                "SET archivedAt = :now " +
                "WHERE endDate IS NOT NULL " +
                "AND endDate < :todayMidnight " +
                "AND archivedAt IS NULL"
    )
    suspend fun archiveExpired(now: Long, todayMidnight: Long): Int

    /**
     * ID всех напоминаний, которые подлежат архивации прямо сейчас.
     * Нужно, чтобы перед архивацией закрыть их pending-итерации.
     */
    @Query(
        "SELECT id FROM reminders " +
                "WHERE endDate IS NOT NULL " +
                "AND endDate < :todayMidnight " +
                "AND archivedAt IS NULL"
    )
    suspend fun getIdsToArchive(todayMidnight: Long): List<Long>

    @Insert
    suspend fun insert(r: Reminder): Long

    @Update
    suspend fun update(r: Reminder)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)
}