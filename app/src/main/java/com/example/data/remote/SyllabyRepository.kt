package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.local.CachedAttendanceEntity
import com.example.data.local.CachedChapterEntity
import com.example.data.local.CachedFocusSessionEntity
import com.example.data.local.CachedNoteEntity
import com.example.data.local.CachedSubjectEntity
import com.example.data.local.CachedTaskEntity
import com.example.data.local.SyllabyCacheDao
import com.example.data.model.AchievementUnlockItem
import com.example.data.model.AttendanceItem
import com.example.data.model.AttendanceStatus
import com.example.data.model.ChapterItem
import com.example.data.model.ChapterStatus
import com.example.data.model.FocusSessionItem
import com.example.data.model.GamificationCatalog
import com.example.data.model.NoteItem
import com.example.data.model.RewardTransactionItem
import com.example.data.model.ShopItem
import com.example.data.model.SubjectItem
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.data.model.UserAccount
import com.example.data.model.enumValueOrDefault
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class SyllabyRepository(
    private val db: FirebaseFirestore,
    private val cacheDao: SyllabyCacheDao,
    private val auth: FirebaseAuth = Firebase.auth
) : AcademicSyncService {

    constructor(context: Context, cacheDao: SyllabyCacheDao) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        cacheDao = cacheDao
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    private fun publicProfileDoc(uid: String) = db.collection("publicProfiles").document(uid)

    suspend fun ensureUserInitialized(
        uid: String,
        email: String,
        displayName: String,
        photoUrl: String?
    ): Result<Unit> {
        val path = "users/$uid"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "Signed-in user mismatch." }
            val userSnapshot = userDoc(uid).get().await()
            val safeName = displayName.trim().ifBlank { "Student" }.take(60)
            val safeEmail = email.trim().ifBlank { "student@syllabytrackly.app" }.take(160)

            if (!userSnapshot.exists()) {
                val createMap = mutableMapOf<String, Any?>(
                    "userId" to uid,
                    "email" to safeEmail,
                    "displayName" to safeName,
                    "photoUrl" to photoUrl?.take(500),
                    "xp" to 0L,
                    "level" to 1L,
                    "themePoints" to 50L,
                    "currentStreak" to 0L,
                    "longestStreak" to 0L,
                    "totalFocusMinutes" to 0L,
                    "dailyGoalMinutes" to 60L,
                    "academicSession" to "2026-2027",
                    "selectedFrameId" to "frame_default",
                    "unlockedShopIds" to listOf("frame_default"),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                userDoc(uid).set(createMap).await()
            }

            val pubSnap = publicProfileDoc(uid).get().await()
            if (!pubSnap.exists()) {
                val friendCode = "ST-" + uid.take(6).uppercase(Locale.US)
                val pubMap = mapOf(
                    "userId" to uid,
                    "username" to safeName,
                    "friendCode" to friendCode,
                    "level" to 1L,
                    "xp" to 0L,
                    "rankTitle" to GamificationCatalog.rankForLevel(1L),
                    "currentStreak" to 0L,
                    "totalFocusMinutes" to 0L,
                    "selectedFrameId" to "frame_default",
                    "discoverable" to true,
                    "showStats" to true,
                    "showAchievements" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                publicProfileDoc(uid).set(pubMap).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override fun observeUserAccount(userId: String): Flow<UserAccount?> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId"
        emitAll(
            userDoc(userId).snapshots()
                .map { snap -> snap.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> logFlowError(error, OperationType.GET, path, userId); throw error }
        )
    }

    override fun observeSubjects(userId: String): Flow<List<SubjectItem>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/subjects"
        emitAll(
            userDoc(userId).collection("subjects").snapshots()
                .map { snap ->
                    val items = snap.toObjects(SubjectItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.sortOrder }
                    cacheDao.clearSubjectsForUser(userId)
                    cacheDao.upsertSubjects(items.map {
                        CachedSubjectEntity(it.id, it.userId, it.name, it.colorHex, it.iconKey, it.sortOrder, it.examDateMillis, it.targetChapters)
                    })
                    items
                }
                .catch { error -> logFlowError(error, OperationType.LIST, path, userId); throw error }
        )
    }

    override fun observeChapters(userId: String): Flow<List<ChapterItem>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/chapters"
        emitAll(
            userDoc(userId).collection("chapters").snapshots()
                .map { snap ->
                    val items = snap.toObjects(ChapterItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.sortOrder }
                    cacheDao.clearChaptersForUser(userId)
                    cacheDao.upsertChapters(items.map {
                        CachedChapterEntity(it.id, it.userId, it.subjectId, it.title, it.description, it.personalNotes, it.topicsCsv, it.status, it.sortOrder, it.completedAtMillis)
                    })
                    items
                }
                .catch { error -> logFlowError(error, OperationType.LIST, path, userId); throw error }
        )
    }

    override fun observeTasks(userId: String): Flow<List<TaskItem>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/tasks"
        emitAll(
            userDoc(userId).collection("tasks").snapshots()
                .map { snap ->
                    val items = snap.toObjects(TaskItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareBy<TaskItem> { it.completed }.thenBy { it.dueAtMillis ?: Long.MAX_VALUE })
                    cacheDao.clearTasksForUser(userId)
                    cacheDao.upsertTasks(items.map {
                        CachedTaskEntity(it.id, it.userId, it.title, it.description, it.priority, it.category, it.tags.joinToString(","), it.subjectId, it.chapterId, it.dueAtMillis, it.reminderEnabled, it.completed, it.completedAtMillis)
                    })
                    items
                }
                .catch { error -> logFlowError(error, OperationType.LIST, path, userId); throw error }
        )
    }

    override fun observeNotes(userId: String): Flow<List<NoteItem>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/notes"
        emitAll(
            userDoc(userId).collection("notes").snapshots()
                .map { snap ->
                    val items = snap.toObjects(NoteItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareByDescending<NoteItem> { it.pinned }.thenByDescending { it.updatedAt?.seconds ?: 0L })
                    cacheDao.clearNotesForUser(userId)
                    cacheDao.upsertNotes(items.map {
                        CachedNoteEntity(it.id, it.userId, it.title, it.content, it.subjectId, it.chapterId, it.pinned, it.favorite, (it.updatedAt?.seconds ?: 0L) * 1000L)
                    })
                    items
                }
                .catch { error -> logFlowError(error, OperationType.LIST, path, userId); throw error }
        )
    }

    override fun observeFocusSessions(userId: String): Flow<List<FocusSessionItem>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/focusSessions"
        emitAll(
            userDoc(userId).collection("focusSessions").snapshots()
                .map { snap ->
                    val items = snap.toObjects(FocusSessionItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.endedAtMillis }
                    cacheDao.clearFocusForUser(userId)
                    cacheDao.upsertFocusSessions(items.map {
                        CachedFocusSessionEntity(it.id, it.userId, it.subjectId, it.subjectName, it.durationMinutes, it.breakMinutes, it.modeLabel, it.ambientSound, it.completed, it.startedAtMillis, it.endedAtMillis)
                    })
                    items
                }
                .catch { error -> logFlowError(error, OperationType.LIST, path, userId); throw error }
        )
    }

    override fun observeAttendance(userId: String): Flow<List<AttendanceItem>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/attendance"
        emitAll(
            userDoc(userId).collection("attendance").snapshots()
                .map { snap ->
                    val items = snap.toObjects(AttendanceItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.dateKey }
                    cacheDao.clearAttendanceForUser(userId)
                    cacheDao.upsertAttendance(items.map {
                        CachedAttendanceEntity(it.id, it.userId, it.dateKey, it.subjectId, it.status, it.note)
                    })
                    items
                }
                .catch { error -> logFlowError(error, OperationType.LIST, path, userId); throw error }
        )
    }

    fun observeTransactions(userId: String): Flow<List<RewardTransactionItem>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/transactions"
        emitAll(
            userDoc(userId).collection("transactions").snapshots()
                .map { snap ->
                    snap.toObjects(RewardTransactionItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> logFlowError(error, OperationType.LIST, path, userId); throw error }
        )
    }

    fun observeAchievements(userId: String): Flow<List<AchievementUnlockItem>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/achievements"
        emitAll(
            userDoc(userId).collection("achievements").snapshots()
                .map { snap ->
                    snap.toObjects(AchievementUnlockItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.unlockedAtMillis }
                }
                .catch { error -> logFlowError(error, OperationType.LIST, path, userId); throw error }
        )
    }

    override suspend fun upsertSubject(
        userId: String,
        subjectId: String?,
        name: String,
        colorHex: String,
        iconKey: String,
        sortOrder: Long,
        examDateMillis: Long?,
        targetChapters: Long
    ): Result<String> {
        val cleanName = name.trim().take(80)
        if (cleanName.isBlank()) return Result.failure(IllegalArgumentException("Subject name cannot be empty."))
        val docId = subjectId?.takeIf { it.isNotBlank() } ?: ("sub_" + UUID.randomUUID().toString().replace("-", "").take(16))
        val path = "users/$userId/subjects/$docId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = userDoc(uid).collection("subjects").document(docId)
            if (subjectId.isNullOrBlank()) {
                val payload = mapOf<String, Any?>(
                    "userId" to uid,
                    "name" to cleanName,
                    "colorHex" to colorHex.ifBlank { "#38BDF8" }.take(12),
                    "iconKey" to iconKey.ifBlank { "book" }.take(40),
                    "sortOrder" to sortOrder.coerceIn(0L, 10000L),
                    "examDateMillis" to examDateMillis?.takeIf { it >= 0L },
                    "targetChapters" to targetChapters.coerceIn(0L, 500L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                val updateMap = mutableMapOf<String, Any>(
                    "name" to cleanName,
                    "colorHex" to colorHex.ifBlank { "#38BDF8" }.take(12),
                    "iconKey" to iconKey.ifBlank { "book" }.take(40),
                    "sortOrder" to sortOrder.coerceIn(0L, 10000L),
                    "targetChapters" to targetChapters.coerceIn(0L, 500L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (examDateMillis != null && examDateMillis >= 0L) {
                    updateMap["examDateMillis"] = examDateMillis
                }
                docRef.update(updateMap).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun deleteSubject(userId: String, subjectId: String): Result<Unit> {
        val path = "users/$userId/subjects/$subjectId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            // Also clean up chapters belonging to this subject
            val chapters = userDoc(uid).collection("chapters")
                .whereEqualTo("subjectId", subjectId)
                .get().await()
            val batch = db.batch()
            chapters.documents.forEach { batch.delete(it.reference) }
            batch.delete(userDoc(uid).collection("subjects").document(subjectId))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    override suspend fun upsertChapter(
        userId: String,
        chapterId: String?,
        subjectId: String,
        title: String,
        description: String?,
        personalNotes: String?,
        topicsCsv: String?,
        status: ChapterStatus,
        sortOrder: Long
    ): Result<String> {
        val cleanTitle = title.trim().take(120)
        if (cleanTitle.isBlank()) return Result.failure(IllegalArgumentException("Chapter title cannot be empty."))
        val docId = chapterId?.takeIf { it.isNotBlank() } ?: ("ch_" + UUID.randomUUID().toString().replace("-", "").take(16))
        val path = "users/$userId/chapters/$docId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = userDoc(uid).collection("chapters").document(docId)
            val completedMillis = if (status.countsTowardProgress) System.currentTimeMillis() else null
            if (chapterId.isNullOrBlank()) {
                val payload = mapOf<String, Any?>(
                    "userId" to uid,
                    "subjectId" to subjectId.take(128),
                    "title" to cleanTitle,
                    "description" to description?.trim()?.takeIf { it.isNotBlank() }?.take(1000),
                    "personalNotes" to personalNotes?.trim()?.takeIf { it.isNotBlank() }?.take(4000),
                    "topicsCsv" to topicsCsv?.trim()?.takeIf { it.isNotBlank() }?.take(1000),
                    "status" to status.name,
                    "sortOrder" to sortOrder.coerceIn(0L, 10000L),
                    "completedAtMillis" to completedMillis,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                val updateMap = mutableMapOf<String, Any>(
                    "subjectId" to subjectId.take(128),
                    "title" to cleanTitle,
                    "status" to status.name,
                    "sortOrder" to sortOrder.coerceIn(0L, 10000L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                description?.trim()?.take(1000)?.let { if (it.isNotEmpty()) updateMap["description"] = it }
                personalNotes?.trim()?.take(4000)?.let { if (it.isNotEmpty()) updateMap["personalNotes"] = it }
                topicsCsv?.trim()?.take(1000)?.let { if (it.isNotEmpty()) updateMap["topicsCsv"] = it }
                if (completedMillis != null) {
                    updateMap["completedAtMillis"] = completedMillis
                }
                docRef.update(updateMap).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun deleteChapter(userId: String, chapterId: String): Result<Unit> {
        val path = "users/$userId/chapters/$chapterId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            userDoc(uid).collection("chapters").document(chapterId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    override suspend fun upsertTask(
        userId: String,
        taskId: String?,
        title: String,
        description: String?,
        priority: TaskPriority,
        category: String,
        tags: List<String>,
        subjectId: String?,
        chapterId: String?,
        dueAtMillis: Long?,
        reminderEnabled: Boolean,
        completed: Boolean
    ): Result<String> {
        val cleanTitle = title.trim().take(140)
        if (cleanTitle.isBlank()) return Result.failure(IllegalArgumentException("Task title cannot be empty."))
        val docId = taskId?.takeIf { it.isNotBlank() } ?: ("task_" + UUID.randomUUID().toString().replace("-", "").take(16))
        val path = "users/$userId/tasks/$docId"
        val cleanTags = tags.map { it.trim().take(24) }.filter { it.isNotBlank() }.take(10)
        val completedMillis = if (completed) System.currentTimeMillis() else null
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = userDoc(uid).collection("tasks").document(docId)
            if (taskId.isNullOrBlank()) {
                val payload = mapOf<String, Any?>(
                    "userId" to uid,
                    "title" to cleanTitle,
                    "description" to description?.trim()?.takeIf { it.isNotBlank() }?.take(1000),
                    "priority" to priority.name,
                    "category" to category.trim().ifBlank { "Study" }.take(40),
                    "tags" to cleanTags,
                    "subjectId" to subjectId?.takeIf { it.isNotBlank() }?.take(128),
                    "chapterId" to chapterId?.takeIf { it.isNotBlank() }?.take(128),
                    "dueAtMillis" to dueAtMillis?.takeIf { it >= 0L },
                    "reminderEnabled" to reminderEnabled,
                    "completed" to completed,
                    "completedAtMillis" to completedMillis,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                val updateMap = mutableMapOf<String, Any>(
                    "title" to cleanTitle,
                    "priority" to priority.name,
                    "category" to category.trim().ifBlank { "Study" }.take(40),
                    "tags" to cleanTags,
                    "reminderEnabled" to reminderEnabled,
                    "completed" to completed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                description?.trim()?.take(1000)?.let { if (it.isNotEmpty()) updateMap["description"] = it }
                subjectId?.takeIf { it.isNotBlank() }?.let { updateMap["subjectId"] = it.take(128) }
                chapterId?.takeIf { it.isNotBlank() }?.let { updateMap["chapterId"] = it.take(128) }
                dueAtMillis?.takeIf { it >= 0L }?.let { updateMap["dueAtMillis"] = it }
                completedMillis?.let { updateMap["completedAtMillis"] = it }
                docRef.update(updateMap).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun deleteTask(userId: String, taskId: String): Result<Unit> {
        val path = "users/$userId/tasks/$taskId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            userDoc(uid).collection("tasks").document(taskId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    override suspend fun upsertNote(
        userId: String,
        noteId: String?,
        title: String,
        content: String,
        subjectId: String?,
        chapterId: String?,
        pinned: Boolean,
        favorite: Boolean
    ): Result<String> {
        val cleanTitle = title.trim().take(160)
        if (cleanTitle.isBlank()) return Result.failure(IllegalArgumentException("Note title cannot be empty."))
        val docId = noteId?.takeIf { it.isNotBlank() } ?: ("note_" + UUID.randomUUID().toString().replace("-", "").take(16))
        val path = "users/$userId/notes/$docId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = userDoc(uid).collection("notes").document(docId)
            if (noteId.isNullOrBlank()) {
                val payload = mapOf<String, Any?>(
                    "userId" to uid,
                    "title" to cleanTitle,
                    "content" to content.take(20000),
                    "subjectId" to subjectId?.takeIf { it.isNotBlank() }?.take(128),
                    "chapterId" to chapterId?.takeIf { it.isNotBlank() }?.take(128),
                    "pinned" to pinned,
                    "favorite" to favorite,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                val updateMap = mutableMapOf<String, Any>(
                    "title" to cleanTitle,
                    "content" to content.take(20000),
                    "pinned" to pinned,
                    "favorite" to favorite,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                subjectId?.takeIf { it.isNotBlank() }?.let { updateMap["subjectId"] = it.take(128) }
                chapterId?.takeIf { it.isNotBlank() }?.let { updateMap["chapterId"] = it.take(128) }
                docRef.update(updateMap).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun deleteNote(userId: String, noteId: String): Result<Unit> {
        val path = "users/$userId/notes/$noteId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            userDoc(uid).collection("notes").document(noteId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    override suspend fun recordFocusSession(
        userId: String,
        subjectId: String?,
        subjectName: String?,
        durationMinutes: Long,
        breakMinutes: Long,
        modeLabel: String,
        ambientSound: String,
        startedAtMillis: Long,
        endedAtMillis: Long
    ): Result<Unit> {
        val docId = "fs_" + UUID.randomUUID().toString().replace("-", "").take(16)
        val path = "users/$userId/focusSessions/$docId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val safeDuration = durationMinutes.coerceIn(1L, 600L)
            val payload = mapOf<String, Any?>(
                "userId" to uid,
                "subjectId" to subjectId?.takeIf { it.isNotBlank() }?.take(128),
                "subjectName" to subjectName?.takeIf { it.isNotBlank() }?.take(80),
                "durationMinutes" to safeDuration,
                "breakMinutes" to breakMinutes.coerceIn(0L, 120L),
                "modeLabel" to modeLabel.ifBlank { "Pomodoro" }.take(40),
                "ambientSound" to ambientSound.ifBlank { "Silent" }.take(40),
                "completed" to true,
                "startedAtMillis" to startedAtMillis.coerceAtLeast(0L),
                "endedAtMillis" to endedAtMillis.coerceAtLeast(0L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            userDoc(uid).collection("focusSessions").document(docId).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun markAttendance(
        userId: String,
        dateKey: String,
        subjectId: String,
        status: AttendanceStatus,
        note: String?
    ): Result<Unit> {
        val safeSubject = subjectId.ifBlank { "ALL" }.take(64)
        val docId = "${dateKey.replace("-", "")}_$safeSubject".take(128)
        val path = "users/$userId/attendance/$docId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val ref = userDoc(uid).collection("attendance").document(docId)
            val snap = ref.get().await()
            if (!snap.exists()) {
                val payload = mapOf<String, Any?>(
                    "userId" to uid,
                    "dateKey" to dateKey.take(12),
                    "subjectId" to safeSubject,
                    "status" to status.name,
                    "note" to note?.trim()?.takeIf { it.isNotBlank() }?.take(200),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                ref.set(payload).await()
            } else {
                val updateMap = mutableMapOf<String, Any>(
                    "status" to status.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                note?.trim()?.take(200)?.let { if (it.isNotEmpty()) updateMap["note"] = it }
                ref.update(updateMap).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun awardAcademicActivity(
        userId: String,
        account: UserAccount,
        xpDelta: Long,
        pointsDelta: Long,
        focusMinutesDelta: Long,
        reason: String,
        countForStreak: Boolean
    ): Result<Long> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val today = todayDateKey()
            val yesterday = yesterdayDateKey()

            val newStreak: Long
            val newLongest: Long
            val newLastDate: String?
            if (countForStreak) {
                newStreak = when (account.lastActivityDate) {
                    today -> account.currentStreak.coerceAtLeast(1L)
                    yesterday -> account.currentStreak + 1L
                    else -> 1L
                }
                newLongest = maxOf(account.longestStreak, newStreak)
                newLastDate = today
            } else {
                newStreak = account.currentStreak
                newLongest = account.longestStreak
                newLastDate = account.lastActivityDate
            }

            val newXp = (account.xp + xpDelta).coerceIn(0L, 10000000L)
            val newLevel = GamificationCatalog.levelFromXp(newXp)
            val newPoints = (account.themePoints + pointsDelta).coerceIn(0L, 10000000L)
            val newFocus = (account.totalFocusMinutes + focusMinutesDelta).coerceIn(0L, 10000000L)

            val updates = mutableMapOf<String, Any>(
                "xp" to newXp,
                "level" to newLevel,
                "themePoints" to newPoints,
                "currentStreak" to newStreak,
                "longestStreak" to newLongest,
                "totalFocusMinutes" to newFocus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            newLastDate?.let { updates["lastActivityDate"] = it }
            userDoc(uid).update(updates).await()

            // Mirror summary to public profile
            publicProfileDoc(uid).update(
                mapOf(
                    "level" to newLevel,
                    "xp" to newXp,
                    "rankTitle" to GamificationCatalog.rankForLevel(newLevel),
                    "currentStreak" to newStreak,
                    "totalFocusMinutes" to newFocus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (xpDelta != 0L) {
                recordTransaction(uid, "XP", xpDelta, reason)
            }
            if (pointsDelta != 0L) {
                recordTransaction(uid, "THEME_POINTS", pointsDelta, reason)
            }
            Result.success(newLevel)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    private suspend fun recordTransaction(uid: String, currency: String, amount: Long, reason: String) {
        val txId = "tx_" + UUID.randomUUID().toString().replace("-", "").take(16)
        userDoc(uid).collection("transactions").document(txId).set(
            mapOf(
                "userId" to uid,
                "currency" to currency,
                "amount" to amount.coerceIn(-100000L, 100000L),
                "reason" to reason.take(120),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun unlockAchievementIfEligible(
        userId: String,
        account: UserAccount,
        existingUnlockedKeys: Set<String>,
        key: String
    ): Result<Boolean> {
        if (existingUnlockedKeys.contains(key)) return Result.success(false)
        val def = GamificationCatalog.achievements.firstOrNull { it.key == key }
            ?: return Result.success(false)
        val path = "users/$userId/achievements/$key"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = userDoc(uid).collection("achievements").document(key)
            val snap = docRef.get().await()
            if (snap.exists()) return Result.success(false)
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "achievementKey" to def.key,
                    "title" to def.title,
                    "unlockedAtMillis" to System.currentTimeMillis(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            awardAcademicActivity(
                userId = uid,
                account = account,
                xpDelta = def.xpReward,
                pointsDelta = def.pointsReward,
                focusMinutesDelta = 0L,
                reason = "Achievement: ${def.title}",
                countForStreak = false
            )
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun purchaseShopItem(userId: String, account: UserAccount, item: ShopItem): Result<Unit> {
        if (account.unlockedShopIds.contains(item.id)) {
            return Result.failure(IllegalStateException("Item already unlocked."))
        }
        if (account.themePoints < item.costPoints) {
            return Result.failure(IllegalStateException("Not enough Theme Points."))
        }
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val newBalance = (account.themePoints - item.costPoints).coerceAtLeast(0L)
            val newUnlocked = (account.unlockedShopIds + item.id).distinct().take(100)
            val updates = mutableMapOf<String, Any>(
                "themePoints" to newBalance,
                "unlockedShopIds" to newUnlocked,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (item.category == com.example.data.model.ShopCategory.FRAME) {
                updates["selectedFrameId"] = item.id
            }
            userDoc(uid).update(updates).await()
            if (item.category == com.example.data.model.ShopCategory.FRAME) {
                publicProfileDoc(uid).update(
                    mapOf(
                        "selectedFrameId" to item.id,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            recordTransaction(uid, "THEME_POINTS", -item.costPoints, "Shop: ${item.title}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun equipFrame(userId: String, frameId: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            userDoc(uid).update(
                mapOf(
                    "selectedFrameId" to frameId.take(64),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            publicProfileDoc(uid).update(
                mapOf(
                    "selectedFrameId" to frameId.take(64),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfileDetails(
        userId: String,
        displayName: String,
        bio: String?,
        academicSession: String?,
        academicClass: String?,
        academicStream: String?,
        dailyGoalMinutes: Long,
        discoverable: Boolean,
        showStats: Boolean,
        showAchievements: Boolean
    ): Result<Unit> {
        val cleanName = displayName.trim().take(60)
        if (cleanName.isBlank()) return Result.failure(IllegalArgumentException("Name cannot be blank."))
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val userUpdates = mutableMapOf<String, Any>(
                "displayName" to cleanName,
                "dailyGoalMinutes" to dailyGoalMinutes.coerceIn(5L, 720L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            bio?.trim()?.take(240)?.let { if (it.isNotEmpty()) userUpdates["bio"] = it }
            academicSession?.trim()?.take(60)?.let { if (it.isNotEmpty()) userUpdates["academicSession"] = it }
            academicClass?.trim()?.take(60)?.let { if (it.isNotEmpty()) userUpdates["academicClass"] = it }
            academicStream?.trim()?.take(60)?.let { if (it.isNotEmpty()) userUpdates["academicStream"] = it }
            userDoc(uid).update(userUpdates).await()

            val pubUpdates = mutableMapOf<String, Any>(
                "username" to cleanName,
                "discoverable" to discoverable,
                "showStats" to showStats,
                "showAchievements" to showAchievements,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            bio?.trim()?.take(240)?.let { if (it.isNotEmpty()) pubUpdates["bio"] = it }
            academicClass?.trim()?.take(60)?.let { if (it.isNotEmpty()) pubUpdates["academicClass"] = it }
            academicStream?.trim()?.take(60)?.let { if (it.isNotEmpty()) pubUpdates["academicStream"] = it }
            publicProfileDoc(uid).update(pubUpdates).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun exportBackupJson(
        subjects: List<SubjectItem>,
        chapters: List<ChapterItem>,
        tasks: List<TaskItem>,
        notes: List<NoteItem>,
        sessions: List<FocusSessionItem>,
        attendance: List<AttendanceItem>
    ): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAtMillis", System.currentTimeMillis())

        root.put("subjects", JSONArray().apply {
            subjects.forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("colorHex", s.colorHex)
                    put("iconKey", s.iconKey)
                    put("sortOrder", s.sortOrder)
                    put("targetChapters", s.targetChapters)
                    s.examDateMillis?.let { put("examDateMillis", it) }
                })
            }
        })
        root.put("chapters", JSONArray().apply {
            chapters.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("subjectId", c.subjectId)
                    put("title", c.title)
                    put("status", c.status)
                    put("sortOrder", c.sortOrder)
                    c.description?.let { put("description", it) }
                    c.personalNotes?.let { put("personalNotes", it) }
                    c.topicsCsv?.let { put("topicsCsv", it) }
                })
            }
        })
        root.put("tasks", JSONArray().apply {
            tasks.forEach { t ->
                put(JSONObject().apply {
                    put("title", t.title)
                    put("priority", t.priority)
                    put("category", t.category)
                    put("completed", t.completed)
                    t.dueAtMillis?.let { put("dueAtMillis", it) }
                })
            }
        })
        root.put("notes", JSONArray().apply {
            notes.forEach { n ->
                put(JSONObject().apply {
                    put("title", n.title)
                    put("content", n.content)
                    put("pinned", n.pinned)
                    put("favorite", n.favorite)
                })
            }
        })
        root.put("focusSessionsCount", sessions.size)
        root.put("attendanceCount", attendance.size)
        return root.toString(2)
    }

    suspend fun restoreFromBackupJson(userId: String, rawJson: String): Result<Int> {
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val root = JSONObject(rawJson)
            var restoredCount = 0
            val subjectIdMap = mutableMapOf<String, String>()

            val subs = root.optJSONArray("subjects")
            if (subs != null) {
                for (i in 0 until subs.length()) {
                    val obj = subs.getJSONObject(i)
                    val oldId = obj.optString("id")
                    val res = upsertSubject(
                        userId = uid,
                        subjectId = null,
                        name = obj.optString("name", "Subject"),
                        colorHex = obj.optString("colorHex", "#38BDF8"),
                        iconKey = obj.optString("iconKey", "book"),
                        sortOrder = obj.optLong("sortOrder", i.toLong()),
                        examDateMillis = if (obj.has("examDateMillis")) obj.optLong("examDateMillis") else null,
                        targetChapters = obj.optLong("targetChapters", 10L)
                    )
                    res.getOrNull()?.let { newId ->
                        if (oldId.isNotBlank()) subjectIdMap[oldId] = newId
                        restoredCount++
                    }
                }
            }
            val chaps = root.optJSONArray("chapters")
            if (chaps != null) {
                for (i in 0 until chaps.length()) {
                    val obj = chaps.getJSONObject(i)
                    val mappedSubId = subjectIdMap[obj.optString("subjectId")] ?: continue
                    val status = enumValueOrDefault(obj.optString("status"), ChapterStatus.INCOMPLETE)
                    upsertChapter(
                        userId = uid,
                        chapterId = null,
                        subjectId = mappedSubId,
                        title = obj.optString("title", "Chapter"),
                        description = obj.optString("description").takeIf { it.isNotBlank() },
                        personalNotes = obj.optString("personalNotes").takeIf { it.isNotBlank() },
                        topicsCsv = obj.optString("topicsCsv").takeIf { it.isNotBlank() },
                        status = status,
                        sortOrder = obj.optLong("sortOrder", i.toLong())
                    ).onSuccess { restoredCount++ }
                }
            }
            val tasksArr = root.optJSONArray("tasks")
            if (tasksArr != null) {
                for (i in 0 until tasksArr.length()) {
                    val obj = tasksArr.getJSONObject(i)
                    val priority = enumValueOrDefault(obj.optString("priority"), TaskPriority.MEDIUM)
                    upsertTask(
                        userId = uid,
                        taskId = null,
                        title = obj.optString("title", "Task"),
                        description = null,
                        priority = priority,
                        category = obj.optString("category", "Study"),
                        tags = emptyList(),
                        subjectId = null,
                        chapterId = null,
                        dueAtMillis = if (obj.has("dueAtMillis")) obj.optLong("dueAtMillis") else null,
                        reminderEnabled = false,
                        completed = obj.optBoolean("completed", false)
                    ).onSuccess { restoredCount++ }
                }
            }
            val notesArr = root.optJSONArray("notes")
            if (notesArr != null) {
                for (i in 0 until notesArr.length()) {
                    val obj = notesArr.getJSONObject(i)
                    upsertNote(
                        userId = uid,
                        noteId = null,
                        title = obj.optString("title", "Note"),
                        content = obj.optString("content", ""),
                        subjectId = null,
                        chapterId = null,
                        pinned = obj.optBoolean("pinned", false),
                        favorite = obj.optBoolean("favorite", false)
                    ).onSuccess { restoredCount++ }
                }
            }
            Result.success(restoredCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun logFlowError(error: Throwable, op: OperationType, path: String, expectedUid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }

    private fun todayDateKey(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(Date())
    }

    private fun yesterdayDateKey(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(cal.time)
    }
}
