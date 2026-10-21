package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AgendaTask
import com.example.data.model.DiaryEntry
import com.example.data.model.MorningPlan
import com.example.ui.components.NowIndicator
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleRed
import com.example.ui.theme.GoogleYellow
import com.example.ui.viewmodel.LifestyleTab
import com.example.ui.viewmodel.LifestyleViewModel
import com.google.firebase.auth.FirebaseUser

@Composable
fun DualViewScreen(
    viewModel: LifestyleViewModel,
    currentUser: FirebaseUser? = null,
    onSignInRequest: (() -> Unit)? = null,
    onOpenSyncSheet: (() -> Unit)? = null,
    onOpenTaskDialog: (AgendaTask?) -> Unit,
    onOpenDiaryDialog: (DiaryEntry?) -> Unit,
    onOpenEveningReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val diaryEntries by viewModel.diaryEntries.collectAsStateWithLifecycle()
    val morningPlan by viewModel.morningPlan.collectAsStateWithLifecycle()
    val progress by viewModel.dailyProgress.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isCloudSyncing.collectAsStateWithLifecycle()

    val completedCount = progress.first
    val totalCount = progress.second
    val progressFraction = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f
    val prayerRecord by viewModel.prayerRecord.collectAsStateWithLifecycle()
    val cardOrder by viewModel.dashboardCardOrder.collectAsStateWithLifecycle()
    val isRadioPlaying by com.example.util.QuranRadioManager.isPlaying.collectAsStateWithLifecycle()
    val activeRadioStream by com.example.util.QuranRadioManager.activeStream.collectAsStateWithLifecycle()

    var showQuickAddDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        item {
            Spacer(modifier = Modifier.height(2.dp))
        }

        // Daily Progress & Status Summary Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Daily Execution Progress",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$completedCount of $totalCount agenda deliverables completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (cardOrder != listOf("countdown", "zikr", "hydration", "namaz")) {
                                TextButton(
                                    onClick = { viewModel.resetDashboardCardOrder() },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    modifier = Modifier
                                        .height(28.dp)
                                        .testTag("reset_card_order_btn")
                                ) {
                                    Text("Reset Order", fontSize = 11.sp, color = GoogleBlue)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = GoogleBlue.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "${(progressFraction * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = GoogleBlue
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = GoogleBlue,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dynamic reordered stack with button-based shifting
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        cardOrder.forEachIndexed { index, cardId ->
                            val canMoveUp = index > 0
                            val canMoveDown = index < cardOrder.size - 1
                            when (cardId) {
                                "countdown" -> CountdownProgressCard(
                                    isMorningDone = morningPlan?.completed == true,
                                    canMoveUp = canMoveUp,
                                    canMoveDown = canMoveDown,
                                    onMoveUp = { viewModel.moveDashboardCardUp("countdown") },
                                    onMoveDown = { viewModel.moveDashboardCardDown("countdown") },
                                    onClick = { viewModel.navigateToRoutineSection("countdown") }
                                )
                                "zikr" -> ZikrProgressCard(
                                    prayerRecord = prayerRecord,
                                    canMoveUp = canMoveUp,
                                    canMoveDown = canMoveDown,
                                    onMoveUp = { viewModel.moveDashboardCardUp("zikr") },
                                    onMoveDown = { viewModel.moveDashboardCardDown("zikr") },
                                    onClick = { viewModel.navigateToRoutineSection("zikr") }
                                )
                                "hydration" -> HydrationProgressCard(
                                    waterGlasses = morningPlan?.waterGlasses ?: 0,
                                    canMoveUp = canMoveUp,
                                    canMoveDown = canMoveDown,
                                    onMoveUp = { viewModel.moveDashboardCardUp("hydration") },
                                    onMoveDown = { viewModel.moveDashboardCardDown("hydration") },
                                    onClick = { viewModel.navigateToRoutineSection("hydration") }
                                )
                                "namaz" -> {
                                    NamazProgressCard(
                                        prayerRecord = prayerRecord,
                                        canMoveUp = canMoveUp,
                                        canMoveDown = canMoveDown,
                                        onMoveUp = { viewModel.moveDashboardCardUp("namaz") },
                                        onMoveDown = { viewModel.moveDashboardCardDown("namaz") },
                                        onClick = { viewModel.navigateToRoutineSection("namaz") }
                                    )
                                    // New navigation button labeled "Quran Live" directly below "0/5 Prayers" (Namaz Status) card
                                    QuranLiveNavigationCard(
                                        isPlaying = isRadioPlaying,
                                        streamTitle = activeRadioStream.title,
                                        onClick = { viewModel.navigateToRoutineSection("quran_live") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Evening Review Banner
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = GoogleBlue.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoogleBlue.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GoogleBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Nightlight,
                                contentDescription = "Evening Review",
                                tint = GoogleBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Evening Retrospective",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Capture daily wins & roll pending tasks",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onOpenEveningReview,
                        colors = ButtonDefaults.buttonColors(containerColor = GoogleBlue),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("launch_evening_review_btn")
                    ) {
                        Text("Review", fontSize = 12.sp)
                    }
                }
            }
        }

        // --- Section 1: 24h Agenda Timeline Preview ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📅 24-Hour Agenda",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "${tasks.size}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.setTab(LifestyleTab.AGENDA) },
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Full Day →", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { onOpenTaskDialog(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = GoogleBlue),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("dual_add_task_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", fontSize = 11.sp)
                    }
                }
            }
        }

        // Live "NOW" indicator in Agenda
        item {
            NowIndicator()
        }

        if (tasks.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No scheduled tasks yet for this day",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onOpenTaskDialog(null) },
                                colors = ButtonDefaults.buttonColors(containerColor = GoogleBlue),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Schedule Task / Goal")
                            }
                            OutlinedButton(
                                onClick = { viewModel.resetToSampleTemplate() },
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Text("Day Template")
                            }
                        }
                    }
                }
            }
        } else {
            items(tasks.take(6), key = { it.id }) { task ->
                DualTaskCard(
                    task = task,
                    onToggle = { viewModel.toggleTask(task) },
                    onEdit = { onOpenTaskDialog(task) },
                    onDelete = { viewModel.deleteTask(task) }
                )
            }
            if (tasks.size > 6) {
                item {
                    OutlinedButton(
                        onClick = { viewModel.setTab(LifestyleTab.AGENDA) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("View all ${tasks.size} tasks in 24h Timeline →")
                    }
                }
            }
        }

        // --- Section 2: Diary Reflections (Read-Only Preview) ---
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📝 Diary Reflections",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (diaryEntries.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${diaryEntries.size}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        if (diaryEntries.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No diary reflections logged today yet.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Entries recorded in the Diary tab will appear here as a complete read-only summary.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(diaryEntries, key = { it.id }) { entry ->
                DualDiaryCard(entry = entry)
            }
        }

        item {
            Spacer(modifier = Modifier.height(76.dp))
        }
    }

    // Floating Action Button on 'View' tab for quick task/note creation without full editor
    FloatingActionButton(
        onClick = { showQuickAddDialog = true },
        containerColor = GoogleBlue,
        contentColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(20.dp)
            .testTag("view_quick_add_fab")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Quick Add Task or Note to 24h Schedule",
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Quick Task",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }

    if (showQuickAddDialog) {
        QuickAddScheduleDialog(
            selectedDate = selectedDate,
            onDismiss = { showQuickAddDialog = false },
            onSaveTask = { task ->
                viewModel.saveTask(task)
                showQuickAddDialog = false
            }
        )
    }
}
}

