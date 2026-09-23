package com.clawstack.shellguard.totp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.clawstack.shellguard.totp.data.local.entities.AuditLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * Phase 12 / Task 23: Data Access Object for security audit logging.
 */
@Dao
interface AuditLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: AuditLogEntity): Long

    @Query("SELECT * FROM audit_log ORDER BY timestamp_ms DESC")
    fun observeAll(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_log WHERE event_type LIKE '%' || :query || '%' OR (detail IS NOT NULL AND detail LIKE '%' || :query || '%') ORDER BY timestamp_ms DESC")
    fun searchEvents(query: String): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_log ORDER BY timestamp_ms DESC LIMIT :limit")
    suspend fun getRecentEvents(limit: Int = 100): List<AuditLogEntity>

    @Query("DELETE FROM audit_log")
    suspend fun deleteAll()
}
