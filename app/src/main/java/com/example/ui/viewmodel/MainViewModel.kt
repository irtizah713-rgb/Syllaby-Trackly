package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.DashboardLayoutPreferences
import com.example.data.local.FocusPreferences
import com.example.data.local.NotificationLogEntity
import com.example.data.local.NotificationPreferences
import com.example.data.local.PreferencesRepository
import com.example.data.local.SyllabyCacheDao
import com.example.data.model.AchievementUnlockItem
import com.example.data.model.AttendanceItem
import com.example.data.model.AttendanceStatus
import com.example.data.model.ChapterItem
import com.example.data.model.ChapterStatus
import com.example.data.model.DashboardWidgetId
import com.example.data.model.FocusSessionItem
import com.example.data.model.GamificationCatalog
import com.example.data.model.NoteItem
import com.example.data.model.RewardTransactionItem
import com.example.data.model.ShopItem
import com.example.data.model.SubjectItem
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.data.model.UserAccount
import com.example.data.remote.SyllabyRepository
import com.example.ui.theme.ThemeSettings
import com.example.util.AmbientSoundOption
import com.example.util.AmbientSoundPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PomodoroTimerState(
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val isBreakPhase: Boolean = false,
    val isRunning: Boolean = false,
    val isFullScreen: Boolean = false,
    val remainingSeconds: Int = 25 * 60,
    val totalSeconds: Int = 25 * 60,
    val selectedSubjectId: String? = null,
    val selectedSubjectName: String? = null,
    val modeLabel: String = "25/5 Classic",
    val ambientSound: AmbientSoundOption = AmbientSoundOption.SILENT,
    val sessionStartedAtMillis: Long = 0L
)

data class AppEvent(
    val message: String,
    val postSystemNotification: Boolean = false,
    val notifTitle: String = "",
    val notifCategory: String = "System"
)

