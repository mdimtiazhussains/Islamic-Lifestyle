package com.example.ui.components

import android.Manifest
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DailyPrayerRecord
import com.example.data.service.LivePrayerTimesResult
import com.example.data.service.LocationPrayerService
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleRed
import com.example.ui.theme.GoogleYellow
import com.example.util.AzanAudioPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Islamic palette accents
val EmeraldGreen = Color(0xFF0F9D58)
val DeepEmerald = Color(0xFF0B6623)
val SoftGold = Color(0xFFD4AF37)
val SoftGoldBg = Color(0xFFFFF9E6)

data class PrayerInfo(
    val id: String,
    val nameEnglish: String,
    val nameArabic: String,
    val icon: ImageVector,
    val officialAbuDhabiTime: String,
    val description: String
)

val FIVE_PRAYERS = listOf(
    PrayerInfo("fajr", "Fajr", "فجر", Icons.Default.WbTwilight, "04:53", "Dawn prayer (Abu Dhabi Awqaf)"),
    PrayerInfo("dhuhr", "Dhuhr", "ظهر", Icons.Default.WbSunny, "12:18", "Midday prayer (Abu Dhabi Awqaf)"),
    PrayerInfo("asr", "Asr", "عصر", Icons.Default.AccessTime, "15:43", "Late afternoon prayer (Abu Dhabi Awqaf)"),
    PrayerInfo("maghrib", "Maghrib", "مغرب", Icons.Default.WbTwilight, "18:15", "Sunset prayer (Abu Dhabi Awqaf)"),
    PrayerInfo("isha", "Isha", "عشاء", Icons.Default.Nightlight, "19:45", "Night prayer (Abu Dhabi Awqaf)")
)

