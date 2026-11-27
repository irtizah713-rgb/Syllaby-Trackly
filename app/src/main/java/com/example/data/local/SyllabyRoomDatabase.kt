package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cached_subjects")
data class CachedSubjectEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val colorHex: String,
    val iconKey: String,
    val sortOrder: Long,
    val examDateMillis: Long?,
    val targetChapters: Long
)

@Entity(tableName = "cached_chapters")
data class CachedChapterEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val subjectId: String,
    val title: String,
    val description: String?,
    val personalNotes: String?,
    val topicsCsv: String?,
    val status: String,
    val sortOrder: Long,
    val completedAtMillis: Long?
)

@Entity(tableName = "cached_tasks")
data class CachedTaskEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val description: String?,
    val priority: String,
    val category: String,
    val tagsCsv: String,
    val subjectId: String?,
    val chapterId: String?,
    val dueAtMillis: Long?,
    val reminderEnabled: Boolean,
    val completed: Boolean,
    val completedAtMillis: Long?
)

@Entity(tableName = "cached_notes")
data class CachedNoteEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val content: String,
    val subjectId: String?,
    val chapterId: String?,
    val pinned: Boolean,
    val favorite: Boolean,
    val updatedAtMillis: Long
)

@Entity(tableName = "cached_focus_sessions")
data class CachedFocusSessionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val subjectId: String?,
    val subjectName: String?,
    val durationMinutes: Long,
    val breakMinutes: Long,
    val modeLabel: String,
    val ambientSound: String,
    val completed: Boolean,
    val startedAtMillis: Long,
    val endedAtMillis: Long
)

@Entity(tableName = "cached_attendance")
data class CachedAttendanceEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val dateKey: String,
    val subjectId: String,
    val status: String,
    val note: String?
)

@Entity(tableName = "notification_logs")
data class NotificationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userId: String,
    val title: String,
    val body: String,
    val category: String,
    val read: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Dao
interface SyllabyCacheDao {
    @Query("SELECT * FROM cached_subjects WHERE userId = :userId ORDER BY sortOrder ASC")
    fun observeSubjects(userId: String): Flow<List<CachedSubjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubjects(items: List<CachedSubjectEntity>)

    @Query("DELETE FROM cached_subjects WHERE userId = :userId")
    suspend fun clearSubjectsForUser(userId: String)

    @Query("SELECT * FROM cached_chapters WHERE userId = :userId ORDER BY sortOrder ASC")
    fun observeChapters(userId: String): Flow<List<CachedChapterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChapters(items: List<CachedChapterEntity>)

    @Query("DELETE FROM cached_chapters WHERE userId = :userId")
    suspend fun clearChaptersForUser(userId: String)

    @Query("SELECT * FROM cached_tasks WHERE userId = :userId ORDER BY completed ASC, dueAtMillis ASC")
    fun observeTasks(userId: String): Flow<List<CachedTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(items: List<CachedTaskEntity>)

    @Query("DELETE FROM cached_tasks WHERE userId = :userId")
    suspend fun clearTasksForUser(userId: String)

    @Query("SELECT * FROM cached_notes WHERE userId = :userId ORDER BY pinned DESC, updatedAtMillis DESC")
    fun observeNotes(userId: String): Flow<List<CachedNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotes(items: List<CachedNoteEntity>)

    @Query("DELETE FROM cached_notes WHERE userId = :userId")
    suspend fun clearNotesForUser(userId: String)

    @Query("SELECT * FROM cached_focus_sessions WHERE userId = :userId ORDER BY endedAtMillis DESC")
    fun observeFocusSessions(userId: String): Flow<List<CachedFocusSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFocusSessions(items: List<CachedFocusSessionEntity>)

    @Query("DELETE FROM cached_focus_sessions WHERE userId = :userId")
    suspend fun clearFocusForUser(userId: String)

    @Query("SELECT * FROM cached_attendance WHERE userId = :userId ORDER BY dateKey DESC")
    fun observeAttendance(userId: String): Flow<List<CachedAttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendance(items: List<CachedAttendanceEntity>)

    @Query("DELETE FROM cached_attendance WHERE userId = :userId")
    suspend fun clearAttendanceForUser(userId: String)

    @Query("SELECT * FROM notification_logs WHERE userId = :userId ORDER BY createdAtMillis DESC LIMIT 100")
    fun observeNotifications(userId: String): Flow<List<NotificationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(item: NotificationLogEntity)

    @Query("UPDATE notification_logs SET read = 1 WHERE userId = :userId")
    suspend fun markAllNotificationsRead(userId: String)

    @Query("DELETE FROM notification_logs WHERE userId = :userId")
    suspend fun clearNotifications(userId: String)
}

@Database(
    entities = [
        CachedSubjectEntity::class,
        CachedChapterEntity::class,
        CachedTaskEntity::class,
        CachedNoteEntity::class,
        CachedFocusSessionEntity::class,
        CachedAttendanceEntity::class,
        NotificationLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SyllabyRoomDatabase : RoomDatabase() {
    abstract fun cacheDao(): SyllabyCacheDao

    companion object {
        @Volatile
        private var INSTANCE: SyllabyRoomDatabase? = null

        fun getInstance(context: Context): SyllabyRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SyllabyRoomDatabase::class.java,
                    "syllaby_trackly_local.db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
