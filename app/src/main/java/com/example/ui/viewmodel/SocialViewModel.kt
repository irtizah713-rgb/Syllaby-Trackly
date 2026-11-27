package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.FriendLinkItem
import com.example.data.model.FriendRequestItem
import com.example.data.model.PublicProfile
import com.example.data.model.RoomEnvironment
import com.example.data.model.RoomMessageItem
import com.example.data.model.RoomPrivacy
import com.example.data.model.RoomTimerState
import com.example.data.model.StudyRoomItem
import com.example.data.remote.SocialRoomsRepository
import com.example.util.ConnectivityObserver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SocialViewModel(
    private val repository: SocialRoomsRepository,
    connectivityObserver: ConnectivityObserver,
    private val currentUserId: String
) : ViewModel() {

    private val _socialEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val socialEvents = _socialEvents.asSharedFlow()

    val isOnline: StateFlow<Boolean> = connectivityObserver.observeOnline()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

    val myPublicProfile: StateFlow<UiState<PublicProfile?>> =
        repository.observePublicProfiles(currentUserId)
            .asUiState("Failed to load public profile")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val friendsState: StateFlow<UiState<List<FriendLinkItem>>> =
        repository.observeFriends(currentUserId)
            .asUiState("Failed to load friends")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val incomingRequestsState: StateFlow<UiState<List<FriendRequestItem>>> =
        repository.observeIncomingFriendRequests(currentUserId)
            .asUiState("Failed to load friend requests")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val outgoingRequestsState: StateFlow<UiState<List<FriendRequestItem>>> =
        repository.observeOutgoingFriendRequests(currentUserId)
            .asUiState("Failed to load sent requests")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val publicRoomsState: StateFlow<UiState<List<StudyRoomItem>>> =
        repository.observePublicRooms()
            .asUiState("Failed to load public study rooms")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val myRoomsState: StateFlow<UiState<List<StudyRoomItem>>> =
        repository.observeMyRooms(currentUserId)
            .asUiState("Failed to load your study rooms")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val invitedRoomsState: StateFlow<UiState<List<StudyRoomItem>>> =
        repository.observeInvitedRooms(currentUserId)
            .asUiState("Failed to load room invitations")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _activeRoomSelection = MutableStateFlow<Pair<String, RoomPrivacy>?>(null)

    val activeRoomMessages: StateFlow<UiState<List<RoomMessageItem>>> =
        _activeRoomSelection.flatMapLatest { sel ->
            if (sel == null) flowOf(UiState.Success(emptyList()))
            else repository.observeRoomMessages(sel.first, sel.second, currentUserId)
                .asUiState("Failed to load room chat")
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(emptyList()))

    private val _searchedProfile = MutableStateFlow<PublicProfile?>(null)
    val searchedProfile: StateFlow<PublicProfile?> = _searchedProfile.asStateFlow()

    private val _viewedFriendProfile = MutableStateFlow<PublicProfile?>(null)
    val viewedFriendProfile: StateFlow<PublicProfile?> = _viewedFriendProfile.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAcceptedOutgoingFriendRequests(currentUserId).collect { accepted ->
                val currentFriendUids = (friendsState.value as? UiState.Success)?.data
                    ?.map { it.friendUid }?.toSet().orEmpty()
                accepted.forEach { req ->
                    if (req.toUid !in currentFriendUids) {
                        repository.getPublicProfile(req.toUid).getOrNull()?.let { profile ->
                            repository.addAcceptedFriendForSender(currentUserId, profile)
                        }
                    }
                }
            }
        }
    }

    fun selectRoomForChat(room: StudyRoomItem?) {
        _activeRoomSelection.value = room?.let { it.id to it.roomPrivacy() }
    }

    fun searchByFriendCode(code: String) {
        viewModelScope.launch {
            repository.findProfileByFriendCode(code)
                .onSuccess { profile ->
                    _searchedProfile.value = profile
                    if (profile == null) _socialEvents.tryEmit("No discoverable student found with code $code")
                }
                .onFailure { _socialEvents.tryEmit(it.localizedMessage ?: "Search failed") }
        }
    }

    fun clearSearchedProfile() {
        _searchedProfile.value = null
    }

    fun loadFriendProfile(friendUid: String) {
        viewModelScope.launch {
            repository.getPublicProfile(friendUid)
                .onSuccess { _viewedFriendProfile.value = it }
                .onFailure { _socialEvents.tryEmit("Could not load friend profile") }
        }
    }

    fun clearViewedFriendProfile() {
        _viewedFriendProfile.value = null
    }

    fun sendFriendRequest(target: PublicProfile) {
        val myProf = (myPublicProfile.value as? UiState.Success)?.data
        if (myProf == null) {
            _socialEvents.tryEmit("Profile still loading, try again in a moment.")
            return
        }
        viewModelScope.launch {
            repository.sendFriendRequest(myProf, target)
                .onSuccess {
                    _searchedProfile.value = null
                    _socialEvents.tryEmit("Friend request sent to ${target.username}")
                }
                .onFailure { _socialEvents.tryEmit(it.localizedMessage ?: "Could not send request") }
        }
    }

    fun respondFriendRequest(request: FriendRequestItem, accept: Boolean) {
        viewModelScope.launch {
            repository.respondFriendRequest(request, accept)
                .onSuccess {
                    _socialEvents.tryEmit(if (accept) "Accepted ${request.fromUsername}" else "Declined request")
                }
                .onFailure { _socialEvents.tryEmit(it.localizedMessage ?: "Action failed") }
        }
    }

    fun removeOrBlockFriend(friendUid: String, block: Boolean) {
        viewModelScope.launch {
            repository.removeOrBlockFriend(currentUserId, friendUid, block)
                .onSuccess { _socialEvents.tryEmit(if (block) "User blocked" else "Friend removed") }
        }
    }

    fun createStudyRoom(
        title: String,
        description: String,
        environment: RoomEnvironment,
        privacy: RoomPrivacy,
        invitedFriendUids: List<String>,
        onCreated: (String) -> Unit
    ) {
        val myProf = (myPublicProfile.value as? UiState.Success)?.data
        val hostName = myProf?.username ?: "Student"
        viewModelScope.launch {
            repository.createStudyRoom(
                hostUid = currentUserId,
                hostName = hostName,
                title = title,
                description = description,
                environment = environment,
                privacy = privacy,
                invitedUids = invitedFriendUids
            ).onSuccess { roomId ->
                _socialEvents.tryEmit("Study room created")
                onCreated(roomId)
            }.onFailure { _socialEvents.tryEmit(it.localizedMessage ?: "Could not create room") }
        }
    }

    fun joinRoom(room: StudyRoomItem) {
        viewModelScope.launch {
            repository.joinRoom(room, currentUserId)
                .onSuccess { selectRoomForChat(room) }
                .onFailure { _socialEvents.tryEmit(it.localizedMessage ?: "Could not join room") }
        }
    }

    fun leaveRoom(room: StudyRoomItem) {
        viewModelScope.launch {
            repository.leaveRoom(room, currentUserId)
                .onSuccess {
                    selectRoomForChat(null)
                    _socialEvents.tryEmit("Left room")
                }
        }
    }

    fun inviteFriendToRoom(room: StudyRoomItem, friendUid: String) {
        viewModelScope.launch {
            repository.inviteFriendToRoom(room, friendUid)
                .onSuccess { _socialEvents.tryEmit("Friend invited to ${room.title}") }
                .onFailure { _socialEvents.tryEmit(it.localizedMessage ?: "Invite failed") }
        }
    }

    fun updateRoomTimer(room: StudyRoomItem, state: RoomTimerState, durationMinutes: Long, remainingSeconds: Long) {
        viewModelScope.launch {
            repository.updateRoomTimer(room, state, durationMinutes, remainingSeconds)
                .onFailure { _socialEvents.tryEmit(it.localizedMessage ?: "Could not update shared timer") }
        }
    }

    fun sendRoomMessage(room: StudyRoomItem, text: String) {
        val myName = (myPublicProfile.value as? UiState.Success)?.data?.username ?: "Student"
        viewModelScope.launch {
            repository.sendRoomMessage(room, currentUserId, myName, text)
                .onFailure { _socialEvents.tryEmit(it.localizedMessage ?: "Could not send message") }
        }
    }

    fun submitReport(targetType: String, targetId: String, reason: String, details: String) {
        viewModelScope.launch {
            repository.submitReport(currentUserId, targetType, targetId, reason, details)
                .onSuccess { _socialEvents.tryEmit("Report submitted for moderation review.") }
                .onFailure { _socialEvents.tryEmit(it.localizedMessage ?: "Report failed") }
        }
    }
}
