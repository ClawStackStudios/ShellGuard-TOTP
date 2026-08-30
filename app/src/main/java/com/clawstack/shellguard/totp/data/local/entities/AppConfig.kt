package com.clawstack.shellguard.totp.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * App Configuration and User Preferences stored in the database.
 */
@Serializable
@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1,

    @ColumnInfo(name = "biometric_enabled")
    val isBiometricEnabled: Boolean = true,

    @ColumnInfo(name = "clipboard_timeout_seconds")
    val clipboardTimeoutSeconds: Int = 30,

    @ColumnInfo(name = "dark_mode")
    val isDarkMode: Boolean = true,

    @ColumnInfo(name = "default_period")
    val defaultPeriod: Int = 30,

    @ColumnInfo(name = "default_digits")
    val defaultDigits: Int = 6,

    @ColumnInfo(name = "server_url")
    val serverUrl: String = "https://vault.clawstack.internal"
)
