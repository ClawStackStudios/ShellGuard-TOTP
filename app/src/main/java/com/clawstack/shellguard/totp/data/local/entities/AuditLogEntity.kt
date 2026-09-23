package com.clawstack.shellguard.totp.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Phase 12 / Task 23: Represents an append-only security audit log event record.
 */
@Serializable
@Entity(
    tableName = "audit_log",
    indices = [
        Index(value = ["event_type"]),
        Index(value = ["timestamp_ms"])
    ]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "event_type")
    val eventType: String,

    @ColumnInfo(name = "detail")
    val detail: String? = null,

    @ColumnInfo(name = "timestamp_ms")
    val timestampMs: Long = System.currentTimeMillis()
)