/**
 * DailyPrayerTrackerComponent
 *
 * Comprehensive Islamic spiritual tracker:
 * - Uses attached 'Azan.mp3' file for all Azan notifications and interactive playback
 * - Location-based service utilizing GPS coordinates to dynamically fetch live prayer timings
 *   from assabile.com (official Abu Dhabi Awqaf schedule method) without fixed values
 * - 5 Daily Prayers (Fajr, Dhuhr, Asr, Maghrib, Isha) with live dynamic timings & Azan notification toggles
 * - Zikr & Remembrance with fixed counts: 33 SubhanAllah, 33 Alhamdulillah, 34 Allahu Akbar,
 *   Durood-e-Sharif, 100 La ilaha illallah, and 100 Astaghfirullah
 * - Quran Recitation portion & counters: "Surah / Portion Recited", Pages, Juz Number, and Duration
 * - Dedicated "Save" button to log persistent daily records
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DailyPrayerTrackerComponent(
    selectedDate: String,
    initialRecord: DailyPrayerRecord?,
    onSaveRecord: (DailyPrayerRecord, (Boolean, String) -> Unit) -> Unit,
    targetSection: String? = null,
    onZikrPositioned: ((Int) -> Unit)? = null,
    onRadioPositioned: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isAzanPlaying by AzanAudioPlayer.isPlaying.collectAsStateWithLifecycle()
    val activeAzanPrayer by AzanAudioPlayer.activePrayer.collectAsStateWithLifecycle()
    val azanProgress by AzanAudioPlayer.progressFraction.collectAsStateWithLifecycle()

    val zikrBringIntoViewRequester = remember { BringIntoViewRequester() }
    val radioBringIntoViewRequester = remember { BringIntoViewRequester() }

    // Scroll directly to Zikr & Remembrance or Quran Live Radio section when triggered
    LaunchedEffect(targetSection) {
        if (targetSection == "zikr") {
            kotlinx.coroutines.delay(180)
            try {
                zikrBringIntoViewRequester.bringIntoView()
            } catch (_: Exception) {}
        } else if (targetSection == "quran_live" || targetSection == "radio") {
            kotlinx.coroutines.delay(180)
            try {
                radioBringIntoViewRequester.bringIntoView()
            } catch (_: Exception) {}
        }
    }

    val locationService = remember(context) { LocationPrayerService(context) }
    var isFetchingLiveTimes by remember { mutableStateOf(false) }
    var liveTimesInfo by remember { mutableStateOf<LivePrayerTimesResult?>(null) }
    var liveTimesError by remember { mutableStateOf<String?>(null) }
    // Prayer Completion states
    var fajrDone by remember { mutableStateOf(initialRecord?.fajrCompleted ?: false) }
    var dhuhrDone by remember { mutableStateOf(initialRecord?.dhuhrCompleted ?: false) }
    var asrDone by remember { mutableStateOf(initialRecord?.asrCompleted ?: false) }
    var maghribDone by remember { mutableStateOf(initialRecord?.maghribCompleted ?: false) }
    var ishaDone by remember { mutableStateOf(initialRecord?.ishaCompleted ?: false) }

    // Live Prayer Timings (dynamically fetched, initialized with record or defaults)
    var fajrTime by remember { mutableStateOf(initialRecord?.fajrTime ?: "04:53") }
    var dhuhrTime by remember { mutableStateOf(initialRecord?.dhuhrTime ?: "12:18") }
    var asrTime by remember { mutableStateOf(initialRecord?.asrTime ?: "15:43") }
    var maghribTime by remember { mutableStateOf(initialRecord?.maghribTime ?: "18:15") }
    var ishaTime by remember { mutableStateOf(initialRecord?.ishaTime ?: "19:45") }

    // Automated Azan playback toggle states
    var fajrAzan by remember { mutableStateOf(initialRecord?.fajrAzanEnabled ?: true) }
    var dhuhrAzan by remember { mutableStateOf(initialRecord?.dhuhrAzanEnabled ?: true) }
    var asrAzan by remember { mutableStateOf(initialRecord?.asrAzanEnabled ?: true) }
    var maghribAzan by remember { mutableStateOf(initialRecord?.maghribAzanEnabled ?: true) }
    var ishaAzan by remember { mutableStateOf(initialRecord?.ishaAzanEnabled ?: true) }

    // Zikr & Remembrance states with fixed counts
    var subhanAllah by remember { mutableIntStateOf(initialRecord?.subhanAllahCount ?: 33) }
    var alhamdulillah by remember { mutableIntStateOf(initialRecord?.alhamdulillahCount ?: 33) }
    var allahuAkbar by remember { mutableIntStateOf(initialRecord?.allahuAkbarCount ?: 34) }
    var duroodSharif by remember { mutableIntStateOf(initialRecord?.duroodSharifCount ?: 0) }
    var laIlahaIllallah by remember { mutableIntStateOf(initialRecord?.laIlahaIllallahCount ?: 100) }
    var astaghfirullah by remember { mutableIntStateOf(initialRecord?.astaghfirullahCount ?: 100) }

    var activeZikrPreset by remember { mutableStateOf("SubhanAllah") }
    var morningAdhkarDone by remember { mutableStateOf(initialRecord?.morningAdhkarDone ?: false) }
    var eveningAdhkarDone by remember { mutableStateOf(initialRecord?.eveningAdhkarDone ?: false) }

    // Tilawat (Quran Recitation) states - retained fields
    var tilawatSurah by remember { mutableStateOf(initialRecord?.tilawatSurah ?: "Surah Al-Mulk") }
    var tilawatJuz by remember { mutableIntStateOf(initialRecord?.tilawatJuz ?: 1) }
    var tilawatPages by remember { mutableIntStateOf(initialRecord?.tilawatPages ?: 4) }
    var tilawatMinutes by remember { mutableIntStateOf(initialRecord?.tilawatDurationMinutes ?: 15) }

    var saveFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // Initialize notification channel with attached Azan.mp3 on launch
    LaunchedEffect(Unit) {
        AzanAudioPlayer.initNotificationChannel(context)
    }

    // Synchronize when initialRecord or selectedDate changes
    LaunchedEffect(initialRecord, selectedDate) {
        if (initialRecord != null) {
            fajrDone = initialRecord.fajrCompleted
            dhuhrDone = initialRecord.dhuhrCompleted
            asrDone = initialRecord.asrCompleted
            maghribDone = initialRecord.maghribCompleted
            ishaDone = initialRecord.ishaCompleted

            fajrTime = initialRecord.fajrTime
            dhuhrTime = initialRecord.dhuhrTime
            asrTime = initialRecord.asrTime
            maghribTime = initialRecord.maghribTime
            ishaTime = initialRecord.ishaTime

            fajrAzan = initialRecord.fajrAzanEnabled
            dhuhrAzan = initialRecord.dhuhrAzanEnabled
            asrAzan = initialRecord.asrAzanEnabled
            maghribAzan = initialRecord.maghribAzanEnabled
            ishaAzan = initialRecord.ishaAzanEnabled

            subhanAllah = initialRecord.subhanAllahCount
            alhamdulillah = initialRecord.alhamdulillahCount
            allahuAkbar = initialRecord.allahuAkbarCount
            duroodSharif = initialRecord.duroodSharifCount
            laIlahaIllallah = initialRecord.laIlahaIllallahCount
            astaghfirullah = initialRecord.astaghfirullahCount
            morningAdhkarDone = initialRecord.morningAdhkarDone
            eveningAdhkarDone = initialRecord.eveningAdhkarDone

            tilawatSurah = initialRecord.tilawatSurah
            tilawatJuz = initialRecord.tilawatJuz
            tilawatPages = initialRecord.tilawatPages
            tilawatMinutes = initialRecord.tilawatDurationMinutes
            saveFeedbackMessage = null
        }
    }

    // Function to trigger live GPS fetch from assabile.com
    fun fetchLivePrayerTimingsFromAssabile(customLat: Double? = null, customLng: Double? = null) {
        scope.launch {
            isFetchingLiveTimes = true
            liveTimesError = null
            try {
                val result = locationService.fetchLivePrayerTimes(customLat, customLng)
                if (result.isSuccess) {
                    val data = result.getOrThrow()
                    liveTimesInfo = data

                    // Dynamically update times without fixed values
                    fajrTime = data.fajr
                    dhuhrTime = data.dhuhr
                    asrTime = data.asr
                    maghribTime = data.maghrib
                    ishaTime = data.isha

                    // Auto-persist dynamically fetched times
                    val recordToSave = DailyPrayerRecord(
                        date = selectedDate,
                        fajrCompleted = fajrDone,
                        dhuhrCompleted = dhuhrDone,
                        asrCompleted = asrDone,
                        maghribCompleted = maghribDone,
                        ishaCompleted = ishaDone,
                        fajrTime = data.fajr,
                        dhuhrTime = data.dhuhr,
                        asrTime = data.asr,
                        maghribTime = data.maghrib,
                        ishaTime = data.isha,
                        fajrAzanEnabled = fajrAzan,
                        dhuhrAzanEnabled = dhuhrAzan,
                        asrAzanEnabled = asrAzan,
                        maghribAzanEnabled = maghribAzan,
                        ishaAzanEnabled = ishaAzan,
                        subhanAllahCount = subhanAllah,
                        alhamdulillahCount = alhamdulillah,
                        allahuAkbarCount = allahuAkbar,
                        duroodSharifCount = duroodSharif,
                        laIlahaIllallahCount = laIlahaIllallah,
                        astaghfirullahCount = astaghfirullah,
                        customZikrCount = duroodSharif,
                        customZikrLabel = "Durood-e-Sharif",
                        morningAdhkarDone = morningAdhkarDone,
                        eveningAdhkarDone = eveningAdhkarDone,
                        tilawatCompleted = tilawatPages > 0,
                        tilawatSurah = tilawatSurah,
                        tilawatJuz = tilawatJuz,
                        tilawatPages = tilawatPages,
                        tilawatDurationMinutes = tilawatMinutes,
                        tilawatNotes = ""
                    )
                    onSaveRecord(recordToSave) { _, _ -> }
                } else {
                    val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to fetch live times"
                    liveTimesError = err
                }
            } catch (e: Exception) {
                liveTimesError = e.localizedMessage
            } finally {
                isFetchingLiveTimes = false
            }
        }
    }

    // Permission launcher for Location (Standard Google / Android runtime prompt: Allow / Don't allow)
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
        if (granted) {
            // Permission granted: background fetch via Google GPS
            fetchLivePrayerTimingsFromAssabile()
        } else {
            // Permission denied ("Don't allow"): default to official Abu Dhabi GPS coordinates in background
            fetchLivePrayerTimingsFromAssabile(LocationPrayerService.ABU_DHABI_LAT, LocationPrayerService.ABU_DHABI_LNG)
        }
    }

    // Permission launcher for Android 13+ Notifications
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (locationService.hasLocationPermission()) {
            fetchLivePrayerTimingsFromAssabile()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Automated Azan Notification & Playback Monitor
    var lastNotifiedPrayerMinute by remember { mutableStateOf("") }
    LaunchedEffect(selectedDate, fajrAzan, dhuhrAzan, asrAzan, maghribAzan, ishaAzan, fajrTime, dhuhrTime, asrTime, maghribTime, ishaTime, liveTimesInfo) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            val nowStr = timeFormat.format(Date())
            if (nowStr != lastNotifiedPrayerMinute) {
                val prayerMatches = listOf(
                    Triple("Fajr", fajrTime, fajrAzan),
                    Triple("Dhuhr", dhuhrTime, dhuhrAzan),
                    Triple("Asr", asrTime, asrAzan),
                    Triple("Maghrib", maghribTime, maghribAzan),
                    Triple("Isha", ishaTime, ishaAzan)
                )
                for ((pName, pTime, pEnabled) in prayerMatches) {
                    if (nowStr == pTime && pEnabled && !AzanAudioPlayer.isPlaying.value) {
                        lastNotifiedPrayerMinute = nowStr
                        val loc = liveTimesInfo?.cityName ?: "Abu Dhabi"

                        // Single coordinated trigger ensures no duplicate notifications or overlapping audio
                        AzanAudioPlayer.triggerPrayerCall(context, scope, pName, pTime, loc)

                        Toast.makeText(context, "🕌 Azan: It is time for $pName prayer ($pTime)", Toast.LENGTH_LONG).show()
                        break
                    }
                }
            }
            delay(15000) // check every 15 seconds
        }
    }

    // Helper to open TimePickerDialog
    fun openTimePicker(currentTimeStr: String, onTimeSelected: (String) -> Unit) {
        val parts = currentTimeStr.split(":")
        val defaultHour = parts.getOrNull(0)?.toIntOrNull() ?: 12
        val defaultMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val formatted = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                onTimeSelected(formatted)
            },
            defaultHour,
            defaultMinute,
            true
        ).show()
    }

    // Calculate live upcoming prayer indicator
    val currentTimeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val nowTimeStr = remember { currentTimeFormat.format(Date()) }

    fun calculateLivePrayerStatus(): Pair<String, String> {
        val timings = listOf(
            Triple("Fajr", fajrTime, fajrDone),
            Triple("Dhuhr", dhuhrTime, dhuhrDone),
            Triple("Asr", asrTime, asrDone),
            Triple("Maghrib", maghribTime, maghribDone),
            Triple("Isha", ishaTime, ishaDone)
        )
        for (item in timings) {
            if (nowTimeStr < item.second) {
                return Pair(item.first, item.second)
            }
        }
        return Pair("Fajr (Tomorrow)", fajrTime)
    }

    val (nextPrayerName, nextPrayerTime) = calculateLivePrayerStatus()

    val totalCompletedCount = (if (fajrDone) 1 else 0) +
            (if (dhuhrDone) 1 else 0) +
            (if (asrDone) 1 else 0) +
            (if (maghribDone) 1 else 0) +
            (if (ishaDone) 1 else 0)

    val prayerProgress = totalCompletedCount / 5f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_prayer_tracker_component"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Islamic Header & Live GPS Assabile Synchronization Card ---
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, EmeraldGreen.copy(alpha = 0.4f)),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(EmeraldGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🕌",
                                fontSize = 24.sp
                            )
                        }

                        Column {
                            Text(
                                text = "Namaz Status",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("namaz_status_header")
                            )
                        }
                    }

                    // Progress Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (totalCompletedCount == 5) EmeraldGreen.copy(alpha = 0.15f) else GoogleYellow.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (totalCompletedCount == 5) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (totalCompletedCount == 5) EmeraldGreen else Color(0xFFC07000),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "$totalCompletedCount / 5 Done",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (totalCompletedCount == 5) EmeraldGreen else Color(0xFFC87800),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                // Progress Bar
                LinearProgressIndicator(
                    progress = { prayerProgress },
                    color = EmeraldGreen,
                    trackColor = EmeraldGreen.copy(alpha = 0.15f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )

                // Prayer Timing Status Banner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SoftGoldBg,
                    border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = DeepEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Next: $nextPrayerName at $nextPrayerTime",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF332000)
                                )
                            }
                        }

                        // Audio Player Live Controller Banner (if Azan is playing)
                        if (isAzanPlaying) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = GoogleRed.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, GoogleRed.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = GoogleRed, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "Playing Azan: ${activeAzanPrayer ?: "Call to Prayer"}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoogleRed
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = { AzanAudioPlayer.skipToLastFiveSeconds() },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier
                                                .height(28.dp)
                                                .testTag("btn_skip_azan_five_sec")
                                        ) {
                                            Text("⚡ Skip to Last 5s", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = { AzanAudioPlayer.stop() },
                                            colors = ButtonDefaults.buttonColors(containerColor = GoogleRed),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier
                                                .height(28.dp)
                                                .testTag("btn_stop_azan")
                                        ) {
                                            Icon(Icons.Default.Stop, contentDescription = "Stop Azan", modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Stop", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        } else {
                            // Subtle helper to simulate Adhan completion and test 30s countdown + zero-click Bayan
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        AzanAudioPlayer.simulateAdhanCompletion(context, nextPrayerName)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .height(26.dp)
                                        .testTag("btn_test_30s_flow")
                                ) {
                                    Text("⚡ Test Post-Adhan 30s Flow", fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }



        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Fajr
            PrayerRowCard(
                prayer = FIVE_PRAYERS[0],
                time = fajrTime,
                isCompleted = fajrDone,
                isAzanEnabled = fajrAzan,
                isPlayingAzan = isAzanPlaying && activeAzanPrayer == "Fajr",
                onPlayAzanClick = {
                    if (isAzanPlaying && activeAzanPrayer == "Fajr") AzanAudioPlayer.stop()
                    else AzanAudioPlayer.playAzan(context, scope, "Fajr")
                },
                onTimeClick = {
                    openTimePicker(fajrTime) { fajrTime = it }
                },
                onCompletedChange = { fajrDone = it },
                onAzanToggle = { fajrAzan = it },
                isCurrent = nextPrayerName == "Fajr"
            )

            // Dhuhr
            PrayerRowCard(
                prayer = FIVE_PRAYERS[1],
                time = dhuhrTime,
                isCompleted = dhuhrDone,
                isAzanEnabled = dhuhrAzan,
                isPlayingAzan = isAzanPlaying && activeAzanPrayer == "Dhuhr",
                onPlayAzanClick = {
                    if (isAzanPlaying && activeAzanPrayer == "Dhuhr") AzanAudioPlayer.stop()
                    else AzanAudioPlayer.playAzan(context, scope, "Dhuhr")
                },
                onTimeClick = {
                    openTimePicker(dhuhrTime) { dhuhrTime = it }
                },
                onCompletedChange = { dhuhrDone = it },
                onAzanToggle = { dhuhrAzan = it },
                isCurrent = nextPrayerName == "Dhuhr"
            )

            // Asr
            PrayerRowCard(
                prayer = FIVE_PRAYERS[2],
                time = asrTime,
                isCompleted = asrDone,
                isAzanEnabled = asrAzan,
                isPlayingAzan = isAzanPlaying && activeAzanPrayer == "Asr",
                onPlayAzanClick = {
                    if (isAzanPlaying && activeAzanPrayer == "Asr") AzanAudioPlayer.stop()
                    else AzanAudioPlayer.playAzan(context, scope, "Asr")
                },
                onTimeClick = {
                    openTimePicker(asrTime) { asrTime = it }
                },
                onCompletedChange = { asrDone = it },
                onAzanToggle = { asrAzan = it },
                isCurrent = nextPrayerName == "Asr"
            )

            // Maghrib
            PrayerRowCard(
                prayer = FIVE_PRAYERS[3],
                time = maghribTime,
                isCompleted = maghribDone,
                isAzanEnabled = maghribAzan,
                isPlayingAzan = isAzanPlaying && activeAzanPrayer == "Maghrib",
                onPlayAzanClick = {
                    if (isAzanPlaying && activeAzanPrayer == "Maghrib") AzanAudioPlayer.stop()
                    else AzanAudioPlayer.playAzan(context, scope, "Maghrib")
                },
                onTimeClick = {
                    openTimePicker(maghribTime) { maghribTime = it }
                },
                onCompletedChange = { maghribDone = it },
                onAzanToggle = { maghribAzan = it },
                isCurrent = nextPrayerName == "Maghrib"
            )

            // Isha
            PrayerRowCard(
                prayer = FIVE_PRAYERS[4],
                time = ishaTime,
                isCompleted = ishaDone,
                isAzanEnabled = ishaAzan,
                isPlayingAzan = isAzanPlaying && activeAzanPrayer == "Isha",
                onPlayAzanClick = {
                    if (isAzanPlaying && activeAzanPrayer == "Isha") AzanAudioPlayer.stop()
                    else AzanAudioPlayer.playAzan(context, scope, "Isha")
                },
                onTimeClick = {
                    openTimePicker(ishaTime) { ishaTime = it }
                },
                onCompletedChange = { ishaDone = it },
                onAzanToggle = { ishaAzan = it },
                isCurrent = nextPrayerName == "Isha"
            )
        }

        // --- 3. Zikr & Remembrance Section (with fixed counts) ---
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    val y = coords.positionInParent().y.toInt()
                    if (y > 0) onZikrPositioned?.invoke(y)
                }
                .bringIntoViewRequester(zikrBringIntoViewRequester)
                .testTag("zikr_tracker_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = "Zikr & Remembrance",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Zikr & Remembrance",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "الأذكار",
                            fontSize = 13.sp,
                            color = DeepEmerald,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Reset active Zikr button
                    IconButton(
                        onClick = {
                            when (activeZikrPreset) {
                                "SubhanAllah" -> subhanAllah = 0
                                "Alhamdulillah" -> alhamdulillah = 0
                                "Allahu Akbar" -> allahuAkbar = 0
                                "Durood" -> duroodSharif = 0
                                "La ilaha illallah" -> laIlahaIllallah = 0
                                "Astaghfirullah" -> astaghfirullah = 0
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Zikr",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Morning & Evening Adhkar completion chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = morningAdhkarDone,
                        onClick = { morningAdhkarDone = !morningAdhkarDone },
                        leadingIcon = {
                            Icon(
                                imageVector = if (morningAdhkarDone) Icons.Default.CheckCircle else Icons.Default.WbSunny,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (morningAdhkarDone) EmeraldGreen else GoogleYellow
                            )
                        },
                        label = { Text("Morning Adhkar (أذكار الصباح)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldGreen.copy(alpha = 0.15f),
                            selectedLabelColor = EmeraldGreen
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("morning_adhkar_chip")
                    )

                    FilterChip(
                        selected = eveningAdhkarDone,
                        onClick = { eveningAdhkarDone = !eveningAdhkarDone },
                        leadingIcon = {
                            Icon(
                                imageVector = if (eveningAdhkarDone) Icons.Default.CheckCircle else Icons.Default.Nightlight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (eveningAdhkarDone) EmeraldGreen else Color(0xFF673AB7)
                            )
                        },
                        label = { Text("Evening Adhkar (أذكار المساء)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldGreen.copy(alpha = 0.15f),
                            selectedLabelColor = EmeraldGreen
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("evening_adhkar_chip")
                    )
                }

                // Presets with fixed counts requested:
                // 33 SubhanAllah, 33 Alhamdulillah, 34 Allahu Akbar, Durood-e-Sharif, 100 La ilaha illallah, 100 Astaghfirullah
                val zikrPresets = listOf(
                    Triple("SubhanAllah", "33 SubhanAllah", subhanAllah),
                    Triple("Alhamdulillah", "33 Alhamdulillah", alhamdulillah),
                    Triple("Allahu Akbar", "34 Allahu Akbar", allahuAkbar),
                    Triple("Durood", "Durood-e-Sharif", duroodSharif),
                    Triple("La ilaha illallah", "100 La ilaha illallah", laIlahaIllallah),
                    Triple("Astaghfirullah", "100 Astaghfirullah", astaghfirullah)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    zikrPresets.forEach { (id, label, count) ->
                        val isSelected = activeZikrPreset == id
                        FilterChip(
                            selected = isSelected,
                            onClick = { activeZikrPreset = id },
                            label = { Text("$label ($count)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldGreen.copy(alpha = 0.15f),
                                selectedLabelColor = EmeraldGreen
                            )
                        )
                    }
                }

                // Active Zikr counter & details
                val (currentCount, targetCount, arabicTitle, transliteration) = when (activeZikrPreset) {
                    "SubhanAllah" -> Quad(subhanAllah, 33, "سُبْحَانَ اللَّهِ", "33 SubhanAllah (Glory be to Allah)")
                    "Alhamdulillah" -> Quad(alhamdulillah, 33, "الْحَمْدُ لِلَّهِ", "33 Alhamdulillah (Praise be to Allah)")
                    "Allahu Akbar" -> Quad(allahuAkbar, 34, "اللَّهُ أَكْبَرُ", "34 Allahu Akbar (Allah is the Greatest)")
                    "Durood" -> Quad(duroodSharif, 100, "اللَّهُمَّ صَلِّ عَلَى سَيِّدِنَا مُحَمَّدٍ", "Durood-e-Sharif (Blessings on the Prophet)")
                    "La ilaha illallah" -> Quad(laIlahaIllallah, 100, "لَا إِلٰهَ إِلَّا اللَّهُ", "100 La ilaha illallah (None worthy of worship except Allah)")
                    else -> Quad(astaghfirullah, 100, "أَسْتَغْفِرُ اللَّهَ", "100 Astaghfirullah (I seek forgiveness from Allah)")
                }

                val isTargetReached = currentCount >= targetCount

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isTargetReached) EmeraldGreen.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, if (isTargetReached) EmeraldGreen else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = arabicTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepEmerald
                            )
                            Text(
                                text = transliteration,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Target: $targetCount  •  Progress: $currentCount / $targetCount",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isTargetReached) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isTargetReached) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Target reached",
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        // Tap Counter Button (+1)
                        Button(
                            onClick = {
                                when (activeZikrPreset) {
                                    "SubhanAllah" -> subhanAllah++
                                    "Alhamdulillah" -> alhamdulillah++
                                    "Allahu Akbar" -> allahuAkbar++
                                    "Durood" -> duroodSharif++
                                    "La ilaha illallah" -> laIlahaIllallah++
                                    "Astaghfirullah" -> astaghfirullah++
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTargetReached) DeepEmerald else EmeraldGreen
                            ),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(60.dp)
                                .testTag("tasbeeh_count_btn")
                        ) {
                            Text(
                                text = "+1",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // --- 4. Quran Recitation Portion & Counters Section ---
        // (Retained only: "Surah / Portion Recited" input field and counters for Pages, Juz Number, and Duration.)
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tilawat_tracker_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // "Surah / Portion Recited" input field
                OutlinedTextField(
                    value = tilawatSurah,
                    onValueChange = { tilawatSurah = it },
                    label = { Text("Surah / Portion Recited") },
                    placeholder = { Text("e.g. Surah Al-Mulk, Surah Ya-Sin, Juz 30...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tilawat_surah_input")
                )

                // Counters for Pages, Juz Number, and Duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pages counter
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Pages", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = { tilawatPages = (tilawatPages - 1).coerceAtLeast(0) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease pages", modifier = Modifier.size(16.dp))
                                }
                                Text("$tilawatPages", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                IconButton(
                                    onClick = { tilawatPages++ },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase pages", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Juz Number counter
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Juz Number", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = { tilawatJuz = (tilawatJuz - 1).coerceIn(1, 30) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease juz", modifier = Modifier.size(16.dp))
                                }
                                Text("Juz $tilawatJuz", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                IconButton(
                                    onClick = { tilawatJuz = (tilawatJuz + 1).coerceIn(1, 30) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase juz", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Duration (mins) counter
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Duration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = { tilawatMinutes = (tilawatMinutes - 5).coerceAtLeast(5) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease minutes", modifier = Modifier.size(16.dp))
                                }
                                Text("${tilawatMinutes}m", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                IconButton(
                                    onClick = { tilawatMinutes += 5 },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase minutes", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 5. Save Button to Log Daily Records ---
        if (saveFeedbackMessage != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = EmeraldGreen.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = saveFeedbackMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = DeepEmerald
                    )
                }
            }
        }

        Button(
            onClick = {
                isSaving = true
                val record = DailyPrayerRecord(
                    date = selectedDate,
                    fajrCompleted = fajrDone,
                    dhuhrCompleted = dhuhrDone,
                    asrCompleted = asrDone,
                    maghribCompleted = maghribDone,
                    ishaCompleted = ishaDone,
                    fajrTime = fajrTime,
                    dhuhrTime = dhuhrTime,
                    asrTime = asrTime,
                    maghribTime = maghribTime,
                    ishaTime = ishaTime,
                    fajrAzanEnabled = fajrAzan,
                    dhuhrAzanEnabled = dhuhrAzan,
                    asrAzanEnabled = asrAzan,
                    maghribAzanEnabled = maghribAzan,
                    ishaAzanEnabled = ishaAzan,
                    subhanAllahCount = subhanAllah,
                    alhamdulillahCount = alhamdulillah,
                    allahuAkbarCount = allahuAkbar,
                    duroodSharifCount = duroodSharif,
                    laIlahaIllallahCount = laIlahaIllallah,
                    astaghfirullahCount = astaghfirullah,
                    customZikrCount = duroodSharif,
                    customZikrLabel = "Durood-e-Sharif",
                    morningAdhkarDone = morningAdhkarDone,
                    eveningAdhkarDone = eveningAdhkarDone,
                    tilawatCompleted = tilawatPages > 0,
                    tilawatSurah = tilawatSurah,
                    tilawatJuz = tilawatJuz,
                    tilawatPages = tilawatPages,
                    tilawatDurationMinutes = tilawatMinutes,
                    tilawatNotes = ""
                )

                onSaveRecord(record) { _, msg ->
                    isSaving = false
                    saveFeedbackMessage = msg
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_prayer_record_btn")
        ) {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Save Prayer & Spiritual Records",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- Dedicated Quran Live Radio Section (ExoPlayer & Media3 Session) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(radioBringIntoViewRequester)
                .onGloballyPositioned { coordinates ->
                    onRadioPositioned?.invoke(coordinates.positionInParent().y.toInt())
                }
        ) {
            QuranLiveRadioSection()
        }
    }


}

private data class Quad(
    val count: Int,
    val target: Int,
    val arabic: String,
    val title: String
)

/**
 * Individual Prayer Row Card with:
 * - Prayer English & Arabic name
 * - Live dynamic timing badge (clickable to change)
 * - Play/Stop Azan.mp3 button (using attached authentic Azan audio)
 * - Automated Azan notification toggle switch
 * - Prayed Checkbox
 */
