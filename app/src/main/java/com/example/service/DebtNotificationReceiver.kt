package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class DebtNotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DebtNotificationRcvr"
        const val CHANNEL_ID = "debt_reminders_channel"
        const val ACTION_DEBT_ALARM = "com.example.service.action.DEBT_ALARM"

        const val EXTRA_RECORD_ID = "extra_record_id"
        const val EXTRA_CONTACT_NAME = "extra_contact_name"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_IS_RECEIVABLE = "extra_is_receivable"
        const val EXTRA_DUE_DATE = "extra_due_date"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val contactName = intent.getStringExtra(EXTRA_CONTACT_NAME) ?: "Financial Contact"
        val amount = intent.getStringExtra(EXTRA_AMOUNT) ?: ""
        val isReceivable = intent.getBooleanExtra(EXTRA_IS_RECEIVABLE, true)
        val dueDate = intent.getStringExtra(EXTRA_DUE_DATE) ?: "Today"

        Log.i(TAG, "Debt reminder alarm triggered for $contactName, amount $amount, due: $dueDate")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Debt & Payment Schedules",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Smart reminders and alarms for loan collections and due repayments"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to_tab", "MONEY")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            contactName.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isReceivable) {
            "⏰ Payment Collection Due: $contactName"
        } else {
            "⚠️ Repayment Schedule Due: $contactName"
        }

        val body = if (isReceivable) {
            "Reminder to collect $amount from $contactName (Scheduled for $dueDate)."
        } else {
            "Upcoming payment of $amount is due to $contactName on $dueDate."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(contactName.hashCode(), notification)
    }
}
