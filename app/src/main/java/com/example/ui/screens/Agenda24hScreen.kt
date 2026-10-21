package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AgendaTask
import com.example.ui.components.Timeline24hVertical
import com.example.ui.theme.GoogleBlue
import com.example.ui.viewmodel.LifestyleViewModel

val AGENDA_FILTER_CHIPS = listOf("All", "Work", "Health", "Personal", "Deep Work", "Learning")

@Composable
fun Agenda24hScreen(
    viewModel: LifestyleViewModel,
    onOpenTaskDialog: (AgendaTask?, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.filteredTasks.collectAsStateWithLifecycle()
    val activeCategory by viewModel.categoryFilter.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Category Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                AGENDA_FILTER_CHIPS.forEach { cat ->
                    FilterChip(
                        selected = activeCategory == cat,
                        onClick = { viewModel.setCategoryFilter(cat) },
                        label = { Text(cat, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoogleBlue.copy(alpha = 0.15f),
                            selectedLabelColor = GoogleBlue
                        ),
                        modifier = Modifier.testTag("agenda_filter_$cat")
                    )
                }
            }

            // 24-Hour Vertical Timeline Component with 15-Minute Interval Tapping
            Timeline24hVertical(
                tasks = tasks,
                onToggleTask = { viewModel.toggleTask(it) },
                onEditTask = { onOpenTaskDialog(it, it.timeSlot) },
                onDeleteTask = { viewModel.deleteTask(it) },
                onAddIntervalTask = { timeSlot -> onOpenTaskDialog(null, timeSlot) },
                modifier = Modifier.weight(1f)
            )
        }

        // Floating Action Button to add task at current time
        FloatingActionButton(
            onClick = { onOpenTaskDialog(null, "09:00") },
            containerColor = GoogleBlue,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("agenda_fab_add_task")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Task")
        }
    }
}
