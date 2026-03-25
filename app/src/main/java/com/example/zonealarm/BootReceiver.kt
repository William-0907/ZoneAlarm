package com.example.zonealarm

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.zonealarm.data.AlarmDatabase
import com.example.zonealarm.data.AlarmEntity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            reRegisterGeofences(context)
        }
    }

    private fun reRegisterGeofences(context: Context) {
        val db = AlarmDatabase.getDatabase(context)
        val dao = db.alarmDao()
        val geofencingClient = LocationServices.getGeofencingClient(context)

        CoroutineScope(Dispatchers.IO).launch {
            val alarms = dao.getAllAlarmsDirect() // Need to add this to DAO
            alarms.filter { it.isEnabled }.forEach { alarm ->
                setupGeofence(context, geofencingClient, alarm)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupGeofence(context: Context, client: com.google.android.gms.location.GeofencingClient, alarm: AlarmEntity) {
        val geofence = Geofence.Builder()
            .setRequestId(alarm.id.toString())
            .setCircularRegion(alarm.latitude, alarm.longitude, alarm.radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, alarm.id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        client.addGeofences(request, pendingIntent)
    }
}
