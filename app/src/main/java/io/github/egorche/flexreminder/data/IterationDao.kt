package io.github.egorche.flexreminder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface IterationDao {

    @Query("SELECT * FROM iterations ORDER BY dateMillis ASC")
    fun observeAll(): Flow<List<Iteration>>

    @Query("SELECT * FROM iterations WHERE reminderId = :reminderId ORDER BY dateMillis ASC")
    fun observeByReminder(reminderId: Long): Flow<List<Iteration>>

    @Query("SELECT * FROM iterations WHERE reminderId = :reminderId ORDER BY dateMillis ASC")
    suspend fun getByReminder(reminderId: Long): List<Iteration>

    @Query("SELECT * FROM iterations WHERE reminderId = :reminderId AND dateMillis = :dateMillis LIMIT 1")
    suspend fun get(reminderId: Long, dateMillis: Long): Iteration?

    @Query(
        "SELECT * FROM iterations " +
                "WHERE reminderId = :reminderId AND dateMillis BETWEEN :from AND :to " +
                "ORDER BY dateMillis ASC"
    )
    suspend fun getInRange(reminderId: Long, from: Long, to: Long): List<Iteration>

    @Query(
        "SELECT * FROM iterations " +
                "WHERE status = 'PENDING' AND dateMillis < :boundaryMillis " +
                "ORDER BY dateMillis ASC"
    )
    suspend fun getPendingBefore(boundaryMillis: Long): List<Iteration>

    @Query("SELECT * FROM iterations WHERE snoozeUntil IS NOT NULL")
    suspend fun getAllWithActiveSnooze(): List<Iteration>

    /**
     * Помечает все PENDING-итерации напоминания как SKIPPED (SYSTEM).
     * Используется при архивации, чтобы закрыть оставшиеся «висящие» итерации.
     * Возвращает количество обновлённых записей.
     */
    @Query(
        "UPDATE iterations " +
                "SET status = 'SKIPPED', " +
                "    statusSource = 'SYSTEM', " +
                "    statusChangedAt = :now, " +
                "    snoozeUntil = NULL " +
                "WHERE reminderId = :reminderId " +
                "AND status = 'PENDING'"
    )
    suspend fun markPendingAsSkippedForReminder(reminderId: Long, now: Long): Int

    @Insert
    suspend fun insert(iteration: Iteration): Long

    @Update
    suspend fun update(iteration: Iteration)

    @Query("DELETE FROM iterations WHERE reminderId = :reminderId AND dateMillis = :dateMillis")
    suspend fun delete(reminderId: Long, dateMillis: Long)

    @Query("DELETE FROM iterations WHERE reminderId = :reminderId")
    suspend fun deleteByReminder(reminderId: Long)

    @Transaction
    suspend fun upsertByDate(iteration: Iteration): Long {
        val existing = get(iteration.reminderId, iteration.dateMillis)
        return if (existing != null) {
            update(iteration.copy(id = existing.id))
            existing.id
        } else {
            insert(iteration)
        }
    }

    @Transaction
    suspend fun upsertAll(iterations: List<Iteration>) {
        iterations.forEach { upsertByDate(it) }
    }
}