@Composable
fun DualTaskCard(
    task: AgendaTask,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = when (task.category.lowercase()) {
        "deep work" -> GoogleBlue
        "health" -> GoogleGreen
        "personal" -> GoogleYellow
        "learning" -> Color(0xFF9C27B0)
        else -> GoogleBlue
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = GoogleGreen),
                modifier = Modifier.testTag("task_checkbox_${task.id}")
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Time & Content
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (task.timeEnd.isNotBlank()) "${task.timeSlot} - ${task.timeEnd}" else task.timeSlot,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = categoryColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = task.category,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = categoryColor,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Edit & Delete actions
            Row {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Task",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Task",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DualDiaryCard(
    entry: DiaryEntry,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("diary_card_${entry.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GoogleBlue.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = entry.mood,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = GoogleBlue
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = entry.time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Energy bolts indicator (read-only)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Energy",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    for (i in 1..entry.energy.coerceIn(1, 5)) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = GoogleYellow,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            if (entry.title.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Full content with complete details and comfortable line height
            Text(
                text = entry.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )

            // Gratitude Notes if present
            if (entry.gratitudeNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GoogleGreen.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, GoogleGreen.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "🌱",
                            fontSize = 13.sp,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Column {
                            Text(
                                text = "Gratitude Notes",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = GoogleGreen
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = entry.gratitudeNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // Tags if present
            if (entry.tags.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    entry.tags.split(",", " ").filter { it.isNotBlank() }.forEach { tag ->
                        val cleanTag = if (tag.startsWith("#")) tag else "#$tag"
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = cleanTag,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = GoogleBlue,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardReorderButtons(
    cardName: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "Hold to drag and reorder $cardName",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.size(18.dp)
        )
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = onMoveUp,
                enabled = canMoveUp,
                modifier = Modifier
                    .size(26.dp)
                    .testTag("reorder_up_${cardName.lowercase()}")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Move $cardName up",
                    tint = if (canMoveUp) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = onMoveDown,
                enabled = canMoveDown,
                modifier = Modifier
                    .size(26.dp)
                    .testTag("reorder_down_${cardName.lowercase()}")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Move $cardName down",
                    tint = if (canMoveDown) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CountdownProgressCard(
    isMorningDone: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragDistanceY by remember { mutableFloatStateOf(0f) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isMorningDone) GoogleGreen.copy(alpha = 0.08f) else GoogleYellow.copy(alpha = 0.10f),
        border = BorderStroke(
            if (isDragging) 2.dp else 1.dp,
            if (isDragging) GoogleBlue else if (isMorningDone) GoogleGreen.copy(alpha = 0.25f) else GoogleYellow.copy(alpha = 0.35f)
        ),
        shadowElevation = if (isDragging) 6.dp else 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .graphicsLayer {
                translationY = dragDistanceY.coerceIn(-60f, 60f)
                scaleX = if (isDragging) 1.02f else 1f
                scaleY = if (isDragging) 1.02f else 1f
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        isDragging = true
                        dragDistanceY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragDistanceY += dragAmount.y
                        if (dragDistanceY > 50f && canMoveDown) {
                            onMoveDown()
                            dragDistanceY = 0f
                        } else if (dragDistanceY < -50f && canMoveUp) {
                            onMoveUp()
                            dragDistanceY = 0f
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        dragDistanceY = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        dragDistanceY = 0f
                    }
                )
            }
            .clickable { onClick() }
            .testTag("progress_item_countdown")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onClick() }
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isMorningDone) GoogleGreen.copy(alpha = 0.15f) else GoogleYellow.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Countdown",
                        tint = if (isMorningDone) GoogleGreen else Color(0xFFC07000),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Countdown",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "15-Minute Focus Countdown in Routine",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isMorningDone) GoogleGreen.copy(alpha = 0.15f) else GoogleYellow.copy(alpha = 0.20f),
                    modifier = Modifier
                        .wrapContentWidth()
                        .widthIn(min = 72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onClick() }
                ) {
                    Text(
                        text = if (isMorningDone) "Completed ✓" else "15m Focus →",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isMorningDone) GoogleGreen else Color(0xFFB06000)
                        ),
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }

                CardReorderButtons(
                    cardName = "Countdown",
                    canMoveUp = canMoveUp,
                    canMoveDown = canMoveDown,
                    onMoveUp = onMoveUp,
                    onMoveDown = onMoveDown
                )
            }
        }
    }
}

@Composable
private fun ZikrProgressCard(
    prayerRecord: com.example.data.model.DailyPrayerRecord?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val morningZikrDone = prayerRecord?.morningAdhkarDone == true
    val eveningZikrDone = prayerRecord?.eveningAdhkarDone == true
    val zikrCompletedCount = listOf(morningZikrDone, eveningZikrDone).count { it }

    var isDragging by remember { mutableStateOf(false) }
    var dragDistanceY by remember { mutableFloatStateOf(0f) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFE8F5E9),
        border = BorderStroke(
            if (isDragging) 2.dp else 1.dp,
            if (isDragging) GoogleBlue else Color(0xFF81C784).copy(alpha = 0.4f)
        ),
        shadowElevation = if (isDragging) 6.dp else 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .graphicsLayer {
                translationY = dragDistanceY.coerceIn(-60f, 60f)
                scaleX = if (isDragging) 1.02f else 1f
                scaleY = if (isDragging) 1.02f else 1f
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        isDragging = true
                        dragDistanceY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragDistanceY += dragAmount.y
                        if (dragDistanceY > 50f && canMoveDown) {
                            onMoveDown()
                            dragDistanceY = 0f
                        } else if (dragDistanceY < -50f && canMoveUp) {
                            onMoveUp()
                            dragDistanceY = 0f
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        dragDistanceY = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        dragDistanceY = 0f
                    }
                )
            }
            .clickable { onClick() }
            .testTag("progress_item_zikr")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onClick() }
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF81C784).copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = "Zikr & Remembrance",
                        tint = GoogleGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Zikr",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Zikr & Remembrance (الأذكار) in Routine",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (zikrCompletedCount > 0) GoogleGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .wrapContentWidth()
                        .widthIn(min = 72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onClick() }
                ) {
                    Text(
                        text = if (zikrCompletedCount == 2) "All Done ✓" else "$zikrCompletedCount/2 Adhkar →",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (zikrCompletedCount > 0) GoogleGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }

                CardReorderButtons(
                    cardName = "Zikr",
                    canMoveUp = canMoveUp,
                    canMoveDown = canMoveDown,
                    onMoveUp = onMoveUp,
                    onMoveDown = onMoveDown
                )
            }
        }
    }
}

