package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DiaryEntry
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleRed
import com.example.util.DiaryPdfExporter
import com.example.util.PdfExportOptions
import com.example.util.PdfExportResult

enum class PdfExportScope {
    ALL_SAVED,
    CURRENT_FILTERED,
    SELECTED_DATE_ONLY
}

/**
 * DiaryPdfExportDialog
 *
 * A full-featured dialog for customizing and downloading diary entries
 * to a clean, multi-page PDF document.
 */
@Composable
fun DiaryPdfExportDialog(
    allSavedEntries: List<DiaryEntry>,
    filteredEntries: List<DiaryEntry>,
    selectedDateEntries: List<DiaryEntry>,
    selectedDate: String,
    isFilterActive: Boolean,
    isExporting: Boolean,
    lastExportResult: PdfExportResult?,
    exportError: String?,
    onDismissRequest: () -> Unit,
    onExportPdf: (List<DiaryEntry>, PdfExportOptions) -> Unit,
    onClearLastResult: () -> Unit
) {
    val context = LocalContext.current
    val exporter = remember(context) { DiaryPdfExporter(context) }

    // Dialog state
    var selectedScope by remember {
        mutableStateOf(
            if (isFilterActive && filteredEntries.isNotEmpty()) PdfExportScope.CURRENT_FILTERED
            else PdfExportScope.ALL_SAVED
        )
    }

    var documentTitle by remember { mutableStateOf("My Daily Reflections") }
    var documentSubtitle by remember { mutableStateOf("Personal Journal & Mindset Diary") }
    var includeGratitude by remember { mutableStateOf(true) }
    var includeMoodAndEnergy by remember { mutableStateOf(true) }
    var includeTags by remember { mutableStateOf(true) }
    var includeSummaryStats by remember { mutableStateOf(true) }

    val entriesToExport = when (selectedScope) {
        PdfExportScope.ALL_SAVED -> allSavedEntries
        PdfExportScope.CURRENT_FILTERED -> filteredEntries
        PdfExportScope.SELECTED_DATE_ONLY -> selectedDateEntries
    }

    Dialog(
        onDismissRequest = {
            if (!isExporting) {
                onClearLastResult()
                onDismissRequest()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
                .testTag("pdf_export_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(GoogleRed.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = GoogleRed,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Export Diary to PDF",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Generate downloadable journal document",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (!isExporting) {
                                onClearLastResult()
                                onDismissRequest()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // If export result is already available: show success & action buttons!
                if (lastExportResult != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = GoogleGreen.copy(alpha = 0.08f),
                        border = BorderStroke(1.5.dp, GoogleGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = GoogleGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "PDF Ready & Downloaded!",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = GoogleGreen
                                )
                            }

                            Text(
                                text = "File: ${lastExportResult.fileName}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Size: ${lastExportResult.fileSizeFormatted}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Pages: ${lastExportResult.pageCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Entries: ${lastExportResult.entryCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = "Saved to: ${lastExportResult.destinationPath}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Action buttons: Open & Share
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        exporter.openPdf(lastExportResult.uri)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoogleBlue),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("pdf_open_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open PDF")
                                }

                                OutlinedButton(
                                    onClick = {
                                        exporter.sharePdf(lastExportResult.uri, lastExportResult.fileName)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("pdf_share_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share")
                                }
                            }
                        }
                    }
                }

                // If error occurred
                if (exportError != null && lastExportResult == null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GoogleRed.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, GoogleRed.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Error generating PDF: $exportError",
                            color = GoogleRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Scope Selection Section
                Text(
                    text = "Select Entries to Export",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        // Scope 1: All saved entries
                        ScopeRadioRow(
                            title = "All Saved Reflections",
                            subtitle = "${allSavedEntries.size} entries across all dates",
                            isSelected = selectedScope == PdfExportScope.ALL_SAVED,
                            onClick = { selectedScope = PdfExportScope.ALL_SAVED }
                        )

                        // Scope 2: Filtered entries (if filter is active)
                        if (isFilterActive) {
                            ScopeRadioRow(
                                title = "Matching Search Results",
                                subtitle = "${filteredEntries.size} filtered entries",
                                isSelected = selectedScope == PdfExportScope.CURRENT_FILTERED,
                                onClick = { selectedScope = PdfExportScope.CURRENT_FILTERED }
                            )
                        }

                        // Scope 3: Selected Date only
                        ScopeRadioRow(
                            title = "Selected Date ($selectedDate)",
                            subtitle = "${selectedDateEntries.size} entries logged on this day",
                            isSelected = selectedScope == PdfExportScope.SELECTED_DATE_ONLY,
                            onClick = { selectedScope = PdfExportScope.SELECTED_DATE_ONLY }
                        )
                    }
                }

                // Document Metadata Configuration
                Text(
                    text = "Document Details",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = documentTitle,
                    onValueChange = { documentTitle = it },
                    label = { Text("Document Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pdf_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoogleBlue
                    )
                )

                OutlinedTextField(
                    value = documentSubtitle,
                    onValueChange = { documentSubtitle = it },
                    label = { Text("Subtitle / Author") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoogleBlue
                    )
                )

                // Checkbox Options
                Text(
                    text = "Include in Export",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CheckboxOptionRow(
                        label = "Include Summary Statistics (word count, days logged)",
                        checked = includeSummaryStats,
                        onCheckedChange = { includeSummaryStats = it }
                    )
                    CheckboxOptionRow(
                        label = "Include Gratitude Notes",
                        checked = includeGratitude,
                        onCheckedChange = { includeGratitude = it }
                    )
                    CheckboxOptionRow(
                        label = "Include Mood & Energy Ratings",
                        checked = includeMoodAndEnergy,
                        onCheckedChange = { includeMoodAndEnergy = it }
                    )
                    CheckboxOptionRow(
                        label = "Include Tags",
                        checked = includeTags,
                        onCheckedChange = { includeTags = it }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom Action Button
                if (isExporting) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp,
                            color = GoogleBlue
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Rendering and downloading PDF...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            if (entriesToExport.isEmpty()) {
                                Toast.makeText(context, "No entries selected to export.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val options = PdfExportOptions(
                                documentTitle = documentTitle.ifBlank { "My Daily Reflections" },
                                subtitle = documentSubtitle.ifBlank { "Exported Journal" },
                                includeGratitude = includeGratitude,
                                includeMoodAndEnergy = includeMoodAndEnergy,
                                includeTags = includeTags,
                                includeSummaryStats = includeSummaryStats
                            )
                            onExportPdf(entriesToExport, options)
                        },
                        enabled = entriesToExport.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = GoogleBlue),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("pdf_export_confirm_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (lastExportResult != null) "Re-Export ${entriesToExport.size} Entries as PDF"
                            else "Download PDF (${entriesToExport.size} Entries)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScopeRadioRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = GoogleBlue)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CheckboxOptionRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = GoogleBlue)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
