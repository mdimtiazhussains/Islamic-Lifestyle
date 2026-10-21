package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.DiaryEntry
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleRed
import com.example.ui.theme.GoogleYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

val DIARY_MOODS = listOf(
    Pair("Inspired", "✨"),
    Pair("Grateful", "🙏"),
    Pair("Calm", "🌊"),
    Pair("Reflective", "💭"),
    Pair("Energized", "⚡"),
    Pair("Focused", "🎯"),
    Pair("Tired", "🌙")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiaryDialog(
    initialEntry: DiaryEntry? = null,
    defaultDate: String,
    existingEntries: List<DiaryEntry> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (DiaryEntry) -> Unit
) {
    val currentTime = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    var title by remember { mutableStateOf(initialEntry?.title ?: "") }
    var content by remember { mutableStateOf(initialEntry?.content ?: "") }
    var time by remember { mutableStateOf(initialEntry?.time ?: currentTime) }
    var mood by remember { mutableStateOf(initialEntry?.mood ?: "Calm") }
    var energy by remember { mutableIntStateOf(initialEntry?.energy ?: 3) }
    var tags by remember { mutableStateOf(initialEntry?.tags ?: "") }
    var gratitudeNotes by remember { mutableStateOf(initialEntry?.gratitudeNotes ?: "") }

    var contentError by remember { mutableStateOf(false) }
    var forceNewEntry by remember { mutableStateOf(false) }

    // Detect if an existing entry on this date has matching subject/title or content
    val matchingEntry = remember(title, content, existingEntries, initialEntry) {
        if (initialEntry != null) {
            initialEntry
        } else {
            val trimmedTitle = title.trim()
            val trimmedContent = content.trim()
            existingEntries.firstOrNull { existing ->
                val isGeneric = trimmedTitle.equals("Daily Mindset Reflection", ignoreCase = true) ||
                        trimmedTitle.equals("Mindset Reflection", ignoreCase = true) ||
                        trimmedTitle.equals("Daily Reflection", ignoreCase = true) ||
                        trimmedTitle.equals("Voice Reflection", ignoreCase = true)
                val titleMatches = trimmedTitle.isNotBlank() && !isGeneric &&
                        existing.title.trim().equals(trimmedTitle, ignoreCase = true)
                val contentMatches = trimmedContent.isNotBlank() &&
                        (existing.content.trim().equals(trimmedContent, ignoreCase = true) ||
                                (trimmedContent.length >= 25 && existing.content.trim().contains(trimmedContent.take(35))))
                titleMatches || contentMatches
            }
        }
    }

    val isOverwriting = !forceNewEntry && matchingEntry != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isOverwriting) "Update Reflection" else "Write Diary Reflection",
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
                // Clear distinction / option to create new vs update existing entry
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Action:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = forceNewEntry || (!isOverwriting && initialEntry == null),
                                onClick = { forceNewEntry = true },
                                label = { Text("➕ New", fontSize = 11.sp) },
                                modifier = Modifier.testTag("dialog_mode_new_chip")
                            )
                            FilterChip(
                                selected = isOverwriting,
                                onClick = {
                                    forceNewEntry = false
                                    if (matchingEntry != null) {
                                        title = matchingEntry.title
                                        content = matchingEntry.content
                                        gratitudeNotes = matchingEntry.gratitudeNotes
                                        tags = matchingEntry.tags
                                        mood = matchingEntry.mood
                                        energy = matchingEntry.energy
                                    }
                                },
                                label = { Text("✏️ Update", fontSize = 11.sp) },
                                modifier = Modifier.testTag("dialog_mode_update_chip")
                            )
                        }
                    }
                }

                // Overwriting alert banner
                if (isOverwriting) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GoogleBlue.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, GoogleBlue.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth().testTag("dialog_duplicate_notice")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = GoogleBlue, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Overwriting: \"${matchingEntry?.title}\"",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = GoogleBlue)
                                )
                                Text(
                                    text = "Same subject/content detected. Saving will overwrite previous data without duplicating.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = { forceNewEntry = true },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text("New Card", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Voice recording & transcription card
                VoiceJournalRecordingCard(
                    onTranscriptionComplete = { result ->
                        if (result.title.isNotBlank()) {
                            title = result.title
                        }
                        if (result.mindsetReflections.isNotBlank()) {
                            content = result.mindsetReflections
                            contentError = false
                        }
                        if (result.gratitudeHighlights.isNotBlank()) {
                            gratitudeNotes = result.gratitudeHighlights
                        }
                        forceNewEntry = false
                    }
                )

                // Title and Time Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title (Optional)") },
                        placeholder = { Text("e.g. Afternoon Breakthrough") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("diary_title_input")
                    )

                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Time") },
                        singleLine = true,
                        modifier = Modifier
                            .width(90.dp)
                            .testTag("diary_time_input")
                    )
                }

                // Reflection Content
                OutlinedTextField(
                    value = content,
                    onValueChange = {
                        content = it
                        if (it.isNotBlank()) contentError = false
                    },
                    label = { Text("Thoughts & Reflections *") },
                    placeholder = { Text("What insights, challenges, or mindful observations arose?") },
                    minLines = 4,
                    maxLines = 8,
                    isError = contentError,
                    supportingText = {
                        if (contentError) Text("Content is required", color = GoogleRed)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("diary_content_input")
                )

                // Mood Selection
                Column {
                    Text(
                        text = "Current Mood",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DIARY_MOODS.forEach { (m, emoji) ->
                            FilterChip(
                                selected = mood == m,
                                onClick = { mood = m },
                                label = { Text("$emoji $m") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoogleBlue.copy(alpha = 0.15f),
                                    selectedLabelColor = GoogleBlue
                                ),
                                modifier = Modifier.testTag("diary_mood_$m")
                            )
                        }
                    }
                }

                // Energy Rating (1 to 5)
                Column {
                    Text(
                        text = "Energy Rating ($energy / 5)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..5) {
                            val isActive = i <= energy
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isActive) GoogleYellow.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { energy = i }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = "Energy $i",
                                        tint = if (isActive) GoogleYellow else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Gratitude bullets
                OutlinedTextField(
                    value = gratitudeNotes,
                    onValueChange = { gratitudeNotes = it },
                    label = { Text("Gratitude & Highlights (Optional)") },
                    placeholder = { Text("People, small moments, or breakthroughs...") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // Tags
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags") },
                    placeholder = { Text("#mindset, #breakthrough, #calm") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isOverwriting) {
                    OutlinedButton(
                        onClick = {
                            if (content.isBlank() && title.isBlank()) {
                                contentError = true
                                return@OutlinedButton
                            }
                            val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                            val entry = DiaryEntry(
                                id = "diary_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
                                date = defaultDate,
                                time = time.ifBlank { currentTime },
                                title = title.ifBlank { "Mindset Reflection" },
                                content = content.trim(),
                                mood = mood,
                                energy = energy,
                                tags = tags.trim(),
                                gratitudeNotes = gratitudeNotes.trim(),
                                createdAt = nowIso,
                                updatedAt = nowIso
                            )
                            onSave(entry)
                        },
                        modifier = Modifier.testTag("save_as_new_card_dialog_btn")
                    ) {
                        Text("Save as New", fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = {
                        if (content.isBlank() && title.isBlank()) {
                            contentError = true
                            return@Button
                        }
                        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                        val entryId = if (isOverwriting) {
                            matchingEntry!!.id
                        } else {
                            initialEntry?.id ?: "diary_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
                        }
                        val originalCreatedAt = if (isOverwriting) {
                            matchingEntry!!.createdAt
                        } else {
                            initialEntry?.createdAt?.ifBlank { nowIso } ?: nowIso
                        }

                        val entry = DiaryEntry(
                            id = entryId,
                            date = if (isOverwriting) matchingEntry!!.date else (initialEntry?.date ?: defaultDate),
                            time = time.ifBlank { currentTime },
                            title = title.ifBlank { "Mindset Reflection" },
                            content = content.trim(),
                            mood = mood,
                            energy = energy,
                            tags = tags.trim(),
                            gratitudeNotes = gratitudeNotes.trim(),
                            createdAt = originalCreatedAt.ifBlank { nowIso },
                            updatedAt = nowIso
                        )
                        onSave(entry)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOverwriting) GoogleGreen else GoogleBlue
                    ),
                    modifier = Modifier.testTag("save_diary_btn")
                ) {
                    Text(if (isOverwriting) "Update & Overwrite" else "Save Entry")
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_diary_btn")
            ) {
                Text("Cancel")
            }
        }
    )
}