@Composable
private fun HydrationProgressCard(
    waterGlasses: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragDistanceY by remember { mutableFloatStateOf(0f) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = GoogleBlue.copy(alpha = 0.08f),
        border = BorderStroke(
            if (isDragging) 2.dp else 1.dp,
            if (isDragging) GoogleBlue else GoogleBlue.copy(alpha = 0.25f)
        ),
        shadowElevation = if (isDragging) 6.dp else 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .graphicsLayer {
                translationY = dragDistanceY.coerceIn(-60f, 60f)
                scaleX = if (isDragging) 1.02f else 1f
                scaleY = if (isDragging) 1.02f else 1f
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        isDragging = true
                        dragDistanceY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragDistanceY += dragAmount.y
                        if (dragDistanceY > 50f && canMoveDown) {
                            onMoveDown()
                            dragDistanceY = 0f
                        } else if (dragDistanceY < -50f && canMoveUp) {
                            onMoveUp()
                            dragDistanceY = 0f
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        dragDistanceY = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        dragDistanceY = 0f
                    }
                )
            }
            .clickable { onClick() }
            .testTag("progress_item_hydration")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onClick() }
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GoogleBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = "Daily Hydration Tracker",
                        tint = GoogleBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$waterGlasses/8 Glasses",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Daily Hydration Tracker in Routine",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (waterGlasses >= 8) GoogleBlue.copy(alpha = 0.20f) else GoogleBlue.copy(alpha = 0.12f),
                    modifier = Modifier
                        .wrapContentWidth()
                        .widthIn(min = 72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onClick() }
                ) {
                    Text(
                        text = if (waterGlasses >= 8) "Target Met ✓" else "Hydration →",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = GoogleBlue
                        ),
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }

                CardReorderButtons(
                    cardName = "Hydration",
                    canMoveUp = canMoveUp,
                    canMoveDown = canMoveDown,
                    onMoveUp = onMoveUp,
                    onMoveDown = onMoveDown
                )
            }
        }
    }
}

