package com.example.worldbusiness.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.worldbusiness.data.model.AuditLogRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AuditLogRecord>>

    @Query("SELECT * FROM audit_logs WHERE module = :module ORDER BY timestamp DESC")
    fun getLogsByModule(module: String): Flow<List<AuditLogRecord>>

    @Query("SELECT * FROM audit_logs WHERE logId LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR actorUsername LIKE '%' || :query || '%' OR sourceJurisdiction LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchLogs(query: String): Flow<List<AuditLogRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<AuditLogRecord>)

    @Query("SELECT COUNT(*) FROM audit_logs")
    suspend fun countLogs(): Int

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): Flow<List<AuditLogRecord>>

    @Query("DELETE FROM audit_logs")
    suspend fun clearAll()
}
