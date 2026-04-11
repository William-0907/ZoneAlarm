package com.example.zonealarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.zonealarm.data.AlarmHistoryEntity
import com.example.zonealarm.data.AlarmRepository
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmRepository: AlarmRepository

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ZoneAlarm:GeofenceWakeLock")
        wakeLock.acquire(10 * 1000L) 

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            wakeLock.release()
            pendingResult.finish()
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e("GeofenceReceiver", "Error: $errorMessage")
            wakeLock.release()
            pendingResult.finish()
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            if (triggeringGeofences == null) {
                wakeLock.release()
                pendingResult.finish()
                return
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    processGeofences(context, triggeringGeofences)
                } catch (e: Exception) {
                    Log.e("GeofenceReceiver", "Error processing geofences", e)
                } finally {
                    if (wakeLock.isHeld) wakeLock.release()
                    pendingResult.finish()
                }
            }
        } else {
            if (wakeLock.isHeld) wakeLock.release()
            pendingResult.finish()
        }
    }

    private suspend fun processGeofences(context: Context, triggeringGeofences: List<Geofence>) {
        triggeringGeofences.forEach { geofence ->
            val alarmId = geofence.requestId.toIntOrNull() ?: return@forEach
            val alarm = alarmRepository.getAlarmStream(alarmId)
            val alarmName = alarm?.name ?: "Unknown Zone"
            
            alarmRepository.insertHistory(
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
        val channelId = "zone_alarm_channel_v6" 

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Zone Alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                description = "Notification for triggered zone alarms"
                setShowBadge(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_NAME", alarmName)
        }
        
        val fullScreenIntent = PendingIntent.getActivity(
            context, 
            alarmId, 
            alarmIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Zone Alarm Triggered!")
            .setContentText("Entering $alarmName")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenIntent, true)
            .setContentIntent(fullScreenIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(null) 
            .build()

        notificationManager.notify(alarmId, notification)

        try {
            context.startActivity(alarmIntent)
        } catch (e: Exception) {
            Log.e("GeofenceReceiver", "Forced activity start failed: ${e.message}")
        }
    }
}
