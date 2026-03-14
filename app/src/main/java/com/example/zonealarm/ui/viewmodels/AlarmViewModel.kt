package com.example.zonealarm.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zonealarm.data.AlarmDatabase
import com.example.zonealarm.data.AlarmEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmViewModel(application: Application) : AndroidViewModel(application) {
    private val alarmDao = AlarmDatabase.getDatabase(application).alarmDao()

    val alarms: StateFlow<List<AlarmEntity>> = alarmDao.getAllAlarms()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addAlarm(name: String, latitude: Double, longitude: Double, radius: Float) {
        viewModelScope.launch {
            alarmDao.insertAlarm(AlarmEntity(name = name, latitude = latitude, longitude = longitude, radius = radius))
        }
    }

    fun updateAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            alarmDao.updateAlarm(alarm)
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            alarmDao.deleteAlarm(alarm)
        }
    }

    fun clearAlarms() {
        viewModelScope.launch {
            alarmDao.clearAlarms()
        }
    }
}
