package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgendaTask
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleRed
import com.example.ui.theme.GoogleYellow
import java.util.UUID

val AGENDA_CATEGORIES = listOf("Work", "Health", "Personal", "Deep Work", "Learning", "Mindset")
val AGENDA_PRIORITIES = listOf("High", "Medium", "Low")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskDialog(
    initialTask: AgendaTask? = null,
    defaultDate: String,
    defaultTimeSlot: String = "09:00",
    onDismiss: () -> Unit,
    onSave: (AgendaTask) -> Unit
) {
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var timeSlot by remember { mutableStateOf(initialTask?.timeSlot ?: defaultTimeSlot) }
    var timeEnd by remember { mutableStateOf(initialTask?.timeEnd ?: "") }
    var category by remember { mutableStateOf(initialTask?.category ?: "Work") }
    var priority by remember { mutableStateOf(initialTask?.priority ?: "Medium") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }

    var titleError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialTask == null) "Schedule 24h Agenda Task" else "Edit Agenda Task",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Task Title
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = false
                    },
                    label = { Text("Task Title *") },
                    placeholder = { Text("e.g. Deep Work on Core Milestone") },
                    isError = titleError,
                    supportingText = {
                        if (titleError) Text("Title is required", color = GoogleRed)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )

                // Time Block (Start & End)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = timeSlot,
                        onValueChange = { timeSlot = it },
                        label = { Text("Start Time") },
                        placeholder = { Text("09:00") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("task_start_time_input")
                    )

                    OutlinedTextField(
                        value = timeEnd,
                        onValueChange = { timeEnd = it },
                        label = { Text("End Time (Optional)") },
                        placeholder = { Text("09:45") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("task_end_time_input")
                    )
                }

                // 15-Minute Interval Quick Selectors
                Column {
                    Text(
                        text = "15-Min Interval Adjuster",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("00", "15", "30", "45").forEach { min ->
                            val currentParts = timeSlot.split(":")
                            val currentH = currentParts.firstOrNull()?.padStart(2, '0') ?: "09"
                            val targetSlot = "$currentH:$min"
                            val isSelected = currentParts.getOrNull(1) == min

                            FilterChip(
                                selected = isSelected,
                                onClick = { timeSlot = targetSlot },
                                label = { Text(":$min", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoogleBlue.copy(alpha = 0.15f),
                                    selectedLabelColor = GoogleBlue
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Quick Duration Helper (15m, 30m, 45m, 1h, 1.5h, 2h)
                Column {
                    Text(
                        text = "Duration Quick Add",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Pair("15m", 15),
                            Pair("30m", 30),
                            Pair("45m", 45),
                            Pair("1h", 60),
                            Pair("2h", 120)
                        ).forEach { (label, mins) ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val parts = timeSlot.split(":")
                                    val startH = parts.firstOrNull()?.toIntOrNull() ?: 9
                                    val startM = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                    val totalM = startH * 60 + startM + mins
                                    val endH = (totalM / 60) % 24
                                    val endM = totalM % 60
                                    timeEnd = String.format(java.util.Locale.US, "%02d:%02d", endH, endM)
                                },
                                label = { Text(label, fontSize = 10.5.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Category Chips
                Column {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AGENDA_CATEGORIES.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoogleBlue.copy(alpha = 0.15f),
                                    selectedLabelColor = GoogleBlue
                                ),
                                modifier = Modifier.testTag("task_cat_chip_$cat")
                            )
                        }
                    }
                }

                // Priority
                Column {
                    Text(
                        text = "Priority Level",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AGENDA_PRIORITIES.forEach { prio ->
                            val prioColor = when (prio) {
                                "High" -> GoogleRed
                                "Medium" -> GoogleYellow
                                else -> GoogleGreen
                            }
                            FilterChip(
                                selected = priority == prio,
                                onClick = { priority = prio },
                                label = { Text(prio) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = prioColor.copy(alpha = 0.18f),
                                    selectedLabelColor = prioColor
                                )
                            )
                        }
                    }
                }

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Notes / Context") },
                    placeholder = { Text("Key objectives, meeting link, or reminders...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_desc_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                        return@Button
                    }
                    val nowIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date())
                    val task = AgendaTask(
                        id = initialTask?.id ?: "task_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
                        date = initialTask?.date ?: defaultDate,
                        timeSlot = timeSlot.ifBlank { "09:00" },
                        timeEnd = timeEnd.trim(),
                        title = title.trim(),
                        description = description.trim(),
                        category = category,
                        status = initialTask?.status ?: "pending",
                        priority = priority,
                        orderIndex = initialTask?.orderIndex ?: 0,
                        createdAt = initialTask?.createdAt?.ifBlank { nowIso } ?: nowIso,
                        updatedAt = nowIso
                    )
                    onSave(task)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoogleBlue),
                modifier = Modifier.testTag("save_task_btn")
            ) {
                Text("Save Task")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_task_btn")
            ) {
                Text("Cancel")
            }
        }
    )
}
