package com.example.zonealarm

import android.app.Application
import com.example.zonealarm.data.AppContainer
import com.example.zonealarm.data.AppDataContainer
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ZoneAlarmApplication : Application() {
    /**
     * AppContainer instance used by the rest of the classes to obtain dependencies
     */
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
    }
}
