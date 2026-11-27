package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Biotech
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ChapterItem
import com.example.data.model.ChapterStatus
import com.example.data.model.SubjectItem
import com.example.data.model.UserAccount
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MinimalCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalSyllabyColors
import com.example.ui.theme.parseHexColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SubjectColorSwatches = listOf(
    "#38BDF8", "#34D399", "#F472B6", "#FBBF24", "#A78BFA", "#FB923C", "#FB7185", "#2DD4BF"
)

private val SubjectIcons = mapOf(
    "book" to Icons.AutoMirrored.Outlined.MenuBook,
    "science" to Icons.Outlined.Science,
    "math" to Icons.Outlined.Calculate,
    "code" to Icons.Outlined.Code,
    "bio" to Icons.Outlined.Biotech,
    "history" to Icons.Outlined.HistoryEdu,
    "world" to Icons.Outlined.Public
)

@Composable
fun SyllabusScreen(
    account: UserAccount?,
    subjects: List<SubjectItem>,
    chapters: List<ChapterItem>,
    onSaveAcademicStructure: (session: String, klass: String, stream: String) -> Unit,
    onSaveSubject: (id: String?, name: String, colorHex: String, iconKey: String, examMillis: Long?, target: Long) -> Unit,
    onMoveSubject: (SubjectItem, Int) -> Unit,
    onDeleteSubject: (String) -> Unit,
    onSaveChapter: (id: String?, subjectId: String, title: String, desc: String?, notes: String?, topics: String?, status: ChapterStatus) -> Unit,
    onUpdateChapterStatus: (ChapterItem, ChapterStatus) -> Unit,
    onMoveChapter: (ChapterItem, Int) -> Unit,
    onDeleteChapter: (String) -> Unit
) {
    val colors = LocalSyllabyColors.current
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }
    var showAcademicDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<SubjectItem?>(null) }
    var showSubjectDialog by remember { mutableStateOf(false) }

    val selectedSubject = subjects.firstOrNull { it.id == selectedSubjectId }
    if (selectedSubject != null) {
        BackHandler { selectedSubjectId = null }
        SubjectChaptersDetailView(
            subject = selectedSubject,
            chapters = chapters.filter { it.subjectId == selectedSubject.id },
            onBack = { selectedSubjectId = null },
            onSaveChapter = onSaveChapter,
            onUpdateChapterStatus = onUpdateChapterStatus,
            onMoveChapter = onMoveChapter,
            onDeleteChapter = onDeleteChapter
        )
        return
    }

    val completedTotal = chapters.count { it.chapterStatus().countsTowardProgress }
    val overallProgress = if (chapters.isEmpty()) 0f else completedTotal.toFloat() / chapters.size

    Scaffold(
        containerColor = colors.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingSubject = null
                    showSubjectDialog = true
                },
                containerColor = colors.accent,
                contentColor = colors.onAccent,
                modifier = Modifier.testTag("add_subject_fab")
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add subject")
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "Syllabus Architecture",
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "$completedTotal of ${chapters.size} chapters completed across ${subjects.size} subjects",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }

                // Academic Hierarchy Card
                item {
                    MinimalCard(onClick = { showAcademicDialog = true }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Outlined.School,
                                    contentDescription = "Academic hierarchy",
                                    tint = colors.accent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = account?.academicSession?.ifBlank { null } ?: "2026–2027 Session",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.textPrimary
                                    )
                                    val classText = account?.academicClass?.ifBlank { null } ?: "Set Class / Degree"
                                    val streamText = account?.academicStream?.ifBlank { null } ?: "Set Stream / Major"
                                    Text(
                                        text = "$classText • $streamText",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit academic structure",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { overallProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = colors.accent,
                            trackColor = colors.surfaceElevated
                        )
                    }
                }

                if (subjects.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.AutoMirrored.Outlined.MenuBook,
                            title = "Build Your Syllabus",
                            subtitle = "Create your subjects, add chapters, and track statuses from Incomplete to Numericals Practised.",
                            actionLabel = "Add First Subject",
                            testTag = "empty_add_subject_btn",
                            onAction = {
                                editingSubject = null
                                showSubjectDialog = true
                            }
                        )
                    }
                } else {
                    items(subjects, key = { it.id }) { subject ->
                        val subChapters = chapters.filter { it.subjectId == subject.id }
                        val done = subChapters.count { it.chapterStatus().countsTowardProgress }
                        val pct = if (subChapters.isEmpty()) 0f else done.toFloat() / subChapters.size
                        val subColor = parseHexColor(subject.colorHex, colors.accent)
                        val iconVec: ImageVector = SubjectIcons[subject.iconKey] ?: Icons.AutoMirrored.Outlined.MenuBook

                        MinimalCard(
                            onClick = { selectedSubjectId = subject.id },
                            modifier = Modifier.testTag("subject_card_${subject.id}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(subColor.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(iconVec, contentDescription = null, tint = subColor, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = subject.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$done / ${subChapters.size} chapters • ${(pct * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                }
                                IconButton(onClick = { onMoveSubject(subject, -1) }) {
                                    Icon(Icons.Outlined.ArrowUpward, contentDescription = "Move up", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { onMoveSubject(subject, 1) }) {
                                    Icon(Icons.Outlined.ArrowDownward, contentDescription = "Move down", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = {
                                    editingSubject = subject
                                    showSubjectDialog = true
                                }) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "Edit subject", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { onDeleteSubject(subject.id) }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete subject", tint = colors.danger, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { pct },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = subColor,
                                trackColor = colors.surfaceElevated
                            )
                            val revisedCount = subChapters.count { it.chapterStatus() == ChapterStatus.REVISED }
                            val practisedCount = subChapters.count {
                                it.chapterStatus() == ChapterStatus.PRACTISED || it.chapterStatus() == ChapterStatus.NUMERICALS_PRACTISED
                            }
                            if (subChapters.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Revised: $revisedCount", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                                    Text("Practised: $practisedCount", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                                    if (subject.examDateMillis != null) {
                                        val dateStr = SimpleDateFormat("MMM d", Locale.US).format(Date(subject.examDateMillis))
                                        Text("Exam: $dateStr", style = MaterialTheme.typography.labelSmall, color = colors.warning)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAcademicDialog) {
        var session by remember { mutableStateOf(account?.academicSession ?: "2026-2027") }
        var klass by remember { mutableStateOf(account?.academicClass ?: "") }
        var stream by remember { mutableStateOf(account?.academicStream ?: "") }
        AlertDialog(
            onDismissRequest = { showAcademicDialog = false },
            title = { Text("Academic Structure") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = session, onValueChange = { session = it }, label = { Text("Academic Session") }, singleLine = true)
                    OutlinedTextField(value = klass, onValueChange = { klass = it }, label = { Text("Class / Semester") }, singleLine = true)
                    OutlinedTextField(value = stream, onValueChange = { stream = it }, label = { Text("Stream / Major") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSaveAcademicStructure(session, klass, stream)
                    showAcademicDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAcademicDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showSubjectDialog) {
        SubjectEditorDialog(
            initial = editingSubject,
            onDismiss = { showSubjectDialog = false },
            onSave = { name, colorHex, iconKey, examDays, target ->
                val examMillis = examDays?.let { System.currentTimeMillis() + it * 24L * 3600L * 1000L }
                    ?: editingSubject?.examDateMillis
                onSaveSubject(editingSubject?.id, name, colorHex, iconKey, examMillis, target)
                showSubjectDialog = false
            }
        )
    }
}

@Composable
private fun SubjectChaptersDetailView(
    subject: SubjectItem,
    chapters: List<ChapterItem>,
    onBack: () -> Unit,
    onSaveChapter: (id: String?, subjectId: String, title: String, desc: String?, notes: String?, topics: String?, status: ChapterStatus) -> Unit,
    onUpdateChapterStatus: (ChapterItem, ChapterStatus) -> Unit,
    onMoveChapter: (ChapterItem, Int) -> Unit,
    onDeleteChapter: (String) -> Unit
) {
    val colors = LocalSyllabyColors.current
    val subColor = parseHexColor(subject.colorHex, colors.accent)
    var search by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf<ChapterStatus?>(null) }
    var showChapterDialog by remember { mutableStateOf(false) }
    var editingChapter by remember { mutableStateOf<ChapterItem?>(null) }

    val filtered = remember(chapters, search, filterStatus) {
        chapters.filter { ch ->
            val matchQuery = search.isBlank() || ch.title.contains(search, ignoreCase = true) ||
                (ch.topicsCsv?.contains(search, ignoreCase = true) == true)
            val matchStatus = filterStatus == null || ch.chapterStatus() == filterStatus
            matchQuery && matchStatus
        }
    }

    val doneCount = chapters.count { it.chapterStatus().countsTowardProgress }
    val progress = if (chapters.isEmpty()) 0f else doneCount.toFloat() / chapters.size

    Scaffold(
        containerColor = colors.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingChapter = null
                    showChapterDialog = true
                },
                containerColor = subColor,
                contentColor = colors.onAccent,
                modifier = Modifier.testTag("add_chapter_fab")
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add chapter")
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("subject_back_button")) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back to subjects", tint = colors.textPrimary)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(subject.name, style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                            Text(
                                text = "$doneCount / ${chapters.size} chapters mastered (${(progress * 100).toInt()}%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = subColor,
                        trackColor = colors.surfaceElevated
                    )
                }

                item {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        placeholder = { Text("Search chapters or topics...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChipPill(
                            label = "All (${chapters.size})",
                            selected = filterStatus == null,
                            onClick = { filterStatus = null }
                        )
                        ChapterStatus.entries.forEach { st ->
                            val count = chapters.count { it.chapterStatus() == st }
                            FilterChipPill(
                                label = "${st.label} ($count)",
                                selected = filterStatus == st,
                                onClick = { filterStatus = st }
                            )
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.AutoMirrored.Outlined.MenuBook,
                            title = if (chapters.isEmpty()) "No chapters yet" else "No matching chapters",
                            subtitle = "Add chapters with topics, descriptions, and personal study notes.",
                            actionLabel = "Add Chapter",
                            onAction = {
                                editingChapter = null
                                showChapterDialog = true
                            }
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { chapter ->
                        val status = chapter.chapterStatus()
                        MinimalCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = chapter.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.textPrimary
                                    )
                                    if (!chapter.topicsCsv.isNullOrBlank()) {
                                        Text(
                                            text = "Topics: ${chapter.topicsCsv}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (!chapter.description.isNullOrBlank()) {
                                        Text(
                                            text = chapter.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (chapter.completedAtMillis != null && status.countsTowardProgress) {
                                        val completedStr = SimpleDateFormat("MMM d, yyyy", Locale.US)
                                            .format(Date(chapter.completedAtMillis))
                                        Text(
                                            text = "Marked ${status.label} on $completedStr",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.success
                                        )
                                    }
                                }
                                IconButton(onClick = { onMoveChapter(chapter, -1) }) {
                                    Icon(Icons.Outlined.ArrowUpward, contentDescription = "Move chapter up", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { onMoveChapter(chapter, 1) }) {
                                    Icon(Icons.Outlined.ArrowDownward, contentDescription = "Move chapter down", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = {
                                    editingChapter = chapter
                                    showChapterDialog = true
                                }) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "Edit chapter", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { onDeleteChapter(chapter.id) }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete chapter", tint = colors.danger, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ChapterStatus.entries.forEach { st ->
                                    val selected = status == st
                                    Surface(
                                        onClick = { onUpdateChapterStatus(chapter, st) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (selected) {
                                            if (st.countsTowardProgress) colors.success.copy(alpha = 0.2f) else colors.surfaceElevated
                                        } else colors.background,
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) (if (st.countsTowardProgress) colors.success else colors.accent) else colors.borderSubtle
                                        )
                                    ) {
                                        Text(
                                            text = st.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selected) (if (st.countsTowardProgress) colors.success else colors.textPrimary) else colors.textSecondary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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

    if (showChapterDialog) {
        var title by remember { mutableStateOf(editingChapter?.title ?: "") }
        var desc by remember { mutableStateOf(editingChapter?.description ?: "") }
        var topics by remember { mutableStateOf(editingChapter?.topicsCsv ?: "") }
        var notes by remember { mutableStateOf(editingChapter?.personalNotes ?: "") }
        var status by remember { mutableStateOf(editingChapter?.chapterStatus() ?: ChapterStatus.INCOMPLETE) }
        AlertDialog(
            onDismissRequest = { showChapterDialog = false },
            title = { Text(if (editingChapter == null) "Add Chapter" else "Edit Chapter") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Chapter Title") }, singleLine = true)
                    OutlinedTextField(value = topics, onValueChange = { topics = it }, label = { Text("Topics (comma-separated)") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, maxLines = 2)
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Personal Chapter Notes") }, maxLines = 3)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ChapterStatus.entries.forEach { st ->
                            FilterChipPill(
                                label = st.label,
                                selected = status == st,
                                onClick = { status = st }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank()) {
                        onSaveChapter(editingChapter?.id, subject.id, title, desc, notes, topics, status)
                        showChapterDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showChapterDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun SubjectEditorDialog(
    initial: SubjectItem?,
    onDismiss: () -> Unit,
    onSave: (name: String, colorHex: String, iconKey: String, examInDays: Long?, targetChapters: Long) -> Unit
) {
    val colors = LocalSyllabyColors.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var colorHex by remember { mutableStateOf(initial?.colorHex ?: SubjectColorSwatches.first()) }
    var iconKey by remember { mutableStateOf(initial?.iconKey ?: "book") }
    var examDaysText by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf((initial?.targetChapters ?: 10L).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Create Subject" else "Edit Subject") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("subject_name_input")
                )
                Text("Color", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SubjectColorSwatches.forEach { hex ->
                        val c = parseHexColor(hex, colors.accent)
                        Surface(
                            onClick = { colorHex = hex },
                            shape = CircleShape,
                            color = c,
                            border = if (colorHex == hex) BorderStroke(2.5.dp, colors.textPrimary) else null,
                            modifier = Modifier.size(28.dp)
                        ) {}
                    }
                }
                Text("Icon", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SubjectIcons.forEach { (key, icon) ->
                        Surface(
                            onClick = { iconKey = key },
                            shape = RoundedCornerShape(8.dp),
                            color = if (iconKey == key) colors.accent else colors.surfaceElevated
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = key,
                                tint = if (iconKey == key) colors.onAccent else colors.textPrimary,
                                modifier = Modifier.padding(8.dp).size(20.dp)
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Target Chapters") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = examDaysText,
                        onValueChange = { examDaysText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Exam in (days)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onSave(
                        name,
                        colorHex,
                        iconKey,
                        examDaysText.toLongOrNull(),
                        targetText.toLongOrNull() ?: 10L
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
