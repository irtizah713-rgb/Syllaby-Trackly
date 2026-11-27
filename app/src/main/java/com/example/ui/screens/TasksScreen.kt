package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ChapterItem
import com.example.data.model.SubjectItem
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MinimalCard
import com.example.ui.theme.LocalSyllabyColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class TaskTab(val label: String) {
    TODAY("Today"),
    UPCOMING("Upcoming"),
    OVERDUE("Overdue"),
    COMPLETED("Done"),
    CALENDAR("Calendar")
}

@Composable
fun TasksScreen(
    tasks: List<TaskItem>,
    subjects: List<SubjectItem>,
    chapters: List<ChapterItem>,
    onSaveTask: (
        id: String?,
        title: String,
        desc: String?,
        priority: TaskPriority,
        category: String,
        tags: List<String>,
        subjectId: String?,
        chapterId: String?,
        dueMillis: Long?,
        reminder: Boolean,
        completed: Boolean
    ) -> Unit,
    onToggleCompleted: (TaskItem) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    val colors = LocalSyllabyColors.current
    var selectedTab by remember { mutableStateOf(TaskTab.TODAY) }
    var selectedDayOffset by remember { mutableIntStateOf(0) }
    var showTaskDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskItem?>(null) }

    val startOfDay = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val endOfDay = startOfDay + 24L * 3600L * 1000L - 1L

    val filteredTasks = remember(tasks, selectedTab, selectedDayOffset) {
        when (selectedTab) {
            TaskTab.TODAY -> tasks.filter { !it.completed && (it.dueAtMillis == null || it.dueAtMillis in startOfDay..endOfDay) }
            TaskTab.UPCOMING -> tasks.filter { !it.completed && (it.dueAtMillis ?: 0L) > endOfDay }
            TaskTab.OVERDUE -> tasks.filter { !it.completed && it.dueAtMillis != null && it.dueAtMillis < startOfDay }
            TaskTab.COMPLETED -> tasks.filter { it.completed }
            TaskTab.CALENDAR -> {
                val targetStart = startOfDay + selectedDayOffset * 24L * 3600L * 1000L
                val targetEnd = targetStart + 24L * 3600L * 1000L - 1L
                tasks.filter { (it.dueAtMillis ?: startOfDay) in targetStart..targetEnd }
            }
        }
    }

    Scaffold(
        containerColor = colors.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTask = null
                    showTaskDialog = true
                },
                containerColor = colors.accent,
                contentColor = colors.onAccent,
                modifier = Modifier.testTag("add_task_fab")
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Create task")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Academic Tasks", style = MaterialTheme.typography.headlineLarge, color = colors.textPrimary)
                    Text(
                        text = "${tasks.count { !it.completed }} pending • ${tasks.count { it.completed }} completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TaskTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            Surface(
                                onClick = { selectedTab = tab },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) colors.accent else colors.surface,
                                border = BorderStroke(1.dp, if (selected) colors.accent else colors.borderSubtle)
                            ) {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) colors.onAccent else colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                if (selectedTab == TaskTab.CALENDAR) {
                    item {
                        MinimalCard {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                (-2..11).forEach { offset ->
                                    val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
                                    val dayName = SimpleDateFormat("EEE", Locale.US).format(c.time)
                                    val dayNum = SimpleDateFormat("d", Locale.US).format(c.time)
                                    val selected = selectedDayOffset == offset
                                    Surface(
                                        onClick = { selectedDayOffset = offset },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selected) colors.accent else colors.surfaceElevated
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(dayName, style = MaterialTheme.typography.labelSmall, color = if (selected) colors.onAccent else colors.textSecondary)
                                            Text(dayNum, style = MaterialTheme.typography.titleSmall, color = if (selected) colors.onAccent else colors.textPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (filteredTasks.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Outlined.TaskAlt,
                            title = "No tasks in ${selectedTab.label}",
                            subtitle = "Create prioritized tasks linked to subjects, chapters, tags, and deadlines.",
                            actionLabel = "Create Task",
                            onAction = {
                                editingTask = null
                                showTaskDialog = true
                            }
                        )
                    }
                } else {
                    items(filteredTasks, key = { it.id }) { task ->
                        val priorityColor = when (task.taskPriority()) {
                            TaskPriority.URGENT -> colors.danger
                            TaskPriority.HIGH -> colors.warning
                            TaskPriority.MEDIUM -> colors.accent
                            TaskPriority.LOW -> colors.textSecondary
                        }
                        val subjectName = subjects.firstOrNull { it.id == task.subjectId }?.name

                        MinimalCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                IconButton(
                                    onClick = { onToggleCompleted(task) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (task.completed) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                        contentDescription = "Toggle completed",
                                        tint = if (task.completed) colors.success else priorityColor
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        color = colors.textPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!task.description.isNullOrBlank()) {
                                        Text(
                                            text = task.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary,
                                            maxLines = 2
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BadgePill(task.taskPriority().label, priorityColor)
                                        BadgePill(task.category, colors.textSecondary)
                                        if (subjectName != null) BadgePill(subjectName, colors.accent)
                                        task.tags.forEach { tag -> BadgePill("#$tag", colors.textSecondary) }
                                        if (task.dueAtMillis != null) {
                                            val dueText = SimpleDateFormat("MMM d", Locale.US).format(Date(task.dueAtMillis))
                                            BadgePill("Due $dueText", colors.warning)
                                        }
                                        if (task.reminderEnabled) {
                                            Icon(Icons.Outlined.NotificationsActive, contentDescription = "Reminder on", tint = colors.accent, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                                IconButton(onClick = {
                                    editingTask = task
                                    showTaskDialog = true
                                }) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "Edit task", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { onDeleteTask(task.id) }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete task", tint = colors.danger, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTaskDialog) {
        TaskEditorDialog(
            initial = editingTask,
            subjects = subjects,
            chapters = chapters,
            onDismiss = { showTaskDialog = false },
            onSave = { title, desc, priority, category, tags, subId, chId, dueDays, reminder ->
                val dueMillis = dueDays?.let { startOfDay + it * 24L * 3600L * 1000L + 18L * 3600L * 1000L }
                    ?: editingTask?.dueAtMillis
                onSaveTask(
                    editingTask?.id,
                    title,
                    desc,
                    priority,
                    category,
                    tags,
                    subId,
                    chId,
                    dueMillis,
                    reminder,
                    editingTask?.completed ?: false
                )
                showTaskDialog = false
            }
        )
    }
}

@Composable
private fun BadgePill(text: String, tint: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = tint.copy(alpha = 0.14f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun TaskEditorDialog(
    initial: TaskItem?,
    subjects: List<SubjectItem>,
    chapters: List<ChapterItem>,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        desc: String?,
        priority: TaskPriority,
        category: String,
        tags: List<String>,
        subjectId: String?,
        chapterId: String?,
        dueInDays: Int?,
        reminder: Boolean
    ) -> Unit
) {
    val colors = LocalSyllabyColors.current
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var priority by remember { mutableStateOf(initial?.taskPriority() ?: TaskPriority.MEDIUM) }
    var category by remember { mutableStateOf(initial?.category ?: "Study") }
    var tagsText by remember { mutableStateOf(initial?.tags?.joinToString(", ") ?: "") }
    var selectedSubId by remember { mutableStateOf(initial?.subjectId) }
    var selectedChId by remember { mutableStateOf(initial?.chapterId) }
    var dueInDays by remember { mutableStateOf<Int?>(null) }
    var reminder by remember { mutableStateOf(initial?.reminderEnabled ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Academic Task" else "Edit Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task Title") }, singleLine = true)
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, maxLines = 2)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = tagsText,
                        onValueChange = { tagsText = it },
                        label = { Text("Tags (csv)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text("Deadline", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0 to "Today", 1 to "Tomorrow", 3 to "+3 Days", 7 to "+1 Week").forEach { (d, lbl) ->
                        Surface(
                            onClick = { dueInDays = d },
                            shape = RoundedCornerShape(8.dp),
                            color = if (dueInDays == d) colors.accent else colors.surfaceElevated
                        ) {
                            Text(lbl, style = MaterialTheme.typography.labelSmall, color = if (dueInDays == d) colors.onAccent else colors.textPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                        }
                    }
                }
                if (subjects.isNotEmpty()) {
                    Text("Subject", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subjects.forEach { s ->
                            Surface(
                                onClick = {
                                    selectedSubId = if (selectedSubId == s.id) null else s.id
                                    selectedChId = null
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedSubId == s.id) colors.accent else colors.surfaceElevated
                            ) {
                                Text(s.name, style = MaterialTheme.typography.labelSmall, color = if (selectedSubId == s.id) colors.onAccent else colors.textPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                            }
                        }
                    }
                }
                val subjectChapters = chapters.filter { it.subjectId == selectedSubId }
                if (subjectChapters.isNotEmpty()) {
                    Text("Chapter", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subjectChapters.forEach { ch ->
                            Surface(
                                onClick = { selectedChId = if (selectedChId == ch.id) null else ch.id },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedChId == ch.id) colors.accent else colors.surfaceElevated
                            ) {
                                Text(ch.title, style = MaterialTheme.typography.labelSmall, color = if (selectedChId == ch.id) colors.onAccent else colors.textPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Reminder", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = reminder, onCheckedChange = { reminder = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) {
                    val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    onSave(title, desc, priority, category, tags, selectedSubId, selectedChId, dueInDays, reminder)
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
