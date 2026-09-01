package com.clawstack.shellguard.totp.data.migration

import com.clawstack.shellguard.totp.data.local.entities.TotpItemEntity
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Supported backup and vault export formats recognized by the Multi-Vault Ingestion Engine.
 */
enum class BackupSchemaType {
    SHELLGUARD_ENCRYPTED,
    SHELLGUARD_PLAIN,
    BITWARDEN_VAULT,
    BITWARDEN_AUTHENTICATOR,
    AEGIS,
    TWO_FAS,
    UNKNOWN
}

/**
 * Resolution policies for duplicate TOTP tokens detected during vault migration.
 */
enum class ConflictPolicy {
    SKIP_DUPLICATES,
    OVERWRITE_EXISTING,
    KEEP_BOTH
}

/**
 * Sanitized, decoupled in-memory representation of an imported 2FA/TOTP item.
 * Zero-knowledge guarantee: strictly holds OTP secrets and metadata without retaining passwords or notes.
 */
@Serializable
data class ParsedTotpItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val username: String? = null,
    val secret: String,
    val issuer: String? = null,
    val category: String? = null,
    val algorithm: String = "SHA1",
    val digits: Int = 6,
    val period: Int = 30,
    val schemaSource: BackupSchemaType = BackupSchemaType.UNKNOWN
) {
    /**
     * Converts the parsed item into a persistent Room SQLCipher entity.
     */
    fun toEntity(
        id: String = this.id.ifBlank { UUID.randomUUID().toString() },
        ownerUuid: String = "local",
        isLocalOnly: Boolean = true,
        syncState: String = "PENDING_SYNC",
        customTitle: String? = null
    ): TotpItemEntity {
        val cleanSecret = secret.replace(" ", "").replace("-", "").uppercase()
        return TotpItemEntity(
            id = id,
            ownerUuid = ownerUuid,
            title = customTitle ?: title.ifBlank { issuer ?: "2FA Account" },
            username = username,
            category = category?.ifBlank { null } ?: "General",
            secret = cleanSecret,
            algorithm = algorithm.uppercase(),
            digits = digits,
            period = period,
            isLocalOnly = isLocalOnly,
            syncState = syncState,
            remoteUpdatedAt = null,
            localUpdatedAt = System.currentTimeMillis()
        )
    }
}

/**
 * Fast-path pre-validation result for backup files before execution or decryption.
 */
@Serializable
data class PreValidationResult(
    val isValid: Boolean,
    val schemaType: BackupSchemaType,
    val isEncrypted: Boolean = false,
    val tokenCount: Int = 0,
    val summaryMessage: String = "",
    val parsedTokens: List<ParsedTotpItem> = emptyList(),
    val errorMessage: String? = null,
    val rawOwnerUuid: String? = null,
    val checksumSha256: String? = null
)
