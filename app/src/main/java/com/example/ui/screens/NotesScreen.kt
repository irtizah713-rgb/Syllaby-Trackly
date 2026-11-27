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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ChapterItem
import com.example.data.model.NoteItem
import com.example.data.model.SubjectItem
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MinimalCard
import com.example.ui.theme.LocalSyllabyColors
import kotlinx.coroutines.delay

@Composable
fun NotesScreen(
    notes: List<NoteItem>,
    subjects: List<SubjectItem>,
    chapters: List<ChapterItem>,
    onBack: () -> Unit,
    onSaveNote: (
        id: String?,
        title: String,
        content: String,
        subjectId: String?,
        chapterId: String?,
        pinned: Boolean,
        favorite: Boolean
    ) -> Unit,
    onDeleteNote: (String) -> Unit
) {
    val colors = LocalSyllabyColors.current
    var search by remember { mutableStateOf("") }
    var filterSubjectId by remember { mutableStateOf<String?>(null) }
    var onlyFavorites by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<NoteItem?>(null) }
    var isCreating by remember { mutableStateOf(false) }

    if (isCreating || editingNote != null) {
        BackHandler {
            editingNote = null
            isCreating = false
        }
        NoteEditorView(
            initial = editingNote,
            subjects = subjects,
            chapters = chapters,
            onClose = {
                editingNote = null
                isCreating = false
            },
            onSave = { id, title, content, subId, chId, pinned, fav ->
                onSaveNote(id, title, content, subId, chId, pinned, fav)
            }
        )
        return
    }

    val filtered = remember(notes, search, filterSubjectId, onlyFavorites) {
        notes.filter { n ->
            val matchSearch = search.isBlank() || n.title.contains(search, ignoreCase = true) ||
                n.content.contains(search, ignoreCase = true)
            val matchSub = filterSubjectId == null || n.subjectId == filterSubjectId
            val matchFav = !onlyFavorites || n.favorite
            matchSearch && matchSub && matchFav
        }
    }

    Scaffold(
        containerColor = colors.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isCreating = true },
                containerColor = colors.accent,
                contentColor = colors.onAccent,
                modifier = Modifier.testTag("add_note_fab")
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "New note")
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
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                        }
                        Column {
                            Text("Study Notes", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                            Text("${notes.size} notes organized locally & in cloud", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        placeholder = { Text("Search notes...") },
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
                        NoteFilterChip("All", filterSubjectId == null && !onlyFavorites) {
                            filterSubjectId = null; onlyFavorites = false
                        }
                        NoteFilterChip("Favorites", onlyFavorites) {
                            onlyFavorites = !onlyFavorites
                        }
                        subjects.forEach { sub ->
                            NoteFilterChip(sub.name, filterSubjectId == sub.id) {
                                filterSubjectId = if (filterSubjectId == sub.id) null else sub.id
                            }
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Outlined.Description,
                            title = "No notes yet",
                            subtitle = "Create structured subject and chapter notes with automatic saving.",
                            actionLabel = "Create Note",
                            onAction = { isCreating = true }
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { note ->
                        val subName = subjects.firstOrNull { it.id == note.subjectId }?.name
                        val chName = chapters.firstOrNull { it.id == note.chapterId }?.title
                        MinimalCard(onClick = { editingNote = note }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = note.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (subName != null || chName != null) {
                                        Text(
                                            text = listOfNotNull(subName, chName).joinToString(" • "),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.accent
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = note.content.ifBlank { "Empty note" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = {
                                    onSaveNote(note.id, note.title, note.content, note.subjectId, note.chapterId, !note.pinned, note.favorite)
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.PushPin,
                                        contentDescription = "Pin note",
                                        tint = if (note.pinned) colors.accent else colors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(onClick = {
                                    onSaveNote(note.id, note.title, note.content, note.subjectId, note.chapterId, note.pinned, !note.favorite)
                                }) {
                                    Icon(
                                        imageVector = if (note.favorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                                        contentDescription = "Favorite note",
                                        tint = if (note.favorite) colors.warning else colors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(onClick = { onDeleteNote(note.id) }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete note", tint = colors.danger, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteEditorView(
    initial: NoteItem?,
    subjects: List<SubjectItem>,
    chapters: List<ChapterItem>,
    onClose: () -> Unit,
    onSave: (
        id: String?,
        title: String,
        content: String,
        subjectId: String?,
        chapterId: String?,
        pinned: Boolean,
        favorite: Boolean
    ) -> Unit
) {
    val colors = LocalSyllabyColors.current
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var content by remember { mutableStateOf(initial?.content ?: "") }
    var subjectId by remember { mutableStateOf(initial?.subjectId) }
    var chapterId by remember { mutableStateOf(initial?.chapterId) }
    var pinned by remember { mutableStateOf(initial?.pinned ?: false) }
    var favorite by remember { mutableStateOf(initial?.favorite ?: false) }

    // Auto-save existing notes after 1.5s of inactivity
    LaunchedEffect(title, content, subjectId, chapterId, pinned, favorite) {
        if (initial != null && title.isNotBlank()) {
            delay(1500L)
            onSave(initial.id, title, content, subjectId, chapterId, pinned, favorite)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (title.isNotBlank()) {
                        onSave(initial?.id, title, content, subjectId, chapterId, pinned, favorite)
                    }
                    onClose()
                }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Save and go back", tint = colors.textPrimary)
                }
                Text(if (initial == null) "New Note" else "Edit Note (Auto-saving)", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            }
            Row {
                IconButton(onClick = { pinned = !pinned }) {
                    Icon(Icons.Outlined.PushPin, contentDescription = "Pin", tint = if (pinned) colors.accent else colors.textSecondary)
                }
                IconButton(onClick = { favorite = !favorite }) {
                    Icon(if (favorite) Icons.Outlined.Star else Icons.Outlined.StarBorder, contentDescription = "Favorite", tint = if (favorite) colors.warning else colors.textSecondary)
                }
                Surface(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSave(initial?.id, title, content, subjectId, chapterId, pinned, favorite)
                            onClose()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = colors.accent
                ) {
                    Text("Save", style = MaterialTheme.typography.labelLarge, color = colors.onAccent, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Note Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subject & Chapter Association
        if (subjects.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                subjects.forEach { s ->
                    NoteFilterChip(s.name, subjectId == s.id) {
                        subjectId = if (subjectId == s.id) null else s.id
                        chapterId = null
                    }
                }
            }
            val subChapters = chapters.filter { it.subjectId == subjectId }
            if (subChapters.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    subChapters.forEach { c ->
                        NoteFilterChip(c.title, chapterId == c.id) {
                            chapterId = if (chapterId == c.id) null else c.id
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Rich-Text Markdown Formatting Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "H1" to "\n# ",
                "H2" to "\n## ",
                "Bold" to "**key point**",
                "Bullet" to "\n• ",
                "Checklist" to "\n[ ] ",
                "Formula" to "\n```\n\n```"
            ).forEach { (label, snippet) ->
                Surface(
                    onClick = { content += snippet },
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surfaceElevated,
                    border = BorderStroke(1.dp, colors.borderSubtle)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            placeholder = { Text("Write formulas, key definitions, or revision summaries...") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}

@Composable
private fun NoteFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalSyllabyColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) colors.accent else colors.surface,
        border = BorderStroke(1.dp, if (selected) colors.accent else colors.borderSubtle)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) colors.onAccent else colors.textSecondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
