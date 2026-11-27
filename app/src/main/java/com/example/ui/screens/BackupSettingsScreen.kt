package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.data.model.ChapterItem
import com.example.data.model.PublicProfile
import com.example.data.model.SubjectItem
import com.example.data.model.TaskItem
import com.example.ui.components.MinimalCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LocalSyllabyColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupSettingsScreen(
    lastBackupMillis: Long,
    savedBackupJson: String,
    subjects: List<SubjectItem>,
    chapters: List<ChapterItem>,
    tasks: List<TaskItem>,
    publicProfile: PublicProfile?,
    onCreateBackup: ((String) -> Unit) -> Unit,
    onRestoreBackup: (String) -> Unit,
    onUpdatePrivacy: (Boolean, Boolean, Boolean) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalSyllabyColors.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonInput by remember { mutableStateOf("") }

    BackHandler { onBack() }

    val lastBackupLabel = remember(lastBackupMillis) {
        if (lastBackupMillis <= 0L) "No manual local snapshot yet"
        else "Last snapshot: ${SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.US).format(Date(lastBackupMillis))}"
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("backup_settings_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text("Backup & Privacy", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                        Text("JSON snapshots, academic exports & social visibility", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
            }

            item {
                MinimalCard {
                    SectionHeader(title = "Local Backup & Restore", subtitle = lastBackupLabel)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Contains ${subjects.size} subjects, ${chapters.size} chapters, and ${tasks.size} tasks plus notes, sessions, and attendance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onCreateBackup {} },
                            modifier = Modifier.weight(1f).testTag("save_local_backup_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Outlined.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Snapshot")
                        }
                        OutlinedButton(
                            onClick = {
                                restoreJsonInput = savedBackupJson
                                showRestoreDialog = true
                            },
                            modifier = Modifier.weight(1f).testTag("open_restore_backup_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore JSON")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                onCreateBackup { json ->
                                    clipboard.setText(AnnotatedString(json))
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("copy_backup_json_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy JSON")
                        }
                        OutlinedButton(
                            onClick = {
                                onCreateBackup { json ->
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "SyllabyTrackly Backup")
                                        putExtra(Intent.EXTRA_TEXT, json)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Export SyllabyTrackly Backup"))
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("share_backup_json_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share File")
                        }
                    }
                }
            }

            item {
                val discoverable = publicProfile?.discoverable ?: true
                val showStats = publicProfile?.showStats ?: true
                val showAchievements = publicProfile?.showAchievements ?: true

                MinimalCard {
                    SectionHeader(title = "Social Privacy Controls", subtitle = "Control what friends & search see")
                    Spacer(modifier = Modifier.height(8.dp))
                    PrivacyToggleRow(
                        title = "Discoverable by Friend Code",
                        subtitle = "Allow classmates with your exact code to find you",
                        checked = discoverable,
                        tag = "privacy_discoverable_switch",
                        onToggle = { onUpdatePrivacy(it, showStats, showAchievements) }
                    )
                    PrivacyToggleRow(
                        title = "Share XP, Streak & Focus Stats",
                        subtitle = "Visible to accepted friends on your study card",
                        checked = showStats,
                        tag = "privacy_show_stats_switch",
                        onToggle = { onUpdatePrivacy(discoverable, it, showAchievements) }
                    )
                    PrivacyToggleRow(
                        title = "Share Unlocked Achievements",
                        subtitle = "Highlight earned academic badges to friends",
                        checked = showAchievements,
                        tag = "privacy_show_achievements_switch",
                        onToggle = { onUpdatePrivacy(discoverable, showStats, it) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Backup Snapshot") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Paste a previously exported SyllabyTrackly JSON backup. Matching subjects, chapters, tasks, and notes will be restored.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    OutlinedTextField(
                        value = restoreJsonInput,
                        onValueChange = { restoreJsonInput = it },
                        label = { Text("Backup JSON") },
                        minLines = 5,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth().testTag("restore_json_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (restoreJsonInput.isNotBlank()) {
                            onRestoreBackup(restoreJsonInput)
                            showRestoreDialog = false
                        }
                    },
                    enabled = restoreJsonInput.isNotBlank(),
                    modifier = Modifier.testTag("confirm_restore_backup_button")
                ) {
                    Text("Restore Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PrivacyToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    tag: String,
    onToggle: (Boolean) -> Unit
) {
    val colors = LocalSyllabyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            modifier = Modifier.testTag(tag)
        )
    }
}
