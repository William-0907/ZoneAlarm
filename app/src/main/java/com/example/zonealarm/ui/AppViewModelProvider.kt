package com.example.zonealarm.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.zonealarm.ZoneAlarmApplication
import com.example.zonealarm.ui.viewmodels.AlarmViewModel

/**
 * Provides Factory to create instance of ViewModel for the entire ZoneAlarm app
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        // Initializer for AlarmViewModel
        initializer {
            AlarmViewModel(
                zoneAlarmApplication().container.alarmRepository,
                zoneAlarmApplication()
            )
        }
    }
}

/**
 * Extension function to queries for [Application] object and returns an instance of
 * [ZoneAlarmApplication].
 */
fun CreationExtras.zoneAlarmApplication(): ZoneAlarmApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ZoneAlarmApplication)
