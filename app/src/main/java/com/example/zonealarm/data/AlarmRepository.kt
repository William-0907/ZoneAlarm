package com.example.zonealarm.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository that provides insert, update, delete, and retrieve of [AlarmEntity] from a given data source.
 */
interface AlarmRepository {
    /**
     * Retrieve all the alarms from the the given data source.
     */
    fun getAllAlarmsStream(): Flow<List<AlarmEntity>>

    /**
     * Retrieve all the alarms directly.
     */
    suspend fun getAllAlarmsDirect(): List<AlarmEntity>

    /**
     * Retrieve an alarm from the given data source that matches with the [id].
     */
    suspend fun getAlarmStream(id: Int): AlarmEntity?

    /**
     * Insert alarm in the data source
     */
    suspend fun insertAlarm(alarm: AlarmEntity): Long

    /**
     * Delete alarm from the data source
     */
    suspend fun deleteAlarm(alarm: AlarmEntity)

    /**
     * Update alarm in the data source
     */
    suspend fun updateAlarm(alarm: AlarmEntity)

    /**
     * Set alarm enabled/disabled
     */
    suspend fun setAlarmEnabled(id: Int, enabled: Boolean)

    /**
     * Clear all alarms
     */
    suspend fun clearAlarms()

    // History methods
    fun getAllHistoryStream(): Flow<List<AlarmHistoryEntity>>
    suspend fun insertHistory(history: AlarmHistoryEntity)
    suspend fun deleteHistoryById(id: Int)
    suspend fun clearHistory()
}

/**
 * Concrete implementation of [AlarmRepository] that uses [AlarmDao] to access the database.
 */
class OfflineAlarmRepository(private val alarmDao: AlarmDao) : AlarmRepository {
    override fun getAllAlarmsStream(): Flow<List<AlarmEntity>> = alarmDao.getAllAlarms()

    override suspend fun getAllAlarmsDirect(): List<AlarmEntity> = alarmDao.getAllAlarmsDirect()

    override suspend fun getAlarmStream(id: Int): AlarmEntity? = alarmDao.getAlarmById(id)

    override suspend fun insertAlarm(alarm: AlarmEntity): Long = alarmDao.insertAlarm(alarm)

    override suspend fun deleteAlarm(alarm: AlarmEntity) = alarmDao.deleteAlarm(alarm)

    override suspend fun updateAlarm(alarm: AlarmEntity) = alarmDao.updateAlarm(alarm)

    override suspend fun setAlarmEnabled(id: Int, enabled: Boolean) = alarmDao.setAlarmEnabled(id, enabled)

    override suspend fun clearAlarms() = alarmDao.clearAlarms()

    override fun getAllHistoryStream(): Flow<List<AlarmHistoryEntity>> = alarmDao.getAllHistory()

    override suspend fun insertHistory(history: AlarmHistoryEntity) = alarmDao.insertHistory(history)

    override suspend fun deleteHistoryById(id: Int) = alarmDao.deleteHistoryById(id)

    override suspend fun clearHistory() = alarmDao.clearHistory()
}
