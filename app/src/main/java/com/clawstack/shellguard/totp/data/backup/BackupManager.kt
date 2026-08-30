package com.clawstack.shellguard.totp.data.backup

import com.clawstack.shellguard.totp.crypto.ShellCryptionEngine
import com.clawstack.shellguard.totp.data.local.dao.TotpItemDao
import com.clawstack.shellguard.totp.data.local.entities.TotpItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@Serializable
data class BackupItemDto(
    val id: String,
    val ownerUuid: String,
    val title: String,
    val username: String? = null,
    val category: String? = null,
    val secret: String,
    val algorithm: String = "SHA1",
    val digits: Int = 6,
    val period: Int = 30,
    val isLocalOnly: Boolean = false,
    val syncState: String = "SYNCED",
    val remoteUpdatedAt: String? = null,
    val localUpdatedAt: Long = 0L
)

@Serializable
data class BackupEnvelope(
    val version: Int = 1,
    val type: String = "shellguard-totp-backup-v1",
    val createdAt: Long = System.currentTimeMillis(),
    val ownerUuid: String,
    val itemCount: Int,
    val checksumSha256: String,
    val encryptedEnvelopeJson: String
)

@Serializable
data class PlainBackupExport(
    val version: Int = 1,
    val format: String = "shellguard-totp-plain-export-v1",
    val createdAt: Long = System.currentTimeMillis(),
    val itemCount: Int,
    val checksumSha256: String,
    val items: List<BackupItemDto>
)

