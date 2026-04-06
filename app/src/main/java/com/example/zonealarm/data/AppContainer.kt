package com.example.zonealarm.data

import android.content.Context

/**
 * Dependency Injection container at the application level.
 */
interface AppContainer {
    val alarmRepository: AlarmRepository
}

/**
 * [AppContainer] implementation that provides instance of [OfflineAlarmRepository]
 */
class AppDataContainer(private val context: Context) : AppContainer {
    /**
     * Implementation for [AlarmRepository]
     */
    override val alarmRepository: AlarmRepository by lazy {
        OfflineAlarmRepository(AlarmDatabase.getDatabase(context).alarmDao())
    }
}
