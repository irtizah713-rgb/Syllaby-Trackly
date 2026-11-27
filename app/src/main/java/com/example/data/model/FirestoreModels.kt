package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String?, default: T): T {
    if (raw.isNullOrBlank()) return default
    return enumValues<T>().firstOrNull { it.name == raw } ?: default
}

enum class ChapterStatus(val label: String, val countsTowardProgress: Boolean) {
    INCOMPLETE("Incomplete", false),
    COMPLETED("Completed", true),
    REVISED("Revised", true),
    PRACTISED("Practised", true),
    NUMERICALS_PRACTISED("Numericals Practised", true)
}

enum class TaskPriority(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    URGENT("Urgent")
}

enum class AttendanceStatus(val label: String) {
    PRESENT("Present"),
    ABSENT("Absent"),
    HOLIDAY("Holiday")
}

enum class RoomEnvironment(val label: String, val subtitle: String) {
    FOREST("Forest", "Quiet pine canopy & rain"),
    LIBRARY("Library", "Warm lamp-lit bookshelves"),
    CLASSROOM("Classroom", "Focused desk & chalkboard")
}

enum class RoomPrivacy(val label: String) {
    PUBLIC("Public"),
    PRIVATE("Private")
}

enum class RoomTimerState(val label: String) {
    IDLE("Ready"),
    RUNNING("Focusing"),
    PAUSED("Paused"),
    BREAK("Break")
}

@Keep
@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val xp: Long = 0L,
    val level: Long = 1L,
    val themePoints: Long = 50L,
    val currentStreak: Long = 0L,
    val longestStreak: Long = 0L,
    val lastActivityDate: String? = null,
    val totalFocusMinutes: Long = 0L,
    val dailyGoalMinutes: Long = 60L,
    val academicSession: String? = null,
    val academicClass: String? = null,
    val academicStream: String? = null,
    val bio: String? = null,
    val selectedFrameId: String = "frame_default",
    val unlockedShopIds: List<String> = listOf("frame_default"),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PublicProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val friendCode: String = "",
    val bio: String? = null,
    val academicClass: String? = null,
    val academicStream: String? = null,
    val level: Long = 1L,
    val xp: Long = 0L,
    val rankTitle: String = "Initiate",
    val currentStreak: Long = 0L,
    val totalFocusMinutes: Long = 0L,
    val selectedFrameId: String = "frame_default",
    val discoverable: Boolean = true,
    val showStats: Boolean = true,
    val showAchievements: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SubjectItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val colorHex: String = "#38BDF8",
    val iconKey: String = "book",
    val sortOrder: Long = 0L,
    val examDateMillis: Long? = null,
    val targetChapters: Long = 10L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChapterItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subjectId: String = "",
    val title: String = "",
    val description: String? = null,
    val personalNotes: String? = null,
    val topicsCsv: String? = null,
    val status: String = ChapterStatus.INCOMPLETE.name,
    val sortOrder: Long = 0L,
    val completedAtMillis: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun chapterStatus(): ChapterStatus = enumValueOrDefault(status, ChapterStatus.INCOMPLETE)
}

@Keep
@IgnoreExtraProperties
data class TaskItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String? = null,
    val priority: String = TaskPriority.MEDIUM.name,
    val category: String = "Study",
    val tags: List<String> = emptyList(),
    val subjectId: String? = null,
    val chapterId: String? = null,
    val dueAtMillis: Long? = null,
    val reminderEnabled: Boolean = false,
    val completed: Boolean = false,
    val completedAtMillis: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun taskPriority(): TaskPriority = enumValueOrDefault(priority, TaskPriority.MEDIUM)
}

@Keep
@IgnoreExtraProperties
data class NoteItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val subjectId: String? = null,
    val chapterId: String? = null,
    val pinned: Boolean = false,
    val favorite: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FocusSessionItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subjectId: String? = null,
    val subjectName: String? = null,
    val durationMinutes: Long = 25L,
    val breakMinutes: Long = 5L,
    val modeLabel: String = "25/5 Classic",
    val ambientSound: String = "Silent",
    val completed: Boolean = true,
    val startedAtMillis: Long = 0L,
    val endedAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AttendanceItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val subjectId: String = "ALL",
    val status: String = AttendanceStatus.PRESENT.name,
    val note: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun attendanceStatus(): AttendanceStatus = enumValueOrDefault(status, AttendanceStatus.PRESENT)
}

@Keep
@IgnoreExtraProperties
data class RewardTransactionItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val currency: String = "XP",
    val amount: Long = 0L,
    val reason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AchievementUnlockItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val achievementKey: String = "",
    val title: String = "",
    val unlockedAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FriendLinkItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val friendUid: String = "",
    val friendUsername: String = "",
    val friendCode: String = "",
    val relationStatus: String = "FRIEND",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FriendRequestItem(
    @DocumentId val id: String = "",
    val fromUid: String = "",
    val fromUsername: String = "",
    val fromFriendCode: String = "",
    val toUid: String = "",
    val toUsername: String = "",
    val status: String = "PENDING",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudyRoomItem(
    @DocumentId val id: String = "",
    val hostUid: String = "",
    val hostName: String = "",
    val title: String = "",
    val description: String = "",
    val environment: String = RoomEnvironment.LIBRARY.name,
    val privacy: String = RoomPrivacy.PUBLIC.name,
    val inviteCode: String = "",
    val invitedUids: List<String> = emptyList(),
    val memberUids: List<String> = emptyList(),
    val timerState: String = RoomTimerState.IDLE.name,
    val timerDurationMinutes: Long = 25L,
    val timerRemainingSeconds: Long = 1500L,
    val timerUpdatedAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun roomEnvironment(): RoomEnvironment = enumValueOrDefault(environment, RoomEnvironment.LIBRARY)
    fun roomPrivacy(): RoomPrivacy = enumValueOrDefault(privacy, RoomPrivacy.PUBLIC)
    fun roomTimerState(): RoomTimerState = enumValueOrDefault(timerState, RoomTimerState.IDLE)
}

@Keep
@IgnoreExtraProperties
data class RoomMessageItem(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val roomHostUid: String = "",
    val roomPrivacy: String = RoomPrivacy.PUBLIC.name,
    val roomAllowedUids: List<String> = emptyList(),
    val senderUid: String = "",
    val senderName: String = "",
    val text: String = "",
    val expiresAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)
