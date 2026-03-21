package com.example.zonealarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarm_history")
data class AlarmHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val alarmName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val transitionType: String // e.g., "Entered"
)
