package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.data.model.ChapterItem
import com.example.data.model.ChapterStatus
import com.example.data.model.FocusSessionItem
import com.example.data.model.SubjectItem
import com.example.data.model.TaskItem
import com.example.data.model.UserAccount
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
fun AnalyticsScreen(
    account: UserAccount?,
    subjects: List<SubjectItem>,
    chapters: List<ChapterItem>,
    tasks: List<TaskItem>,
    sessions: List<FocusSessionItem>,
    onBack: () -> Unit
) {
    val colors = LocalSyllabyColors.current
    var breakdownDialog by remember { mutableStateOf<Pair<String, List<String>>?>(null) }

    val completedCount = chapters.count { it.chapterStatus() == ChapterStatus.COMPLETED }
    val revisedCount = chapters.count { it.chapterStatus() == ChapterStatus.REVISED }
    val practisedCount = chapters.count {
        it.chapterStatus() == ChapterStatus.PRACTISED || it.chapterStatus() == ChapterStatus.NUMERICALS_PRACTISED
    }
    val masteredTotal = chapters.count { it.chapterStatus().countsTowardProgress }
    val syllabusPct = if (chapters.isEmpty()) 0f else masteredTotal.toFloat() / chapters.size

    val taskCompleted = tasks.count { it.completed }
    val taskRate = if (tasks.isEmpty()) 0f else taskCompleted.toFloat() / tasks.size

    val fmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val todayKey = remember { fmt.format(Date()) }
    val todayMins = sessions.filter { fmt.format(Date(it.endedAtMillis)) == todayKey }.sumOf { it.durationMinutes }

    val dailySeries14 = remember(sessions) {
        val cal = Calendar.getInstance()
        (13 downTo 0).map { offset ->
            val c = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }
            val k = fmt.format(c.time)
            val m = sessions.filter { fmt.format(Date(it.endedAtMillis)) == k }.sumOf { it.durationMinutes }
            k to m
        }
    }

    val monthlyMins = remember(sessions) {
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24L * 3600L * 1000L
        sessions.filter { it.endedAtMillis >= thirtyDaysAgo }.sumOf { it.durationMinutes }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                    }
                    Column {
                        Text("Academic Analytics", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                        Text("Tap any statistic card for a detailed breakdown", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
            }

            // Progress Rings Overview
            item {
                MinimalCard(
                    onClick = {
                        breakdownDialog = "Syllabus Status Breakdown" to listOf(
                            "Total Chapters: ${chapters.size}",
                            "Incomplete: ${chapters.count { it.chapterStatus() == ChapterStatus.INCOMPLETE }}",
                            "Completed: $completedCount",
                            "Revised: $revisedCount",
                            "Practised / Numericals: $practisedCount"
                        )
                    }
                ) {
                    SectionHeader(title = "Mastery & Task Completion")
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ProgressRing(progress = syllabusPct, size = 84.dp, strokeWidth = 8.dp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Syllabus", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                            Text("$masteredTotal/${chapters.size}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ProgressRing(progress = taskRate, size = 84.dp, strokeWidth = 8.dp, accentColor = colors.success)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Tasks Done", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                            Text("$taskCompleted/${tasks.size}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                }
            }

            // Productivity Metrics Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MinimalCard(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            breakdownDialog = "Focus Time Breakdown" to listOf(
                                "Today: $todayMins min",
                                "Last 7 Days: ${dailySeries14.takeLast(7).sumOf { it.second }} min",
                                "Last 30 Days: $monthlyMins min",
                                "All-time Focus: ${account?.totalFocusMinutes ?: 0} min",
                                "Completed Sessions: ${sessions.size}"
                            )
                        }
                    ) {
                        Text("FOCUS HOURS", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        val hours = (account?.totalFocusMinutes ?: 0L) / 60.0
                        Text(
                            text = String.format(Locale.US, "%.1f h", hours),
                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMonoFamily),
                            color = colors.accent
                        )
                        Text("30d: ${monthlyMins}m", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }

                    MinimalCard(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            breakdownDialog = "Study Streak History" to listOf(
                                "Current Streak: ${account?.currentStreak ?: 0} days",
                                "Longest Streak: ${account?.longestStreak ?: 0} days",
                                "Last Active: ${account?.lastActivityDate ?: "No activity yet"}"
                            )
                        }
                    ) {
                        Text("STUDY STREAK", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${account?.currentStreak ?: 0} days",
                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMonoFamily),
                            color = colors.warning
                        )
                        Text("Best: ${account?.longestStreak ?: 0}d", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
            }

            // 14-Day Line Graph + Activity Calendar
            item {
                val maxVal = (dailySeries14.maxOfOrNull { it.second } ?: 1L).coerceAtLeast(30L)
                MinimalCard(
                    onClick = {
                        breakdownDialog = "14-Day Productivity Log" to dailySeries14.map { "${it.first}: ${it.second} min" }
                    }
                ) {
                    SectionHeader(title = "14-Day Focus Trend", subtitle = "Minutes focused per day")
                    Spacer(modifier = Modifier.height(14.dp))
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    ) {
                        val stepX = size.width / (dailySeries14.size - 1).coerceAtLeast(1)
                        val path = Path()
                        dailySeries14.forEachIndexed { index, (_, mins) ->
                            val x = index * stepX
                            val y = size.height - ((mins.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f) * (size.height - 16f)) - 8f
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            drawCircle(color = colors.accent, radius = 5f, center = Offset(x, y))
                        }
                        drawPath(path = path, color = colors.accent, style = Stroke(width = 4f, cap = StrokeCap.Round))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    // Activity heatmap dots
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        dailySeries14.forEach { (_, mins) ->
                            val alpha = when {
                                mins == 0L -> 0.12f
                                mins < 30L -> 0.4f
                                mins < 60L -> 0.7f
                                else -> 1f
                            }
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colors.accent.copy(alpha = alpha))
                            )
                        }
                    }
                }
            }

            // Subject-wise Progress Bars
            item {
                MinimalCard {
                    SectionHeader(title = "Subject-Wise Breakdown")
                    Spacer(modifier = Modifier.height(10.dp))
                    if (subjects.isEmpty()) {
                        Text("Add subjects in Syllabus to inspect subject analytics.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            subjects.forEach { sub ->
                                val subCh = chapters.filter { it.subjectId == sub.id }
                                val done = subCh.count { it.chapterStatus().countsTowardProgress }
                                val pct = if (subCh.isEmpty()) 0f else done.toFloat() / subCh.size
                                val subColor = parseHexColor(sub.colorHex, colors.accent)
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(sub.name, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                                        Text("${(pct * 100).toInt()}% ($done/${subCh.size})", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { pct },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = subColor,
                                        trackColor = colors.surfaceElevated
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (breakdownDialog != null) {
        val (title, lines) = breakdownDialog!!
        AlertDialog(
            onDismissRequest = { breakdownDialog = null },
            title = { Text(title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    lines.forEach { line ->
                        Text(line, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { breakdownDialog = null }) { Text("Close") }
            }
        )
    }
}
