package com.example.ui.components

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.GoogleRed
import com.example.ui.theme.GoogleYellow

import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Sync

@Composable
fun GoogleHeader(
    currentDateText: String,
    selectedDateStr: String,
    datesWithTasks: Set<String>,
    datesWithDiaryEntries: Set<String>,
    isToday: Boolean,
    onOpenCalendarPicker: () -> Unit,
    onSelectDate: (String) -> Unit,
    onToday: () -> Unit,
    onSyncStatusClick: () -> Unit,
    onResetTemplate: () -> Unit,
    currentUser: com.google.firebase.auth.FirebaseUser? = null,
    isSyncing: Boolean = false,
    onSignInClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding() // CRITICAL: Prevents overlap with status bar icons & clock
                .padding(top = 10.dp, bottom = 6.dp)
        ) {
            // Top Bar: Prominent App Title & Sync Status Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clear, Prominent "Imtiaz Lifestyle" branding
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("app_brand_header")
                ) {
                    // Google 4-Color Dots Logo
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Box(Modifier.size(9.dp).clip(CircleShape).background(GoogleBlue))
                        Box(Modifier.size(9.dp).clip(CircleShape).background(GoogleRed))
                        Box(Modifier.size(9.dp).clip(CircleShape).background(GoogleYellow))
                        Box(Modifier.size(9.dp).clip(CircleShape).background(GoogleGreen))
                    }

                    Column {
                        Text(
                            text = "Imtiaz Lifestyle",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 19.sp,
                                letterSpacing = (-0.2).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Diary & 24h Plan",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Sync Badge & Reset Template Icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentUser == null) {
                        Surface(
                            modifier = Modifier
                                .testTag("header_sign_in_btn")
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSignInClick?.invoke() ?: onSyncStatusClick() },
                            color = GoogleBlue.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoogleBlue.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Login,
                                    contentDescription = "Sign in with Google",
                                    tint = GoogleBlue,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Sign in",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = GoogleBlue,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier
                                .testTag("sync_status_badge")
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSyncStatusClick() },
                            color = if (isSyncing) GoogleBlue.copy(alpha = 0.12f) else GoogleGreen.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(if (isSyncing) GoogleBlue else GoogleGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val initial = (currentUser.email ?: currentUser.displayName ?: "U").take(1).uppercase()
                                    Text(
                                        text = initial,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(5.dp))
                                Icon(
                                    imageVector = if (isSyncing) Icons.Default.Sync else Icons.Default.CloudDone,
                                    contentDescription = if (isSyncing) "Syncing" else "Synced",
                                    tint = if (isSyncing) GoogleBlue else GoogleGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onResetTemplate,
                        modifier = Modifier
                            .testTag("reset_template_button")
                            .size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Template Day",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Google Calendar Style Date Dropdown Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Dropdown Date Selector Button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .testTag("date_dropdown_picker_btn")
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onOpenCalendarPicker() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Select Date",
                            tint = GoogleBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentDateText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Open Calendar",
                            tint = GoogleBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Quick "Today" Jump Button
                if (!isToday) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = GoogleBlue.copy(alpha = 0.12f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onToday() }
                            .testTag("header_jump_today_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Today,
                                contentDescription = null,
                                tint = GoogleBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = GoogleBlue
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Weekly Calendar Strip for 1-tap switching across current week days
            WeeklyCalendarStrip(
                selectedDateStr = selectedDateStr,
                datesWithTasks = datesWithTasks,
                datesWithDiaryEntries = datesWithDiaryEntries,
                onSelectDate = onSelectDate,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
