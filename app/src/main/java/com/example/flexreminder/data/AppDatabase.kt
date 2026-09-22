package com.example.flexreminder.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Converters {
    @TypeConverter
    fun fromLongSet(value: Set<Long>?): String {
        if (value.isNullOrEmpty()) return ""
        return value.sorted().joinToString(",")
    }

    @TypeConverter
    fun toLongSet(value: String?): Set<Long> {
        if (value.isNullOrBlank()) return emptySet()
        return value.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
    }

    @TypeConverter
    fun fromScheduleMode(mode: ScheduleMode?): String = (mode ?: ScheduleMode.INTERVAL).name

    @TypeConverter
    fun toScheduleMode(value: String?): ScheduleMode {
        return try {
            ScheduleMode.valueOf(value ?: ScheduleMode.INTERVAL.name)
        } catch (_: Exception) {
            ScheduleMode.INTERVAL
        }
    }

    @TypeConverter
    fun fromIterationStatus(status: IterationStatus?): String =
        (status ?: IterationStatus.PENDING).name

    @TypeConverter
    fun toIterationStatus(value: String?): IterationStatus {
        return try {
            IterationStatus.valueOf(value ?: IterationStatus.PENDING.name)
        } catch (_: Exception) {
            IterationStatus.PENDING
        }
    }

    @TypeConverter
    fun fromStatusSource(source: StatusSource?): String? = source?.name

    @TypeConverter
    fun toStatusSource(value: String?): StatusSource? {
        if (value == null) return null
        return try {
            StatusSource.valueOf(value)
        } catch (_: Exception) {
            null
        }
    }
}

@Database(
    entities = [Reminder::class, Iteration::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao
    abstract fun iterationDao(): IterationDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE reminders_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        startDate INTEGER NOT NULL,
                        endDate INTEGER,
                        daysOn INTEGER NOT NULL,
                        daysOff INTEGER NOT NULL,
                        hour INTEGER NOT NULL,
                        minute INTEGER NOT NULL,
                        enabled INTEGER NOT NULL,
                        silent INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO reminders_new
                        (id, title, notes, startDate, endDate, daysOn, daysOff, hour, minute, enabled, silent)
                    SELECT
                        id, title, notes, startDate, endDate,
                        1, MAX(0, intervalDays - 1),
                        hour, minute, enabled, silent
                    FROM reminders
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE reminders")
                db.execSQL("ALTER TABLE reminders_new RENAME TO reminders")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE reminders ADD COLUMN mode TEXT NOT NULL DEFAULT 'INTERVAL'"
                )
                db.execSQL(
                    "ALTER TABLE reminders ADD COLUMN customDates TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE iterations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        reminderId INTEGER NOT NULL,
                        dateMillis INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        statusSource TEXT,
                        statusChangedAt INTEGER,
                        firedAt INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX index_iterations_reminderId_dateMillis " +
                            "ON iterations (reminderId, dateMillis)"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE iterations ADD COLUMN snoozeCount INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE iterations ADD COLUMN lastSnoozeAt INTEGER"
                )
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "reminders.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}