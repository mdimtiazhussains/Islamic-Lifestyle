package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleYellow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class WeekDayData(
    val dayOfWeekLetter: String, // "M", "T", "W"...
    val dayOfMonth: Int,
    val dateStr: String, // "YYYY-MM-DD"
    val isToday: Boolean,
    val isSelected: Boolean,
    val hasTasks: Boolean,
    val hasDiary: Boolean
)

@Composable
fun WeeklyCalendarStrip(
    selectedDateStr: String,
    datesWithTasks: Set<String>,
    datesWithDiaryEntries: Set<String>,
    onSelectDate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val todayStr = remember { dateFormat.format(Date()) }

    // Calculate 7 days for the week containing selectedDateStr
    val weekDays = remember(selectedDateStr, datesWithTasks, datesWithDiaryEntries) {
        val cal = Calendar.getInstance(Locale.US)
        cal.firstDayOfWeek = Calendar.SUNDAY
        try {
            cal.time = dateFormat.parse(selectedDateStr) ?: Date()
        } catch (e: Exception) {
            cal.time = Date()
        }

        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // Accurately align to the Sunday of the current week
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysSinceSunday = (dayOfWeek - Calendar.SUNDAY + 7) % 7
        cal.add(Calendar.DAY_OF_MONTH, -daysSinceSunday)

        val days = mutableListOf<WeekDayData>()
        val dayLetters = listOf("S", "M", "T", "W", "T", "F", "S")

        for (i in 0..6) {
            val dStr = dateFormat.format(cal.time)
            days.add(
                WeekDayData(
                    dayOfWeekLetter = dayLetters[i],
                    dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
                    dateStr = dStr,
                    isToday = dStr == todayStr,
                    isSelected = dStr == selectedDateStr,
                    hasTasks = datesWithTasks.contains(dStr),
                    hasDiary = datesWithDiaryEntries.contains(dStr)
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        days
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Week Jump button
            IconButton(
                onClick = {
                    val cal = Calendar.getInstance()
                    try {
                        cal.time = dateFormat.parse(selectedDateStr) ?: Date()
                        cal.add(Calendar.DAY_OF_YEAR, -7)
                        onSelectDate(dateFormat.format(cal.time))
                    } catch (e: Exception) {
                        // ignore
                    }
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Week",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }

            // 7 Days
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                weekDays.forEach { day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (day.isSelected) GoogleBlue else Color.Transparent
                            )
                            .border(
                                width = if (day.isToday && !day.isSelected) 1.5.dp else 0.dp,
                                color = if (day.isToday && !day.isSelected) GoogleBlue else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelectDate(day.dateStr) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("weekly_day_${day.dateStr}")
                    ) {
                        Text(
                            text = day.dayOfWeekLetter,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (day.isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "${day.dayOfMonth}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (day.isSelected || day.isToday) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (day.isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        )

                        // Dots
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.height(4.dp)
                        ) {
                            if (day.hasTasks) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(if (day.isSelected) Color.White else GoogleBlue)
                                )
                            }
                            if (day.hasDiary) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(if (day.isSelected) GoogleYellow else GoogleGreen)
                                )
                            }
                        }
                    }
                }
            }

            // Next Week Jump button
            IconButton(
                onClick = {
                    val cal = Calendar.getInstance()
                    try {
                        cal.time = dateFormat.parse(selectedDateStr) ?: Date()
                        cal.add(Calendar.DAY_OF_YEAR, 7)
                        onSelectDate(dateFormat.format(cal.time))
                    } catch (e: Exception) {
                        // ignore
                    }
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Week",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
