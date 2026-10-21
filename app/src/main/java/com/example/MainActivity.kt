package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.LifestyleDatabase
import com.example.data.model.AgendaTask
import com.example.data.model.DiaryEntry
import com.example.data.repository.LifestyleRepository
import com.example.data.repository.MoneyRepository
import com.example.ui.components.DiaryDialog
import com.example.ui.components.EveningReviewDialog
import com.example.ui.components.GoogleCalendarPickerDialog
import com.example.ui.components.GoogleHeader
import com.example.ui.components.SyncInfoBottomSheet
import com.example.ui.components.TaskDialog
import com.example.ui.screens.Agenda24hScreen
import com.example.ui.screens.DiaryScreen
import com.example.ui.screens.DualViewScreen
import com.example.ui.screens.MoneyScreen
import com.example.ui.screens.MorningRoutineScreen
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.ImtiazLifestyleTheme
import androidx.lifecycle.lifecycleScope
import com.example.auth.AuthManager
import com.example.data.firestore.FirestoreSyncService
import com.example.ui.viewmodel.LifestyleTab
import com.example.ui.viewmodel.LifestyleViewModel
import com.example.ui.viewmodel.LifestyleViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = LifestyleDatabase.getDatabase(applicationContext)
        val firestoreService = FirestoreSyncService(applicationContext)
        val moneyRepository = MoneyRepository(database.moneyDao(), applicationContext)
        val repository = LifestyleRepository(database.lifestyleDao(), firestoreService, moneyRepository)
        val authManager = AuthManager(this)

        authManager.attemptAutoSignIn(lifecycleScope)

        // Schedule exact prayer alarms with zero battery drain outside prayer times
        com.example.service.PrayerAlarmScheduler.schedulePrayerAlarms(applicationContext)

        setContent {
            val viewModel: LifestyleViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = LifestyleViewModelFactory(repository)
            )

            ImtiazLifestyleTheme {
                LifestyleApp(
                    viewModel = viewModel,
                    authManager = authManager,
                    activity = this
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        com.example.util.QuranRadioManager.release()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifestyleApp(
    viewModel: LifestyleViewModel,
    authManager: AuthManager,
    activity: ComponentActivity
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val diaryEntries by viewModel.diaryEntries.collectAsStateWithLifecycle()
    val datesWithTasks by viewModel.datesWithTasks.collectAsStateWithLifecycle()
    val datesWithDiaryEntries by viewModel.datesWithDiaryEntries.collectAsStateWithLifecycle()

    val currentUser by authManager.currentUser.collectAsStateWithLifecycle()
    val isSigningIn by authManager.isSigningIn.collectAsStateWithLifecycle()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsStateWithLifecycle()
    val lastCloudSyncStatus by viewModel.lastCloudSyncStatus.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // When authentication state changes, activate realtime multi-device sync
    androidx.compose.runtime.LaunchedEffect(currentUser) {
        viewModel.onUserAuthenticated(currentUser != null)
    }

    // Back handling: if not on DUAL tab, back navigates to DUAL
    BackHandler(enabled = currentTab != LifestyleTab.DUAL) {
        viewModel.setTab(LifestyleTab.DUAL)
    }

    // Prompt permissions once on installation / first launch
    val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { _ -> }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        // 1. Android 13+ Notification Permission
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val hasNotif = androidx.core.content.ContextCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasNotif) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 2. Google GPS Location Permission (Allow / Don't allow once on installation)
        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
            activity,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
            activity,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        // 3. Battery Optimization Permission (Request exemption so app runs 24/7 in background even when closed)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                val powerManager = activity.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
                if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(activity.packageName)) {
                    val batteryIntent = android.content.Intent(
                        android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    ).apply {
                        data = android.net.Uri.parse("package:${activity.packageName}")
                    }
                    activity.startActivity(batteryIntent)
                }
            } catch (e: Exception) {
                android.util.Log.w("MainActivity", "Battery optimization prompt error: ${e.message}")
            }
        }

        // Navigate to Money tab if opened from debt reminder notification
        val targetTab = activity.intent?.getStringExtra("navigate_to_tab")
        if (targetTab == "MONEY") {
            viewModel.setTab(LifestyleTab.MONEY)
        }
    }

    // Dialog & Bottom Sheet States
    var showCalendarPicker by remember { mutableStateOf(false) }

    var showTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<AgendaTask?>(null) }
    var taskDefaultTimeSlot by remember { mutableStateOf("09:00") }

    var showDiaryDialog by remember { mutableStateOf(false) }
    var diaryToEdit by remember { mutableStateOf<DiaryEntry?>(null) }

    var showEveningReviewDialog by remember { mutableStateOf(false) }

    var showSyncSheet by remember { mutableStateOf(false) }
    val syncSheetState = rememberModalBottomSheetState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            GoogleHeader(
                currentDateText = viewModel.getFormattedSelectedDate(),
                selectedDateStr = selectedDate,
                datesWithTasks = datesWithTasks,
                datesWithDiaryEntries = datesWithDiaryEntries,
                isToday = viewModel.isSelectedDateToday(),
                currentUser = currentUser,
                isSyncing = isCloudSyncing,
                onSignInClick = {
                    authManager.signInWithGoogle(
                        activity = activity,
                        scope = scope,
                        onSuccess = {
                            viewModel.syncBidirectional { _, msg ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(msg)
                                }
                            }
                        },
                        onError = { err ->
                            scope.launch {
                                snackbarHostState.showSnackbar("Sign in error: $err")
                            }
                        }
                    )
                },
                onOpenCalendarPicker = { showCalendarPicker = true },
                onSelectDate = { newDate -> viewModel.setDate(newDate) },
                onToday = { viewModel.selectToday() },
                onSyncStatusClick = { showSyncSheet = true },
                onResetTemplate = {
                    viewModel.resetToSampleTemplate()
                    scope.launch {
                        snackbarHostState.showSnackbar("Daily template restored for $selectedDate")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                tonalElevation = 2.dp,
                modifier = Modifier
                    .testTag("lifestyle_bottom_nav")
                    .fillMaxWidth()
            ) {
                LifestyleTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab
                    val (icon, testId) = when (tab) {
                        LifestyleTab.DUAL -> Pair(Icons.Default.Dashboard, "tab_dual")
                        LifestyleTab.MORNING -> Pair(Icons.Default.WbSunny, "tab_morning")
                        LifestyleTab.AGENDA -> Pair(Icons.Default.CalendarMonth, "tab_agenda")
                        LifestyleTab.DIARY -> Pair(Icons.Default.MenuBook, "tab_diary")
                        LifestyleTab.MONEY -> Pair(Icons.Default.AttachMoney, "tab_money")
                    }

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.setTab(tab) },
                        alwaysShowLabel = true,
                        icon = {
                            if (tab == LifestyleTab.MONEY) {
                                Icon(
                                    painter = painterResource(id = com.example.R.drawable.ic_rupee_pouch),
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 10.sp
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GoogleBlue,
                            selectedTextColor = GoogleBlue,
                            indicatorColor = GoogleBlue.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag(testId)
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "tab_fade_transition"
            ) { tab ->
                when (tab) {
                    LifestyleTab.DUAL -> {
                        DualViewScreen(
                            viewModel = viewModel,
                            currentUser = currentUser,
                            onSignInRequest = {
                                authManager.signInWithGoogle(
                                    activity = activity,
                                    scope = scope,
                                    onSuccess = {
                                        viewModel.syncBidirectional { _, msg ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar(msg)
                                            }
                                        }
                                    },
                                    onError = { err ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Sign in error: $err")
                                        }
                                    }
                                )
                            },
                            onOpenSyncSheet = { showSyncSheet = true },
                            onOpenTaskDialog = { task ->
                                taskToEdit = task
                                taskDefaultTimeSlot = "09:00"
                                showTaskDialog = true
                            },
                            onOpenDiaryDialog = { entry ->
                                diaryToEdit = entry
                                showDiaryDialog = true
                            },
                            onOpenEveningReview = {
                                showEveningReviewDialog = true
                            }
                        )
                    }

                    LifestyleTab.MORNING -> {
                        MorningRoutineScreen(viewModel = viewModel)
                    }

                    LifestyleTab.AGENDA -> {
                        Agenda24hScreen(
                            viewModel = viewModel,
                            onOpenTaskDialog = { task, hourSlot ->
                                taskToEdit = task
                                taskDefaultTimeSlot = hourSlot
                                showTaskDialog = true
                            }
                        )
                    }

                    LifestyleTab.DIARY -> {
                        DiaryScreen(
                            viewModel = viewModel,
                            currentUser = currentUser,
                            onSignInRequest = {
                                authManager.signInWithGoogle(
                                    activity = activity,
                                    scope = scope,
                                    onSuccess = {
                                        viewModel.syncBidirectional { _, msg ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar(msg)
                                            }
                                        }
                                    },
                                    onError = { err ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Sign in error: $err")
                                        }
                                    }
                                )
                            },
                            onOpenSyncSheet = { showSyncSheet = true },
                            onOpenDiaryDialog = { entry ->
                                diaryToEdit = entry
                                showDiaryDialog = true
                            }
                        )
                    }

                    LifestyleTab.MONEY -> {
                        MoneyScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    // Task Dialog
    if (showTaskDialog) {
        TaskDialog(
            initialTask = taskToEdit,
            defaultDate = selectedDate,
            defaultTimeSlot = taskDefaultTimeSlot,
            onDismiss = {
                showTaskDialog = false
                taskToEdit = null
            },
            onSave = { savedTask ->
                viewModel.saveTask(savedTask)
                showTaskDialog = false
                taskToEdit = null
                scope.launch {
                    snackbarHostState.showSnackbar("Task saved to 24h Agenda")
                }
            }
        )
    }

    // Diary Dialog
    if (showDiaryDialog) {
        DiaryDialog(
            initialEntry = diaryToEdit,
            defaultDate = selectedDate,
            existingEntries = diaryEntries,
            onDismiss = {
                showDiaryDialog = false
                diaryToEdit = null
            },
            onSave = { savedEntry ->
                viewModel.saveDiaryEntry(savedEntry)
                showDiaryDialog = false
                diaryToEdit = null
                scope.launch {
                    snackbarHostState.showSnackbar("Diary reflection saved")
                }
            }
        )
    }

    // Evening Review Dialog
    if (showEveningReviewDialog) {
        EveningReviewDialog(
            onDismiss = { showEveningReviewDialog = false },
            onSubmit = { wins, lessons, rating, carryOver ->
                viewModel.completeEveningReview(wins, lessons, rating, carryOver)
                showEveningReviewDialog = false
                scope.launch {
                    val message = if (carryOver) {
                        "Evening review completed & pending tasks rolled to tomorrow"
                    } else {
                        "Evening review completed and saved to Diary"
                    }
                    snackbarHostState.showSnackbar(message)
                }
            }
        )
    }

    // Sync & Offline Storage Bottom Sheet
    if (showSyncSheet) {
        SyncInfoBottomSheet(
            sheetState = syncSheetState,
            totalTasks = tasks.size,
            totalEntries = diaryEntries.size,
            currentUser = currentUser,
            isSigningIn = isSigningIn,
            isSyncing = isCloudSyncing,
            lastSyncStatus = lastCloudSyncStatus,
            onSignIn = {
                authManager.signInWithGoogle(
                    activity = activity,
                    scope = scope,
                    onSuccess = {
                        viewModel.syncBidirectional { _, msg ->
                            scope.launch {
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    },
                    onError = { err ->
                        scope.launch {
                            snackbarHostState.showSnackbar("Sign in error: $err")
                        }
                    }
                )
            },
            onSignOut = {
                authManager.signOut(scope) {
                    viewModel.stopRealtimeSync()
                    scope.launch {
                        snackbarHostState.showSnackbar("Signed out. Operating in offline mode.")
                    }
                }
            },
            onSyncToCloud = {
                viewModel.syncBidirectional { _, msg ->
                    scope.launch {
                        snackbarHostState.showSnackbar(msg)
                    }
                }
            },
            onDismiss = { showSyncSheet = false },
            onResetTemplate = {
                viewModel.resetToSampleTemplate()
                scope.launch {
                    snackbarHostState.showSnackbar("Reset to balanced daily template")
                }
            }
        )
    }

    // Google Calendar Style Date & Month Picker Dialog
    if (showCalendarPicker) {
        GoogleCalendarPickerDialog(
            selectedDateStr = selectedDate,
            datesWithTasks = datesWithTasks,
            datesWithDiaryEntries = datesWithDiaryEntries,
            onSelectDate = { chosenDate ->
                viewModel.setDate(chosenDate)
            },
            onDismiss = { showCalendarPicker = false }
        )
    }
}
