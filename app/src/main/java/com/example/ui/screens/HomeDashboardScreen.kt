package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.DashboardLayoutPreferences
import com.example.data.model.AchievementUnlockItem
import com.example.data.model.ChapterItem
import com.example.data.model.DashboardWidgetId
import com.example.data.model.FocusSessionItem
import com.example.data.model.GamificationCatalog
import com.example.data.model.SubjectItem
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.data.model.UserAccount
import com.example.ui.components.AvatarWithFrame
import com.example.ui.components.MinimalCard
import com.example.ui.components.ProgressRing
import com.example.ui.components.SectionHeader
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalSyllabyColors
import com.example.ui.theme.parseHexColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeDashboardScreen(
    account: UserAccount?,
    subjects: List<SubjectItem>,
    chapters: List<ChapterItem>,
    tasks: List<TaskItem>,
    focusSessions: List<FocusSessionItem>,
    achievements: List<AchievementUnlockItem>,
    unreadNotificationCount: Int,
    layoutPrefs: DashboardLayoutPreferences,
    onNavigateToSyllabus: () -> Unit,
    onNavigateToFocus: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onToggleTask: (TaskItem) -> Unit,
    onQuickAddTask: (String, TaskPriority) -> Unit,
    onSaveLayout: (List<DashboardWidgetId>, Set<DashboardWidgetId>) -> Unit
) {
    val colors = LocalSyllabyColors.current
    var showQuickTaskDialog by remember { mutableStateOf(false) }
    var showCustomizeDialog by remember { mutableStateOf(false) }

    val now = remember { System.currentTimeMillis() }
    val dateLabel = remember {
        SimpleDateFormat("EEEE, MMM d", Locale.US).format(Date())
    }
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    val startOfDay = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val endOfDay = startOfDay + 24L * 3600L * 1000L - 1L

    val todayFocusMinutes = remember(focusSessions) {
        focusSessions.filter { it.endedAtMillis in startOfDay..endOfDay }.sumOf { it.durationMinutes }
    }
    val dailyGoal = (account?.dailyGoalMinutes ?: 60L).coerceAtLeast(1L)
    val todayFocusProgress = (todayFocusMinutes.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)

    val completedChapters = chapters.count { it.chapterStatus().countsTowardProgress }
    val syllabusProgress = if (chapters.isEmpty()) 0f else completedChapters.toFloat() / chapters.size.toFloat()

    val todayTasks = remember(tasks) {
        tasks.filter { !it.completed && (it.dueAtMillis == null || it.dueAtMillis <= endOfDay) }.take(5)
    }

    val upcomingExams = remember(subjects) {
        subjects.filter { (it.examDateMillis ?: 0L) > now }.sortedBy { it.examDateMillis }.take(4)
    }

    val weeklyMinutes = remember(focusSessions) {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val days = (6 downTo 0).map { offset ->
            val c = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }
            val key = fmt.format(c.time)
            val label = SimpleDateFormat("E", Locale.US).format(c.time).take(2)
            val mins = focusSessions.filter { fmt.format(Date(it.endedAtMillis)) == key }.sumOf { it.durationMinutes }
            label to mins
        }
        days
    }

    val visibleWidgets = layoutPrefs.orderedWidgets.filter { !layoutPrefs.hiddenWidgets.contains(it) }

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
            // Top Personal Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        AvatarWithFrame(
                            name = account?.displayName ?: "Student",
                            photoUrl = account?.photoUrl,
                            frameId = account?.selectedFrameId ?: "frame_default",
                            size = 46.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = dateLabel.uppercase(Locale.US),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textSecondary
                            )
                            Text(
                                text = "$greeting, ${account?.displayName?.substringBefore(" ") ?: "Scholar"}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Streak chip
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.borderSubtle)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LocalFireDepartment,
                                    contentDescription = "Study streak",
                                    tint = colors.warning,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${account?.currentStreak ?: 0}d",
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                                    color = colors.textPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onNavigateToNotifications,
                            modifier = Modifier.testTag("dashboard_notifications_button")
                        ) {
                            Box {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = "Notification center",
                                    tint = colors.textPrimary
                                )
                                if (unreadNotificationCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(colors.accent)
                                            .align(Alignment.TopEnd)
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = { showCustomizeDialog = true },
                            modifier = Modifier.testTag("dashboard_customize_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DashboardCustomize,
                                contentDescription = "Customize dashboard widgets",
                                tint = colors.textSecondary
                            )
                        }
                    }
                }
            }

            // Dynamic Ordered Widgets
            items(visibleWidgets, key = { it.name }) { widget ->
                when (widget) {
                    DashboardWidgetId.TODAY_PROGRESS -> {
                        MinimalCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "TODAY'S GOAL",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${todayFocusMinutes}m / ${dailyGoal}m focused",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val level = account?.level ?: 1L
                                    Text(
                                        text = "Lv.$level ${GamificationCatalog.rankForLevel(level)} • ${account?.themePoints ?: 0} TP",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.accent
                                    )
                                }
                                ProgressRing(progress = todayFocusProgress, size = 68.dp)
                            }
                        }
                    }

                    DashboardWidgetId.SYLLABUS_OVERVIEW -> {
                        MinimalCard(onClick = onNavigateToSyllabus) {
                            SectionHeader(
                                title = "Syllabus Progress",
                                subtitle = "$completedChapters of ${chapters.size} chapters mastered",
                                trailing = {
                                    Text(
                                        text = "${(syllabusProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                                        color = colors.accent
                                    )
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { syllabusProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = colors.accent,
                                trackColor = colors.surfaceElevated
                            )
                        }
                    }

                    DashboardWidgetId.SUBJECT_PROGRESS -> {
                        MinimalCard(onClick = onNavigateToSyllabus) {
                            SectionHeader(
                                title = "Subjects",
                                subtitle = if (subjects.isEmpty()) "Add subjects to track syllabus" else "${subjects.size} active subjects",
                                trailing = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                        contentDescription = "Open syllabus",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                            if (subjects.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    subjects.take(4).forEach { sub ->
                                        val subChapters = chapters.filter { it.subjectId == sub.id }
                                        val done = subChapters.count { it.chapterStatus().countsTowardProgress }
                                        val pct = if (subChapters.isEmpty()) 0f else done.toFloat() / subChapters.size
                                        val subColor = parseHexColor(sub.colorHex, colors.accent)
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = sub.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = colors.textPrimary
                                                )
                                                Text(
                                                    text = "$done/${subChapters.size}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colors.textSecondary
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { pct },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(5.dp)
                                                    .clip(RoundedCornerShape(3.dp)),
                                                color = subColor,
                                                trackColor = colors.surfaceElevated
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    DashboardWidgetId.TODAY_TASKS -> {
                        MinimalCard {
                            SectionHeader(
                                title = "Today's Tasks",
                                subtitle = "${todayTasks.size} pending",
                                trailing = {
                                    Row {
                                        Surface(
                                            onClick = { showQuickTaskDialog = true },
                                            shape = RoundedCornerShape(8.dp),
                                            color = colors.accent,
                                            modifier = Modifier.testTag("quick_add_task_button")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Add,
                                                    contentDescription = "Quick add task",
                                                    tint = colors.onAccent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Quick",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = colors.onAccent
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            if (todayTasks.isEmpty()) {
                                Text(
                                    text = "All caught up for today. Tap Quick to add an academic task.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    todayTasks.forEach { task ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { onToggleTask(task) }
                                                .padding(vertical = 6.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (task.completed) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                                contentDescription = "Toggle task",
                                                tint = if (task.completed) colors.success else colors.accent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = task.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                                                ),
                                                color = colors.textPrimary,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = task.taskPriority().label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.textSecondary
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "View all tasks →",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.accent,
                                    modifier = Modifier.clickable(onClick = onNavigateToTasks)
                                )
                            }
                        }
                    }

                    DashboardWidgetId.FOCUS_SHORTCUT -> {
                        MinimalCard(
                            onClick = onNavigateToFocus,
                            highlightBorder = colors.accent.copy(alpha = 0.45f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "DEEP FOCUS MODE",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.accent
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Launch Pomodoro Session",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "Rain, Forest & White Noise built in",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(colors.accent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.PlayArrow,
                                        contentDescription = "Start focus timer",
                                        tint = colors.onAccent
                                    )
                                }
                            }
                        }
                    }

                    DashboardWidgetId.UPCOMING_EXAMS -> {
                        if (upcomingExams.isNotEmpty()) {
                            MinimalCard {
                                SectionHeader(title = "Upcoming Examinations")
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    upcomingExams.forEach { sub ->
                                        val daysLeft = (((sub.examDateMillis ?: now) - now) / (24L * 3600L * 1000L)).coerceAtLeast(0L)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Event,
                                                    contentDescription = null,
                                                    tint = parseHexColor(sub.colorHex, colors.accent),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = sub.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = colors.textPrimary
                                                )
                                            }
                                            Text(
                                                text = if (daysLeft == 0L) "Today" else "in ${daysLeft}d",
                                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                                                color = if (daysLeft <= 7L) colors.warning else colors.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    DashboardWidgetId.WEEKLY_OVERVIEW -> {
                        val maxMins = (weeklyMinutes.maxOfOrNull { it.second } ?: 1L).coerceAtLeast(30L)
                        MinimalCard {
                            SectionHeader(
                                title = "Weekly Focus Overview",
                                subtitle = "${weeklyMinutes.sumOf { it.second }}m in the last 7 days"
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(86.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                weeklyMinutes.forEach { (dayLabel, mins) ->
                                    val fraction = (mins.toFloat() / maxMins.toFloat()).coerceIn(0.08f, 1f)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Bottom
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(24.dp)
                                                .height((56f * fraction).dp)
                                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                                .background(if (mins > 0L) colors.accent else colors.surfaceElevated)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = dayLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    DashboardWidgetId.RECENT_ACHIEVEMENTS -> {
                        MinimalCard(onClick = onNavigateToAchievements) {
                            SectionHeader(
                                title = "Recent Achievements",
                                subtitle = "${achievements.size} / ${GamificationCatalog.achievements.size} unlocked",
                                trailing = {
                                    Icon(
                                        imageVector = Icons.Outlined.EmojiEvents,
                                        contentDescription = "Achievements",
                                        tint = colors.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (achievements.isEmpty()) {
                                Text(
                                    text = "Complete a chapter or Pomodoro session to unlock your first badge.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    achievements.take(3).forEach { item ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.Stars,
                                                contentDescription = null,
                                                tint = colors.accent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = colors.textPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQuickTaskDialog) {
        var title by remember { mutableStateOf("") }
        var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
        AlertDialog(
            onDismissRequest = { showQuickTaskDialog = false },
            title = { Text("Quick Academic Task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("quick_task_title_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TaskPriority.entries.forEach { p ->
                            Surface(
                                onClick = { priority = p },
                                shape = RoundedCornerShape(8.dp),
                                color = if (priority == p) colors.accent else colors.surfaceElevated
                            ) {
                                Text(
                                    text = p.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (priority == p) colors.onAccent else colors.textPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (title.isNotBlank()) {
                            onQuickAddTask(title, priority)
                            showQuickTaskDialog = false
                        }
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showQuickTaskDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showCustomizeDialog) {
        var ordered by remember { mutableStateOf(layoutPrefs.orderedWidgets) }
        var hidden by remember { mutableStateOf(layoutPrefs.hiddenWidgets) }
        AlertDialog(
            onDismissRequest = { showCustomizeDialog = false },
            title = { Text("Customize Dashboard") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(ordered, key = { it.name }) { widget ->
                        val idx = ordered.indexOf(widget)
                        val visible = !hidden.contains(widget)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = visible,
                                onCheckedChange = { checked ->
                                    hidden = if (checked) hidden - widget else hidden + widget
                                }
                            )
                            Text(
                                text = widget.label,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    if (idx > 0) {
                                        val mutable = ordered.toMutableList()
                                        val tmp = mutable[idx - 1]
                                        mutable[idx - 1] = mutable[idx]
                                        mutable[idx] = tmp
                                        ordered = mutable
                                    }
                                },
                                enabled = idx > 0
                            ) {
                                Icon(Icons.Outlined.ArrowUpward, contentDescription = "Move up", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = {
                                    if (idx < ordered.lastIndex) {
                                        val mutable = ordered.toMutableList()
                                        val tmp = mutable[idx + 1]
                                        mutable[idx + 1] = mutable[idx]
                                        mutable[idx] = tmp
                                        ordered = mutable
                                    }
                                },
                                enabled = idx < ordered.lastIndex
                            ) {
                                Icon(Icons.Outlined.ArrowDownward, contentDescription = "Move down", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSaveLayout(ordered, hidden)
                        showCustomizeDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomizeDialog = false }) { Text("Cancel") }
            }
        )
    }
}
