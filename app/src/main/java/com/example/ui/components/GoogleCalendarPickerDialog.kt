package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleRed
import com.example.ui.theme.GoogleYellow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun GoogleCalendarPickerDialog(
    selectedDateStr: String, // "YYYY-MM-DD"
    datesWithTasks: Set<String>,
    datesWithDiaryEntries: Set<String>,
    onSelectDate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val todayStr = remember { dateFormat.format(Date()) }

    // Parse initially selected date to center the month view
    val initialCal = remember(selectedDateStr) {
        val cal = Calendar.getInstance()
        try {
            cal.time = dateFormat.parse(selectedDateStr) ?: Date()
        } catch (e: Exception) {
            cal.time = Date()
        }
        cal
    }

    var displayedYear by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var displayedMonth by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH)) } // 0..11

    // Quick filter / Tab inside the dialog: "Month Grid" or "Recorded Dates Overview"
    var pickerTab by remember { mutableIntStateOf(0) }

    val monthName = remember(displayedYear, displayedMonth) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, displayedYear)
        cal.set(Calendar.MONTH, displayedMonth)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time)
    }

    // Days in displayed month
    val daysInMonth = remember(displayedYear, displayedMonth) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, displayedYear)
        cal.set(Calendar.MONTH, displayedMonth)
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Pad with empty leading days
        val items = mutableListOf<CalendarDayItem?>()
        val leadingBlanks = firstDayOfWeek - Calendar.SUNDAY
        for (i in 0 until leadingBlanks) {
            items.add(null)
        }
        for (day in 1..maxDays) {
            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", displayedYear, displayedMonth + 1, day)
            items.add(
                CalendarDayItem(
                    dayNumber = day,
                    dateStr = dateStr,
                    isToday = dateStr == todayStr,
                    isSelected = dateStr == selectedDateStr,
                    hasTasks = datesWithTasks.contains(dateStr),
                    hasDiary = datesWithDiaryEntries.contains(dateStr)
                )
            )
        }
        items
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.96f),
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Dialog Close & Today shortcut
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(GoogleBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = GoogleBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google Calendar Style Date",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs: "Month Calendar" vs "Past & Future Records"
                TabRow(
                    selectedTabIndex = pickerTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = pickerTab == 0,
                        onClick = { pickerTab = 0 },
                        text = { Text("Full Month Grid", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = pickerTab == 1,
                        onClick = { pickerTab = 1 },
                        text = { Text("All Dates with Records", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (pickerTab == 0) {
                    // Month Navigation Header: [<] Month Year [>] and Jump Today
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (displayedMonth == 0) {
                                    displayedMonth = 11
                                    displayedYear -= 1
                                } else {
                                    displayedMonth -= 1
                                }
                            },
                            modifier = Modifier
                                .testTag("calendar_prev_month_btn")
                                .size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                        }

                        Text(
                            text = monthName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = GoogleBlue
                            )
                        )

                        IconButton(
                            onClick = {
                                if (displayedMonth == 11) {
                                    displayedMonth = 0
                                    displayedYear += 1
                                } else {
                                    displayedMonth += 1
                                }
                            },
                            modifier = Modifier
                                .testTag("calendar_next_month_btn")
                                .size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Day of Week Labels: S M T W T F S
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.width(36.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Calendar Month Grid (42 cells max)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    ) {
                        items(daysInMonth) { dayItem ->
                            if (dayItem == null) {
                                Box(modifier = Modifier.aspectRatio(1f))
                            } else {
                                CalendarDayCell(
                                    item = dayItem,
                                    onSelect = {
                                        onSelectDate(dayItem.dateStr)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }

                    // Legend: Indicators for tasks & diary
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(GoogleBlue))
                        Spacer(Modifier.width(4.dp))
                        Text("Task / Goal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(Modifier.width(12.dp))
                        Box(Modifier.size(6.dp).clip(CircleShape).background(GoogleGreen))
                        Spacer(Modifier.width(4.dp))
                        Text("Diary Reflection", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    // "All Dates with Records" View:
                    // Lists all past or future dates that have tasks, plans, or diary reflections
                    val allRecordedDates = (datesWithTasks + datesWithDiaryEntries).sortedDescending()
                    if (allRecordedDates.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No past or future dates with records found yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Dates with past study/work records or future plans:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            allRecordedDates.take(12).forEach { dateItemStr ->
                                val isSelected = dateItemStr == selectedDateStr
                                val hasTask = datesWithTasks.contains(dateItemStr)
                                val hasDiary = datesWithDiaryEntries.contains(dateItemStr)

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) GoogleBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            onSelectDate(dateItemStr)
                                            onDismiss()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.EventNote,
                                                contentDescription = null,
                                                tint = if (isSelected) GoogleBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = dateItemStr,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                color = if (isSelected) GoogleBlue else MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (hasTask) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = GoogleBlue.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "Tasks",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = GoogleBlue,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            if (hasDiary) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = GoogleGreen.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "Diary",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = GoogleGreen,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
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
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Quick Navigation Shortcuts: Today / Tomorrow / Yesterday
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = selectedDateStr == todayStr,
                        onClick = {
                            onSelectDate(todayStr)
                            onDismiss()
                        },
                        label = { Text("Jump to Today", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoogleBlue.copy(alpha = 0.15f),
                            selectedLabelColor = GoogleBlue
                        ),
                        modifier = Modifier.testTag("jump_today_chip")
                    )

                    // Quick Tomorrow button for future planning
                    val tomorrowStr = remember {
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                        dateFormat.format(cal.time)
                    }
                    FilterChip(
                        selected = selectedDateStr == tomorrowStr,
                        onClick = {
                            onSelectDate(tomorrowStr)
                            onDismiss()
                        },
                        label = { Text("Tomorrow", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoogleBlue.copy(alpha = 0.15f),
                            selectedLabelColor = GoogleBlue
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GoogleBlue),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Done")
            }
        }
    )
}

data class CalendarDayItem(
    val dayNumber: Int,
    val dateStr: String,
    val isToday: Boolean,
    val isSelected: Boolean,
    val hasTasks: Boolean,
    val hasDiary: Boolean
)

@Composable
fun CalendarDayCell(
    item: CalendarDayItem,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                when {
                    item.isSelected -> GoogleBlue
                    item.isToday -> GoogleBlue.copy(alpha = 0.15f)
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (item.isToday && !item.isSelected) 1.5.dp else 0.dp,
                color = if (item.isToday && !item.isSelected) GoogleBlue else Color.Transparent,
                shape = CircleShape
            )
            .clickable { onSelect() }
            .testTag("day_cell_${item.dateStr}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${item.dayNumber}",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (item.isSelected || item.isToday) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp,
                    color = when {
                        item.isSelected -> Color.White
                        item.isToday -> GoogleBlue
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            )

            // Dots for tasks and diary entries
            if (item.hasTasks || item.hasDiary) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(top = 1.dp)
                ) {
                    if (item.hasTasks) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (item.isSelected) Color.White else GoogleBlue)
                        )
                    }
                    if (item.hasDiary) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (item.isSelected) GoogleYellow else GoogleGreen)
                        )
                    }
                }
            }
        }
    }
}
