package com.example.zonealarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.zonealarm.data.AlarmDatabase
import com.example.zonealarm.data.AlarmHistoryEntity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            pendingResult.finish()
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e("GeofenceReceiver", "Error: $errorMessage")
            pendingResult.finish()
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            if (triggeringGeofences == null) {
                pendingResult.finish()
                return
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    processGeofences(context, triggeringGeofences)
                } catch (e: Exception) {
                    Log.e("GeofenceReceiver", "Error processing geofences", e)
                } finally {
                    pendingResult.finish()
                }
            }
        } else {
            pendingResult.finish()
        }
    }

    private suspend fun processGeofences(context: Context, triggeringGeofences: List<Geofence>) {
        val db = AlarmDatabase.getDatabase(context)
        val dao = db.alarmDao()

        triggeringGeofences.forEach { geofence ->
            val alarmId = geofence.requestId.toIntOrNull() ?: return@forEach
            val alarm = dao.getAlarmById(alarmId)
            val alarmName = alarm?.name ?: "Unknown Zone"
            
            dao.insertHistory(
                AlarmHistoryEntity(
                    alarmName = alarmName,
                    transitionType = "Entered"
                )
            )

            sendNotification(context, alarmId, alarmName)
        }
    }

    private fun sendNotification(context: Context, alarmId: Int, alarmName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "zone_alarm_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Zone Alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                description = "Notification for triggered zone alarms"
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_NAME", alarmName)
        }
        
        val fullScreenIntent = PendingIntent.getActivity(
            context, 
            alarmId, 
            alarmIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Zone Alarm Triggered!")
            .setContentText("You have entered $alarmName")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenIntent, true)
            .setContentIntent(fullScreenIntent)
            .setAutoCancel(true)
            .setOngoing(true)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .build()

        notificationManager.notify(alarmId, notification)

        // Force launch activity if possible (best effort for when device is unlocked)
        try {
            context.startActivity(alarmIntent)
        } catch (e: Exception) {
            Log.e("GeofenceReceiver", "Direct activity start failed (expected on some Android versions): ${e.message}")
        }
    }
}
