package com.clawstack.shellguard.totp.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Represents an individual 2FA / TOTP token record stored locally in the encrypted database.
 */
@Serializable
@Entity(
    tableName = "totp_items",
    indices = [
        Index(value = ["owner_uuid"]),
        Index(value = ["category"]),
        Index(value = ["is_local_only"]),
        Index(value = ["sync_state"])
    ]
)
data class TotpItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // Matches server pearl UUID, or generated UUID for local-only items

    @ColumnInfo(name = "owner_uuid")
    val ownerUuid: String = "local",

    @ColumnInfo(name = "title")
    val title: String, // Service / Account Name (e.g., "GitHub", "AWS Console")

    @ColumnInfo(name = "username")
    val username: String? = null, // e.g., "lucas@example.com"

    @ColumnInfo(name = "category")
    val category: String? = null, // Pod / Folder path (e.g., "Work/DevOps")

    @ColumnInfo(name = "secret")
    val secret: String, // Base32 secret string (stored encrypted on disk via SQLCipher)

    @ColumnInfo(name = "algorithm")
    val algorithm: String = "SHA1", // SHA1, SHA256, SHA512

    @ColumnInfo(name = "digits")
    val digits: Int = 6, // 6 or 8 digits

    @ColumnInfo(name = "period")
    val period: Int = 30, // 30 or 60 seconds

    @ColumnInfo(name = "is_local_only")
    val isLocalOnly: Boolean = false, // True if created via QR scan without server sync

    @ColumnInfo(name = "sync_state")
    val syncState: String = "SYNCED", // "SYNCED", "PENDING_SYNC", "ERROR"

    @ColumnInfo(name = "remote_updated_at")
    val remoteUpdatedAt: String? = null,

    @ColumnInfo(name = "local_updated_at")
    val localUpdatedAt: Long = System.currentTimeMillis()
)
