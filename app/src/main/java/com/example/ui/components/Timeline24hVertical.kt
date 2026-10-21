package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgendaTask
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleRed
import com.example.ui.theme.GoogleYellow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

val FIFTEEN_MINUTE_INTERVALS = listOf(0, 15, 30, 45)

/**
 * 24-Hour Vertical Timeline Component with 15-Minute Intervals.
 *
 * Provides a comprehensive 24-hour visual schedule (00:00 to 23:45)
 * divided into 15-minute tap targets. Users can tap on any 15-minute slot
 * to schedule a new task or tap an existing task to edit it.
 */
@Composable
fun Timeline24hVertical(
    tasks: List<AgendaTask>,
    onToggleTask: (AgendaTask) -> Unit,
    onEditTask: (AgendaTask) -> Unit,
    onDeleteTask: (AgendaTask) -> Unit,
    onAddIntervalTask: (timeSlot: String) -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState()
) {
    val coroutineScope = rememberCoroutineScope()

    // Determine current hour and 15m bracket
    val currentCalendar = remember { Calendar.getInstance() }
    val currentHour = remember { currentCalendar.get(Calendar.HOUR_OF_DAY) }
    val currentMinute = remember { currentCalendar.get(Calendar.MINUTE) }
    val currentBracket15 = remember { (currentMinute / 15) * 15 }

    // View mode: Full 96 15-min intervals vs Compact Hour mode
    var isDetailed15mGrid by remember { mutableStateOf(true) }

    // Scroll near current time on first composition
    LaunchedEffect(Unit) {
        val targetIndex = (currentHour * 4 + (currentBracket15 / 15)).coerceIn(0, 95)
        // Scroll to hour with some offset for visibility
        val scrollHourIndex = (currentHour * 2).coerceAtMost(48)
        lazyListState.animateScrollToItem(scrollHourIndex)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Timeline Header: Mode toggle & Jump to NOW button
        // Timeline Header: Consistent Button-Style Display Options (15-MIN, Hourly, Now)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = isDetailed15mGrid,
                onClick = { isDetailed15mGrid = true },
                label = { Text("15-MIN", fontSize = 12.sp, fontWeight = if (isDetailed15mGrid) FontWeight.Bold else FontWeight.Medium) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GoogleBlue.copy(alpha = 0.15f),
                    selectedLabelColor = GoogleBlue
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("timeline_mode_15m_grid")
            )

            FilterChip(
                selected = !isDetailed15mGrid,
                onClick = { isDetailed15mGrid = false },
                label = { Text("Hourly", fontSize = 12.sp, fontWeight = if (!isDetailed15mGrid) FontWeight.Bold else FontWeight.Medium) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GoogleBlue.copy(alpha = 0.15f),
                    selectedLabelColor = GoogleBlue
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("timeline_mode_hourly")
            )

            // Button-style quick jump to NOW (consistent with category filters)
            FilterChip(
                selected = false,
                onClick = {
                    coroutineScope.launch {
                        val targetItem = if (isDetailed15mGrid) {
                            (currentHour * 4 + (currentBracket15 / 15)).coerceIn(0, 95)
                        } else {
                            currentHour
                        }
                        lazyListState.animateScrollToItem(targetItem)
                    }
                },
                label = { Text("Now", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(GoogleRed)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = GoogleBlue.copy(alpha = 0.08f),
                    labelColor = GoogleBlue
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("timeline_jump_now_btn")
            )
        }

        // Vertical 24-Hour Timeline
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .testTag("timeline_24h_list"),
            verticalArrangement = Arrangement.spacedBy(if (isDetailed15mGrid) 4.dp else 8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (isDetailed15mGrid) {
                // RENDER ALL 96 15-MINUTE INTERVALS WITH MERGED 1-HOUR CONTINUOUS BLOCKS
                var slotIndex = 0
                while (slotIndex < 96) {
                    val hour = slotIndex / 4
                    val minute = (slotIndex % 4) * 15
                    val slotString = String.format(Locale.US, "%02d:%02d", hour, minute)

                    // Find tasks scheduled to start in this 15-minute slot
                    val tasksStartingInSlot = tasks.filter { task ->
                        normalizeTimeSlot(task.timeSlot) == slotString
                    }

                    // Check if there is a task spanning 1 full hour starting in this slot
                    val oneHourTask = tasksStartingInSlot.firstOrNull { isOneHourTask(it) }

                    if (oneHourTask != null && slotIndex + 4 <= 96) {
                        // Merge the four 15-minute slots into a single, continuous 1-hour block!
                        val coveredSlots = slotIndex until (slotIndex + 4)
                        val otherTasksInBlock = tasks.filter { task ->
                            task.id != oneHourTask.id && coveredSlots.any { idx ->
                                val h = idx / 4
                                val m = (idx % 4) * 15
                                normalizeTimeSlot(task.timeSlot) == String.format(Locale.US, "%02d:%02d", h, m)
                            }
                        }

                        val isNowInBlock = coveredSlots.any { idx ->
                            val h = idx / 4
                            val m = (idx % 4) * 15
                            h == currentHour && currentBracket15 == m
                        }

                        if (isNowInBlock) {
                            item(key = "now_line_merged_$slotIndex") {
                                NowTimelineIndicator(hour = currentHour, minute = currentMinute)
                            }
                        }

                        val endSlotString = if (oneHourTask.timeEnd.isNotBlank()) {
                            normalizeTimeSlot(oneHourTask.timeEnd)
                        } else {
                            String.format(Locale.US, "%02d:%02d", (hour + 1) % 24, minute)
                        }

                        item(key = "merged_1h_slot_${oneHourTask.id}_$slotIndex") {
                            MergedOneHourIntervalRow(
                                task = oneHourTask,
                                additionalTasks = otherTasksInBlock,
                                startHour = hour,
                                startMinute = minute,
                                startTimeString = slotString,
                                endTimeString = endSlotString,
                                isCurrentBlock = isNowInBlock,
                                onToggleTask = onToggleTask,
                                onEditTask = onEditTask,
                                onDeleteTask = onDeleteTask,
                                modifier = Modifier.testTag("timeline_merged_1h_${slotString.replace(":", "_")}")
                            )
                        }

                        slotIndex += 4
                    } else {
                        // Normal 15-minute interval slot
                        val isCurrentSlot = (hour == currentHour) && (currentBracket15 == minute)
                        if (isCurrentSlot) {
                            item(key = "now_line_${hour}_$minute") {
                                NowTimelineIndicator(hour = hour, minute = currentMinute)
                            }
                        }

                        item(key = "slot_${hour}_$minute") {
                            FifteenMinuteIntervalRow(
                                hour = hour,
                                minute = minute,
                                timeString = slotString,
                                isCurrentSlot = isCurrentSlot,
                                isHourStart = minute == 0,
                                tasks = tasksStartingInSlot,
                                onToggleTask = onToggleTask,
                                onEditTask = onEditTask,
                                onDeleteTask = onDeleteTask,
                                onSlotClick = { onAddIntervalTask(slotString) }
                            )
                        }

                        slotIndex += 1
                    }
                }
            } else {
                // HOURLY VIEW WITH EXPANDABLE 15-MINUTE SUB-SLOTS
                for (hour in 0..23) {
                    val hourString = String.format(Locale.US, "%02d:00", hour)
                    val isCurrentHour = hour == currentHour

                    val tasksForHour = tasks.filter { task ->
                        val parts = task.timeSlot.split(":")
                        parts.firstOrNull()?.toIntOrNull() == hour
                    }

                    if (isCurrentHour) {
                        item(key = "now_line_hour_$hour") {
                            NowTimelineIndicator(hour = hour, minute = currentMinute)
                        }
                    }

                    item(key = "hour_block_$hour") {
                        HourlyBlockWith15mExpansion(
                            hour = hour,
                            hourString = hourString,
                            isCurrentHour = isCurrentHour,
                            tasks = tasksForHour,
                            onToggleTask = onToggleTask,
                            onEditTask = onEditTask,
                            onDeleteTask = onDeleteTask,
                            onAddIntervalTask = onAddIntervalTask
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(88.dp))
            }
        }
    }
}

/**
 * Single 15-Minute Interval Row inside the vertical timeline.
 * Explicitly displays :00, :15, :30, or :45 with tap target to add or edit tasks.
 */
@Composable
fun FifteenMinuteIntervalRow(
    hour: Int,
    minute: Int,
    timeString: String,
    isCurrentSlot: Boolean,
    isHourStart: Boolean,
    tasks: List<AgendaTask>,
    onToggleTask: (AgendaTask) -> Unit,
    onEditTask: (AgendaTask) -> Unit,
    onDeleteTask: (AgendaTask) -> Unit,
    onSlotClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("timeline_slot_${hour}_$minute"),
        verticalAlignment = Alignment.Top
    ) {
        // Left Column: Time label & Timeline Rail node
        Column(
            modifier = Modifier.width(54.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = timeString,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isHourStart || isCurrentSlot) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = FontFamily.Monospace,
                    fontSize = if (isHourStart) 11.sp else 10.sp
                ),
                color = when {
                    isCurrentSlot -> GoogleBlue
                    isHourStart -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                }
            )

            // Timeline dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp, end = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isHourStart) 7.dp else 4.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCurrentSlot -> GoogleBlue
                                isHourStart -> GoogleBlue.copy(alpha = 0.8f)
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right Column: Interval Content (Tasks or Empty 15-minute slot)
        Column(
            modifier = Modifier
                .weight(1f)
                .background(
                    if (isCurrentSlot) GoogleBlue.copy(alpha = 0.05f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (tasks.isEmpty()) {
                // Empty 15-minute interval: Tapping opens TaskDialog prefilled with this exact interval
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isHourStart) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    },
                    border = BorderStroke(
                        0.5.dp,
                        if (isCurrentSlot) GoogleBlue.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSlotClick() }
                        .testTag("timeline_tap_add_${timeString.replace(":", "_")}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "+ $timeString (15m slot)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add task at $timeString",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            } else {
                // Task(s) scheduled for this 15-minute slot
                tasks.forEach { task ->
                    TimelineTaskCard(
                        task = task,
                        onToggle = { onToggleTask(task) },
                        onEdit = { onEditTask(task) },
                        onDelete = { onDeleteTask(task) }
                    )
                }
            }
        }
    }
}

/**
 * Hourly block with quick 15-minute expansion chips (:00, :15, :30, :45).
 */
@Composable
fun HourlyBlockWith15mExpansion(
    hour: Int,
    hourString: String,
    isCurrentHour: Boolean,
    tasks: List<AgendaTask>,
    onToggleTask: (AgendaTask) -> Unit,
    onEditTask: (AgendaTask) -> Unit,
    onDeleteTask: (AgendaTask) -> Unit,
    onAddIntervalTask: (timeSlot: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isCurrentHour) GoogleBlue.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("hour_summary_block_$hour")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Hour Header with 15m Quick Add Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = hourString,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (isCurrentHour) GoogleBlue else MaterialTheme.colorScheme.onSurface
                    )
                    if (isCurrentHour) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = GoogleBlue.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "CURRENT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoogleBlue,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // 15-minute interval quick add buttons: :00, :15, :30, :45
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FIFTEEN_MINUTE_INTERVALS.forEach { min ->
                        val slotTime = String.format(Locale.US, "%02d:%02d", hour, min)
                        val hasTask = tasks.any { normalizeTimeSlot(it.timeSlot) == slotTime }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when {
                                hasTask -> GoogleBlue.copy(alpha = 0.2f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onAddIntervalTask(slotTime) }
                                .testTag("quick_slot_${hour}_$min")
                        ) {
                            Text(
                                text = String.format(Locale.US, ":%02d", min),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (hasTask) FontWeight.Bold else FontWeight.Normal,
                                color = if (hasTask) GoogleBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (tasks.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onAddIntervalTask(hourString) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Free Hour — Tap any 15m button above to schedule",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add task",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    tasks.forEach { task ->
                        TimelineTaskCard(
                            task = task,
                            onToggle = { onToggleTask(task) },
                            onEdit = { onEditTask(task) },
                            onDelete = { onDeleteTask(task) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Task Card displayed inside a timeline slot.
 * Clicking the card opens it for editing. Checkbox toggles completion.
 */
@Composable
fun TimelineTaskCard(
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
        "work" -> GoogleBlue
        else -> GoogleBlue
    }

    val priorityColor = when (task.priority.lowercase()) {
        "high" -> GoogleRed
        "medium" -> GoogleYellow
        else -> GoogleGreen
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (task.priority.equals("high", ignoreCase = true)) GoogleRed.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onEdit() }
            .testTag("timeline_task_${task.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = GoogleGreen),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Category Badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = categoryColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = task.category,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = categoryColor
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    // Time Range
                    Text(
                        text = if (task.timeEnd.isNotBlank()) "${task.timeSlot} - ${task.timeEnd}" else task.timeSlot,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Quick actions
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit task",
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete task",
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Now Timeline Indicator with live time display.
 */
@Composable
fun NowTimelineIndicator(
    hour: Int,
    minute: Int,
    modifier: Modifier = Modifier
) {
    val timeFormatted = String.format(Locale.US, "%02d:%02d", hour, minute)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = GoogleRed,
            modifier = Modifier.padding(end = 6.dp)
        ) {
            Text(
                text = "NOW $timeFormatted",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = Color.White
                ),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(GoogleRed)
        )
    }
}

/**
 * Helper to normalize time slot string to standard "HH:mm".
 */
fun normalizeTimeSlot(time: String): String {
    val trimmed = time.trim()
    val parts = trimmed.split(":")
    if (parts.size != 2) return trimmed
    val h = parts[0].toIntOrNull() ?: return trimmed
    val m = parts[1].toIntOrNull() ?: return trimmed
    return String.format(Locale.US, "%02d:%02d", h, m)
}

/**
 * Parses "HH:mm" time string into total minutes from start of day.
 */
fun parseTimeToMinutes(time: String): Int? {
    val parts = time.trim().split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    return h * 60 + m
}

/**
 * Calculates task duration in minutes.
 */
fun getTaskDurationMinutes(task: AgendaTask): Int {
    if (task.timeEnd.isBlank()) return 0
    val startMin = parseTimeToMinutes(task.timeSlot) ?: return 0
    val endMin = parseTimeToMinutes(task.timeEnd) ?: return 0
    val diff = endMin - startMin
    return if (diff > 0) diff else 0
}

/**
 * Checks whether a task spans 1 hour (60 minutes / 4 15-minute intervals).
 */
fun isOneHourTask(task: AgendaTask): Boolean {
    val duration = getTaskDurationMinutes(task)
    return duration == 60 || duration in 55..65
}

/**
 * Merged 1-Hour Continuous Block Row inside the 15-Minute Grid.
 * Combines 4 consecutive 15-minute slots into a single, seamless, continuous block
 * to clearly represent the total 1-hour duration without fragmented slots.
 */
@Composable
fun MergedOneHourIntervalRow(
    task: AgendaTask,
    additionalTasks: List<AgendaTask>,
    startHour: Int,
    startMinute: Int,
    startTimeString: String,
    endTimeString: String,
    isCurrentBlock: Boolean,
    onToggleTask: (AgendaTask) -> Unit,
    onEditTask: (AgendaTask) -> Unit,
    onDeleteTask: (AgendaTask) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = when (task.category.lowercase()) {
        "deep work" -> GoogleBlue
        "health" -> GoogleGreen
        "personal" -> GoogleYellow
        "learning" -> Color(0xFF9C27B0)
        "work" -> GoogleBlue
        else -> GoogleBlue
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Left Column: Time span & continuous vertical rail spanning the 1-hour duration
        Column(
            modifier = Modifier
                .width(54.dp)
                .heightIn(min = 110.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Start time label
            Text(
                text = startTimeString,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                ),
                color = if (isCurrentBlock) GoogleBlue else MaterialTheme.colorScheme.onSurface
            )

            // Continuous vertical rail connecting start to end of the 1-hour block
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .width(3.dp)
                    .weight(1f)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(
                        if (isCurrentBlock) GoogleBlue else categoryColor.copy(alpha = 0.5f)
                    )
            )

            // End time label
            Text(
                text = endTimeString,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right Column: Single, continuous 1-hour block card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = categoryColor.copy(alpha = 0.08f),
            border = BorderStroke(
                1.5.dp,
                if (isCurrentBlock) GoogleBlue else categoryColor.copy(alpha = 0.35f)
            ),
            shadowElevation = 2.dp,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 110.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { onEditTask(task) }
                .testTag("timeline_merged_1h_block_${task.id}")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top row: Badges & Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Category Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = categoryColor.copy(alpha = 0.20f)
                        ) {
                            Text(
                                text = task.category,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = categoryColor
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // 1-Hour Continuous Block indicator
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "1-Hour Block",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Edit & Delete actions
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onEditTask(task) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit task",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        IconButton(
                            onClick = { onDeleteTask(task) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete task",
                                tint = GoogleRed,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Middle: Checkbox + Title + Description
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { onToggleTask(task) },
                        colors = CheckboxDefaults.colors(checkedColor = GoogleGreen),
                        modifier = Modifier
                            .size(24.dp)
                            .padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (task.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = task.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // If any additional tasks are also in this block
                if (additionalTasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        additionalTasks.forEach { extraTask ->
                            TimelineTaskCard(
                                task = extraTask,
                                onToggle = { onToggleTask(extraTask) },
                                onEdit = { onEditTask(extraTask) },
                                onDelete = { onDeleteTask(extraTask) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom: Time Range Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏱️ $startTimeString – $endTimeString (4 merged slots)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    if (task.priority.equals("high", ignoreCase = true)) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = GoogleRed.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "High Priority",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoogleRed,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
