package com.example.zonealarm.di

import android.content.Context
import com.example.zonealarm.data.AlarmDao
import com.example.zonealarm.data.AlarmDatabase
import com.example.zonealarm.data.AlarmRepository
import com.example.zonealarm.data.OfflineAlarmRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): AlarmDatabase {
        return AlarmDatabase.getDatabase(context)
    }

    @Provides
    fun provideAlarmDao(database: AlarmDatabase): AlarmDao {
        return database.alarmDao()
    }

    @Singleton
    @Provides
    fun provideAlarmRepository(alarmDao: AlarmDao): AlarmRepository {
        return OfflineAlarmRepository(alarmDao)
    }
}
