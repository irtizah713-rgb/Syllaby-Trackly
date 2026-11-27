package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.model.FriendLinkItem
import com.example.data.model.FriendRequestItem
import com.example.data.model.PublicProfile
import com.example.data.model.RoomEnvironment
import com.example.data.model.RoomMessageItem
import com.example.data.model.RoomPrivacy
import com.example.data.model.RoomTimerState
import com.example.data.model.StudyRoomItem
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
import java.util.Locale
import java.util.UUID

class SocialRoomsRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : SocialRoomsService {

    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google.")
    }

    override fun observePublicProfiles(userId: String): Flow<PublicProfile?> = flow {
        require(userId.isNotBlank())
        val path = "publicProfiles/$userId"
        emitAll(
            db.collection("publicProfiles").document(userId).snapshots()
                .map { it.toObject(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> logFlowError(error, OperationType.GET, path, userId); throw error }
        )
    }

    override fun observeFriends(userId: String): Flow<List<FriendLinkItem>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/friends"
        emitAll(
            db.collection("users").document(userId).collection("friends").snapshots()
                .map { snap -> snap.toObjects(FriendLinkItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> logFlowError(error, OperationType.LIST, path, userId); throw error }
        )
    }

    override fun observeIncomingFriendRequests(userId: String): Flow<List<FriendRequestItem>> = flow {
        require(userId.isNotBlank())
        val path = "friendRequests"
        emitAll(
            db.collection("friendRequests")
                .whereEqualTo("toUid", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(FriendRequestItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.status == "PENDING" }
                }
                .catch { error -> logFlowError(error, OperationType.LIST, path, userId); throw error }
        )
    }

    override fun observeOutgoingFriendRequests(userId: String): Flow<List<FriendRequestItem>> = flow {
        require(userId.isNotBlank())
        val path = "friendRequests"
        emitAll(
            db.collection("friendRequests")
                .whereEqualTo("fromUid", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(FriendRequestItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.status == "PENDING" }
                }
                .catch { error -> logFlowError(error, OperationType.LIST, path, userId); throw error }
        )
    }

    fun observeAcceptedOutgoingFriendRequests(userId: String): Flow<List<FriendRequestItem>> = flow {
        require(userId.isNotBlank())
        val path = "friendRequests"
        emitAll(
            db.collection("friendRequests")
                .whereEqualTo("fromUid", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(FriendRequestItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.status == "ACCEPTED" }
                }
                .catch { error -> logFlowError(error, OperationType.LIST, path, userId); throw error }
        )
    }

    override fun observePublicRooms(): Flow<List<StudyRoomItem>> = flow {
        val uid = auth.currentUser?.uid ?: return@flow
        val path = "studyRooms"
        emitAll(
            db.collection("studyRooms")
                .whereEqualTo("privacy", RoomPrivacy.PUBLIC.name)
                .snapshots()
                .map { snap ->
                    snap.toObjects(StudyRoomItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error -> logFlowError(error, OperationType.LIST, path, uid); throw error }
        )
    }

    override fun observeMyRooms(userId: String): Flow<List<StudyRoomItem>> = flow {
        require(userId.isNotBlank())
        val path = "studyRooms"
        emitAll(
            db.collection("studyRooms")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(StudyRoomItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error -> logFlowError(error, OperationType.LIST, path, userId); throw error }
        )
    }

    override fun observeInvitedRooms(userId: String): Flow<List<StudyRoomItem>> = flow {
        require(userId.isNotBlank())
        val path = "studyRooms"
        emitAll(
            db.collection("studyRooms")
                .whereArrayContains("invitedUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(StudyRoomItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error -> logFlowError(error, OperationType.LIST, path, userId); throw error }
        )
    }

    override fun observeRoomMessages(
        roomId: String,
        privacy: RoomPrivacy,
        userId: String
    ): Flow<List<RoomMessageItem>> = flow {
        require(roomId.isNotBlank() && userId.isNotBlank())
        val path = "studyRooms/$roomId/messages"
        val baseCollection = db.collection("studyRooms").document(roomId).collection("messages")
        val query = if (privacy == RoomPrivacy.PUBLIC) {
            baseCollection.whereEqualTo("roomPrivacy", RoomPrivacy.PUBLIC.name)
        } else {
            baseCollection.whereArrayContains("roomAllowedUids", userId)
        }
        val nowMillis = System.currentTimeMillis()
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(RoomMessageItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.expiresAtMillis > nowMillis }
                        .sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error -> logFlowError(error, OperationType.LIST, path, userId); throw error }
        )
    }

    override suspend fun findProfileByFriendCode(friendCode: String): Result<PublicProfile?> {
        val clean = friendCode.trim().uppercase(Locale.US)
        if (clean.length < 4) return Result.failure(IllegalArgumentException("Enter a valid friend code (e.g., ST-A1B2C3)."))
        val path = "publicProfiles"
        return try {
            requireUserId()
            val snap = db.collection("publicProfiles")
                .whereEqualTo("discoverable", true)
                .whereEqualTo("friendCode", clean)
                .limit(1)
                .get()
                .await()
            val profile = snap.documents.firstOrNull()
                ?.toObject(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun getPublicProfile(targetUid: String): Result<PublicProfile?> {
        val path = "publicProfiles/$targetUid"
        return try {
            requireUserId()
            val snap = db.collection("publicProfiles").document(targetUid).get().await()
            val profile = snap.toObject(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    override suspend fun sendFriendRequest(fromProfile: PublicProfile, toProfile: PublicProfile): Result<Unit> {
        if (fromProfile.userId == toProfile.userId) {
            return Result.failure(IllegalArgumentException("You cannot add yourself as a friend."))
        }
        val reqId = "freq_${fromProfile.userId}_${toProfile.userId}".take(128)
        val path = "friendRequests/$reqId"
        return try {
            val uid = requireUserId()
            check(uid == fromProfile.userId)
            val payload = mapOf(
                "fromUid" to uid,
                "fromUsername" to fromProfile.username.take(60),
                "fromFriendCode" to fromProfile.friendCode.take(20),
                "toUid" to toProfile.userId,
                "toUsername" to toProfile.username.take(60),
                "status" to "PENDING",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("friendRequests").document(reqId).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun respondFriendRequest(request: FriendRequestItem, accept: Boolean): Result<Unit> {
        val path = "friendRequests/${request.id}"
        return try {
            val uid = requireUserId()
            check(uid == request.toUid || uid == request.fromUid)
            val newStatus = if (accept) "ACCEPTED" else "DECLINED"
            db.collection("friendRequests").document(request.id).update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (accept && uid == request.toUid) {
                db.collection("users").document(uid).collection("friends")
                    .document(request.fromUid)
                    .set(
                        mapOf(
                            "userId" to uid,
                            "friendUid" to request.fromUid,
                            "friendUsername" to request.fromUsername.take(60),
                            "friendCode" to request.fromFriendCode.take(20),
                            "relationStatus" to "FRIEND",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addAcceptedFriendForSender(
        myUid: String,
        friendProfile: PublicProfile
    ): Result<Unit> {
        val path = "users/$myUid/friends/${friendProfile.userId}"
        return try {
            val uid = requireUserId()
            check(uid == myUid)
            db.collection("users").document(uid).collection("friends")
                .document(friendProfile.userId)
                .set(
                    mapOf(
                        "userId" to uid,
                        "friendUid" to friendProfile.userId,
                        "friendUsername" to friendProfile.username.take(60),
                        "friendCode" to friendProfile.friendCode.take(20),
                        "relationStatus" to "FRIEND",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun removeOrBlockFriend(userId: String, friendUid: String, block: Boolean): Result<Unit> {
        val path = "users/$userId/friends/$friendUid"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val ref = db.collection("users").document(uid).collection("friends").document(friendUid)
            if (block) {
                ref.update(
                    mapOf(
                        "relationStatus" to "BLOCKED",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun createStudyRoom(
        hostUid: String,
        hostName: String,
        title: String,
        description: String,
        environment: RoomEnvironment,
        privacy: RoomPrivacy,
        invitedUids: List<String>
    ): Result<String> {
        val cleanTitle = title.trim().take(80)
        if (cleanTitle.isBlank()) return Result.failure(IllegalArgumentException("Room title cannot be empty."))
        val roomId = "room_" + UUID.randomUUID().toString().replace("-", "").take(14)
        val inviteCode = "SR-" + UUID.randomUUID().toString().replace("-", "").take(6).uppercase(Locale.US)
        val path = "studyRooms/$roomId"
        return try {
            val uid = requireUserId()
            check(uid == hostUid)
            val payload = mapOf(
                "hostUid" to uid,
                "hostName" to hostName.trim().ifBlank { "Host" }.take(60),
                "title" to cleanTitle,
                "description" to description.trim().take(240),
                "environment" to environment.name,
                "privacy" to privacy.name,
                "inviteCode" to inviteCode,
                "invitedUids" to invitedUids.distinct().take(50),
                "memberUids" to listOf(uid),
                "timerState" to RoomTimerState.IDLE.name,
                "timerDurationMinutes" to 25L,
                "timerRemainingSeconds" to 1500L,
                "timerUpdatedAtMillis" to System.currentTimeMillis(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("studyRooms").document(roomId).set(payload).await()
            Result.success(roomId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun joinRoom(room: StudyRoomItem, userId: String): Result<Unit> {
        val path = "studyRooms/${room.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val newMembers = (room.memberUids + uid).distinct().take(100)
            db.collection("studyRooms").document(room.id).update(
                mapOf(
                    "memberUids" to newMembers,
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

    override suspend fun leaveRoom(room: StudyRoomItem, userId: String): Result<Unit> {
        val path = "studyRooms/${room.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val newMembers = room.memberUids.filterNot { it == uid }
            db.collection("studyRooms").document(room.id).update(
                mapOf(
                    "memberUids" to newMembers,
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

    override suspend fun inviteFriendToRoom(room: StudyRoomItem, friendUid: String): Result<Unit> {
        val path = "studyRooms/${room.id}"
        return try {
            val uid = requireUserId()
            check(uid == room.hostUid)
            val updatedInvites = (room.invitedUids + friendUid).distinct().take(50)
            db.collection("studyRooms").document(room.id).update(
                mapOf(
                    "invitedUids" to updatedInvites,
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

    override suspend fun updateRoomTimer(
        room: StudyRoomItem,
        timerState: RoomTimerState,
        durationMinutes: Long,
        remainingSeconds: Long
    ): Result<Unit> {
        val path = "studyRooms/${room.id}"
        return try {
            requireUserId()
            val payload = mutableMapOf<String, Any>(
                "timerState" to timerState.name,
                "timerRemainingSeconds" to remainingSeconds.coerceIn(0L, 10800L),
                "timerUpdatedAtMillis" to System.currentTimeMillis(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (auth.currentUser?.uid == room.hostUid) {
                payload["timerDurationMinutes"] = durationMinutes.coerceIn(1L, 180L)
            }
            db.collection("studyRooms").document(room.id).update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun sendRoomMessage(
        room: StudyRoomItem,
        senderUid: String,
        senderName: String,
        text: String
    ): Result<Unit> {
        val cleanText = text.trim().take(500)
        if (cleanText.isBlank()) return Result.failure(IllegalArgumentException("Message cannot be empty."))
        val msgId = "msg_" + UUID.randomUUID().toString().replace("-", "").take(16)
        val path = "studyRooms/${room.id}/messages/$msgId"
        val sevenDaysMillis = 7L * 24L * 3600L * 1000L
        return try {
            val uid = requireUserId()
            check(uid == senderUid)
            val allowed = (listOf(room.hostUid) + room.memberUids + room.invitedUids).distinct().take(100)
            val payload = mapOf(
                "roomId" to room.id,
                "roomHostUid" to room.hostUid,
                "roomPrivacy" to room.privacy,
                "roomAllowedUids" to allowed,
                "senderUid" to uid,
                "senderName" to senderName.trim().ifBlank { "Student" }.take(60),
                "text" to cleanText,
                "expiresAtMillis" to (System.currentTimeMillis() + sevenDaysMillis),
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("studyRooms").document(room.id)
                .collection("messages").document(msgId)
                .set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun submitReport(
        reporterUid: String,
        targetType: String,
        targetId: String,
        reason: String,
        details: String
    ): Result<Unit> {
        val repId = "rep_" + UUID.randomUUID().toString().replace("-", "").take(16)
        val path = "reports/$repId"
        return try {
            val uid = requireUserId()
            check(uid == reporterUid)
            val payload = mapOf(
                "reporterUid" to uid,
                "targetType" to targetType.take(20),
                "targetId" to targetId.take(128),
                "reason" to reason.trim().ifBlank { "Community Violation" }.take(80),
                "details" to details.trim().take(500),
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("reports").document(repId).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
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
}