@Composable
fun PrayerRowCard(
    prayer: PrayerInfo,
    time: String,
    isCompleted: Boolean,
    isAzanEnabled: Boolean,
    isPlayingAzan: Boolean,
    onPlayAzanClick: () -> Unit,
    onTimeClick: () -> Unit,
    onCompletedChange: (Boolean) -> Unit,
    onAzanToggle: (Boolean) -> Unit,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isPlayingAzan) GoogleRed
        else if (isCurrent) EmeraldGreen
        else if (isCompleted) EmeraldGreen.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.outlineVariant,
        label = "prayer_card_border"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isPlayingAzan) GoogleRed.copy(alpha = 0.05f)
        else if (isCompleted) EmeraldGreen.copy(alpha = 0.06f)
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isPlayingAzan || isCurrent) 1.8.dp else 1.dp,
            color = borderColor
        ),
        shadowElevation = if (isCurrent || isPlayingAzan) 2.dp else 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("prayer_row_${prayer.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Icon + Prayer Name & Arabic + Dynamic Timing badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPlayingAzan) GoogleRed.copy(alpha = 0.18f)
                            else if (isCompleted) EmeraldGreen.copy(alpha = 0.18f)
                            else if (isCurrent) SoftGold.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = prayer.icon,
                        contentDescription = prayer.nameEnglish,
                        tint = if (isPlayingAzan) GoogleRed
                        else if (isCompleted) EmeraldGreen
                        else if (isCurrent) Color(0xFFC07000)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = prayer.nameEnglish,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = prayer.nameArabic,
                            fontSize = 12.sp,
                            color = DeepEmerald,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Clickable Live timing badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onTimeClick)
                            .testTag("prayer_time_${prayer.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit timing",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = time,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Live",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepEmerald
                            )
                        }
                    }
                }
            }

            // Right: Play Azan.mp3 Button + Automated Azan Notification Toggle + Prayed Checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Play attached authentic Azan.mp3 audio
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isPlayingAzan) GoogleRed.copy(alpha = 0.15f) else EmeraldGreen.copy(alpha = 0.10f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onPlayAzanClick)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("play_azan_${prayer.id}")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isPlayingAzan) Icons.Default.Stop else Icons.Default.VolumeUp,
                            contentDescription = "Play Azan.mp3 for ${prayer.nameEnglish}",
                            tint = if (isPlayingAzan) GoogleRed else EmeraldGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isPlayingAzan) "Stop" else "Azan",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPlayingAzan) GoogleRed else EmeraldGreen
                        )
                    }
                }

                // Automated Azan notification toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onAzanToggle(!isAzanEnabled) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("toggle_azan_${prayer.id}")
                ) {
                    Icon(
                        imageVector = if (isAzanEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                        contentDescription = "Automated Azan notification toggle",
                        tint = if (isAzanEnabled) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = if (isAzanEnabled) "Alert" else "Muted",
                        fontSize = 10.sp,
                        color = if (isAzanEnabled) DeepEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isAzanEnabled) FontWeight.Bold else FontWeight.Normal
                    )
                }

                // Prayed Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onCompletedChange(!isCompleted) }
                        .padding(start = 2.dp)
                ) {
                    Checkbox(
                        checked = isCompleted,
                        onCheckedChange = onCompletedChange,
                        colors = CheckboxDefaults.colors(
                            checkedColor = EmeraldGreen,
                            checkmarkColor = Color.White
                        ),
                        modifier = Modifier.testTag("prayer_checkbox_${prayer.id}")
                    )
                }
            }
        }
    }
}
