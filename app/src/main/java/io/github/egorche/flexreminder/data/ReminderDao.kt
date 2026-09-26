package io.github.egorche.flexreminder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query(
        "SELECT * FROM reminders " +
                "WHERE archivedAt IS NULL " +
                "ORDER BY startDate ASC, id ASC"
    )
    fun observeAll(): Flow<List<Reminder>>

    @Query(
        "SELECT * FROM reminders " +
                "WHERE archivedAt IS NOT NULL " +
                "ORDER BY archivedAt DESC, id DESC"
    )
    fun observeArchived(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE enabled = 1 AND archivedAt IS NULL")
    suspend fun getEnabled(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Reminder?

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<Reminder?>

    @Query(
        "UPDATE reminders " +
                "SET archivedAt = :now " +
                "WHERE endDate IS NOT NULL " +
                "AND endDate < :todayMidnight " +
                "AND archivedAt IS NULL"
    )
    suspend fun archiveExpired(now: Long, todayMidnight: Long): Int

    @Query(
        "SELECT id FROM reminders " +
                "WHERE endDate IS NOT NULL " +
                "AND endDate < :todayMidnight " +
                "AND archivedAt IS NULL"
    )
    suspend fun getIdsToArchive(todayMidnight: Long): List<Long>

    /**
     * ID всех архивных напоминаний. Нужны, чтобы удалить их итерации перед
     * массовым удалением.
     */
    @Query("SELECT id FROM reminders WHERE archivedAt IS NOT NULL")
    suspend fun getArchivedIds(): List<Long>

    /**
     * Удаляет все архивные напоминания. Возвращает количество удалённых.
     */
    @Query("DELETE FROM reminders WHERE archivedAt IS NOT NULL")
    suspend fun deleteAllArchived(): Int

    @Insert
    suspend fun insert(r: Reminder): Long

    @Update
    suspend fun update(r: Reminder)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)
}