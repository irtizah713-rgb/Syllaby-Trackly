package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Forest
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.FriendLinkItem
import com.example.data.model.RoomEnvironment
import com.example.data.model.RoomMessageItem
import com.example.data.model.RoomPrivacy
import com.example.data.model.RoomTimerState
import com.example.data.model.StudyRoomItem
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MinimalCard
import com.example.ui.components.OfflineNoticeBanner
import com.example.ui.components.SectionHeader
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalSyllabyColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class RoomListTab(val label: String) {
    PUBLIC("Public Rooms"),
    MINE("My Rooms"),
    INVITES("Invites")
}

@Composable
fun StudyRoomsScreen(
    currentUserId: String,
    isOnline: Boolean,
    publicRooms: List<StudyRoomItem>,
    myRooms: List<StudyRoomItem>,
    invitedRooms: List<StudyRoomItem>,
    friends: List<FriendLinkItem>,
    activeRoomMessages: List<RoomMessageItem>,
    onBack: () -> Unit,
    onSelectRoomForChat: (StudyRoomItem?) -> Unit,
    onCreateRoom: (String, String, RoomEnvironment, RoomPrivacy, List<String>, (String) -> Unit) -> Unit,
    onJoinRoom: (StudyRoomItem) -> Unit,
    onLeaveRoom: (StudyRoomItem) -> Unit,
    onInviteFriend: (StudyRoomItem, String) -> Unit,
    onUpdateRoomTimer: (StudyRoomItem, RoomTimerState, Long, Long) -> Unit,
    onSendMessage: (StudyRoomItem, String) -> Unit,
    onSubmitReport: (String, String, String, String) -> Unit
) {
    val colors = LocalSyllabyColors.current
    var selectedTab by remember { mutableStateOf(RoomListTab.PUBLIC) }
    var activeRoomId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val allKnownRooms = remember(publicRooms, myRooms, invitedRooms) {
        (myRooms + publicRooms + invitedRooms).distinctBy { it.id }
    }
    val activeRoom = remember(activeRoomId, allKnownRooms) {
        allKnownRooms.firstOrNull { it.id == activeRoomId }
    }

    BackHandler {
        if (activeRoom != null) {
            activeRoomId = null
            onSelectRoomForChat(null)
        } else {
            onBack()
        }
    }

    if (activeRoom != null) {
        ActiveStudyRoomView(
            room = activeRoom,
            currentUserId = currentUserId,
            isOnline = isOnline,
            friends = friends,
            messages = activeRoomMessages,
            onBack = {
                activeRoomId = null
                onSelectRoomForChat(null)
            },
            onLeave = {
                onLeaveRoom(activeRoom)
                activeRoomId = null
            },
            onInviteFriend = { uid -> onInviteFriend(activeRoom, uid) },
            onUpdateTimer = { st, dur, rem -> onUpdateRoomTimer(activeRoom, st, dur, rem) },
            onSendMessage = { text -> onSendMessage(activeRoom, text) },
            onReport = { targetType, targetId, reason, details ->
                onSubmitReport(targetType, targetId, reason, details)
            }
        )
        return
    }

    val displayedRooms = when (selectedTab) {
        RoomListTab.PUBLIC -> publicRooms
        RoomListTab.MINE -> myRooms
        RoomListTab.INVITES -> invitedRooms
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("study_rooms_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = colors.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "Study Rooms",
                                style = MaterialTheme.typography.headlineMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Shared focus timers & silent accountability",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Button(
                        onClick = { showCreateDialog = true },
                        enabled = isOnline,
                        modifier = Modifier.testTag("create_room_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.onAccent
                        )
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create")
                    }
                }
            }

            if (!isOnline) {
                item { OfflineNoticeBanner("Study Rooms") }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoomListTab.entries.forEach { tab ->
                        val countSuffix = when (tab) {
                            RoomListTab.PUBLIC -> " (${publicRooms.size})"
                            RoomListTab.MINE -> " (${myRooms.size})"
                            RoomListTab.INVITES -> if (invitedRooms.isNotEmpty()) " (${invitedRooms.size})" else ""
                        }
                        FilterChip(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            label = { Text("${tab.label}$countSuffix") },
                            modifier = Modifier.testTag("room_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }

            if (displayedRooms.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Outlined.Groups,
                        title = when (selectedTab) {
                            RoomListTab.PUBLIC -> "No public study rooms open"
                            RoomListTab.MINE -> "You haven't joined any rooms yet"
                            RoomListTab.INVITES -> "No pending room invitations"
                        },
                        subtitle = if (isOnline) {
                            "Create a Forest, Library, or Classroom room to study live with friends."
                        } else {
                            "Reconnect to the internet to browse and join live study rooms."
                        },
                        actionLabel = if (isOnline) "Create Study Room" else null,
                        testTag = "empty_create_room_button",
                        onAction = if (isOnline) ({ showCreateDialog = true }) else null
                    )
                }
            } else {
                items(displayedRooms, key = { it.id }) { room ->
                    StudyRoomCard(
                        room = room,
                        isMember = currentUserId in room.memberUids,
                        onOpen = {
                            if (currentUserId in room.memberUids) {
                                activeRoomId = room.id
                                onSelectRoomForChat(room)
                            } else {
                                onJoinRoom(room)
                                activeRoomId = room.id
                            }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showCreateDialog) {
        CreateStudyRoomDialog(
            friends = friends,
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, desc, env, privacy, invited ->
                showCreateDialog = false
                onCreateRoom(title, desc, env, privacy, invited) { createdId ->
                    activeRoomId = createdId
                }
            }
        )
    }
}

@Composable
private fun StudyRoomCard(
    room: StudyRoomItem,
    isMember: Boolean,
    onOpen: () -> Unit
) {
    val colors = LocalSyllabyColors.current
    val env = room.roomEnvironment()
    val privacy = room.roomPrivacy()
    val envIcon = when (env) {
        RoomEnvironment.FOREST -> Icons.Outlined.Forest
        RoomEnvironment.LIBRARY -> Icons.Outlined.LocalLibrary
        RoomEnvironment.CLASSROOM -> Icons.Outlined.School
    }

    MinimalCard(onClick = onOpen, modifier = Modifier.testTag("study_room_card_${room.id}")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(envIcon, contentDescription = env.label, tint = colors.accent)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = room.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (privacy == RoomPrivacy.PUBLIC) Icons.Outlined.Public else Icons.Outlined.Lock,
                            contentDescription = privacy.label,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = "${env.label} • Host: ${room.hostName} • ${room.memberUids.size} inside",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isMember) colors.surfaceElevated else colors.accent
            ) {
                Text(
                    text = if (isMember) "Open" else "Join",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isMember) colors.textPrimary else colors.onAccent,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        if (room.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = room.description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActiveStudyRoomView(
    room: StudyRoomItem,
    currentUserId: String,
    isOnline: Boolean,
    friends: List<FriendLinkItem>,
    messages: List<RoomMessageItem>,
    onBack: () -> Unit,
    onLeave: () -> Unit,
    onInviteFriend: (String) -> Unit,
    onUpdateTimer: (RoomTimerState, Long, Long) -> Unit,
    onSendMessage: (String) -> Unit,
    onReport: (String, String, String, String) -> Unit
) {
    val colors = LocalSyllabyColors.current
    val env = room.roomEnvironment()
    val timerState = room.roomTimerState()

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timerState, room.timerUpdatedAtMillis) {
        while (timerState == RoomTimerState.RUNNING || timerState == RoomTimerState.BREAK) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val liveRemainingSeconds = remember(room, nowMillis) {
        if ((timerState == RoomTimerState.RUNNING || timerState == RoomTimerState.BREAK) && room.timerUpdatedAtMillis > 0L) {
            val elapsed = ((nowMillis - room.timerUpdatedAtMillis) / 1000L).coerceAtLeast(0L)
            (room.timerRemainingSeconds - elapsed).coerceAtLeast(0L)
        } else {
            room.timerRemainingSeconds
        }
    }

    val mins = liveRemainingSeconds / 60L
    val secs = liveRemainingSeconds % 60L
    val timeLabel = String.format(Locale.US, "%02d:%02d", mins, secs)

    var messageInput by remember { mutableStateOf("") }
    var showInviteDialog by remember { mutableStateOf(false) }
    var reportTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    val envAccent = when (env) {
        RoomEnvironment.FOREST -> Color(0xFF34D399)
        RoomEnvironment.LIBRARY -> Color(0xFFFBBF24)
        RoomEnvironment.CLASSROOM -> Color(0xFF38BDF8)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .padding(16.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("active_room_back_button")) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                    }
                    Column {
                        Text(
                            text = room.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${env.label} • Code ${room.inviteCode} • ${room.memberUids.size} members",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
                Row {
                    IconButton(onClick = { showInviteDialog = true }, modifier = Modifier.testTag("invite_to_room_button")) {
                        Icon(Icons.Outlined.PersonAdd, contentDescription = "Invite friend", tint = colors.accent)
                    }
                    IconButton(
                        onClick = { reportTarget = "ROOM" to room.id },
                        modifier = Modifier.testTag("report_room_button")
                    ) {
                        Icon(Icons.Outlined.Flag, contentDescription = "Report room", tint = colors.textSecondary)
                    }
                    IconButton(onClick = onLeave, modifier = Modifier.testTag("leave_room_button")) {
                        Icon(Icons.AutoMirrored.Outlined.ExitToApp, contentDescription = "Leave room", tint = colors.danger)
                    }
                }
            }

            if (!isOnline) {
                Spacer(modifier = Modifier.height(8.dp))
                OfflineNoticeBanner("Study Room Sync")
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Shared Timer Card
            MinimalCard(highlightBorder = envAccent.copy(alpha = 0.45f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "${env.label.uppercase()} • ${timerState.label}",
                            style = MaterialTheme.typography.labelMedium,
                            color = envAccent
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = timeLabel,
                            style = MaterialTheme.typography.displayMedium.copy(fontFamily = JetBrainsMonoFamily),
                            color = colors.textPrimary
                        )
                        Text(
                            text = env.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (timerState == RoomTimerState.RUNNING || timerState == RoomTimerState.BREAK) {
                            OutlinedButton(
                                onClick = {
                                    onUpdateTimer(RoomTimerState.PAUSED, room.timerDurationMinutes, liveRemainingSeconds)
                                },
                                modifier = Modifier.testTag("room_timer_pause_button")
                            ) {
                                Icon(Icons.Outlined.Pause, contentDescription = "Pause", modifier = Modifier.size(18.dp))
                            }
                        } else {
                            Button(
                                onClick = {
                                    val startSecs = if (liveRemainingSeconds <= 0L) 25L * 60L else liveRemainingSeconds
                                    onUpdateTimer(RoomTimerState.RUNNING, 25L, startSecs)
                                },
                                modifier = Modifier.testTag("room_timer_start_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = envAccent, contentColor = Color(0xFF061310))
                            ) {
                                Icon(Icons.Outlined.PlayArrow, contentDescription = "Start", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Focus")
                            }
                        }
                        IconButton(
                            onClick = { onUpdateTimer(RoomTimerState.BREAK, 5L, 5L * 60L) },
                            modifier = Modifier.testTag("room_timer_break_button")
                        ) {
                            Icon(Icons.Outlined.Coffee, contentDescription = "5m Break", tint = colors.textSecondary)
                        }
                        IconButton(
                            onClick = { onUpdateTimer(RoomTimerState.IDLE, 25L, 25L * 60L) },
                            modifier = Modifier.testTag("room_timer_reset_button")
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Reset Timer", tint = colors.textSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            SectionHeader(
                title = "Room Chat",
                subtitle = "Text-only study chat • messages auto-expire after 7 days"
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Outlined.Timer,
                            title = "Quiet focus in progress",
                            subtitle = "Say hello or share what chapter you are working on."
                        )
                    }
                } else {
                    items(messages, key = { it.id }) { msg ->
                        val mine = msg.senderUid == currentUserId
                        val ts = msg.createdAt?.toDate()?.let {
                            SimpleDateFormat("HH:mm", Locale.US).format(it)
                        } ?: ""
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = if (mine) colors.accent.copy(alpha = 0.12f) else colors.surface,
                            border = BorderStroke(1.dp, if (mine) colors.accent.copy(alpha = 0.35f) else colors.borderSubtle)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (mine) "You" else msg.senderName,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (mine) colors.accent else colors.textPrimary
                                        )
                                        if (ts.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = ts,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.textSecondary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.textPrimary
                                    )
                                }
                                if (!mine) {
                                    IconButton(
                                        onClick = { reportTarget = "MESSAGE" to msg.id },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Flag,
                                            contentDescription = "Report message",
                                            tint = colors.textSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { if (it.length <= 500) messageInput = it },
                    placeholder = { Text("Share study update (max 500 chars)...") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("room_chat_input")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    onClick = {
                        val trimmed = messageInput.trim()
                        if (trimmed.isNotBlank() && isOnline) {
                            onSendMessage(trimmed)
                            messageInput = ""
                        }
                    },
                    shape = CircleShape,
                    color = if (messageInput.isNotBlank() && isOnline) colors.accent else colors.surfaceElevated,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("room_chat_send_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Send,
                            contentDescription = "Send message",
                            tint = if (messageInput.isNotBlank() && isOnline) colors.onAccent else colors.textSecondary
                        )
                    }
                }
            }
        }
    }

    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Invite Friend") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Room Invite Code: ${room.inviteCode}",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.accent
                    )
                    if (friends.isEmpty()) {
                        Text(
                            text = "Add friends in the Friends screen to send direct room invitations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    } else {
                        friends.forEach { friend ->
                            val already = friend.friendUid in room.invitedUids || friend.friendUid in room.memberUids
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = !already) {
                                        onInviteFriend(friend.friendUid)
                                        showInviteDialog = false
                                    }
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(friend.friendUsername, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = if (already) "Invited" else "Invite",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (already) colors.textSecondary else colors.accent
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInviteDialog = false }) { Text("Done") }
            }
        )
    }

    reportTarget?.let { (type, targetId) ->
        var reason by remember { mutableStateOf("Spam or off-topic") }
        var details by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { reportTarget = null },
            title = { Text("Report Content") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Spam or off-topic", "Harassment", "Inappropriate content").forEach { option ->
                        FilterChip(
                            selected = reason == option,
                            onClick = { reason = option },
                            label = { Text(option) }
                        )
                    }
                    OutlinedTextField(
                        value = details,
                        onValueChange = { details = it },
                        label = { Text("Additional details (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onReport(type, targetId, reason, details)
                    reportTarget = null
                }) { Text("Submit Report", color = colors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { reportTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CreateStudyRoomDialog(
    friends: List<FriendLinkItem>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, RoomEnvironment, RoomPrivacy, List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var environment by remember { mutableStateOf(RoomEnvironment.LIBRARY) }
    var privacy by remember { mutableStateOf(RoomPrivacy.PUBLIC) }
    var selectedFriends by remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Study Room") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 60) title = it },
                    label = { Text("Room Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_room_title_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 160) description = it },
                    label = { Text("Study Goal / Description") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Environment", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RoomEnvironment.entries.forEach { env ->
                        FilterChip(
                            selected = environment == env,
                            onClick = { environment = env },
                            label = { Text(env.label) }
                        )
                    }
                }
                Text("Privacy", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoomPrivacy.entries.forEach { p ->
                        FilterChip(
                            selected = privacy == p,
                            onClick = { privacy = p },
                            label = { Text(p.label) }
                        )
                    }
                }
                if (friends.isNotEmpty()) {
                    Text("Invite Friends (${selectedFriends.size})", style = MaterialTheme.typography.labelLarge)
                    friends.take(4).forEach { f ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedFriends = if (f.friendUid in selectedFriends) {
                                        selectedFriends - f.friendUid
                                    } else {
                                        selectedFriends + f.friendUid
                                    }
                                }
                        ) {
                            Checkbox(
                                checked = f.friendUid in selectedFriends,
                                onCheckedChange = { checked ->
                                    selectedFriends = if (checked) selectedFriends + f.friendUid else selectedFriends - f.friendUid
                                }
                            )
                            Text(f.friendUsername, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val clean = title.trim()
                    if (clean.isNotBlank()) {
                        onConfirm(clean, description.trim(), environment, privacy, selectedFriends.toList())
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_room_button")
            ) {
                Text("Create Room")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
