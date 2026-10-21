package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DiaryEntry
import com.example.ui.components.DIARY_MOODS
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleRed
import com.example.ui.theme.GoogleYellow
import com.example.ui.viewmodel.LifestyleViewModel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.components.DailyDiaryInputComponent
import com.example.ui.components.DiaryPdfExportDialog
import com.example.ui.components.DiarySearchBar
import com.google.firebase.auth.FirebaseUser

@Composable
fun DiaryScreen(
    viewModel: LifestyleViewModel,
    currentUser: FirebaseUser? = null,
    onSignInRequest: (() -> Unit)? = null,
    onOpenSyncSheet: (() -> Unit)? = null,
    onOpenDiaryDialog: (DiaryEntry?) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val entries by viewModel.filteredDiaryEntries.collectAsStateWithLifecycle()
    val rawEntries by viewModel.diaryEntries.collectAsStateWithLifecycle()
    val allSavedEntries by viewModel.allDiaryEntries.collectAsStateWithLifecycle()
    val datesWithEntries by viewModel.datesWithDiaryEntries.collectAsStateWithLifecycle()
    val searchQuery by viewModel.diarySearchQuery.collectAsStateWithLifecycle()
    val dateFilter by viewModel.diaryDateFilter.collectAsStateWithLifecycle()
    val activeMood by viewModel.diaryMoodFilter.collectAsStateWithLifecycle()
    val searchScopeAll by viewModel.diarySearchScopeAll.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isCloudSyncing.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val isExportingPdf by viewModel.isExportingPdf.collectAsStateWithLifecycle()
    val lastExportResult by viewModel.lastExportResult.collectAsStateWithLifecycle()
    val exportErrorMessage by viewModel.exportErrorMessage.collectAsStateWithLifecycle()

    var entryBeingEdited by remember { mutableStateOf<DiaryEntry?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // 🔍 Search bar at the top of the diary component (filter by keyword or date) with PDF export
            DiarySearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.setDiarySearchQuery(it) },
                dateFilter = dateFilter,
                onDateFilterChange = { viewModel.setDiaryDateFilter(it) },
                activeMood = activeMood,
                onMoodFilterChange = { viewModel.setDiaryMoodFilter(it) },
                searchScopeAll = searchScopeAll,
                onSearchScopeAllChange = { viewModel.setDiarySearchScopeAll(it) },
                datesWithEntries = datesWithEntries,
                totalSavedEntriesCount = allSavedEntries.size,
                currentDayEntriesCount = rawEntries.size,
                matchingResultsCount = entries.size,
                onClearAllFilters = { viewModel.clearDiaryFilters() },
                onExportPdfClick = { showExportDialog = true }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Primary Daily Diary Input Component saving to Firestore
                item {
                    DailyDiaryInputComponent(
                        selectedDate = selectedDate,
                        currentUser = currentUser,
                        entryToEdit = entryBeingEdited,
                        existingEntries = rawEntries,
                        onSaveToFirestore = { entry, callback ->
                            viewModel.saveDiaryEntry(entry) { success, msg ->
                                if (success) {
                                    entryBeingEdited = null
                                }
                                callback(success, msg)
                            }
                        },
                        onCancelEdit = {
                            entryBeingEdited = null
                        },
                        onSignInRequest = onSignInRequest,
                        initiallyExpanded = false
                    )
                }

                // 2. Entries count header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank() || dateFilter != null || activeMood != null)
                                "Search Results (${entries.size})"
                            else if (searchScopeAll)
                                "Saved Journal Entries (${entries.size})"
                            else
                                "Reflections for $selectedDate (${entries.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (searchQuery.isNotBlank() || dateFilter != null || activeMood != null) {
                                Text(
                                    text = "of ${allSavedEntries.size} total",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Quick PDF export button in header
                            OutlinedButton(
                                onClick = { showExportDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("diary_header_export_pdf_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = GoogleRed,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PDF", fontSize = 11.sp, color = GoogleRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

            // 4. Diary Entries List
            if (entries.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(GoogleBlue.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.FormatQuote, contentDescription = null, tint = GoogleBlue, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (searchQuery.isNotBlank() || activeMood != null) "No reflections match filters" else "No reflections logged for $selectedDate yet",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Use the Daily Diary composer above to record your thoughts, mindset, and gratitude into Firestore.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(entries, key = { it.id }) { entry ->
                    DiaryFullCard(
                        entry = entry,
                        onEdit = {
                            entryBeingEdited = entry
                        },
                        onDelete = { viewModel.deleteDiaryEntry(entry) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }

        // Floating Action Button to quickly open or add a reflection
        FloatingActionButton(
            onClick = { onOpenDiaryDialog(null) },
            containerColor = GoogleBlue,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("diary_fab_write")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Write Entry")
        }

        // PDF Export Dialog
        if (showExportDialog) {
            DiaryPdfExportDialog(
                allSavedEntries = allSavedEntries,
                filteredEntries = entries,
                selectedDateEntries = rawEntries,
                selectedDate = selectedDate,
                isFilterActive = searchQuery.isNotBlank() || dateFilter != null || activeMood != null,
                isExporting = isExportingPdf,
                lastExportResult = lastExportResult,
                exportError = exportErrorMessage,
                onDismissRequest = { showExportDialog = false },
                onExportPdf = { entriesToExport, options ->
                    viewModel.exportDiaryToPdf(context, entriesToExport, options)
                },
                onClearLastResult = { viewModel.clearLastExportResult() }
            )
        }
    }
}

@Composable
fun DiaryFullCard(
    entry: DiaryEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("full_diary_card_${entry.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Mood, Time, Energy, Actions
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
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${entry.date} • ${entry.time}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Energy bolts
                    Row {
                        for (i in 1..entry.energy.coerceIn(1, 5)) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = GoogleYellow,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Content
            Text(
                text = entry.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                lineHeight = 22.sp
            )

            // Gratitude Notes Block
            if (entry.gratitudeNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GoogleGreen.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoogleGreen.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "🌱 Gratitude Highlights:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = GoogleGreen
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = entry.gratitudeNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Tags
            if (entry.tags.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    entry.tags.split(",", " ").filter { it.isNotBlank() }.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (tag.startsWith("#")) tag else "#$tag",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = GoogleBlue,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
