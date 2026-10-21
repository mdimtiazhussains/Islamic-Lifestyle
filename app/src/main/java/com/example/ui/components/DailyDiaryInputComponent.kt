package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

val QUICK_INSPIRATION_PROMPTS = listOf(
    Pair("🌟 Win", "🎯 Today's Greatest Win:\n"),
    Pair("💡 Insight", "💡 Key Insight / Lesson Learned:\n"),
    Pair("🙏 Gratitude", "🙏 3 Things I Am Truly Grateful For:\n1. \n2. \n3. \n"),
    Pair("🧘 Mindfulness", "🌊 Mindful Reflection: How did I respond to stress or challenges today?\n"),
    Pair("🚀 Tomorrow", "🚀 Tomorrow's #1 Focus & Growth Opportunity:\n")
)

val SUGGESTED_DIARY_TAGS = listOf(
    "#mindset", "#clarity", "#growth", "#productivity", "#gratitude", "#health", "#deepwork"
)

/**
 * DailyDiaryInputComponent
 *
 * An interactive daily diary input component that saves entries into Cloud Firestore
 * and local database storage, allowing users to write daily mindset reflections,
 * mood ratings, gratitude observations, and insights for any given day.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyDiaryInputComponent(
    selectedDate: String,
    currentUser: FirebaseUser?,
    entryToEdit: DiaryEntry? = null,
    existingEntries: List<DiaryEntry> = emptyList(),
    onSaveToFirestore: (DiaryEntry, (Boolean, String) -> Unit) -> Unit,
    onCancelEdit: (() -> Unit)? = null,
    onSignInRequest: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false
) {
    // Strictly collapsed/closed by default; remains collapsed during scroll & navigation
    var isExpanded by remember(selectedDate) { mutableStateOf(entryToEdit != null) }

    val currentTime = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    var title by remember { mutableStateOf(entryToEdit?.title ?: "") }
    var content by remember { mutableStateOf(entryToEdit?.content ?: "") }
    var time by remember { mutableStateOf(entryToEdit?.time ?: currentTime) }
    var mood by remember { mutableStateOf(entryToEdit?.mood ?: "Calm") }
    var energy by remember { mutableIntStateOf(entryToEdit?.energy ?: 3) }
    var gratitudeNotes by remember { mutableStateOf(entryToEdit?.gratitudeNotes ?: "") }
    var tags by remember { mutableStateOf(entryToEdit?.tags ?: "") }

    var isSaving by remember { mutableStateOf(false) }
    var saveFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var isSaveSuccess by remember { mutableStateOf(false) }
    var contentError by remember { mutableStateOf(false) }

    // User preference: explicit option to force create a new entry vs updating existing
    var forceNewEntry by remember { mutableStateOf(false) }
    var selectedExistingEntryId by remember { mutableStateOf<String?>(entryToEdit?.id) }

    // Intelligently detect if an existing entry on this date has matching subject/title or content
    val matchingEntry = remember(title, content, existingEntries, entryToEdit, selectedExistingEntryId) {
        if (selectedExistingEntryId != null) {
            existingEntries.firstOrNull { it.id == selectedExistingEntryId } ?: entryToEdit
        } else if (entryToEdit != null) {
            entryToEdit
        } else {
            val trimmedTitle = title.trim()
            val trimmedContent = content.trim()
            existingEntries.firstOrNull { existing ->
                val isGenericTitle = trimmedTitle.equals("Daily Mindset Reflection", ignoreCase = true) ||
                        trimmedTitle.equals("Mindset Reflection", ignoreCase = true) ||
                        trimmedTitle.equals("Daily Reflection", ignoreCase = true) ||
                        trimmedTitle.equals("Voice Reflection", ignoreCase = true)
                val titleMatches = trimmedTitle.isNotBlank() && !isGenericTitle &&
                        existing.title.trim().equals(trimmedTitle, ignoreCase = true)
                val contentMatches = trimmedContent.isNotBlank() &&
                        (existing.content.trim().equals(trimmedContent, ignoreCase = true) ||
                                (trimmedContent.length >= 25 && existing.content.trim().contains(trimmedContent.take(35))))
                titleMatches || contentMatches
            }
        }
    }

    // Ensure completely clean, empty composer state when selectedDate changes unless editing an existing entry
    LaunchedEffect(selectedDate) {
        if (entryToEdit == null) {
            isExpanded = false
            title = ""
            content = ""
            gratitudeNotes = ""
            tags = ""
            saveFeedbackMessage = null
            forceNewEntry = false
            selectedExistingEntryId = null
        }
    }

    // Synchronize when entryToEdit changes (e.g. user taps Edit on an existing entry)
    LaunchedEffect(entryToEdit) {
        if (entryToEdit != null) {
            isExpanded = true
            title = entryToEdit.title
            content = entryToEdit.content
            time = entryToEdit.time
            mood = entryToEdit.mood
            energy = entryToEdit.energy
            gratitudeNotes = entryToEdit.gratitudeNotes
            tags = entryToEdit.tags
            saveFeedbackMessage = null
        }
    }

    // Word and character counts
    val wordCount = remember(content) {
        if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
    }
    val charCount = remember(content) { content.length }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, if (entryToEdit != null) GoogleBlue else MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_diary_input_component")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Title, Date Badge, Cloud Sync status, and Expand/Collapse toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (entryToEdit != null) GoogleYellow.copy(alpha = 0.2f) else GoogleBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (entryToEdit != null) Icons.Default.Edit else Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = if (entryToEdit != null) GoogleYellow else GoogleBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (entryToEdit != null) "Edit Reflection" else "Daily Diary Reflection",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Date pill
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Today,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = selectedDate,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Cloud Firestore status indicator
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (currentUser != null) GoogleGreen.copy(alpha = 0.12f) else GoogleYellow.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (currentUser != null) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = if (currentUser != null) GoogleGreen else GoogleYellow
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (currentUser != null) "Firestore Sync Active" else "Local Storage",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.sp
                                        ),
                                        color = if (currentUser != null) GoogleGreen else GoogleYellow
                                    )
                                }
                            }
                        }
                    }
                }

                // Expand/Collapse toggle button
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.testTag("toggle_diary_input_expand")
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse Input" else "Expand Input"
                    )
                }
            }

            // Prominent, styled button when collapsed
            if (!isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                if (content.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "📝 Unsaved Draft: ${content.take(70)}...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                Button(
                    onClick = { isExpanded = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_diary_composer_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoogleBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (content.isBlank()) "Open Journal Composer" else "Open Journal Composer (Draft Active)",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                }
            }

            // Expanded Rich Input Component
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Inspiration Prompts Carousel
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = GoogleYellow,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Writing Prompts & Prompts Inspiration:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            QUICK_INSPIRATION_PROMPTS.forEach { (label, promptText) ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.clickable {
                                        content = if (content.isBlank()) promptText else "$content\n\n$promptText"
                                        contentError = false
                                    }
                                ) {
                                    Text(
                                        text = "+ $label",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Voice-to-Text Recording & Speech Transcription Component
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
                            saveFeedbackMessage = "Voice transcribed in ${result.detectedLanguage}! Fields auto-populated."
                            isSaveSuccess = true
                        }
                    )

                    // Clear distinction / option to create a completely new journal entry versus updating an existing one
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth().testTag("diary_entry_distinction_card")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Entry Action:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val isNewActive = forceNewEntry || (matchingEntry == null && entryToEdit == null && selectedExistingEntryId == null)
                                    FilterChip(
                                        selected = isNewActive,
                                        onClick = {
                                            forceNewEntry = true
                                            selectedExistingEntryId = null
                                        },
                                        label = { Text("➕ New Entry", fontSize = 11.sp, fontWeight = if (isNewActive) FontWeight.Bold else FontWeight.Normal) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = GoogleBlue.copy(alpha = 0.18f),
                                            selectedLabelColor = GoogleBlue
                                        ),
                                        modifier = Modifier.testTag("entry_mode_new_chip")
                                    )

                                    val isUpdateActive = !forceNewEntry && (matchingEntry != null || entryToEdit != null || existingEntries.isNotEmpty())
                                    FilterChip(
                                        selected = isUpdateActive,
                                        onClick = {
                                            forceNewEntry = false
                                            if (matchingEntry != null) {
                                                selectedExistingEntryId = matchingEntry.id
                                            } else if (existingEntries.isNotEmpty()) {
                                                val target = existingEntries.first()
                                                selectedExistingEntryId = target.id
                                                title = target.title
                                                content = target.content
                                                gratitudeNotes = target.gratitudeNotes
                                                tags = target.tags
                                                mood = target.mood
                                                energy = target.energy
                                                time = target.time
                                            }
                                        },
                                        label = { Text("✏️ Update Existing", fontSize = 11.sp, fontWeight = if (isUpdateActive) FontWeight.Bold else FontWeight.Normal) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = GoogleYellow.copy(alpha = 0.25f),
                                            selectedLabelColor = Color(0xFFE65100)
                                        ),
                                        modifier = Modifier.testTag("entry_mode_update_chip")
                                    )
                                }
                            }

                            // Notification Banner when matching entry is detected (Deduplication guarantee)
                            if (matchingEntry != null && !forceNewEntry) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = GoogleBlue.copy(alpha = 0.10f),
                                    border = BorderStroke(1.dp, GoogleBlue.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth().testTag("duplicate_prevention_notice")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = GoogleBlue,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Matching entry found: \"${matchingEntry.title}\"",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = GoogleBlue)
                                            )
                                            Text(
                                                text = "Saving will overwrite this existing card instead of creating a duplicate.",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        TextButton(
                                            onClick = { forceNewEntry = true },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                            modifier = Modifier.testTag("switch_to_new_card_btn")
                                        ) {
                                            Text("Save as New", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Selectable chips of existing entries for the day when in update mode
                            if (!forceNewEntry && existingEntries.isNotEmpty() && entryToEdit == null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    existingEntries.forEach { itemEntry ->
                                        val isChosen = (selectedExistingEntryId == itemEntry.id) || (selectedExistingEntryId == null && matchingEntry?.id == itemEntry.id)
                                        FilterChip(
                                            selected = isChosen,
                                            onClick = {
                                                forceNewEntry = false
                                                selectedExistingEntryId = itemEntry.id
                                                title = itemEntry.title
                                                content = itemEntry.content
                                                gratitudeNotes = itemEntry.gratitudeNotes
                                                tags = itemEntry.tags
                                                mood = itemEntry.mood
                                                energy = itemEntry.energy
                                                time = itemEntry.time
                                            },
                                            label = { Text("${itemEntry.time} • ${itemEntry.title.take(18)}", fontSize = 10.sp) },
                                            modifier = Modifier.testTag("select_existing_entry_${itemEntry.id}")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Title & Time row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Reflection Title (Optional)") },
                            placeholder = { Text("e.g. Evening Clarity, Milestone Win") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("diary_input_title")
                        )

                        OutlinedTextField(
                            value = time,
                            onValueChange = { time = it },
                            label = { Text("Time") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .width(95.dp)
                                .testTag("diary_input_time")
                        )
                    }

                    // Reflection Content Text Area
                    OutlinedTextField(
                        value = content,
                        onValueChange = {
                            content = it
                            if (it.isNotBlank()) contentError = false
                        },
                        label = { Text("Mindset Reflections & Insights *") },
                        placeholder = { Text("What did you accomplish, discover, or feel today? Note your daily learnings, emotions, and mindset reflections...") },
                        minLines = 4,
                        maxLines = 8,
                        isError = contentError,
                        shape = RoundedCornerShape(12.dp),
                        supportingText = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (contentError) {
                                    Text("Reflection content is required to save", color = GoogleRed, fontSize = 11.sp)
                                } else {
                                    Text("Saves directly to Cloud Firestore", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = "$wordCount words • $charCount chars",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("diary_input_content")
                    )

                    // Mood Selector
                    Column {
                        Text(
                            text = "Current Mood State",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            DIARY_MOODS.forEach { (m, emoji) ->
                                val isSelected = mood == m
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { mood = m },
                                    label = { Text("$emoji $m", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoogleBlue.copy(alpha = 0.18f),
                                        selectedLabelColor = GoogleBlue
                                    ),
                                    modifier = Modifier.testTag("diary_input_mood_$m")
                                )
                            }
                        }
                    }

                    // Energy Level (1 to 5)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Vitality / Energy Rating",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val energyLabel = when (energy) {
                                1 -> "1 - Low / Rest Needed"
                                2 -> "2 - Gentle / Quiet"
                                3 -> "3 - Balanced"
                                4 -> "4 - High Drive"
                                else -> "5 - Peak Vitality"
                            }
                            Text(
                                text = energyLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = GoogleYellow
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 1..5) {
                                val isActive = i <= energy
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isActive) GoogleYellow.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, if (isActive) GoogleYellow.copy(alpha = 0.6f) else Color.Transparent),
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clickable { energy = i }
                                        .testTag("diary_input_energy_$i")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = "Energy level $i",
                                            tint = if (isActive) GoogleYellow else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Gratitude Notes Block
                    OutlinedTextField(
                        value = gratitudeNotes,
                        onValueChange = { gratitudeNotes = it },
                        label = { Text("Gratitude & Positive Highlights (Optional)") },
                        placeholder = { Text("What made you smile? People, small victories, or quiet blessings...") },
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("diary_input_gratitude")
                    )

                    // Tags & Suggested Tag Pills
                    Column {
                        OutlinedTextField(
                            value = tags,
                            onValueChange = { tags = it },
                            label = { Text("Tags & Keywords") },
                            placeholder = { Text("#mindset, #clarity, #growth") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("diary_input_tags")
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Suggested Tags Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SUGGESTED_DIARY_TAGS.forEach { suggestedTag ->
                                val alreadyAdded = tags.contains(suggestedTag, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (alreadyAdded) GoogleBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.clickable {
                                        tags = if (alreadyAdded) {
                                            tags.replace(suggestedTag, "").replace(",,", ",").trim(',', ' ')
                                        } else {
                                            if (tags.isBlank()) suggestedTag else "$tags, $suggestedTag"
                                        }
                                    }
                                ) {
                                    Text(
                                        text = suggestedTag,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = if (alreadyAdded) FontWeight.Bold else FontWeight.Normal,
                                            color = if (alreadyAdded) GoogleBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Save Feedback Banner if present
                    if (saveFeedbackMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSaveSuccess) GoogleGreen.copy(alpha = 0.12f) else GoogleYellow.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, if (isSaveSuccess) GoogleGreen.copy(alpha = 0.3f) else GoogleYellow.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSaveSuccess) Icons.Default.CheckCircle else Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = if (isSaveSuccess) GoogleGreen else GoogleYellow,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = saveFeedbackMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSaveSuccess) GoogleGreen else MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { saveFeedbackMessage = null },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Dismiss",
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    val isOverwriting = !forceNewEntry && matchingEntry != null

                    // Action Buttons (Save to Firestore & Clear Draft)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (content.isBlank() && title.isBlank()) {
                                    contentError = true
                                    return@Button
                                }
                                isSaving = true
                                saveFeedbackMessage = null

                                val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                                val entryId = if (isOverwriting) {
                                    matchingEntry!!.id
                                } else {
                                    "diary_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
                                }
                                val originalCreatedAt = if (isOverwriting) {
                                    matchingEntry!!.createdAt
                                } else {
                                    entryToEdit?.createdAt?.ifBlank { nowIso } ?: nowIso
                                }

                                val entry = DiaryEntry(
                                    id = entryId,
                                    date = if (isOverwriting) matchingEntry!!.date else (entryToEdit?.date ?: selectedDate),
                                    time = time.ifBlank { currentTime },
                                    title = title.ifBlank { "Daily Mindset Reflection" },
                                    content = content.trim(),
                                    mood = mood,
                                    energy = energy,
                                    tags = tags.trim(),
                                    gratitudeNotes = gratitudeNotes.trim(),
                                    createdAt = originalCreatedAt.ifBlank { nowIso },
                                    updatedAt = nowIso
                                )

                                onSaveToFirestore(entry) { success, msg ->
                                    isSaving = false
                                    isSaveSuccess = success
                                    saveFeedbackMessage = if (success && isOverwriting) {
                                        "✓ Reflection updated! Overwrote previous entry \"${entry.title}\" without duplicating."
                                    } else {
                                        msg
                                    }
                                    if (success) {
                                        title = ""
                                        content = ""
                                        gratitudeNotes = ""
                                        tags = ""
                                        forceNewEntry = false
                                        selectedExistingEntryId = null
                                        isExpanded = false
                                    }
                                }
                            },
                            enabled = !isSaving,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isOverwriting) GoogleGreen else GoogleBlue
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("save_daily_diary_input_btn")
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Saving...", fontSize = 13.sp)
                            } else {
                                Icon(
                                    imageVector = if (isOverwriting) Icons.Default.Check else Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isOverwriting) "Update & Overwrite Entry" else "Save as New Entry",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Explicit button to create a separate new card when an existing match was found
                        if (isOverwriting) {
                            OutlinedButton(
                                onClick = {
                                    if (content.isBlank() && title.isBlank()) {
                                        contentError = true
                                        return@OutlinedButton
                                    }
                                    isSaving = true
                                    saveFeedbackMessage = null

                                    val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                                    val newEntryId = "diary_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"

                                    val entry = DiaryEntry(
                                        id = newEntryId,
                                        date = selectedDate,
                                        time = time.ifBlank { currentTime },
                                        title = title.ifBlank { "Daily Mindset Reflection" },
                                        content = content.trim(),
                                        mood = mood,
                                        energy = energy,
                                        tags = tags.trim(),
                                        gratitudeNotes = gratitudeNotes.trim(),
                                        createdAt = nowIso,
                                        updatedAt = nowIso
                                    )

                                    onSaveToFirestore(entry) { success, msg ->
                                        isSaving = false
                                        isSaveSuccess = success
                                        saveFeedbackMessage = if (success) "✓ Saved as new reflection card!" else msg
                                        if (success) {
                                            title = ""
                                            content = ""
                                            gratitudeNotes = ""
                                            tags = ""
                                            forceNewEntry = false
                                            selectedExistingEntryId = null
                                            isExpanded = false
                                        }
                                    }
                                },
                                enabled = !isSaving,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(46.dp)
                                    .testTag("force_save_as_new_card_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save as New Card", fontSize = 12.sp)
                            }
                        }

                        if (entryToEdit != null && onCancelEdit != null) {
                            OutlinedButton(
                                onClick = onCancelEdit,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(46.dp)
                                    .testTag("cancel_edit_diary_btn")
                            ) {
                                Text("Cancel", fontSize = 13.sp)
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    title = ""
                                    content = ""
                                    gratitudeNotes = ""
                                    tags = ""
                                    saveFeedbackMessage = null
                                    contentError = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(46.dp)
                                    .testTag("clear_diary_draft_btn")
                            ) {
                                Text("Clear", fontSize = 13.sp)
                            }
                        }
                    }

                    // Prompt to sign in if not currently authenticated
                    if (currentUser == null && onSignInRequest != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSignInRequest() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = GoogleBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Cloud Firestore Sync available",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Tap to sign in with Google to backup diary reflections to your cloud account.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "Sign In →",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = GoogleBlue
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
