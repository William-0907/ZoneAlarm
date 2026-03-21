package com.example.zonealarm.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms")
    fun getAllAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Int): AlarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmEntity): Long

    @Update
    suspend fun updateAlarm(alarm: AlarmEntity)

    @Query("UPDATE alarms SET isEnabled = :enabled WHERE id = :id")
    suspend fun setAlarmEnabled(id: Int, enabled: Boolean)

    @Delete
    suspend fun deleteAlarm(alarm: AlarmEntity)

    @Query("DELETE FROM alarms")
    suspend fun clearAlarms()

    // History methods
    @Query("SELECT * FROM alarm_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<AlarmHistoryEntity>>

    @Insert
    suspend fun insertHistory(history: AlarmHistoryEntity)

    @Query("DELETE FROM alarm_history")
    suspend fun clearHistory()
}
