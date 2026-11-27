package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.PreferencesRepository
import com.example.data.local.SyllabyRoomDatabase
import com.example.data.model.GamificationCatalog
import com.example.data.remote.SocialRoomsRepository
import com.example.data.remote.SyllabyRepository
import com.example.data.remote.authStateFlow
import com.example.data.remote.signOut
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BackupSettingsScreen
import com.example.ui.screens.FocusScreen
import com.example.ui.screens.FriendsScreen
import com.example.ui.screens.GamificationShopScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileHubScreen
import com.example.ui.screens.StudyRoomsScreen
import com.example.ui.screens.SyllabusScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.ThemeStudioScreen
import com.example.ui.theme.LocalSyllabyColors
import com.example.ui.theme.SyllabyTracklyTheme
import com.example.ui.theme.ThemeSettings
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.SocialViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.ConnectivityObserver
import com.example.util.NotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.merge

const val ROUTE_HOME = "home"
const val ROUTE_SYLLABUS = "syllabus"
const val ROUTE_FOCUS = "focus"
const val ROUTE_TASKS = "tasks"
const val ROUTE_PROFILE = "profile"
const val ROUTE_NOTES = "notes"
const val ROUTE_ANALYTICS = "analytics"
const val ROUTE_ATTENDANCE = "attendance"
const val ROUTE_GAMIFICATION = "gamification"
const val ROUTE_STUDY_ROOMS = "study_rooms"
const val ROUTE_FRIENDS = "friends"
const val ROUTE_THEMES = "themes"
const val ROUTE_NOTIFICATIONS = "notifications"
const val ROUTE_BACKUP = "backup"

private data class PrimaryNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

private val PrimaryDestinations = listOf(
    PrimaryNavDestination(ROUTE_HOME, "Home", Icons.Outlined.Home, "nav_home"),
    PrimaryNavDestination(ROUTE_SYLLABUS, "Syllabus", Icons.AutoMirrored.Outlined.MenuBook, "nav_syllabus"),
    PrimaryNavDestination(ROUTE_FOCUS, "Focus", Icons.Outlined.Timer, "nav_focus"),
    PrimaryNavDestination(ROUTE_TASKS, "Tasks", Icons.Outlined.CheckBox, "nav_tasks"),
    PrimaryNavDestination(ROUTE_PROFILE, "Profile", Icons.Outlined.Person, "nav_profile")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.ensureChannel(this)
        setContent {
            AppContent()
        }
    }
}