class MainViewModel(
    private val repository: SyllabyRepository,
    private val prefsRepository: PreferencesRepository,
    private val cacheDao: SyllabyCacheDao,
    private val currentUserId: String,
    initialEmail: String,
    initialDisplayName: String,
    initialPhotoUrl: String?
) : ViewModel() {

    private val soundPlayer = AmbientSoundPlayer()
    private var timerJob: Job? = null

    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    val accountState: StateFlow<UiState<UserAccount?>> =
        repository.observeUserAccount(currentUserId)
            .asUiState("Failed to load student profile")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val subjectsState: StateFlow<UiState<List<SubjectItem>>> =
        repository.observeSubjects(currentUserId)
            .asUiState("Failed to load subjects")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val chaptersState: StateFlow<UiState<List<ChapterItem>>> =
        repository.observeChapters(currentUserId)
            .asUiState("Failed to load chapters")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val tasksState: StateFlow<UiState<List<TaskItem>>> =
        repository.observeTasks(currentUserId)
            .asUiState("Failed to load tasks")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val notesState: StateFlow<UiState<List<NoteItem>>> =
        repository.observeNotes(currentUserId)
            .asUiState("Failed to load notes")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val focusSessionsState: StateFlow<UiState<List<FocusSessionItem>>> =
        repository.observeFocusSessions(currentUserId)
            .asUiState("Failed to load focus sessions")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val attendanceState: StateFlow<UiState<List<AttendanceItem>>> =
        repository.observeAttendance(currentUserId)
            .asUiState("Failed to load attendance")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val transactionsState: StateFlow<UiState<List<RewardTransactionItem>>> =
        repository.observeTransactions(currentUserId)
            .asUiState("Failed to load transactions")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val achievementsState: StateFlow<UiState<List<AchievementUnlockItem>>> =
        repository.observeAchievements(currentUserId)
            .asUiState("Failed to load achievements")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val notifications: StateFlow<List<NotificationLogEntity>> =
        cacheDao.observeNotifications(currentUserId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val themeSettings: StateFlow<ThemeSettings> = prefsRepository.themeSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ThemeSettings())

    val notificationPrefs: StateFlow<NotificationPreferences> = prefsRepository.notificationPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), NotificationPreferences())

    val dashboardLayout: StateFlow<DashboardLayoutPreferences> = prefsRepository.dashboardLayoutFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DashboardLayoutPreferences())

    val lastBackupMillis: StateFlow<Long> = prefsRepository.lastBackupMillisFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0L)

    val savedBackupJson: StateFlow<String> = prefsRepository.autoBackupJsonFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), "")

    private val _timerState = MutableStateFlow(PomodoroTimerState())
    val timerState: StateFlow<PomodoroTimerState> = _timerState.asStateFlow()

    private val _levelUpDialogLevel = MutableStateFlow<Long?>(null)
    val levelUpDialogLevel: StateFlow<Long?> = _levelUpDialogLevel.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureUserInitialized(
                uid = currentUserId,
                email = initialEmail,
                displayName = initialDisplayName,
                photoUrl = initialPhotoUrl
            )
        }
        viewModelScope.launch {
            prefsRepository.focusPreferencesFlow.collect { pref ->
                _timerState.update { current ->
                    if (current.isRunning) current
                    else {
                        val secs = pref.focusMinutes * 60
                        current.copy(
                            focusMinutes = pref.focusMinutes,
                            breakMinutes = pref.breakMinutes,
                            longBreakMinutes = pref.longBreakMinutes,
                            remainingSeconds = secs,
                            totalSeconds = secs,
                            ambientSound = AmbientSoundOption.entries.firstOrNull { it.label == pref.ambientSound }
                                ?: AmbientSoundOption.SILENT
                        )
                    }
                }
            }
        }
    }

    private fun currentAccount(): UserAccount {
        return (accountState.value as? UiState.Success)?.data ?: UserAccount(userId = currentUserId)
    }

    private fun unlockedKeys(): Set<String> {
        return (achievementsState.value as? UiState.Success)?.data?.map { it.achievementKey }?.toSet() ?: emptySet()
    }

    private fun emitMessage(msg: String) {
        _events.tryEmit(AppEvent(message = msg))
    }

    private suspend fun logAndNotify(title: String, body: String, category: String, postSystem: Boolean) {
        cacheDao.insertNotification(
            NotificationLogEntity(
                userId = currentUserId,
                title = title,
                body = body,
                category = category
            )
        )
        _events.tryEmit(
            AppEvent(
                message = "$title: $body",
                postSystemNotification = postSystem,
                notifTitle = title,
                notifCategory = category
            )
        )
    }

    fun dismissLevelUpDialog() {
        _levelUpDialogLevel.value = null
    }

    // --- Theme & Dashboard Preferences ---
    fun updateThemeSettings(settings: ThemeSettings) {
        viewModelScope.launch { prefsRepository.updateThemeSettings(settings) }
    }

    fun updateNotificationPrefs(prefs: NotificationPreferences) {
        viewModelScope.launch { prefsRepository.updateNotificationPreferences(prefs) }
    }

    fun updateDashboardLayout(ordered: List<DashboardWidgetId>, hidden: Set<DashboardWidgetId>) {
        viewModelScope.launch { prefsRepository.updateDashboardLayout(ordered, hidden) }
    }

    fun markNotificationsRead() {
        viewModelScope.launch { cacheDao.markAllNotificationsRead(currentUserId) }
    }

    fun clearNotifications() {
        viewModelScope.launch { cacheDao.clearNotifications(currentUserId) }
    }

    // --- Syllabus: Subjects & Chapters ---
    fun saveSubject(
        subjectId: String?,
        name: String,
        colorHex: String,
        iconKey: String,
        examDateMillis: Long?,
        targetChapters: Long
    ) {
        viewModelScope.launch {
            val currentSubs = (subjectsState.value as? UiState.Success)?.data.orEmpty()
            val sortOrder = currentSubs.firstOrNull { it.id == subjectId }?.sortOrder ?: currentSubs.size.toLong()
            repository.upsertSubject(
                userId = currentUserId,
                subjectId = subjectId,
                name = name,
                colorHex = colorHex,
                iconKey = iconKey,
                sortOrder = sortOrder,
                examDateMillis = examDateMillis,
                targetChapters = targetChapters
            ).onSuccess {
                emitMessage(if (subjectId == null) "Subject added" else "Subject updated")
                if (subjectId == null) {
                    checkAchievement("first_step")
                }
            }.onFailure { emitMessage(it.localizedMessage ?: "Could not save subject") }
        }
    }

    fun moveSubject(subject: SubjectItem, delta: Int) {
        viewModelScope.launch {
            val list = (subjectsState.value as? UiState.Success)?.data ?: return@launch
            val idx = list.indexOfFirst { it.id == subject.id }
            val targetIdx = idx + delta
            if (idx < 0 || targetIdx !in list.indices) return@launch
            val other = list[targetIdx]
            repository.upsertSubject(currentUserId, subject.id, subject.name, subject.colorHex, subject.iconKey, targetIdx.toLong(), subject.examDateMillis, subject.targetChapters)
            repository.upsertSubject(currentUserId, other.id, other.name, other.colorHex, other.iconKey, idx.toLong(), other.examDateMillis, other.targetChapters)
        }
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            repository.deleteSubject(currentUserId, subjectId)
                .onSuccess { emitMessage("Subject deleted") }
                .onFailure { emitMessage(it.localizedMessage ?: "Failed to delete subject") }
        }
    }

    fun saveChapter(
        chapterId: String?,
        subjectId: String,
        title: String,
        description: String?,
        personalNotes: String?,
        topicsCsv: String?,
        status: ChapterStatus
    ) {
        viewModelScope.launch {
            val chapters = (chaptersState.value as? UiState.Success)?.data.orEmpty()
            val prevChapter = chapters.firstOrNull { it.id == chapterId }
            val sortOrder = prevChapter?.sortOrder ?: chapters.count { it.subjectId == subjectId }.toLong()
            repository.upsertChapter(
                userId = currentUserId,
                chapterId = chapterId,
                subjectId = subjectId,
                title = title,
                description = description,
                personalNotes = personalNotes,
                topicsCsv = topicsCsv,
                status = status,
                sortOrder = sortOrder
            ).onSuccess {
                val prevCompleted = prevChapter?.chapterStatus()?.countsTowardProgress == true
                if (!prevCompleted && status.countsTowardProgress) {
                    rewardAction(xp = 35L, points = 15L, focusMins = 0L, reason = "Chapter: $title", countStreak = true)
                    evaluateChapterAchievements(subjectId)
                }
                emitMessage(if (chapterId == null) "Chapter created" else "Chapter updated")
            }.onFailure { emitMessage(it.localizedMessage ?: "Failed to save chapter") }
        }
    }

    fun updateChapterStatus(chapter: ChapterItem, newStatus: ChapterStatus) {
        viewModelScope.launch {
            val wasCounted = chapter.chapterStatus().countsTowardProgress
            repository.upsertChapter(
                userId = currentUserId,
                chapterId = chapter.id,
                subjectId = chapter.subjectId,
                title = chapter.title,
                description = chapter.description,
                personalNotes = chapter.personalNotes,
                topicsCsv = chapter.topicsCsv,
                status = newStatus,
                sortOrder = chapter.sortOrder
            ).onSuccess {
                if (!wasCounted && newStatus.countsTowardProgress) {
                    rewardAction(xp = 35L, points = 15L, focusMins = 0L, reason = "${newStatus.label}: ${chapter.title}", countStreak = true)
                    evaluateChapterAchievements(chapter.subjectId)
                } else if (wasCounted && newStatus != chapter.chapterStatus() && newStatus.countsTowardProgress) {
                    // Reward revision/practice progression modestly once
                    rewardAction(xp = 15L, points = 8L, focusMins = 0L, reason = "${newStatus.label}: ${chapter.title}", countStreak = true)
                }
            }.onFailure { emitMessage(it.localizedMessage ?: "Failed to update status") }
        }
    }

    fun moveChapter(chapter: ChapterItem, delta: Int) {
        viewModelScope.launch {
            val list = ((chaptersState.value as? UiState.Success)?.data ?: return@launch)
                .filter { it.subjectId == chapter.subjectId }
            val idx = list.indexOfFirst { it.id == chapter.id }
            val targetIdx = idx + delta
            if (idx < 0 || targetIdx !in list.indices) return@launch
            val other = list[targetIdx]
            repository.upsertChapter(currentUserId, chapter.id, chapter.subjectId, chapter.title, chapter.description, chapter.personalNotes, chapter.topicsCsv, chapter.chapterStatus(), targetIdx.toLong())
            repository.upsertChapter(currentUserId, other.id, other.subjectId, other.title, other.description, other.personalNotes, other.topicsCsv, other.chapterStatus(), idx.toLong())
        }
    }

    fun deleteChapter(chapterId: String) {
        viewModelScope.launch {
            repository.deleteChapter(currentUserId, chapterId)
                .onSuccess { emitMessage("Chapter deleted") }
                .onFailure { emitMessage(it.localizedMessage ?: "Failed to delete chapter") }
        }
    }

    // --- Tasks ---
    fun saveTask(
        taskId: String?,
        title: String,
        description: String?,
        priority: TaskPriority,
        category: String,
        tags: List<String>,
        subjectId: String?,
        chapterId: String?,
        dueAtMillis: Long?,
        reminderEnabled: Boolean,
        completed: Boolean
    ) {
        viewModelScope.launch {
            val existing = (tasksState.value as? UiState.Success)?.data?.firstOrNull { it.id == taskId }
            repository.upsertTask(
                userId = currentUserId,
                taskId = taskId,
                title = title,
                description = description,
                priority = priority,
                category = category,
                tags = tags,
                subjectId = subjectId,
                chapterId = chapterId,
                dueAtMillis = dueAtMillis,
                reminderEnabled = reminderEnabled,
                completed = completed
            ).onSuccess {
                if (existing?.completed != true && completed) {
                    rewardAction(xp = 20L, points = 10L, focusMins = 0L, reason = "Task: $title", countStreak = true)
                    evaluateTaskAchievements()
                }
                emitMessage(if (taskId == null) "Task added" else "Task updated")
            }.onFailure { emitMessage(it.localizedMessage ?: "Failed to save task") }
        }
    }

    fun toggleTaskCompleted(task: TaskItem) {
        saveTask(
            taskId = task.id,
            title = task.title,
            description = task.description,
            priority = task.taskPriority(),
            category = task.category,
            tags = task.tags,
            subjectId = task.subjectId,
            chapterId = task.chapterId,
            dueAtMillis = task.dueAtMillis,
            reminderEnabled = task.reminderEnabled,
            completed = !task.completed
        )
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(currentUserId, taskId)
                .onSuccess { emitMessage("Task deleted") }
        }
    }

    // --- Notes ---
    fun saveNote(
        noteId: String?,
        title: String,
        content: String,
        subjectId: String?,
        chapterId: String?,
        pinned: Boolean,
        favorite: Boolean
    ) {
        viewModelScope.launch {
            repository.upsertNote(
                userId = currentUserId,
                noteId = noteId,
                title = title,
                content = content,
                subjectId = subjectId,
                chapterId = chapterId,
                pinned = pinned,
                favorite = favorite
            ).onSuccess {
                val totalNotes = ((notesState.value as? UiState.Success)?.data?.size ?: 0) + if (noteId == null) 1 else 0
                if (totalNotes >= 3) checkAchievement("note_taker")
                emitMessage("Note saved")
            }.onFailure { emitMessage(it.localizedMessage ?: "Failed to save note") }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            repository.deleteNote(currentUserId, noteId)
                .onSuccess { emitMessage("Note deleted") }
        }
    }

    // --- Pomodoro Timer ---
    fun selectTimerPreset(focusMins: Int, breakMins: Int, longBreakMins: Int, label: String) {
        val safeFocus = focusMins.coerceIn(1, 180)
        val safeBreak = breakMins.coerceIn(1, 60)
        val safeLong = longBreakMins.coerceIn(5, 90)
        timerJob?.cancel()
        soundPlayer.stop()
        _timerState.update {
            it.copy(
                focusMinutes = safeFocus,
                breakMinutes = safeBreak,
                longBreakMinutes = safeLong,
                isBreakPhase = false,
                isRunning = false,
                remainingSeconds = safeFocus * 60,
                totalSeconds = safeFocus * 60,
                modeLabel = label
            )
        }
        viewModelScope.launch {
            prefsRepository.updateFocusPreferences(
                FocusPreferences(safeFocus, safeBreak, safeLong, _timerState.value.ambientSound.label)
            )
        }
    }

    fun selectTimerSubject(subject: SubjectItem?) {
        _timerState.update { it.copy(selectedSubjectId = subject?.id, selectedSubjectName = subject?.name) }
    }

    fun selectAmbientSound(option: AmbientSoundOption) {
        _timerState.update { it.copy(ambientSound = option) }
        soundPlayer.setSound(option, enabled = _timerState.value.isRunning)
        viewModelScope.launch {
            val st = _timerState.value
            prefsRepository.updateFocusPreferences(
                FocusPreferences(st.focusMinutes, st.breakMinutes, st.longBreakMinutes, option.label)
            )
        }
    }

    fun toggleFullScreenFocus(fullScreen: Boolean) {
        _timerState.update { it.copy(isFullScreen = fullScreen) }
    }

    fun startOrResumeTimer() {
        val st = _timerState.value
        if (st.isRunning) return
        val startedAt = if (st.sessionStartedAtMillis == 0L) System.currentTimeMillis() else st.sessionStartedAtMillis
        _timerState.update { it.copy(isRunning = true, sessionStartedAtMillis = startedAt) }
        soundPlayer.setSound(st.ambientSound, enabled = true)

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val current = _timerState.value
                if (!current.isRunning) break
                if (current.remainingSeconds > 1) {
                    _timerState.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
                } else {
                    onPhaseCompleted()
                    break
                }
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        soundPlayer.stop()
        _timerState.update { it.copy(isRunning = false) }
    }

    fun resetTimer() {
        timerJob?.cancel()
        soundPlayer.stop()
        _timerState.update {
            val secs = (if (it.isBreakPhase) it.breakMinutes else it.focusMinutes) * 60
            it.copy(
                isRunning = false,
                remainingSeconds = secs,
                totalSeconds = secs,
                sessionStartedAtMillis = 0L
            )
        }
    }

    fun completeSessionEarly() {
        val st = _timerState.value
        val elapsedMinutes = ((st.totalSeconds - st.remainingSeconds) / 60).coerceAtLeast(1)
        if (!st.isBreakPhase) {
            timerJob?.cancel()
            soundPlayer.stop()
            viewModelScope.launch {
                finishFocusPhase(st, elapsedMinutes.toLong())
            }
        }
    }

    private suspend fun onPhaseCompleted() {
        val st = _timerState.value
        soundPlayer.stop()
        if (!st.isBreakPhase) {
            finishFocusPhase(st, st.focusMinutes.toLong())
            val breakSecs = st.breakMinutes * 60
            _timerState.update {
                it.copy(
                    isBreakPhase = true,
                    isRunning = false,
                    remainingSeconds = breakSecs,
                    totalSeconds = breakSecs,
                    sessionStartedAtMillis = 0L
                )
            }
        } else {
            val focusSecs = st.focusMinutes * 60
            _timerState.update {
                it.copy(
                    isBreakPhase = false,
                    isRunning = false,
                    remainingSeconds = focusSecs,
                    totalSeconds = focusSecs,
                    sessionStartedAtMillis = 0L
                )
            }
            logAndNotify(
                title = "Break Finished",
                body = "Ready for your next ${st.focusMinutes}m study sprint.",
                category = "Focus",
                postSystem = notificationPrefs.value.focusCompletion
            )
        }
    }

    private suspend fun finishFocusPhase(st: PomodoroTimerState, recordedMinutes: Long) {
        val now = System.currentTimeMillis()
        val start = if (st.sessionStartedAtMillis > 0L) st.sessionStartedAtMillis else now - (recordedMinutes * 60_000L)
        repository.recordFocusSession(
            userId = currentUserId,
            subjectId = st.selectedSubjectId,
            subjectName = st.selectedSubjectName,
            durationMinutes = recordedMinutes,
            breakMinutes = st.breakMinutes.toLong(),
            modeLabel = st.modeLabel,
            ambientSound = st.ambientSound.label,
            startedAtMillis = start,
            endedAtMillis = now
        )
        val xp = recordedMinutes * 2L
        val pts = (recordedMinutes / 5L).coerceAtLeast(3L)
        rewardAction(
            xp = xp,
            points = pts,
            focusMins = recordedMinutes,
            reason = "Focus Session (${recordedMinutes}m)",
            countStreak = true
        )
        checkAchievement("focus_beginner")
        val totalFocus = currentAccount().totalFocusMinutes + recordedMinutes
        if (totalFocus >= 600L) checkAchievement("focus_legend")
        logAndNotify(
            title = "Focus Session Complete",
            body = "Logged ${recordedMinutes}m (+$xp XP, +$pts Theme Points).",
            category = "Focus",
            postSystem = notificationPrefs.value.focusCompletion
        )
        resetTimer()
    }

    // --- Attendance ---
    fun markAttendance(dateKey: String, subjectId: String, status: AttendanceStatus, note: String?) {
        viewModelScope.launch {
            repository.markAttendance(currentUserId, dateKey, subjectId, status, note)
                .onSuccess { emitMessage("Attendance saved ($dateKey)") }
                .onFailure { emitMessage(it.localizedMessage ?: "Failed to mark attendance") }
        }
    }

    // --- Shop & Profile ---
    fun purchaseShopItem(item: ShopItem) {
        viewModelScope.launch {
            repository.purchaseShopItem(currentUserId, currentAccount(), item)
                .onSuccess { emitMessage("Unlocked ${item.title}!") }
                .onFailure { emitMessage(it.localizedMessage ?: "Purchase failed") }
        }
    }

    fun equipFrame(frameId: String) {
        viewModelScope.launch {
            repository.equipFrame(currentUserId, frameId)
                .onSuccess { emitMessage("Profile frame updated") }
        }
    }

    fun updateProfile(
        displayName: String,
        bio: String?,
        academicSession: String?,
        academicClass: String?,
        academicStream: String?,
        dailyGoalMinutes: Long,
        discoverable: Boolean,
        showStats: Boolean,
        showAchievements: Boolean
    ) {
        viewModelScope.launch {
            repository.updateProfileDetails(
                userId = currentUserId,
                displayName = displayName,
                bio = bio,
                academicSession = academicSession,
                academicClass = academicClass,
                academicStream = academicStream,
                dailyGoalMinutes = dailyGoalMinutes,
                discoverable = discoverable,
                showStats = showStats,
                showAchievements = showAchievements
            ).onSuccess { emitMessage("Profile updated") }
                .onFailure { emitMessage(it.localizedMessage ?: "Failed to update profile") }
        }
    }

    // --- Backup & Restore ---
    fun createLocalBackup(onExported: (String) -> Unit = {}) {
        viewModelScope.launch {
            val subjects = (subjectsState.value as? UiState.Success)?.data.orEmpty()
            val chapters = (chaptersState.value as? UiState.Success)?.data.orEmpty()
            val tasks = (tasksState.value as? UiState.Success)?.data.orEmpty()
            val notes = (notesState.value as? UiState.Success)?.data.orEmpty()
            val sessions = (focusSessionsState.value as? UiState.Success)?.data.orEmpty()
            val attendance = (attendanceState.value as? UiState.Success)?.data.orEmpty()
            val json = repository.exportBackupJson(subjects, chapters, tasks, notes, sessions, attendance)
            prefsRepository.saveLocalBackupSnapshot(json)
            emitMessage("Local backup saved")
            onExported(json)
        }
    }

    fun restoreFromBackup(json: String) {
        viewModelScope.launch {
            repository.restoreFromBackupJson(currentUserId, json)
                .onSuccess { count -> emitMessage("Restored $count items from backup") }
                .onFailure { emitMessage("Invalid backup format") }
        }
    }

    // --- Gamification Helpers ---
    private suspend fun rewardAction(
        xp: Long,
        points: Long,
        focusMins: Long,
        reason: String,
        countStreak: Boolean
    ) {
        val acc = currentAccount()
        val oldLevel = acc.level
        repository.awardAcademicActivity(
            userId = currentUserId,
            account = acc,
            xpDelta = xp,
            pointsDelta = points,
            focusMinutesDelta = focusMins,
            reason = reason,
            countForStreak = countStreak
        ).onSuccess { newLevel ->
            if (newLevel > oldLevel) {
                _levelUpDialogLevel.value = newLevel
                logAndNotify(
                    title = "Level Up! Level $newLevel",
                    body = "You reached ${GamificationCatalog.rankForLevel(newLevel)}.",
                    category = "Gamification",
                    postSystem = notificationPrefs.value.achievementAlerts
                )
            }
            val updatedStreak = if (countStreak) acc.currentStreak + 1L else acc.currentStreak
            if (updatedStreak >= 7L) checkAchievement("seven_day_streak")
            if (updatedStreak >= 30L) checkAchievement("thirty_day_streak")
        }
    }

    private suspend fun checkAchievement(key: String) {
        val acc = currentAccount()
        repository.unlockAchievementIfEligible(currentUserId, acc, unlockedKeys(), key)
            .onSuccess { unlocked ->
                if (unlocked) {
                    val def = GamificationCatalog.achievements.firstOrNull { it.key == key }
                    if (def != null) {
                        logAndNotify(
                            title = "Achievement Unlocked: ${def.title}",
                            body = "+${def.xpReward} XP, +${def.pointsReward} Theme Points",
                            category = "Achievement",
                            postSystem = notificationPrefs.value.achievementAlerts
                        )
                    }
                }
            }
    }

    private suspend fun evaluateChapterAchievements(subjectId: String) {
        val chapters = (chaptersState.value as? UiState.Success)?.data.orEmpty()
        val completedTotal = chapters.count { it.chapterStatus().countsTowardProgress } + 1
        if (completedTotal >= 1) checkAchievement("first_chapter")
        if (completedTotal >= 25) checkAchievement("syllabus_champion")

        val subjectChapters = chapters.filter { it.subjectId == subjectId }
        if (subjectChapters.size >= 3 && subjectChapters.all { it.chapterStatus().countsTowardProgress }) {
            checkAchievement("subject_master")
        }
    }

    private suspend fun evaluateTaskAchievements() {
        val completedTasks = (tasksState.value as? UiState.Success)?.data?.count { it.completed } ?: 0
        if (completedTasks + 1 >= 5) checkAchievement("task_finisher")
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        soundPlayer.stop()
    }
}
