package com.example.zonealarm.ui.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zonealarm.GeofenceBroadcastReceiver
import com.example.zonealarm.data.AlarmDatabase
import com.example.zonealarm.data.AlarmEntity
import com.example.zonealarm.data.AlarmHistoryEntity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmViewModel(application: Application) : AndroidViewModel(application) {
    private val alarmDao = AlarmDatabase.getDatabase(application).alarmDao()
    private val geofencingClient = LocationServices.getGeofencingClient(application)

    val alarms: StateFlow<List<AlarmEntity>> = alarmDao.getAllAlarms()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favoriteAlarms: StateFlow<List<AlarmEntity>> = alarms
        .map { list -> list.filter { it.isFavorite } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val history: StateFlow<List<AlarmHistoryEntity>> = alarmDao.getAllHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addAlarm(name: String, latitude: Double, longitude: Double, radius: Float, isFavorite: Boolean = false) {
        viewModelScope.launch {
            val alarm = AlarmEntity(
                name = name,
                latitude = latitude,
                longitude = longitude,
                radius = radius,
                isFavorite = isFavorite
            )
            val id = alarmDao.insertAlarm(alarm).toInt()
            val savedAlarm = alarm.copy(id = id)
            setupGeofence(savedAlarm)
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupGeofence(alarm: AlarmEntity) {
        // Use ID as request ID to easily identify it in the receiver
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

        val intent = Intent(getApplication(), GeofenceBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            getApplication(), 
            alarm.id, // Unique requestCode per alarm
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        geofencingClient.addGeofences(request, pendingIntent).run {
            addOnSuccessListener { Log.d("Geofence", "Successfully added alarm ${alarm.id}") }
            addOnFailureListener { Log.e("Geofence", "Failed to add alarm ${alarm.id}: ${it.message}") }
        }
    }

    fun updateAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            alarmDao.updateAlarm(alarm)
            if (alarm.isEnabled) setupGeofence(alarm)
            else geofencingClient.removeGeofences(listOf(alarm.id.toString()))
        }
    }

    fun disableAlarm(id: Int) {
        viewModelScope.launch {
            alarmDao.setAlarmEnabled(id, false)
            geofencingClient.removeGeofences(listOf(id.toString()))
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            alarmDao.deleteAlarm(alarm)
            geofencingClient.removeGeofences(listOf(alarm.id.toString()))
        }
    }

    fun clearAlarms() {
        viewModelScope.launch {
            val currentAlarms = alarms.value
            alarmDao.clearAlarms()
            geofencingClient.removeGeofences(currentAlarms.map { it.id.toString() })
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            alarmDao.clearHistory()
        }
    }
}
