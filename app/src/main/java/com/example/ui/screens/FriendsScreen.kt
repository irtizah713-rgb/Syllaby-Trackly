package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.FriendLinkItem
import com.example.data.model.FriendRequestItem
import com.example.data.model.PublicProfile
import com.example.ui.components.AvatarWithFrame
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MinimalCard
import com.example.ui.components.OfflineNoticeBanner
import com.example.ui.components.SectionHeader
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalSyllabyColors

@Composable
fun FriendsScreen(
    isOnline: Boolean,
    myPublicProfile: PublicProfile?,
    friends: List<FriendLinkItem>,
    incomingRequests: List<FriendRequestItem>,
    outgoingRequests: List<FriendRequestItem>,
    searchedProfile: PublicProfile?,
    viewedFriendProfile: PublicProfile?,
    onBack: () -> Unit,
    onSearchByCode: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSendRequest: (PublicProfile) -> Unit,
    onRespondRequest: (FriendRequestItem, Boolean) -> Unit,
    onInspectFriend: (String) -> Unit,
    onClearInspectedFriend: () -> Unit,
    onRemoveOrBlockFriend: (String, Boolean) -> Unit,
    onReportUser: (String, String, String) -> Unit
) {
    val colors = LocalSyllabyColors.current
    val clipboard = LocalClipboardManager.current
    var searchCode by remember { mutableStateOf("") }
    var codeCopied by remember { mutableStateOf(false) }

    BackHandler { onBack() }

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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("friends_back_button")
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
                            text = "Friends & Accountability",
                            style = MaterialTheme.typography.headlineMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Connect via private student friend codes",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            if (!isOnline) {
                item { OfflineNoticeBanner("Friends & Requests") }
            }

            // My Friend Code card
            item {
                val myCode = myPublicProfile?.friendCode?.ifBlank { "Generating..." } ?: "Loading..."
                MinimalCard(highlightBorder = colors.accent.copy(alpha = 0.35f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "YOUR FRIEND CODE",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = myCode,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.accent
                            )
                            Text(
                                text = if (myPublicProfile?.discoverable != false) {
                                    "Discoverable in search"
                                } else {
                                    "Hidden from friend code search"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                        IconButton(
                            onClick = {
                                if (myPublicProfile != null && myPublicProfile.friendCode.isNotBlank()) {
                                    clipboard.setText(AnnotatedString(myPublicProfile.friendCode))
                                    codeCopied = true
                                }
                            },
                            modifier = Modifier.testTag("copy_friend_code_button")
                        ) {
                            Icon(
                                imageVector = if (codeCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                                contentDescription = "Copy friend code",
                                tint = colors.accent
                            )
                        }
                    }
                }
            }

            // Search by Friend Code
            item {
                MinimalCard {
                    SectionHeader(title = "Find a Study Partner")
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchCode,
                            onValueChange = { searchCode = it.uppercase().take(12) },
                            placeholder = { Text("Enter friend code (e.g. ST-A1B2C3)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("friend_code_search_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (searchCode.isNotBlank()) onSearchByCode(searchCode)
                            },
                            enabled = isOnline && searchCode.isNotBlank(),
                            modifier = Modifier.testTag("friend_code_search_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = colors.onAccent
                            )
                        ) {
                            Icon(Icons.Outlined.Search, contentDescription = "Search")
                        }
                    }

                    if (searchedProfile != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.surfaceElevated
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    AvatarWithFrame(
                                        name = searchedProfile.username,
                                        photoUrl = null,
                                        frameId = searchedProfile.selectedFrameId,
                                        size = 38.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = searchedProfile.username,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = "${searchedProfile.rankTitle} • Lvl ${searchedProfile.level}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                                Row {
                                    IconButton(onClick = onClearSearch) {
                                        Icon(Icons.Outlined.Close, contentDescription = "Dismiss", tint = colors.textSecondary)
                                    }
                                    Button(
                                        onClick = { onSendRequest(searchedProfile) },
                                        modifier = Modifier.testTag("send_friend_request_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.accent,
                                            contentColor = colors.onAccent
                                        )
                                    ) {
                                        Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Incoming Requests
            if (incomingRequests.isNotEmpty()) {
                item { SectionHeader(title = "Incoming Requests (${incomingRequests.size})") }
                items(incomingRequests, key = { it.id }) { req ->
                    MinimalCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(req.fromUsername, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                Text("Code: ${req.fromFriendCode}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = { onRespondRequest(req, false) },
                                    modifier = Modifier.testTag("decline_request_${req.id}")
                                ) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Decline", tint = colors.danger)
                                }
                                IconButton(
                                    onClick = { onRespondRequest(req, true) },
                                    modifier = Modifier.testTag("accept_request_${req.id}")
                                ) {
                                    Icon(Icons.Outlined.Check, contentDescription = "Accept", tint = colors.success)
                                }
                            }
                        }
                    }
                }
            }

            // Outgoing Requests
            if (outgoingRequests.isNotEmpty()) {
                item { SectionHeader(title = "Sent Requests (${outgoingRequests.size})") }
                items(outgoingRequests, key = { it.id }) { req ->
                    MinimalCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(req.toUsername, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                            Text("Pending", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                        }
                    }
                }
            }

            // Friends list
            item { SectionHeader(title = "Your Study Circle (${friends.size})") }
            if (friends.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Outlined.Group,
                        title = "No friends added yet",
                        subtitle = "Share your Friend Code or search a classmate's code above to invite them to Study Rooms."
                    )
                }
            } else {
                items(friends, key = { it.id }) { friend ->
                    MinimalCard(
                        onClick = { onInspectFriend(friend.friendUid) },
                        modifier = Modifier.testTag("friend_card_${friend.friendUid}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(friend.friendUsername, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                Text(
                                    text = "Code ${friend.friendCode} • Tap to view academic card",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = { onRemoveOrBlockFriend(friend.friendUid, false) },
                                    modifier = Modifier.testTag("remove_friend_${friend.friendUid}")
                                ) {
                                    Icon(Icons.Outlined.PersonRemove, contentDescription = "Remove friend", tint = colors.textSecondary)
                                }
                                IconButton(
                                    onClick = { onRemoveOrBlockFriend(friend.friendUid, true) },
                                    modifier = Modifier.testTag("block_friend_${friend.friendUid}")
                                ) {
                                    Icon(Icons.Outlined.Block, contentDescription = "Block user", tint = colors.danger)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    viewedFriendProfile?.let { profile ->
        AlertDialog(
            onDismissRequest = onClearInspectedFriend,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarWithFrame(
                        name = profile.username,
                        photoUrl = null,
                        frameId = profile.selectedFrameId,
                        size = 42.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(profile.username, style = MaterialTheme.typography.titleMedium)
                        Text(profile.friendCode, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!profile.bio.isNullOrBlank()) {
                        Text(profile.bio, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (!profile.academicClass.isNullOrBlank() || !profile.academicStream.isNullOrBlank()) {
                        Text(
                            text = listOfNotNull(profile.academicClass, profile.academicStream).joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                    if (profile.showStats) {
                        MinimalCard {
                            Text("Rank: ${profile.rankTitle} (Lvl ${profile.level})", style = MaterialTheme.typography.labelLarge, color = colors.accent)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("XP: ${profile.xp} • Streak: ${profile.currentStreak}d • Focus: ${profile.totalFocusMinutes}m", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Text("This student keeps study stats private.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onClearInspectedFriend) { Text("Close") }
            },
            dismissButton = {
                TextButton(onClick = {
                    onReportUser(profile.userId, "User Report", "Reported from friend profile")
                    onClearInspectedFriend()
                }) {
                    Icon(Icons.Outlined.Flag, contentDescription = null, tint = colors.danger, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Report", color = colors.danger)
                }
            }
        )
    }
}
