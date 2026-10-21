package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.util.Log
import com.example.data.model.DebtCreditRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DebtAlarmScheduler {
    private const val TAG = "DebtAlarmScheduler"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Schedules a system alarm notification for the debt/credit record due date.
     */
    fun scheduleDebtAlarm(context: Context, record: DebtCreditRecord) {
        if (!record.hasReminderAlarm) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        try {
            val parsedDate = dateFormat.parse(record.dueDate) ?: return
            val triggerCal = Calendar.getInstance().apply {
                time = parsedDate
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }

            // If time is already past, trigger slightly later today or skip
            val triggerMillis = if (triggerCal.timeInMillis > System.currentTimeMillis()) {
                triggerCal.timeInMillis
            } else {
                System.currentTimeMillis() + 10_000L // 10s demo alert if due today/past
            }

            val intent = Intent(context, DebtNotificationReceiver::class.java).apply {
                action = DebtNotificationReceiver.ACTION_DEBT_ALARM
                putExtra(DebtNotificationReceiver.EXTRA_RECORD_ID, record.id)
                putExtra(DebtNotificationReceiver.EXTRA_CONTACT_NAME, record.contactName)
                putExtra(DebtNotificationReceiver.EXTRA_AMOUNT, record.formattedRemaining)
                putExtra(DebtNotificationReceiver.EXTRA_IS_RECEIVABLE, record.type.isReceivable)
                putExtra(DebtNotificationReceiver.EXTRA_DUE_DATE, record.dueDate)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                record.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
            Log.d(TAG, "Scheduled debt alarm for ${record.contactName} at $triggerMillis")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule debt alarm for ${record.contactName}: ${e.message}")
        }
    }

    /**
     * Cancels an existing debt reminder alarm.
     */
    fun cancelDebtAlarm(context: Context, recordId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DebtNotificationReceiver::class.java).apply {
            action = DebtNotificationReceiver.ACTION_DEBT_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            recordId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled debt alarm for record $recordId")
        }
    }

    /**
     * Launches the system Google Calendar or stock Android calendar app to add the due date event.
     */
    fun launchCalendarInsert(context: Context, record: DebtCreditRecord) {
        try {
            val parsedDate = dateFormat.parse(record.dueDate)
            val startMillis = parsedDate?.time ?: System.currentTimeMillis()
            val endMillis = startMillis + (2 * 60 * 60 * 1000L)

            val titlePrefix = if (record.type.isReceivable) "Collect Payment from" else "Repay Loan to"
            val title = "💰 $titlePrefix ${record.contactName} (${record.formattedRemaining})"
            val description = "Financial Schedule:\n" +
                    "Contact: ${record.contactName}\n" +
                    "Remaining Balance: ${record.formattedRemaining} (of ${record.formattedOriginal})\n" +
                    "Terms: ${record.interestOrTerms}\n" +
                    "Notes: ${record.notes}"

            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.Events.DESCRIPTION, description)
                putExtra(CalendarContract.Events.ALL_DAY, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Could not open system calendar: ${e.message}")
        }
    }
}
