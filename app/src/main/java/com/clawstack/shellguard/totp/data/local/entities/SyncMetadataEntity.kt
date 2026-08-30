package com.clawstack.shellguard.totp.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Tracks server synchronization state, offline status, and last sync timestamps.
 */
@Serializable
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1, // Singleton row

    @ColumnInfo(name = "server_url")
    val serverUrl: String = "",

    @ColumnInfo(name = "owner_uuid")
    val ownerUuid: String = "local",

    @ColumnInfo(name = "last_sync_timestamp")
    val lastSyncTimestamp: Long = 0L,

    @ColumnInfo(name = "last_sync_status")
    val lastSyncStatus: String = "IDLE", // "SUCCESS", "FAILED", "OFFLINE", "IDLE"

    @ColumnInfo(name = "item_count")
    val itemCount: Int = 0,

    @ColumnInfo(name = "last_error_message")
    val lastErrorMessage: String? = null
)