@Composable
private fun NamazProgressCard(
    prayerRecord: com.example.data.model.DailyPrayerRecord?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prayersDone = listOf(
        prayerRecord?.fajrCompleted,
        prayerRecord?.dhuhrCompleted,
        prayerRecord?.asrCompleted,
        prayerRecord?.maghribCompleted,
        prayerRecord?.ishaCompleted
    ).count { it == true }
    val isAllPrayersDone = prayersDone == 5

    var isDragging by remember { mutableStateOf(false) }
    var dragDistanceY by remember { mutableFloatStateOf(0f) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isAllPrayersDone) GoogleGreen.copy(alpha = 0.10f) else Color(0xFF0F9D58).copy(alpha = 0.08f),
        border = BorderStroke(
            if (isDragging) 2.dp else 1.dp,
            if (isDragging) GoogleBlue else if (isAllPrayersDone) GoogleGreen.copy(alpha = 0.35f) else Color(0xFF0F9D58).copy(alpha = 0.25f)
        ),
        shadowElevation = if (isDragging) 6.dp else 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .graphicsLayer {
                translationY = dragDistanceY.coerceIn(-60f, 60f)
                scaleX = if (isDragging) 1.02f else 1f
                scaleY = if (isDragging) 1.02f else 1f
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        isDragging = true
                        dragDistanceY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragDistanceY += dragAmount.y
                        if (dragDistanceY > 50f && canMoveDown) {
                            onMoveDown()
                            dragDistanceY = 0f
                        } else if (dragDistanceY < -50f && canMoveUp) {
                            onMoveUp()
                            dragDistanceY = 0f
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        dragDistanceY = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        dragDistanceY = 0f
                    }
                )
            }
            .clickable { onClick() }
            .testTag("progress_item_namaz")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onClick() }
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isAllPrayersDone) GoogleGreen.copy(alpha = 0.18f) else Color(0xFF0F9D58).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🕌",
                        fontSize = 16.sp
                    )
                }
                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$prayersDone/5 Prayers",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Namaz Status in Routine",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isAllPrayersDone) GoogleGreen.copy(alpha = 0.20f) else Color(0xFF0F9D58).copy(alpha = 0.15f),
                    modifier = Modifier
                        .wrapContentWidth()
                        .widthIn(min = 72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onClick() }
                ) {
                    Text(
                        text = if (isAllPrayersDone) "5/5 Prayed ✓" else "Namaz Status →",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isAllPrayersDone) GoogleGreen else Color(0xFF0F9D58)
                        ),
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }

                CardReorderButtons(
                    cardName = "Namaz",
                    canMoveUp = canMoveUp,
                    canMoveDown = canMoveDown,
                    onMoveUp = onMoveUp,
                    onMoveDown = onMoveDown
                )
            }
        }
    }
}

