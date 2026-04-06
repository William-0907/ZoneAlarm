package com.example.zonealarm

import android.app.Application
import com.example.zonealarm.data.AppContainer
import com.example.zonealarm.data.AppDataContainer

class ZoneAlarmApplication : Application() {
    /**
     * AppContainer instance used by the rest of classes to obtain dependencies
     */
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
    }
}
