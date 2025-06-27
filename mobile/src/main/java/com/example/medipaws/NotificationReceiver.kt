package com.example.medipaws

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("name") ?: "Medicine Reminder"
        val dose = intent.getStringExtra("dose") ?: ""
        val notes = intent.getStringExtra("notes") ?: ""
        val id = intent.getLongExtra("id", 0L).toInt()

        val channelId = "medipaws_reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Medicine Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(name)
            .setContentText("Dose: $dose" + if (notes.isNotBlank()) "\nNotes: $notes" else "")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Dose: $dose" + if (notes.isNotBlank()) "\nNotes: $notes" else ""))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(id, builder.build())
        }
    }
} 