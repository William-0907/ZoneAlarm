package com.example.zonealarm.ui.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zonealarm.GeofenceBroadcastReceiver
import com.example.zonealarm.data.AlarmEntity
import com.example.zonealarm.data.AlarmHistoryEntity
import com.example.zonealarm.data.AlarmRepository
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng

class AlarmViewModel(
    private val alarmRepository: AlarmRepository,
    application: Application
) : AndroidViewModel(application) {
    private val geofencingClient = LocationServices.getGeofencingClient(application)
    private val prefs = application.getSharedPreferences("ZoneAlarmPrefs", Context.MODE_PRIVATE)

    // Map State Persistence
    private var _cameraPosition = mutableStateOf<CameraPosition?>(loadCameraPosition())
    var cameraPosition: CameraPosition?
        get() = _cameraPosition.value
        set(value) {
            _cameraPosition.value = value
            saveCameraPosition(value)
        }
        
    var selectedPoint by mutableStateOf<LatLng?>(null)
    var isPinDropped by mutableStateOf(false)
    var radiusMeters by mutableFloatStateOf(500f)

    private var _isSatellite = mutableStateOf(prefs.getBoolean("is_satellite", false))
    var isSatellite: Boolean
        get() = _isSatellite.value
        set(value) {
            _isSatellite.value = value
            prefs.edit().putBoolean("is_satellite", value).apply()
        }

    // Theme State
    var isDarkMode by mutableStateOf(prefs.getBoolean("dark_mode", true))
        private set

    fun toggleTheme(isDark: Boolean) {
        isDarkMode = isDark
        prefs.edit().putBoolean("dark_mode", isDark).apply()
    }

    val alarms: StateFlow<List<AlarmEntity>> = alarmRepository.getAllAlarmsStream()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val history: StateFlow<List<AlarmHistoryEntity>> = alarmRepository.getAllHistoryStream()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun saveCameraPosition(pos: CameraPosition?) {
        pos?.let {
            it.target?.let { target ->
                prefs.edit()
                    .putFloat("cam_lat", target.latitude.toFloat())
                    .putFloat("cam_lon", target.longitude.toFloat())
                    .putFloat("cam_zoom", it.zoom.toFloat())
                    .putFloat("cam_bearing", it.bearing.toFloat())
                    .apply()
            }
        }
    }

    private fun loadCameraPosition(): CameraPosition? {
        if (!prefs.contains("cam_lat")) return null
        return CameraPosition.Builder()
            .target(LatLng(prefs.getFloat("cam_lat", 0f).toDouble(), prefs.getFloat("cam_lon", 0f).toDouble()))
            .zoom(prefs.getFloat("cam_zoom", 14f).toDouble())
            .bearing(prefs.getFloat("cam_bearing", 0f).toDouble())
            .build()
    }

    fun addAlarm(name: String, latitude: Double, longitude: Double, radius: Float) {
        viewModelScope.launch {
            val alarm = AlarmEntity(
                name = name,
                latitude = latitude,
                longitude = longitude,
                radius = radius
            )
            val id = alarmRepository.insertAlarm(alarm).toInt()
            setupGeofence(alarm.copy(id = id))
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupGeofence(alarm: AlarmEntity) {
        val geofence = Geofence.Builder()
            .setRequestId(alarm.id.toString())
            .setCircularRegion(alarm.latitude, alarm.longitude, alarm.radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .setNotificationResponsiveness(0) // Instant triggering
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val intent = Intent(getApplication(), GeofenceBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            getApplication(), 
            alarm.id,
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
            alarmRepository.updateAlarm(alarm)
            if (alarm.isEnabled) setupGeofence(alarm)
            else geofencingClient.removeGeofences(listOf(alarm.id.toString()))
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            alarmRepository.deleteAlarm(alarm)
            geofencingClient.removeGeofences(listOf(alarm.id.toString()))
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            alarmRepository.clearHistory()
        }
    }

    fun deleteHistoryItems(ids: Set<Int>) {
        viewModelScope.launch {
            ids.forEach { id ->
                alarmRepository.deleteHistoryById(id)
            }
        }
    }
}
