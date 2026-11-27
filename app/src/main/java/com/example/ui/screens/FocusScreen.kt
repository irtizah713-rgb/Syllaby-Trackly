package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FocusSessionItem
import com.example.data.model.SubjectItem
import com.example.data.model.UserAccount
import com.example.ui.components.MinimalCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalSyllabyColors
import com.example.ui.viewmodel.PomodoroTimerState
import com.example.util.AmbientSoundOption
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun FocusScreen(
    timerState: PomodoroTimerState,
    account: UserAccount?,
    subjects: List<SubjectItem>,
    sessions: List<FocusSessionItem>,
    onSelectPreset: (focusMins: Int, breakMins: Int, longBreakMins: Int, label: String) -> Unit,
    onSelectSubject: (SubjectItem?) -> Unit,
    onSelectAmbientSound: (AmbientSoundOption) -> Unit,
    onToggleFullScreen: (Boolean) -> Unit,
    onStartOrResume: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onCompleteEarly: () -> Unit
) {
    val colors = LocalSyllabyColors.current
    var showCustomDialog by remember { mutableStateOf(false) }

    if (timerState.isFullScreen) {
        BackHandler { onToggleFullScreen(false) }
        FullScreenFocusOverlay(
            timerState = timerState,
            onExit = { onToggleFullScreen(false) },
            onStartOrResume = onStartOrResume,
            onPause = onPause,
            onReset = onReset,
            onCompleteEarly = onCompleteEarly,
            onSelectAmbientSound = onSelectAmbientSound
        )
        return
    }

    val startOfDay = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todayMinutes = sessions.filter { it.endedAtMillis >= startOfDay }.sumOf { it.durationMinutes }
    val longestSession = sessions.maxOfOrNull { it.durationMinutes } ?: 0L
    val dailyGoal = (account?.dailyGoalMinutes ?: 60L).coerceAtLeast(1L)

    val subjectFocusBreakdown = remember(sessions) {
        sessions.groupBy { it.subjectName ?: "General Study" }
            .mapValues { (_, list) -> list.sumOf { it.durationMinutes } }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
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
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Focus Mode", style = MaterialTheme.typography.headlineLarge, color = colors.textPrimary)
                        Text("Deep work Pomodoro & ambient audio", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                    IconButton(
                        onClick = { onToggleFullScreen(true) },
                        modifier = Modifier.testTag("fullscreen_focus_button")
                    ) {
                        Icon(Icons.Outlined.Fullscreen, contentDescription = "Enter full-screen focus", tint = colors.accent)
                    }
                }
            }

            // Presets
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetPill(
                        label = "25 / 5 Classic",
                        selected = timerState.focusMinutes == 25 && timerState.breakMinutes == 5,
                        onClick = { onSelectPreset(25, 5, 15, "25/5 Classic") }
                    )
                    PresetPill(
                        label = "50 / 10 Deep",
                        selected = timerState.focusMinutes == 50 && timerState.breakMinutes == 10,
                        onClick = { onSelectPreset(50, 10, 20, "50/10 Deep Work") }
                    )
                    Surface(
                        onClick = { showCustomDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.borderSubtle)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Tune, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Custom", style = MaterialTheme.typography.labelMedium, color = colors.textPrimary)
                        }
                    }
                }
            }

            // Main Timer Card
            item {
                MinimalCard(highlightBorder = if (timerState.isRunning) colors.accent else colors.borderSubtle) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (timerState.isBreakPhase) "BREAK PHASE" else (timerState.selectedSubjectName?.uppercase(Locale.US) ?: "DEEP FOCUS"),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (timerState.isBreakPhase) colors.success else colors.accent
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        TimerRingView(
                            remainingSeconds = timerState.remainingSeconds,
                            totalSeconds = timerState.totalSeconds,
                            isBreak = timerState.isBreakPhase,
                            ringSizeDp = 210
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onReset,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("timer_reset_button")
                            ) {
                                Icon(Icons.Outlined.Refresh, contentDescription = "Reset timer", modifier = Modifier.size(18.dp))
                            }

                            Button(
                                onClick = { if (timerState.isRunning) onPause() else onStartOrResume() },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("timer_play_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (timerState.isRunning) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                    contentDescription = if (timerState.isRunning) "Pause" else "Start"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (timerState.isRunning) "Pause" else "Start")
                            }

                            if (!timerState.isBreakPhase && timerState.remainingSeconds < timerState.totalSeconds) {
                                OutlinedButton(
                                    onClick = onCompleteEarly,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("timer_finish_early_button")
                                ) {
                                    Icon(Icons.Outlined.Check, contentDescription = "Finish and log session", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Subject Selector & Ambient Sounds
            item {
                MinimalCard {
                    SectionHeader(title = "Study Subject")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PresetPill(
                            label = "General",
                            selected = timerState.selectedSubjectId == null,
                            onClick = { onSelectSubject(null) }
                        )
                        subjects.forEach { sub ->
                            PresetPill(
                                label = sub.name,
                                selected = timerState.selectedSubjectId == sub.id,
                                onClick = { onSelectSubject(sub) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    SectionHeader(
                        title = "Ambient Soundscape",
                        trailing = {
                            Icon(Icons.Outlined.GraphicEq, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AmbientSoundOption.entries.forEach { opt ->
                            PresetPill(
                                label = opt.label,
                                selected = timerState.ambientSound == opt,
                                onClick = { onSelectAmbientSound(opt) }
                            )
                        }
                    }
                }
            }

            // Stats Card
            item {
                MinimalCard {
                    SectionHeader(
                        title = "Focus Statistics",
                        subtitle = "Today ${todayMinutes}m / ${dailyGoal}m goal"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (todayMinutes.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = colors.accent,
                        trackColor = colors.surfaceElevated
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatMetric("Total Time", "${account?.totalFocusMinutes ?: 0}m")
                        StatMetric("Sessions", "${sessions.size}")
                        StatMetric("Longest", "${longestSession}m")
                    }
                    if (subjectFocusBreakdown.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        subjectFocusBreakdown.forEach { (name, mins) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(name, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                Text("${mins}m", style = MaterialTheme.typography.labelSmall, color = colors.textPrimary)
                            }
                        }
                    }
                }
            }

            if (sessions.isNotEmpty()) {
                item {
                    Text("Recent Sessions", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                }
                items(sessions.take(10), key = { it.id }) { session ->
                    MinimalCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = session.subjectName ?: "General Study",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "${session.modeLabel} • ${session.ambientSound}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${session.durationMinutes} min",
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = colors.accent
                                )
                                Text(
                                    text = SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(session.endedAtMillis)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        var focusStr by remember { mutableStateOf(timerState.focusMinutes.toString()) }
        var breakStr by remember { mutableStateOf(timerState.breakMinutes.toString()) }
        var longBreakStr by remember { mutableStateOf(timerState.longBreakMinutes.toString()) }
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Custom Pomodoro") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = focusStr,
                        onValueChange = { focusStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Focus minutes (1-180)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = breakStr,
                        onValueChange = { breakStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Break minutes (1-60)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = longBreakStr,
                        onValueChange = { longBreakStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Long break minutes (5-90)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val f = (focusStr.toIntOrNull() ?: 25).coerceIn(1, 180)
                    val b = (breakStr.toIntOrNull() ?: 5).coerceIn(1, 60)
                    val lb = (longBreakStr.toIntOrNull() ?: 15).coerceIn(5, 90)
                    onSelectPreset(f, b, lb, "$f/$b Custom")
                    showCustomDialog = false
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FullScreenFocusOverlay(
    timerState: PomodoroTimerState,
    onExit: () -> Unit,
    onStartOrResume: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onCompleteEarly: () -> Unit,
    onSelectAmbientSound: (AmbientSoundOption) -> Unit
) {
    val colors = LocalSyllabyColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .padding(24.dp)
    ) {
        IconButton(
            onClick = onExit,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(Icons.Outlined.FullscreenExit, contentDescription = "Exit full-screen", tint = colors.textPrimary)
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (timerState.isBreakPhase) "REST & RECHARGE" else (timerState.selectedSubjectName?.uppercase(Locale.US) ?: "DEEP WORK"),
                style = MaterialTheme.typography.labelLarge,
                color = colors.accent
            )
            Spacer(modifier = Modifier.height(28.dp))
            TimerRingView(
                remainingSeconds = timerState.remainingSeconds,
                totalSeconds = timerState.totalSeconds,
                isBreak = timerState.isBreakPhase,
                ringSizeDp = 270
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = onReset, shape = CircleShape, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Reset")
                }
                Button(
                    onClick = { if (timerState.isRunning) onPause() else onStartOrResume() },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(if (timerState.isRunning) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (timerState.isRunning) "Pause" else "Focus")
                }
                if (!timerState.isBreakPhase && timerState.remainingSeconds < timerState.totalSeconds) {
                    OutlinedButton(onClick = onCompleteEarly, shape = CircleShape, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Outlined.Check, contentDescription = "Finish")
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AmbientSoundOption.entries.forEach { opt ->
                    PresetPill(
                        label = opt.label,
                        selected = timerState.ambientSound == opt,
                        onClick = { onSelectAmbientSound(opt) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerRingView(
    remainingSeconds: Int,
    totalSeconds: Int,
    isBreak: Boolean,
    ringSizeDp: Int
) {
    val colors = LocalSyllabyColors.current
    val progress = if (totalSeconds <= 0) 0f else remainingSeconds.toFloat() / totalSeconds.toFloat()
    val ringColor = if (isBreak) colors.success else colors.accent
    val mins = remainingSeconds / 60
    val secs = remainingSeconds % 60
    val formatted = String.format(Locale.US, "%02d:%02d", mins, secs)

    Box(
        modifier = Modifier.size(ringSizeDp.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 10.dp.toPx()
            drawArc(
                color = colors.surfaceElevated,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Text(
            text = formatted,
            style = MaterialTheme.typography.displayLarge.copy(
                fontFamily = JetBrainsMonoFamily,
                fontSize = (ringSizeDp / 5).sp,
                fontWeight = FontWeight.Bold
            ),
            color = colors.textPrimary
        )
    }
}

@Composable
private fun PresetPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalSyllabyColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) colors.accent else colors.surface,
        border = BorderStroke(1.dp, if (selected) colors.accent else colors.borderSubtle)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) colors.onAccent else colors.textSecondary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun StatMetric(label: String, value: String) {
    val colors = LocalSyllabyColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
            color = colors.textPrimary
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
    }
}
