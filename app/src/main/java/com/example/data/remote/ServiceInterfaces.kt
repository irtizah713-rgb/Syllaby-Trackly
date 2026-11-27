package com.example.data.remote

import com.example.data.model.AttendanceItem
import com.example.data.model.AttendanceStatus
import com.example.data.model.ChapterItem
import com.example.data.model.ChapterStatus
import com.example.data.model.FocusSessionItem
import com.example.data.model.FriendLinkItem
import com.example.data.model.FriendRequestItem
import com.example.data.model.NoteItem
import com.example.data.model.PublicProfile
import com.example.data.model.RoomEnvironment
import com.example.data.model.RoomMessageItem
import com.example.data.model.RoomPrivacy
import com.example.data.model.RoomTimerState
import com.example.data.model.StudyRoomItem
import com.example.data.model.SubjectItem
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.data.model.UserAccount
import kotlinx.coroutines.flow.Flow

interface AcademicSyncService {
    fun observeUserAccount(userId: String): Flow<UserAccount?>
    fun observeSubjects(userId: String): Flow<List<SubjectItem>>
    fun observeChapters(userId: String): Flow<List<ChapterItem>>
    fun observeTasks(userId: String): Flow<List<TaskItem>>
    fun observeNotes(userId: String): Flow<List<NoteItem>>
    fun observeFocusSessions(userId: String): Flow<List<FocusSessionItem>>
    fun observeAttendance(userId: String): Flow<List<AttendanceItem>>

    suspend fun upsertSubject(
        userId: String,
        subjectId: String?,
        name: String,
        colorHex: String,
        iconKey: String,
        sortOrder: Long,
        examDateMillis: Long?,
        targetChapters: Long
    ): Result<String>

    suspend fun deleteSubject(userId: String, subjectId: String): Result<Unit>

    suspend fun upsertChapter(
        userId: String,
        chapterId: String?,
        subjectId: String,
        title: String,
        description: String?,
        personalNotes: String?,
        topicsCsv: String?,
        status: ChapterStatus,
        sortOrder: Long
    ): Result<String>

    suspend fun deleteChapter(userId: String, chapterId: String): Result<Unit>

    suspend fun upsertTask(
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
    ): Result<String>

    suspend fun deleteTask(userId: String, taskId: String): Result<Unit>

    suspend fun upsertNote(
        userId: String,
        noteId: String?,
        title: String,
        content: String,
        subjectId: String?,
        chapterId: String?,
        pinned: Boolean,
        favorite: Boolean
    ): Result<String>

    suspend fun deleteNote(userId: String, noteId: String): Result<Unit>

    suspend fun recordFocusSession(
        userId: String,
        subjectId: String?,
        subjectName: String?,
        durationMinutes: Long,
        breakMinutes: Long,
        modeLabel: String,
        ambientSound: String,
        startedAtMillis: Long,
        endedAtMillis: Long
    ): Result<Unit>

    suspend fun markAttendance(
        userId: String,
        dateKey: String,
        subjectId: String,
        status: AttendanceStatus,
        note: String?
    ): Result<Unit>
}

interface SocialRoomsService {
    fun observePublicProfiles(userId: String): Flow<PublicProfile?>
    fun observeFriends(userId: String): Flow<List<FriendLinkItem>>
    fun observeIncomingFriendRequests(userId: String): Flow<List<FriendRequestItem>>
    fun observeOutgoingFriendRequests(userId: String): Flow<List<FriendRequestItem>>
    fun observePublicRooms(): Flow<List<StudyRoomItem>>
    fun observeMyRooms(userId: String): Flow<List<StudyRoomItem>>
    fun observeInvitedRooms(userId: String): Flow<List<StudyRoomItem>>
    fun observeRoomMessages(roomId: String, privacy: RoomPrivacy, userId: String): Flow<List<RoomMessageItem>>

    suspend fun findProfileByFriendCode(friendCode: String): Result<PublicProfile?>
    suspend fun sendFriendRequest(fromProfile: PublicProfile, toProfile: PublicProfile): Result<Unit>
    suspend fun respondFriendRequest(request: FriendRequestItem, accept: Boolean): Result<Unit>
    suspend fun removeOrBlockFriend(userId: String, friendUid: String, block: Boolean): Result<Unit>

    suspend fun createStudyRoom(
        hostUid: String,
        hostName: String,
        title: String,
        description: String,
        environment: RoomEnvironment,
        privacy: RoomPrivacy,
        invitedUids: List<String>
    ): Result<String>

    suspend fun joinRoom(room: StudyRoomItem, userId: String): Result<Unit>
    suspend fun leaveRoom(room: StudyRoomItem, userId: String): Result<Unit>
    suspend fun inviteFriendToRoom(room: StudyRoomItem, friendUid: String): Result<Unit>
    suspend fun updateRoomTimer(
        room: StudyRoomItem,
        timerState: RoomTimerState,
        durationMinutes: Long,
        remainingSeconds: Long
    ): Result<Unit>

    suspend fun sendRoomMessage(
        room: StudyRoomItem,
        senderUid: String,
        senderName: String,
        text: String
    ): Result<Unit>

    suspend fun submitReport(
        reporterUid: String,
        targetType: String,
        targetId: String,
        reason: String,
        details: String
    ): Result<Unit>
}