@Composable
private fun AppContent() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val prefsRepository = remember { PreferencesRepository(appContext) }
    val themeSettings by prefsRepository.themeSettingsFlow.collectAsStateWithLifecycle(initialValue = ThemeSettings())

    val currentUser by remember { Firebase.auth.authStateFlow() }
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    SyllabyTracklyTheme(settings = themeSettings) {
        if (currentUser == null) {
            AuthScreen(onAuthSuccess = { })
        } else {
            val user = currentUser!!
            key(user.uid) {
                AuthenticatedApp(
                    user = user,
                    prefsRepository = prefsRepository
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedApp(
    user: FirebaseUser,
    prefsRepository: PreferencesRepository
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val db = remember(appContext) {
        FirebaseFirestore.getInstance(appContext.getString(R.string.firestore_database_id))
    }
    val cacheDao = remember(appContext) { SyllabyRoomDatabase.getInstance(appContext).cacheDao() }
    val connectivityObserver = remember(appContext) { ConnectivityObserver(appContext) }

    val mainViewModel: MainViewModel = viewModel(
        key = "main_${user.uid}",
        factory = viewModelFactory {
            initializer {
                MainViewModel(
                    repository = SyllabyRepository(db = db, cacheDao = cacheDao),
                    prefsRepository = prefsRepository,
                    cacheDao = cacheDao,
                    currentUserId = user.uid,
                    initialEmail = user.email.orEmpty(),
                    initialDisplayName = user.displayName ?: user.email?.substringBefore("@") ?: "Scholar",
                    initialPhotoUrl = user.photoUrl?.toString()
                )
            }
        }
    )

    val socialViewModel: SocialViewModel = viewModel(
        key = "social_${user.uid}",
        factory = viewModelFactory {
            initializer {
                SocialViewModel(
                    repository = SocialRoomsRepository(db = db),
                    connectivityObserver = connectivityObserver,
                    currentUserId = user.uid
                )
            }
        }
    )

    val colors = LocalSyllabyColors.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(mainViewModel, socialViewModel) {
        merge(
            mainViewModel.events,
            socialViewModel.socialEvents
        ).collect { event ->
            when (event) {
                is com.example.ui.viewmodel.AppEvent -> {
                    if (event.postSystemNotification) {
                        NotificationHelper.postNotification(
                            context = appContext,
                            id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                            title = event.notifTitle.ifBlank { "SyllabyTrackly" },
                            body = event.message
                        )
                    }
                    snackbarHostState.showSnackbar(event.message)
                }
                is String -> snackbarHostState.showSnackbar(event)
            }
        }
    }

    // Collect core state
    val accountState by mainViewModel.accountState.collectAsStateWithLifecycle()
    val subjectsState by mainViewModel.subjectsState.collectAsStateWithLifecycle()
    val chaptersState by mainViewModel.chaptersState.collectAsStateWithLifecycle()
    val tasksState by mainViewModel.tasksState.collectAsStateWithLifecycle()
    val notesState by mainViewModel.notesState.collectAsStateWithLifecycle()
    val sessionsState by mainViewModel.focusSessionsState.collectAsStateWithLifecycle()
    val attendanceState by mainViewModel.attendanceState.collectAsStateWithLifecycle()
    val transactionsState by mainViewModel.transactionsState.collectAsStateWithLifecycle()
    val achievementsState by mainViewModel.achievementsState.collectAsStateWithLifecycle()
    val notifications by mainViewModel.notifications.collectAsStateWithLifecycle()
    val themeSettings by mainViewModel.themeSettings.collectAsStateWithLifecycle()
    val notifPrefs by mainViewModel.notificationPrefs.collectAsStateWithLifecycle()
    val dashboardLayout by mainViewModel.dashboardLayout.collectAsStateWithLifecycle()
    val lastBackupMillis by mainViewModel.lastBackupMillis.collectAsStateWithLifecycle()
    val savedBackupJson by mainViewModel.savedBackupJson.collectAsStateWithLifecycle()
    val timerState by mainViewModel.timerState.collectAsStateWithLifecycle()
    val levelUpLevel by mainViewModel.levelUpDialogLevel.collectAsStateWithLifecycle()

    // Collect social state
    val isOnline by socialViewModel.isOnline.collectAsStateWithLifecycle()
    val myPublicProfileState by socialViewModel.myPublicProfile.collectAsStateWithLifecycle()
    val friendsState by socialViewModel.friendsState.collectAsStateWithLifecycle()
    val incomingRequestsState by socialViewModel.incomingRequestsState.collectAsStateWithLifecycle()
    val outgoingRequestsState by socialViewModel.outgoingRequestsState.collectAsStateWithLifecycle()
    val publicRoomsState by socialViewModel.publicRoomsState.collectAsStateWithLifecycle()
    val myRoomsState by socialViewModel.myRoomsState.collectAsStateWithLifecycle()
    val invitedRoomsState by socialViewModel.invitedRoomsState.collectAsStateWithLifecycle()
    val activeRoomMessagesState by socialViewModel.activeRoomMessages.collectAsStateWithLifecycle()
    val searchedProfile by socialViewModel.searchedProfile.collectAsStateWithLifecycle()
    val viewedFriendProfile by socialViewModel.viewedFriendProfile.collectAsStateWithLifecycle()

    val account = (accountState as? UiState.Success)?.data
    val subjects = (subjectsState as? UiState.Success)?.data.orEmpty()
    val chapters = (chaptersState as? UiState.Success)?.data.orEmpty()
    val tasks = (tasksState as? UiState.Success)?.data.orEmpty()
    val notes = (notesState as? UiState.Success)?.data.orEmpty()
    val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
    val attendance = (attendanceState as? UiState.Success)?.data.orEmpty()
    val transactions = (transactionsState as? UiState.Success)?.data.orEmpty()
    val achievements = (achievementsState as? UiState.Success)?.data.orEmpty()
    val myPublicProfile = (myPublicProfileState as? UiState.Success)?.data
    val friends = (friendsState as? UiState.Success)?.data.orEmpty()
    val incomingRequests = (incomingRequestsState as? UiState.Success)?.data.orEmpty()
    val outgoingRequests = (outgoingRequestsState as? UiState.Success)?.data.orEmpty()
    val publicRooms = (publicRoomsState as? UiState.Success)?.data.orEmpty()
    val myRooms = (myRoomsState as? UiState.Success)?.data.orEmpty()
    val invitedRooms = (invitedRoomsState as? UiState.Success)?.data.orEmpty()
    val activeRoomMessages = (activeRoomMessagesState as? UiState.Success)?.data.orEmpty()
    val unreadNotifCount = remember(notifications) { notifications.count { !it.read } }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_HOME
    val primaryRoutes = remember { PrimaryDestinations.map { it.route }.toSet() }
    val showChromeNav = currentRoute in primaryRoutes && !timerState.isFullScreen

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = colors.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (showChromeNav && !useRail) {
                    NavigationBar(
                        containerColor = colors.surface,
                        contentColor = colors.textPrimary,
                        tonalElevation = 0.dp
                    ) {
                        PrimaryDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = { Icon(dest.icon, contentDescription = dest.label) },
                                label = { Text(dest.label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag(dest.testTag),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colors.accent,
                                    selectedTextColor = colors.accent,
                                    unselectedIconColor = colors.textSecondary,
                                    unselectedTextColor = colors.textSecondary,
                                    indicatorColor = colors.accent.copy(alpha = 0.14f)
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showChromeNav && useRail) {
                    NavigationRail(
                        containerColor = colors.surface,
                        contentColor = colors.textPrimary
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        PrimaryDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = { Icon(dest.icon, contentDescription = dest.label) },
                                label = { Text(dest.label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("${dest.testTag}_rail"),
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = colors.accent,
                                    selectedTextColor = colors.accent,
                                    unselectedIconColor = colors.textSecondary,
                                    unselectedTextColor = colors.textSecondary,
                                    indicatorColor = colors.accent.copy(alpha = 0.14f)
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_HOME
                    ) {
                        composable(ROUTE_HOME) {
                            HomeDashboardScreen(
                                account = account,
                                subjects = subjects,
                                chapters = chapters,
                                tasks = tasks,
                                focusSessions = sessions,
                                achievements = achievements,
                                unreadNotificationCount = unreadNotifCount,
                                layoutPrefs = dashboardLayout,
                                onNavigateToSyllabus = { navigateTopLevel(ROUTE_SYLLABUS) },
                                onNavigateToFocus = { navigateTopLevel(ROUTE_FOCUS) },
                                onNavigateToTasks = { navigateTopLevel(ROUTE_TASKS) },
                                onNavigateToNotifications = { navController.navigate(ROUTE_NOTIFICATIONS) },
                                onNavigateToAchievements = { navController.navigate(ROUTE_GAMIFICATION) },
                                onToggleTask = { mainViewModel.toggleTaskCompleted(it) },
                                onQuickAddTask = { title, priority ->
                                    mainViewModel.saveTask(
                                        taskId = null,
                                        title = title,
                                        description = null,
                                        priority = priority,
                                        category = "Study",
                                        tags = emptyList(),
                                        subjectId = null,
                                        chapterId = null,
                                        dueAtMillis = System.currentTimeMillis(),
                                        reminderEnabled = false,
                                        completed = false
                                    )
                                },
                                onSaveLayout = { ordered, hidden ->
                                    mainViewModel.updateDashboardLayout(ordered, hidden)
                                }
                            )
                        }

                        composable(ROUTE_SYLLABUS) {
                            SyllabusScreen(
                                account = account,
                                subjects = subjects,
                                chapters = chapters,
                                onSaveAcademicStructure = { session, klass, stream ->
                                    mainViewModel.updateProfile(
                                        displayName = account?.displayName ?: "Scholar",
                                        bio = account?.bio,
                                        academicSession = session,
                                        academicClass = klass,
                                        academicStream = stream,
                                        dailyGoalMinutes = account?.dailyGoalMinutes ?: 60L,
                                        discoverable = myPublicProfile?.discoverable ?: true,
                                        showStats = myPublicProfile?.showStats ?: true,
                                        showAchievements = myPublicProfile?.showAchievements ?: true
                                    )
                                },
                                onSaveSubject = { id, name, colorHex, iconKey, examMillis, target ->
                                    mainViewModel.saveSubject(id, name, colorHex, iconKey, examMillis, target)
                                },
                                onMoveSubject = { sub, delta -> mainViewModel.moveSubject(sub, delta) },
                                onDeleteSubject = { mainViewModel.deleteSubject(it) },
                                onSaveChapter = { id, subId, title, desc, pNotes, topics, status ->
                                    mainViewModel.saveChapter(id, subId, title, desc, pNotes, topics, status)
                                },
                                onUpdateChapterStatus = { ch, status -> mainViewModel.updateChapterStatus(ch, status) },
                                onMoveChapter = { ch, delta -> mainViewModel.moveChapter(ch, delta) },
                                onDeleteChapter = { mainViewModel.deleteChapter(it) }
                            )
                        }

                        composable(ROUTE_FOCUS) {
                            FocusScreen(
                                timerState = timerState,
                                account = account,
                                subjects = subjects,
                                sessions = sessions,
                                onSelectPreset = { f, b, lb, label -> mainViewModel.selectTimerPreset(f, b, lb, label) },
                                onSelectSubject = { mainViewModel.selectTimerSubject(it) },
                                onSelectAmbientSound = { mainViewModel.selectAmbientSound(it) },
                                onToggleFullScreen = { mainViewModel.toggleFullScreenFocus(it) },
                                onStartOrResume = { mainViewModel.startOrResumeTimer() },
                                onPause = { mainViewModel.pauseTimer() },
                                onReset = { mainViewModel.resetTimer() },
                                onCompleteEarly = { mainViewModel.completeSessionEarly() }
                            )
                        }

                        composable(ROUTE_TASKS) {
                            TasksScreen(
                                tasks = tasks,
                                subjects = subjects,
                                chapters = chapters,
                                onSaveTask = { id, title, desc, prio, cat, tags, subId, chId, due, rem, comp ->
                                    mainViewModel.saveTask(id, title, desc, prio, cat, tags, subId, chId, due, rem, comp)
                                },
                                onToggleCompleted = { mainViewModel.toggleTaskCompleted(it) },
                                onDeleteTask = { mainViewModel.deleteTask(it) }
                            )
                        }

                        composable(ROUTE_PROFILE) {
                            ProfileHubScreen(
                                account = account,
                                publicProfile = myPublicProfile,
                                unreadNotificationCount = unreadNotifCount,
                                onUpdateProfile = { name, bio, sess, klass, stream, goal, disc, stats, ach ->
                                    mainViewModel.updateProfile(name, bio, sess, klass, stream, goal, disc, stats, ach)
                                },
                                onNavigateToNotes = { navController.navigate(ROUTE_NOTES) },
                                onNavigateToAnalytics = { navController.navigate(ROUTE_ANALYTICS) },
                                onNavigateToAttendance = { navController.navigate(ROUTE_ATTENDANCE) },
                                onNavigateToGamification = { navController.navigate(ROUTE_GAMIFICATION) },
                                onNavigateToStudyRooms = { navController.navigate(ROUTE_STUDY_ROOMS) },
                                onNavigateToFriends = { navController.navigate(ROUTE_FRIENDS) },
                                onNavigateToThemes = { navController.navigate(ROUTE_THEMES) },
                                onNavigateToNotifications = { navController.navigate(ROUTE_NOTIFICATIONS) },
                                onNavigateToBackup = { navController.navigate(ROUTE_BACKUP) },
                                onSignOut = {
                                    signOut(credentialManager = credentialManager, scope = scope)
                                }
                            )
                        }

                        composable(ROUTE_NOTES) {
                            NotesScreen(
                                notes = notes,
                                subjects = subjects,
                                chapters = chapters,
                                onBack = { navController.popBackStack() },
                                onSaveNote = { id, title, content, subId, chId, pinned, fav ->
                                    mainViewModel.saveNote(id, title, content, subId, chId, pinned, fav)
                                },
                                onDeleteNote = { mainViewModel.deleteNote(it) }
                            )
                        }

                        composable(ROUTE_ANALYTICS) {
                            AnalyticsScreen(
                                account = account,
                                subjects = subjects,
                                chapters = chapters,
                                tasks = tasks,
                                sessions = sessions,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_ATTENDANCE) {
                            AttendanceScreen(
                                attendance = attendance,
                                subjects = subjects,
                                onBack = { navController.popBackStack() },
                                onMarkAttendance = { dateKey, subId, status, note ->
                                    mainViewModel.markAttendance(dateKey, subId, status, note)
                                }
                            )
                        }

                        composable(ROUTE_GAMIFICATION) {
                            GamificationShopScreen(
                                account = account,
                                unlockedAchievements = achievements,
                                transactions = transactions,
                                onBack = { navController.popBackStack() },
                                onPurchaseItem = { mainViewModel.purchaseShopItem(it) },
                                onEquipFrame = { mainViewModel.equipFrame(it) }
                            )
                        }

                        composable(ROUTE_STUDY_ROOMS) {
                            StudyRoomsScreen(
                                currentUserId = user.uid,
                                isOnline = isOnline,
                                publicRooms = publicRooms,
                                myRooms = myRooms,
                                invitedRooms = invitedRooms,
                                friends = friends,
                                activeRoomMessages = activeRoomMessages,
                                onBack = { navController.popBackStack() },
                                onSelectRoomForChat = { socialViewModel.selectRoomForChat(it) },
                                onCreateRoom = { title, desc, env, privacy, invited, onCreated ->
                                    socialViewModel.createStudyRoom(title, desc, env, privacy, invited, onCreated)
                                },
                                onJoinRoom = { socialViewModel.joinRoom(it) },
                                onLeaveRoom = { socialViewModel.leaveRoom(it) },
                                onInviteFriend = { room, friendUid -> socialViewModel.inviteFriendToRoom(room, friendUid) },
                                onUpdateRoomTimer = { room, st, dur, rem ->
                                    socialViewModel.updateRoomTimer(room, st, dur, rem)
                                },
                                onSendMessage = { room, text -> socialViewModel.sendRoomMessage(room, text) },
                                onSubmitReport = { type, id, reason, details ->
                                    socialViewModel.submitReport(type, id, reason, details)
                                }
                            )
                        }

                        composable(ROUTE_FRIENDS) {
                            FriendsScreen(
                                isOnline = isOnline,
                                myPublicProfile = myPublicProfile,
                                friends = friends,
                                incomingRequests = incomingRequests,
                                outgoingRequests = outgoingRequests,
                                searchedProfile = searchedProfile,
                                viewedFriendProfile = viewedFriendProfile,
                                onBack = { navController.popBackStack() },
                                onSearchByCode = { socialViewModel.searchByFriendCode(it) },
                                onClearSearch = { socialViewModel.clearSearchedProfile() },
                                onSendRequest = { socialViewModel.sendFriendRequest(it) },
                                onRespondRequest = { req, accept -> socialViewModel.respondFriendRequest(req, accept) },
                                onInspectFriend = { socialViewModel.loadFriendProfile(it) },
                                onClearInspectedFriend = { socialViewModel.clearViewedFriendProfile() },
                                onRemoveOrBlockFriend = { uid, block -> socialViewModel.removeOrBlockFriend(uid, block) },
                                onReportUser = { uid, reason, details ->
                                    socialViewModel.submitReport("USER", uid, reason, details)
                                }
                            )
                        }

                        composable(ROUTE_THEMES) {
                            ThemeStudioScreen(
                                themeSettings = themeSettings,
                                onUpdateTheme = { mainViewModel.updateThemeSettings(it) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_NOTIFICATIONS) {
                            NotificationsScreen(
                                notifications = notifications,
                                prefs = notifPrefs,
                                onUpdatePrefs = { mainViewModel.updateNotificationPrefs(it) },
                                onMarkAllRead = { mainViewModel.markNotificationsRead() },
                                onClearAll = { mainViewModel.clearNotifications() },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_BACKUP) {
                            BackupSettingsScreen(
                                lastBackupMillis = lastBackupMillis,
                                savedBackupJson = savedBackupJson,
                                subjects = subjects,
                                chapters = chapters,
                                tasks = tasks,
                                publicProfile = myPublicProfile,
                                onCreateBackup = { cb -> mainViewModel.createLocalBackup(cb) },
                                onRestoreBackup = { mainViewModel.restoreFromBackup(it) },
                                onUpdatePrivacy = { discoverable, showStats, showAchievements ->
                                    mainViewModel.updateProfile(
                                        displayName = account?.displayName ?: "Scholar",
                                        bio = account?.bio,
                                        academicSession = account?.academicSession,
                                        academicClass = account?.academicClass,
                                        academicStream = account?.academicStream,
                                        dailyGoalMinutes = account?.dailyGoalMinutes ?: 60L,
                                        discoverable = discoverable,
                                        showStats = showStats,
                                        showAchievements = showAchievements
                                    )
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    levelUpLevel?.let { newLevel ->
        AlertDialog(
            onDismissRequest = { mainViewModel.dismissLevelUpDialog() },
            title = { Text("Level Up! Level $newLevel") },
            text = {
                Text(
                    "Congratulations! Your consistent study earned the rank of ${GamificationCatalog.rankForLevel(newLevel)}.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { mainViewModel.dismissLevelUpDialog() },
                    modifier = Modifier.testTag("dismiss_level_up_button")
                ) {
                    Text("Keep Going")
                }
            }
        )
    }
}
