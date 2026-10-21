package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleRed
import com.example.ui.theme.GoogleYellow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * DiarySearchBar
 *
 * A search and filter bar positioned at the top of the diary component.
 * Allows users to quickly filter and discover their saved journal entries
 * by text keywords (content, title, tags, notes), specific dates, and moods.
 */
@Composable
fun DiarySearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    dateFilter: String?,
    onDateFilterChange: (String?) -> Unit,
    activeMood: String?,
    onMoodFilterChange: (String?) -> Unit,
    searchScopeAll: Boolean,
    onSearchScopeAllChange: (Boolean) -> Unit,
    datesWithEntries: Set<String>,
    totalSavedEntriesCount: Int,
    currentDayEntriesCount: Int,
    matchingResultsCount: Int,
    onClearAllFilters: () -> Unit,
    onExportPdfClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showFilterChips by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val displayDateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }
    val todayStr = remember { dateFormat.format(Date()) }

    // Quick calculation for yesterday's date
    val yesterdayStr = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        dateFormat.format(cal.time)
    }

    val isFilterActive = searchQuery.isNotBlank() || dateFilter != null || activeMood != null

    // Open native Android DatePickerDialog
    fun openDatePicker() {
        val cal = Calendar.getInstance()
        try {
            if (dateFilter != null) {
                cal.time = dateFormat.parse(dateFilter) ?: Date()
            }
        } catch (_: Exception) {}

        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH)
        val d = cal.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(context, { _, year, monthOfYear, dayOfMonth ->
            val formatted = String.format(Locale.US, "%04d-%02d-%02d", year, monthOfYear + 1, dayOfMonth)
            onDateFilterChange(formatted)
        }, y, m, d).show()
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.2.dp, if (isFilterActive) GoogleBlue else MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = if (isFilterActive) 3.dp else 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("diary_search_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Main search text field with leading search icon and trailing calendar / clear actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = if (dateFilter != null) "Search in $dateFilter..." else "Search entries by keyword or date...",
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (searchQuery.isNotBlank()) GoogleBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { onSearchQueryChange("") },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("diary_clear_search_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Date Picker launch button
                            IconButton(
                                onClick = { openDatePicker() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("diary_date_picker_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Filter by date",
                                    tint = if (dateFilter != null) GoogleBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Filter toggle button (tune / filter options)
                            IconButton(
                                onClick = { showFilterChips = !showFilterChips },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("diary_filter_toggle_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Filter options",
                                    tint = if (showFilterChips || dateFilter != null || activeMood != null) GoogleBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Export PDF quick action button
                            if (onExportPdfClick != null) {
                                IconButton(
                                    onClick = onExportPdfClick,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("diary_export_pdf_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = "Export to PDF",
                                        tint = GoogleRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoogleBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("diary_search_input")
                )
            }

            // Active filters pill row (Date filter badge, Mood badge, Result counter)
            if (isFilterActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Matching count badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GoogleBlue.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "$matchingResultsCount found",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = GoogleBlue,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        // Active date filter badge
                        if (dateFilter != null) {
                            val formattedDate = try {
                                val parsed = dateFormat.parse(dateFilter)
                                if (parsed != null) displayDateFormat.format(parsed) else dateFilter
                            } catch (_: Exception) {
                                dateFilter
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = GoogleGreen.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, GoogleGreen.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .clickable { onDateFilterChange(null) }
                                    .testTag("diary_active_date_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        tint = GoogleGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = formattedDate,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = GoogleGreen,
                                            fontSize = 11.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove date filter",
                                        tint = GoogleGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        // Active mood badge
                        if (activeMood != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = GoogleYellow.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, GoogleYellow.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .clickable { onMoodFilterChange(null) }
                                    .testTag("diary_active_mood_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Mood: $activeMood",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove mood filter",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Reset all filters button
                    TextButton(
                        onClick = onClearAllFilters,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("diary_clear_all_filters_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterAltOff,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = GoogleBlue
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Reset", fontSize = 11.sp, color = GoogleBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Quick date & keyword chips row (always accessible or expandable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Scope: All Entries vs This Day
                FilterChip(
                    selected = searchScopeAll,
                    onClick = { onSearchScopeAllChange(true) },
                    label = { Text("All Dates ($totalSavedEntriesCount)", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoogleBlue.copy(alpha = 0.15f),
                        selectedLabelColor = GoogleBlue
                    ),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("diary_scope_all_chip")
                )

                FilterChip(
                    selected = !searchScopeAll,
                    onClick = { onSearchScopeAllChange(false) },
                    label = { Text("Current Day ($currentDayEntriesCount)", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoogleBlue.copy(alpha = 0.15f),
                        selectedLabelColor = GoogleBlue
                    ),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("diary_scope_day_chip")
                )

                // Export PDF Chip
                if (onExportPdfClick != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = GoogleRed.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, GoogleRed.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .height(30.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onExportPdfClick() }
                            .testTag("diary_export_pdf_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = GoogleRed,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "PDF Export",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = GoogleRed,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                // Today quick button
                FilterChip(
                    selected = dateFilter == todayStr,
                    onClick = {
                        onDateFilterChange(if (dateFilter == todayStr) null else todayStr)
                    },
                    label = { Text("Today", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoogleGreen.copy(alpha = 0.15f),
                        selectedLabelColor = GoogleGreen
                    ),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("diary_filter_today_chip")
                )

                // Yesterday quick button
                FilterChip(
                    selected = dateFilter == yesterdayStr,
                    onClick = {
                        onDateFilterChange(if (dateFilter == yesterdayStr) null else yesterdayStr)
                    },
                    label = { Text("Yesterday", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoogleGreen.copy(alpha = 0.15f),
                        selectedLabelColor = GoogleGreen
                    ),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("diary_filter_yesterday_chip")
                )

                // Pick custom date chip
                FilterChip(
                    selected = dateFilter != null && dateFilter != todayStr && dateFilter != yesterdayStr,
                    onClick = { openDatePicker() },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                    },
                    label = {
                        Text(
                            text = if (dateFilter != null && dateFilter != todayStr && dateFilter != yesterdayStr) dateFilter else "Pick Date...",
                            fontSize = 11.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoogleGreen.copy(alpha = 0.15f),
                        selectedLabelColor = GoogleGreen
                    ),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("diary_pick_custom_date_chip")
                )

                // Dates with entries shortcuts (up to 3 recent dates)
                datesWithEntries.filter { it != todayStr && it != yesterdayStr }.take(3).forEach { dStr ->
                    val label = try {
                        val parsed = dateFormat.parse(dStr)
                        if (parsed != null) SimpleDateFormat("MMM d", Locale.US).format(parsed) else dStr
                    } catch (_: Exception) {
                        dStr
                    }
                    FilterChip(
                        selected = dateFilter == dStr,
                        onClick = {
                            onDateFilterChange(if (dateFilter == dStr) null else dStr)
                        },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoogleGreen.copy(alpha = 0.15f),
                            selectedLabelColor = GoogleGreen
                        ),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("diary_date_chip_$dStr")
                    )
                }
            }

            // Expanded filter drawer (Moods & Suggested Tags)
            AnimatedVisibility(
                visible = showFilterChips,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // Mood Filter row
                    Text(
                        text = "Filter by Mood:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = activeMood == null,
                            onClick = { onMoodFilterChange(null) },
                            label = { Text("All", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoogleBlue.copy(alpha = 0.15f),
                                selectedLabelColor = GoogleBlue
                            ),
                            modifier = Modifier.height(28.dp)
                        )

                        DIARY_MOODS.forEach { (mood, emoji) ->
                            FilterChip(
                                selected = activeMood == mood,
                                onClick = {
                                    onMoodFilterChange(if (activeMood == mood) null else mood)
                                },
                                label = { Text("$emoji $mood", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoogleBlue.copy(alpha = 0.15f),
                                    selectedLabelColor = GoogleBlue
                                ),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("mood_chip_$mood")
                            )
                        }
                    }

                    // Keyword ideas
                    Text(
                        text = "Quick Tag Searches:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SUGGESTED_DIARY_TAGS.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (searchQuery.contains(tag, ignoreCase = true)) GoogleBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable {
                                        onSearchQueryChange(if (searchQuery.isBlank()) tag else "$searchQuery $tag".trim())
                                    }
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (searchQuery.contains(tag, ignoreCase = true)) GoogleBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
