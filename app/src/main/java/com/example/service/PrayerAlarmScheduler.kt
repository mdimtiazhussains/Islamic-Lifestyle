package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.database.LifestyleDatabase
import com.example.data.model.DailyPrayerRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * PrayerAlarmScheduler
 *
 * Optimizes the lifestyle app for battery conservation by scheduling exact system alarms
 * for upcoming prayer / Azan times. The audio service and background sync remain COMPLETELY INACTIVE
 * outside of these intervals, preventing continuous 24/7 background CPU wakeups and battery drain.
 */
object PrayerAlarmScheduler {
    private const val TAG = "PrayerAlarmScheduler"

    fun schedulePrayerAlarms(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = LifestyleDatabase.getDatabase(context)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val todayStr = dateFormat.format(Date())
                val record = db.lifestyleDao().getPrayerRecordSync(todayStr)
                    ?: DailyPrayerRecord(date = todayStr)

                scheduleNextPrayerAlarms(context, record)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read prayer record for scheduling: ${e.message}")
                scheduleNextPrayerAlarms(context, DailyPrayerRecord(date = "today"))
            }
        }
    }

    fun scheduleNextPrayerAlarms(context: Context, record: DailyPrayerRecord) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val prayers = listOf(
            Triple("Fajr", record.fajrTime.ifBlank { "04:53" }, record.fajrAzanEnabled),
            Triple("Dhuhr", record.dhuhrTime.ifBlank { "12:18" }, record.dhuhrAzanEnabled),
            Triple("Asr", record.asrTime.ifBlank { "15:43" }, record.asrAzanEnabled),
            Triple("Maghrib", record.maghribTime.ifBlank { "18:15" }, record.maghribAzanEnabled),
            Triple("Isha", record.ishaTime.ifBlank { "19:45" }, record.ishaAzanEnabled)
        )

        val nowCal = Calendar.getInstance()
        var scheduledCount = 0

        for ((pName, pTime, isEnabled) in prayers) {
            if (!isEnabled) continue

            val parts = pTime.split(":")
            if (parts.size != 2) continue
            val hour = parts[0].toIntOrNull() ?: continue
            val minute = parts[1].toIntOrNull() ?: continue

            val targetCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // If time has already passed today, schedule for tomorrow
            if (targetCal.timeInMillis <= nowCal.timeInMillis) {
                targetCal.add(Calendar.DAY_OF_YEAR, 1)
            }

            val requestCode = pName.hashCode()
            val intent = Intent(context, AzanNotificationReceiver::class.java).apply {
                action = AzanNotificationReceiver.ACTION_PRAYER_ALARM
                putExtra(AzanNotificationReceiver.EXTRA_PRAYER_NAME, pName)
                putExtra(AzanNotificationReceiver.EXTRA_PRAYER_TIME, pTime)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        targetCal.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        targetCal.timeInMillis,
                        pendingIntent
                    )
                }
                scheduledCount++
                Log.d(TAG, "Scheduled alarm for $pName at ${targetCal.time}")
            } catch (e: SecurityException) {
                Log.w(TAG, "Exact alarm permission denied, using setAndAllowWhileIdle fallback: ${e.message}")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        targetCal.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling alarm for $pName: ${e.message}")
            }
        }

        Log.i(TAG, "Battery-conserving scheduler set $scheduledCount prayer alarms. Services remain idle until alarm triggers.")
    }
}