/**
 * Dedicated navigation button labeled "Quran Live" directly below "0/5 Prayers" (Namaz Status) card.
 * Styled to match existing progress items with live playback status and direct navigation to Routine radio section.
 */
@Composable
private fun QuranLiveNavigationCard(
    isPlaying: Boolean,
    streamTitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isPlaying) Color(0xFF0F9D58).copy(alpha = 0.12f) else Color(0xFF0F9D58).copy(alpha = 0.08f),
        border = BorderStroke(
            1.dp,
            if (isPlaying) Color(0xFF0F9D58).copy(alpha = 0.45f) else Color(0xFF0F9D58).copy(alpha = 0.25f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("progress_item_quran_live")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onClick() }
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) Color(0xFF0F9D58).copy(alpha = 0.22f) else Color(0xFF0F9D58).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📻",
                        fontSize = 16.sp
                    )
                }

                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Quran Live",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isPlaying) "Playing live $streamTitle" else "24/7 Live Radio in Routine",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isPlaying) Color(0xFF0F9D58).copy(alpha = 0.20f) else Color(0xFF0F9D58).copy(alpha = 0.15f),
                modifier = Modifier
                    .wrapContentWidth()
                    .widthIn(min = 72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onClick() }
            ) {
                Text(
                    text = if (isPlaying) "Playing ● LIVE" else "Quran Live →",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F9D58)
                    ),
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickAddScheduleDialog(
    selectedDate: String,
    onDismiss: () -> Unit,
    onSaveTask: (AgendaTask) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Personal") }
    var isOneHour by remember { mutableStateOf(false) }

    // Compute smart 15-minute slot recommendations based on current time
    val calendar = remember { java.util.Calendar.getInstance() }
    val currentHour = remember { calendar.get(java.util.Calendar.HOUR_OF_DAY) }
    val currentMinute = remember { calendar.get(java.util.Calendar.MINUTE) }
    val roundedMinute = remember { (currentMinute / 15) * 15 }

    fun formatSlot(h: Int, m: Int): String {
        val normalizedH = (h % 24 + 24) % 24
        return String.format(java.util.Locale.US, "%02d:%02d", normalizedH, m)
    }

    val slotNow = remember { formatSlot(currentHour, roundedMinute) }
    val slotPlus15 = remember {
        val totalM = roundedMinute + 15
        formatSlot(currentHour + totalM / 60, totalM % 60)
    }
    val slotPlus30 = remember {
        val totalM = roundedMinute + 30
        formatSlot(currentHour + totalM / 60, totalM % 60)
    }
    val slotPlus60 = remember {
        formatSlot(currentHour + 1, roundedMinute)
    }

    var selectedSlot by remember { mutableStateOf(slotNow) }
    val quickSlots = remember { listOf(slotNow, slotPlus15, slotPlus30, slotPlus60) }
    val categories = remember { listOf("Personal", "Work", "Health", "Deep Work", "Learning") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GoogleBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = GoogleBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "Quick Task / Note",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Instantly schedule a task or note for $selectedDate without opening the full editor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Task Title / Note Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task / Note Title") },
                    placeholder = { Text("e.g. Call dentist, quick review, walk...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_add_title_input")
                )

                // Quick 15-Minute Slot Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Time Slot (15-Minute Interval):",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickSlots.forEach { slot ->
                            val isSelected = selectedSlot == slot
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) GoogleBlue else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedSlot = slot }
                                    .testTag("quick_slot_$slot")
                            ) {
                                Text(
                                    text = slot,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Duration Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Span Full 1 Hour:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Checkbox(
                        checked = isOneHour,
                        onCheckedChange = { isOneHour = it },
                        colors = CheckboxDefaults.colors(checkedColor = GoogleBlue),
                        modifier = Modifier.testTag("quick_add_1h_checkbox")
                    )
                }

                // Category Chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Category:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoogleBlue.copy(alpha = 0.15f),
                                    selectedLabelColor = GoogleBlue
                                ),
                                modifier = Modifier.testTag("quick_cat_$cat")
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = title.trim()
                    if (trimmed.isBlank()) {
                        android.widget.Toast.makeText(context, "Please enter a task title", android.widget.Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val nowIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date())
                    val endSlot = if (isOneHour) {
                        val parts = selectedSlot.split(":")
                        val h = parts.getOrNull(0)?.toIntOrNull() ?: 12
                        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        formatSlot(h + 1, m)
                    } else ""

                    val task = AgendaTask(
                        id = "task_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(4)}",
                        date = selectedDate,
                        timeSlot = selectedSlot,
                        timeEnd = endSlot,
                        title = trimmed,
                        description = "Quick task created from View dashboard",
                        category = selectedCategory,
                        status = "pending",
                        priority = "Medium",
                        orderIndex = 0,
                        createdAt = nowIso,
                        updatedAt = nowIso
                    )
                    onSaveTask(task)
                    android.widget.Toast.makeText(context, "✓ Quick task scheduled at $selectedSlot!", android.widget.Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoogleBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("quick_add_confirm_btn")
            ) {
                Text("Add to 24h", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("quick_add_cancel_btn")
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

