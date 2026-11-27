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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.AttendanceItem
import com.example.data.model.AttendanceStatus
import com.example.data.model.SubjectItem
import com.example.ui.components.MinimalCard
import com.example.ui.components.ProgressRing
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LocalSyllabyColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun AttendanceScreen(
    attendance: List<AttendanceItem>,
    subjects: List<SubjectItem>,
    onBack: () -> Unit,
    onMarkAttendance: (dateKey: String, subjectId: String, status: AttendanceStatus, note: String?) -> Unit
) {
    val colors = LocalSyllabyColors.current
    var monthOffset by remember { mutableIntStateOf(0) }
    var selectedSubjectId by remember { mutableStateOf("ALL") }
    var selectedDateKey by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time))
    }

    val displayCal = remember(monthOffset) {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, monthOffset)
        }
    }
    val monthTitle = SimpleDateFormat("MMMM yyyy", Locale.US).format(displayCal.time)
    val monthPrefix = SimpleDateFormat("yyyy-MM", Locale.US).format(displayCal.time)
    val daysInMonth = displayCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val filteredRecords = remember(attendance, selectedSubjectId) {
        attendance.filter { it.subjectId == selectedSubjectId }
    }
    val recordByDate = remember(filteredRecords) {
        filteredRecords.associateBy { it.dateKey }
    }

    val monthRecords = filteredRecords.filter { it.dateKey.startsWith(monthPrefix) }
    val presentCount = monthRecords.count { it.attendanceStatus() == AttendanceStatus.PRESENT }
    val absentCount = monthRecords.count { it.attendanceStatus() == AttendanceStatus.ABSENT }
    val holidayCount = monthRecords.count { it.attendanceStatus() == AttendanceStatus.HOLIDAY }
    val totalActiveDays = presentCount + absentCount
    val attendancePct = if (totalActiveDays == 0) 1f else presentCount.toFloat() / totalActiveDays

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
                        Text("Attendance Tracker", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                        Text("Tap any date to mark or edit attendance", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
            }

            // Subject Filter
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AttendanceChip("All Classes", selectedSubjectId == "ALL") { selectedSubjectId = "ALL" }
                    subjects.forEach { sub ->
                        AttendanceChip(sub.name, selectedSubjectId == sub.id) { selectedSubjectId = sub.id }
                    }
                }
            }

            // Monthly Percentage Summary
            item {
                MinimalCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(monthTitle, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Present: $presentCount • Absent: $absentCount • Holidays: $holidayCount",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                        ProgressRing(
                            progress = attendancePct,
                            size = 64.dp,
                            accentColor = if (attendancePct >= 0.75f) colors.success else colors.danger
                        )
                    }
                }
            }

            // Calendar Grid
            item {
                MinimalCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { monthOffset-- }) {
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Previous month", tint = colors.textPrimary)
                        }
                        Text(monthTitle, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        IconButton(onClick = { monthOffset++ }) {
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Next month", tint = colors.textPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val rows = (1..daysInMonth).chunked(7)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        rows.forEach { week ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                week.forEach { day ->
                                    val key = String.format(Locale.US, "%s-%02d", monthPrefix, day)
                                    val rec = recordByDate[key]
                                    val bg = when (rec?.attendanceStatus()) {
                                        AttendanceStatus.PRESENT -> colors.success.copy(alpha = 0.25f)
                                        AttendanceStatus.ABSENT -> colors.danger.copy(alpha = 0.25f)
                                        AttendanceStatus.HOLIDAY -> colors.warning.copy(alpha = 0.25f)
                                        null -> colors.surfaceElevated
                                    }
                                    val isSelected = selectedDateKey == key
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(bg)
                                            .clickable { selectedDateKey = key },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.Transparent,
                                            border = if (isSelected) BorderStroke(2.dp, colors.accent) else null,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = day.toString(),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = colors.textPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                                repeat(7 - week.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Action Bar for Selected Date
            item {
                MinimalCard {
                    SectionHeader(title = "Mark $selectedDateKey", subtitle = "Edit any date's attendance record")
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AttendanceActionBtn("Present", colors.success, Modifier.weight(1f)) {
                            onMarkAttendance(selectedDateKey, selectedSubjectId, AttendanceStatus.PRESENT, null)
                        }
                        AttendanceActionBtn("Absent", colors.danger, Modifier.weight(1f)) {
                            onMarkAttendance(selectedDateKey, selectedSubjectId, AttendanceStatus.ABSENT, null)
                        }
                        AttendanceActionBtn("Holiday", colors.warning, Modifier.weight(1f)) {
                            onMarkAttendance(selectedDateKey, selectedSubjectId, AttendanceStatus.HOLIDAY, null)
                        }
                    }
                }
            }

            if (filteredRecords.isNotEmpty()) {
                item {
                    Text("Attendance History", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                }
                items(filteredRecords.take(15), key = { it.id }) { rec ->
                    val statusColor = when (rec.attendanceStatus()) {
                        AttendanceStatus.PRESENT -> colors.success
                        AttendanceStatus.ABSENT -> colors.danger
                        AttendanceStatus.HOLIDAY -> colors.warning
                    }
                    MinimalCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(rec.dateKey, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                            Text(rec.attendanceStatus().label, style = MaterialTheme.typography.labelLarge, color = statusColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceChip(label: String, selected: Boolean, onClick: () -> Unit) {
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
private fun AttendanceActionBtn(label: String, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = tint.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Box(modifier = Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = tint)
        }
    }
}
