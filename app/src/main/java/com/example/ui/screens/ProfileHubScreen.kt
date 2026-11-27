package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.GamificationCatalog
import com.example.data.model.PublicProfile
import com.example.data.model.UserAccount
import com.example.ui.components.AvatarWithFrame
import com.example.ui.components.MinimalCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalSyllabyColors

@Composable
fun ProfileHubScreen(
    account: UserAccount?,
    publicProfile: PublicProfile?,
    unreadNotificationCount: Int,
    onUpdateProfile: (
        displayName: String,
        bio: String?,
        session: String?,
        academicClass: String?,
        stream: String?,
        dailyGoalMins: Long,
        discoverable: Boolean,
        showStats: Boolean,
        showAchievements: Boolean
    ) -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    onNavigateToGamification: () -> Unit,
    onNavigateToStudyRooms: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToThemes: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onSignOut: () -> Unit
) {
    val colors = LocalSyllabyColors.current
    var showEditDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    val name = account?.displayName?.ifBlank { "Scholar" } ?: "Scholar"
    val xp = account?.xp ?: 0L
    val level = account?.level ?: 1L
    val rank = GamificationCatalog.rankForLevel(level)
    val (xpInLevel, xpNeeded) = remember(xp) { GamificationCatalog.xpIntoCurrentLevel(xp) }
    val xpFraction = (xpInLevel.toFloat() / xpNeeded.coerceAtLeast(1L)).coerceIn(0f, 1f)

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
                MinimalCard(highlightBorder = colors.accent.copy(alpha = 0.35f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            AvatarWithFrame(
                                name = name,
                                photoUrl = account?.photoUrl,
                                frameId = account?.selectedFrameId ?: "frame_default",
                                size = 56.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(name, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                                Text(
                                    text = "$rank • Level $level",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.accent
                                )
                                val academicLine = listOfNotNull(
                                    account?.academicSession?.takeIf { it.isNotBlank() },
                                    account?.academicClass?.takeIf { it.isNotBlank() },
                                    account?.academicStream?.takeIf { it.isNotBlank() }
                                ).joinToString(" • ")
                                if (academicLine.isNotBlank()) {
                                    Text(academicLine, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                }
                            }
                        }
                        IconButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit profile", tint = colors.accent)
                        }
                    }

                    if (!account?.bio.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(account!!.bio!!, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("XP Progress", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                        Text(
                            "$xpInLevel / $xpNeeded XP",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                            color = colors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { xpFraction },
                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                        color = colors.accent,
                        trackColor = colors.borderSubtle
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        HubMetric("Streak", "${account?.currentStreak ?: 0L}d")
                        HubMetric("Focus", "${(account?.totalFocusMinutes ?: 0L) / 60L}h ${(account?.totalFocusMinutes ?: 0L) % 60L}m")
                        HubMetric("Points", "${account?.themePoints ?: 0L} TP")
                        HubMetric("Code", publicProfile?.friendCode ?: "...")
                    }
                }
            }

            item { SectionHeader(title = "Academic Workspace") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HubActionTile("Notes", "Rich study notes", Icons.Outlined.Description, "hub_notes", Modifier.weight(1f), onNavigateToNotes)
                        HubActionTile("Analytics", "Charts & heatmaps", Icons.Outlined.Analytics, "hub_analytics", Modifier.weight(1f), onNavigateToAnalytics)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HubActionTile("Attendance", "Monthly calendar", Icons.Outlined.CalendarMonth, "hub_attendance", Modifier.weight(1f), onNavigateToAttendance)
                        HubActionTile("Rewards", "Ranks & Theme Shop", Icons.Outlined.EmojiEvents, "hub_gamification", Modifier.weight(1f), onNavigateToGamification)
                    }
                }
            }

            item { SectionHeader(title = "Social & Study Rooms") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HubActionTile("Study Rooms", "Shared timers & chat", Icons.Outlined.Groups, "hub_study_rooms", Modifier.weight(1f), onNavigateToStudyRooms)
                    HubActionTile("Friends", "Codes & requests", Icons.Outlined.Group, "hub_friends", Modifier.weight(1f), onNavigateToFriends)
                }
            }

            item { SectionHeader(title = "Customization & Data") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HubActionTile("Theme Studio", "8 themes & accents", Icons.Outlined.Palette, "hub_themes", Modifier.weight(1f), onNavigateToThemes)
                        HubActionTile(
                            title = if (unreadNotificationCount > 0) "Alerts ($unreadNotificationCount)" else "Notifications",
                            subtitle = "Feed & permissions",
                            icon = Icons.Outlined.Notifications,
                            tag = "hub_notifications",
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToNotifications
                        )
                    }
                    HubActionTile("Backup, Export & Privacy", "JSON snapshots & friend visibility", Icons.Outlined.Backup, "hub_backup", Modifier.fillMaxWidth(), onNavigateToBackup)
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { showSignOutDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sign_out_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, colors.danger.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = colors.danger, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out", color = colors.danger)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showEditDialog) {
        var editName by remember { mutableStateOf(account?.displayName ?: "") }
        var editBio by remember { mutableStateOf(account?.bio ?: "") }
        var editSession by remember { mutableStateOf(account?.academicSession ?: "") }
        var editClass by remember { mutableStateOf(account?.academicClass ?: "") }
        var editStream by remember { mutableStateOf(account?.academicStream ?: "") }
        var editGoal by remember { mutableStateOf((account?.dailyGoalMinutes ?: 60L).toString()) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Student Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_name_input")
                    )
                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Academic Bio") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editSession,
                            onValueChange = { editSession = it },
                            label = { Text("Session (e.g. 2026-27)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editClass,
                            onValueChange = { editClass = it },
                            label = { Text("Class / Grade") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editStream,
                            onValueChange = { editStream = it },
                            label = { Text("Stream / Major") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editGoal,
                            onValueChange = { v -> editGoal = v.filter { it.isDigit() } },
                            label = { Text("Daily Goal (min)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val goal = (editGoal.toLongOrNull() ?: 60L).coerceIn(10L, 720L)
                        onUpdateProfile(
                            editName.trim().ifBlank { "Scholar" },
                            editBio.trim().ifBlank { null },
                            editSession.trim().ifBlank { null },
                            editClass.trim().ifBlank { null },
                            editStream.trim().ifBlank { null },
                            goal,
                            publicProfile?.discoverable ?: true,
                            publicProfile?.showStats ?: true,
                            publicProfile?.showAchievements ?: true
                        )
                        showEditDialog = false
                    },
                    modifier = Modifier.testTag("save_profile_button")
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out?") },
            text = { Text("Your syllabus, tasks, notes, and focus sessions remain safely synced to your Google account.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    },
                    modifier = Modifier.testTag("confirm_sign_out_button")
                ) { Text("Sign Out", color = colors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun HubMetric(label: String, value: String) {
    val colors = LocalSyllabyColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold
            ),
            color = colors.textPrimary
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
    }
}

@Composable
private fun HubActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalSyllabyColors.current
    Surface(
        onClick = onClick,
        modifier = modifier.testTag(tag),
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = colors.accent, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
    }
}
