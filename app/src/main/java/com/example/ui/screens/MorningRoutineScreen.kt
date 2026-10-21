package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CountdownTimer15m
import com.example.ui.components.DailyPrayerTrackerComponent
import com.example.ui.components.IslamicReminderBayanComponent
import com.example.ui.components.WaterGlassesTracker
import com.example.ui.theme.GoogleGreen
import com.example.ui.viewmodel.LifestyleViewModel

@Composable
fun MorningRoutineScreen(
    viewModel: LifestyleViewModel,
    modifier: Modifier = Modifier
) {
    val morningPlan by viewModel.morningPlan.collectAsStateWithLifecycle()
    val prayerRecord by viewModel.prayerRecord.collectAsStateWithLifecycle()
    val timerSeconds by viewModel.timerSeconds.collectAsStateWithLifecycle()
    val isTimerRunning by viewModel.isTimerRunning.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val routineTarget by viewModel.routineSectionTarget.collectAsStateWithLifecycle()

    val waterGlasses = morningPlan?.waterGlasses ?: 0
    val listState = rememberLazyListState()
    var zikrOffsetY by remember { mutableIntStateOf(0) }
    var radioOffsetY by remember { mutableIntStateOf(0) }

    // Smoothly scroll to the requested target section when triggered from Application View
    LaunchedEffect(routineTarget) {
        routineTarget?.let { target ->
            when (target) {
                "namaz" -> listState.animateScrollToItem(1, 0)
                "zikr" -> {
                    // Scroll directly to the Zikr & Remembrance section
                    val offset = if (zikrOffsetY > 0) zikrOffsetY else 1650
                    listState.animateScrollToItem(1, offset)
                }
                "quran_live", "radio" -> {
                    // Scroll directly to the Quran Live Radio section
                    val offset = if (radioOffsetY > 0) radioOffsetY else 3000
                    listState.animateScrollToItem(1, offset)
                }
                "countdown" -> listState.animateScrollToItem(2)
                "hydration" -> listState.animateScrollToItem(3)
            }
            kotlinx.coroutines.delay(600)
            viewModel.clearRoutineSectionTarget()
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // --- Comprehensive Prayer, Zikr & Tilawat Tracker ---
        item {
            DailyPrayerTrackerComponent(
                selectedDate = selectedDate,
                initialRecord = prayerRecord,
                targetSection = routineTarget,
                onZikrPositioned = { offset ->
                    zikrOffsetY = offset
                },
                onRadioPositioned = { offset ->
                    radioOffsetY = offset
                },
                onSaveRecord = { record, callback ->
                    viewModel.savePrayerRecord(record, callback)
                }
            )
        }

        // --- Automated Islamic Reminder (Short Bayan) Section ---
        item {
            IslamicReminderBayanComponent()
        }

        // 15-Minute Countdown Timer
        item {
            CountdownTimer15m(
                secondsRemaining = timerSeconds,
                isRunning = isTimerRunning,
                onStart = { viewModel.startTimer() },
                onPause = { viewModel.pauseTimer() },
                onReset = { viewModel.resetTimer() }
            )
        }

        // Water Hydration Tracker
        item {
            WaterGlassesTracker(
                waterCount = waterGlasses,
                targetCount = 8,
                onWaterCountChange = { viewModel.updateWaterGlasses(it) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