class BackupManager(
    private val totpItemDao: TotpItemDao
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    /**
     * Exports all TOTP items for the user into an encrypted JSON envelope with SHA-256 integrity checksum.
     */
    suspend fun exportEncryptedBackup(
        outputStream: OutputStream,
        rawKey: String,
        ownerUuid: String = "local"
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val items = totpItemDao.observeAllTotpItems(ownerUuid).first()
            val dtos = items.map { item ->
                BackupItemDto(
                    id = item.id,
                    ownerUuid = item.ownerUuid,
                    title = item.title,
                    username = item.username,
                    category = item.category,
                    secret = item.secret,
                    algorithm = item.algorithm,
                    digits = item.digits,
                    period = item.period,
                    isLocalOnly = item.isLocalOnly,
                    syncState = item.syncState,
                    remoteUpdatedAt = item.remoteUpdatedAt,
                    localUpdatedAt = item.localUpdatedAt
                )
            }

            val plainJson = json.encodeToString(dtos)
            val checksum = computeSha256(plainJson)

            val shellKey = ShellCryptionEngine.deriveShellKey(rawKey, ownerUuid)
            val encryptedEnvelopeJson = ShellCryptionEngine.encryptField(
                plaintext = plainJson,
                shellKey = shellKey,
                table = "totp_backup",
                recordId = ownerUuid
            )

            val backupEnvelope = BackupEnvelope(
                version = 1,
                type = "shellguard-totp-backup-v1",
                createdAt = System.currentTimeMillis(),
                ownerUuid = ownerUuid,
                itemCount = items.size,
                checksumSha256 = checksum,
                encryptedEnvelopeJson = encryptedEnvelopeJson
            )

            val outputJson = json.encodeToString(backupEnvelope)
            outputStream.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
                writer.write(outputJson)
                writer.flush()
            }

            items.size
        }
    }

    /**
     * Restores and verifies an encrypted backup file, checking SHA-256 checksum and decrypting records before upserting.
     */
    suspend fun importEncryptedBackup(
        inputStream: InputStream,
        rawKey: String,
        targetOwnerUuid: String = "local"
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val rawText = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            val envelope = json.decodeFromString<BackupEnvelope>(rawText)

            require(
                envelope.type == "shellguard-totp-backup-v1" || 
                envelope.type == "shellguard_totp_encrypted_backup"
            ) {
                "Invalid backup format type: ${envelope.type}"
            }

            val shellKey = ShellCryptionEngine.deriveShellKey(rawKey, envelope.ownerUuid)
            val decryptedPlainJson = ShellCryptionEngine.decryptField(
                encryptedJson = envelope.encryptedEnvelopeJson,
                shellKey = shellKey,
                table = "totp_backup",
                recordId = envelope.ownerUuid
            )

            // Validate integrity checksum
            val actualChecksum = computeSha256(decryptedPlainJson)
            require(actualChecksum.equals(envelope.checksumSha256, ignoreCase = true)) {
                "Backup integrity checksum mismatch! Backup file may be corrupted or tampered with."
            }

            val items = json.decodeFromString<List<BackupItemDto>>(decryptedPlainJson)
            val entities = items.map { dto ->
                TotpItemEntity(
                    id = dto.id,
                    ownerUuid = targetOwnerUuid,
                    title = dto.title,
                    username = dto.username,
                    category = dto.category,
                    secret = dto.secret.replace(" ", "").replace("-", "").uppercase(),
                    algorithm = dto.algorithm,
                    digits = dto.digits,
                    period = dto.period,
                    isLocalOnly = true, // Safeguard imported items as local
                    syncState = "PENDING_SYNC",
                    remoteUpdatedAt = dto.remoteUpdatedAt,
                    localUpdatedAt = System.currentTimeMillis()
                )
            }

            if (entities.isNotEmpty()) {
                totpItemDao.upsertItems(entities)
            }

            entities.size
        }
    }

    /**
     * Exports all TOTP items in unencrypted portable JSON format for user data portability.
     */
    suspend fun exportPlainJsonBackup(
        outputStream: OutputStream,
        ownerUuid: String = "local"
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val items = totpItemDao.observeAllTotpItems(ownerUuid).first()
            val dtos = items.map { item ->
                BackupItemDto(
                    id = item.id,
                    ownerUuid = item.ownerUuid,
                    title = item.title,
                    username = item.username,
                    category = item.category,
                    secret = item.secret,
                    algorithm = item.algorithm,
                    digits = item.digits,
                    period = item.period,
                    isLocalOnly = item.isLocalOnly,
                    syncState = item.syncState,
                    remoteUpdatedAt = item.remoteUpdatedAt,
                    localUpdatedAt = item.localUpdatedAt
                )
            }

            val plainItemsJson = json.encodeToString(dtos)
            val checksum = computeSha256(plainItemsJson)

            val exportObject = PlainBackupExport(
                version = 1,
                format = "shellguard-totp-plain-export-v1",
                createdAt = System.currentTimeMillis(),
                itemCount = items.size,
                checksumSha256 = checksum,
                items = dtos
            )

            val outputJson = json.encodeToString(exportObject)
            outputStream.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
                writer.write(outputJson)
                writer.flush()
            }

            items.size
        }
    }

    /**
     * Imports an unencrypted JSON backup file or standard JSON item array.
     */
    suspend fun importPlainJsonBackup(
        inputStream: InputStream,
        targetOwnerUuid: String = "local"
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val rawText = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            val dtos: List<BackupItemDto> = try {
                val plainExport = json.decodeFromString<PlainBackupExport>(rawText)
                plainExport.items
            } catch (e: Exception) {
                // Fallback to direct array decoding
                json.decodeFromString<List<BackupItemDto>>(rawText)
            }

            val entities = dtos.map { dto ->
                TotpItemEntity(
                    id = dto.id,
                    ownerUuid = targetOwnerUuid,
                    title = dto.title,
                    username = dto.username,
                    category = dto.category,
                    secret = dto.secret.replace(" ", "").replace("-", "").uppercase(),
                    algorithm = dto.algorithm,
                    digits = dto.digits,
                    period = dto.period,
                    isLocalOnly = true,
                    syncState = "PENDING_SYNC",
                    remoteUpdatedAt = dto.remoteUpdatedAt,
                    localUpdatedAt = System.currentTimeMillis()
                )
            }

            if (entities.isNotEmpty()) {
                totpItemDao.upsertItems(entities)
            }

            entities.size
        }
    }

    private fun computeSha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
