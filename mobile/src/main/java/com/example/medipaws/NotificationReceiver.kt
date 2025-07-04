package com.example.medipaws

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import androidx.room.Room
import com.example.medipaws.EntryStatus
import android.app.PendingIntent
import android.content.SharedPreferences
import android.preference.PreferenceManager

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("name") ?: "Medicine Reminder"
        val dose = intent.getStringExtra("dose") ?: ""
        val notes = intent.getStringExtra("notes") ?: ""
        val id = intent.getLongExtra("id", 0L).toInt()
        val action = intent.getStringExtra("action")

        val db = Room.databaseBuilder(context, AppDatabase::class.java, "medipaws_db").build()
        val dao = db.medicineEntryDao()

        if (action == "DONE" || action == "LOST") {
            GlobalScope.launch {
                val entry = dao.getEntryById(id.toLong())
                if (entry != null) {
                    val newStatus = if (action == "DONE") EntryStatus.DONE else EntryStatus.LOST
                    dao.update(entry.copy(status = newStatus))
                }
            }
            return
        } else if (action == "SNOOZE") {
            // Get snooze duration from settings
            val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val snoozeMinutes = prefs.getInt("snooze_minutes", 10)
            val snoozeTime = System.currentTimeMillis() + snoozeMinutes * 60 * 1000
            val snoozeIntent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("name", name)
                putExtra("dose", dose)
                putExtra("notes", notes)
                putExtra("id", id.toLong())
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            alarmManager.setExactAndAllowWhileIdle(
                android.app.AlarmManager.RTC_WAKEUP,
                snoozeTime,
                pendingIntent
            )
            return
        }

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

        // Add actions
        val doneIntent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("id", id.toLong())
            putExtra("action", "DONE")
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context, id + 100000, doneIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val snoozeIntent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("id", id.toLong())
            putExtra("name", name)
            putExtra("dose", dose)
            putExtra("notes", notes)
            putExtra("action", "SNOOZE")
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, id + 200000, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val lostIntent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("id", id.toLong())
            putExtra("action", "LOST")
        }
        val lostPendingIntent = PendingIntent.getBroadcast(
            context, id + 300000, lostIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(name)
            .setContentText("Dose: $dose" + if (notes.isNotBlank()) "\nNotes: $notes" else "")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Dose: $dose" + if (notes.isNotBlank()) "\nNotes: $notes" else ""))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(android.R.drawable.checkbox_on_background, "Done", donePendingIntent)
            .addAction(android.R.drawable.ic_popup_reminder, "Snooze", snoozePendingIntent)
            .addAction(android.R.drawable.ic_delete, "Lost", lostPendingIntent)

        with(NotificationManagerCompat.from(context)) {
            notify(id, builder.build())
        }
    }
} 