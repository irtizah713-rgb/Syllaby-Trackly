package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.NotificationLogEntity
import com.example.data.local.NotificationPreferences
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MinimalCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LocalSyllabyColors
import com.example.util.NotificationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationsScreen(
    notifications: List<NotificationLogEntity>,
    prefs: NotificationPreferences,
    onUpdatePrefs: (NotificationPreferences) -> Unit,
    onMarkAllRead: () -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalSyllabyColors.current
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(NotificationHelper.hasNotificationPermission(context)) }
    var permissionDeniedNote by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        permissionDeniedNote = !granted
    }

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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("notifications_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text("Notification Centre", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                            Text("System alerts & academic activity feed", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                    Row {
                        IconButton(onClick = onMarkAllRead, modifier = Modifier.testTag("mark_notifications_read_button")) {
                            Icon(Icons.Outlined.DoneAll, contentDescription = "Mark all read", tint = colors.accent)
                        }
                        IconButton(onClick = onClearAll, modifier = Modifier.testTag("clear_notifications_button")) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Clear all", tint = colors.textSecondary)
                        }
                    }
                }
            }

            if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item {
                    MinimalCard(highlightBorder = colors.accent.copy(alpha = 0.4f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Enable Device Notifications", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                Text(
                                    text = if (permissionDeniedNote) {
                                        "Permission was denied. You can enable alerts anytime in Android Settings."
                                    } else {
                                        "Allow Pomodoro completions and achievement alerts on your lock screen."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                modifier = Modifier.testTag("grant_notifications_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent)
                            ) {
                                Text("Allow")
                            }
                        }
                    }
                }
            }

            item {
                MinimalCard {
                    SectionHeader(title = "Alert Preferences")
                    Spacer(modifier = Modifier.height(8.dp))
                    NotifSwitchRow(
                        label = "Focus Timer Completion",
                        checked = prefs.focusCompletion,
                        tag = "notif_focus_switch",
                        onToggle = { onUpdatePrefs(prefs.copy(focusCompletion = it)) }
                    )
                    NotifSwitchRow(
                        label = "Task Deadline Reminders",
                        checked = prefs.taskReminders,
                        tag = "notif_task_switch",
                        onToggle = { onUpdatePrefs(prefs.copy(taskReminders = it)) }
                    )
                    NotifSwitchRow(
                        label = "Daily Study Streak Alerts",
                        checked = prefs.streakAlerts,
                        tag = "notif_streak_switch",
                        onToggle = { onUpdatePrefs(prefs.copy(streakAlerts = it)) }
                    )
                    NotifSwitchRow(
                        label = "Level Ups & Achievements",
                        checked = prefs.achievementAlerts,
                        tag = "notif_achievement_switch",
                        onToggle = { onUpdatePrefs(prefs.copy(achievementAlerts = it)) }
                    )
                }
            }

            item { SectionHeader(title = "Recent Alerts (${notifications.size})") }

            if (notifications.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Outlined.Notifications,
                        title = "All caught up",
                        subtitle = "Pomodoro completions, level-ups, and study alerts appear here."
                    )
                }
            } else {
                items(notifications, key = { it.id }) { item ->
                    val ts = remember(item.createdAtMillis) {
                        SimpleDateFormat("MMM d • HH:mm", Locale.US).format(Date(item.createdAtMillis))
                    }
                    MinimalCard(
                        highlightBorder = if (!item.read) colors.accent.copy(alpha = 0.45f) else null
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsActive,
                                    contentDescription = item.category,
                                    tint = if (!item.read) colors.accent else colors.textSecondary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(item.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                    Text(item.body, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                }
                            }
                            Text(ts, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun NotifSwitchRow(
    label: String,
    checked: Boolean,
    tag: String,
    onToggle: (Boolean) -> Unit
) {
    val colors = LocalSyllabyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            modifier = Modifier.testTag(tag)
        )
    }
}
