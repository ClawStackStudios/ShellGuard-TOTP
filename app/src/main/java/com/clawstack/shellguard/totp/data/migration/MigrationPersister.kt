package com.clawstack.shellguard.totp.data.migration

import com.clawstack.shellguard.totp.crypto.ShellCryptionEngine
import com.clawstack.shellguard.totp.data.local.dao.AuditLogDao
import com.clawstack.shellguard.totp.data.local.dao.TotpItemDao
import com.clawstack.shellguard.totp.data.local.entities.AuditLogEntity
import com.clawstack.shellguard.totp.data.remote.ShellGuardTotpClient
import com.clawstack.shellguard.totp.data.remote.models.CreateVaultItemRequest
import java.util.UUID
import javax.crypto.spec.SecretKeySpec

enum class ImportDestination {
    LOCAL_VAULT,
    REMOTE_GATEWAY
}

data class ImportResult(
    val added: Int,
    val updated: Int,
    val skipped: Int
)

class MigrationPersister(
    private val totpItemDao: TotpItemDao,
    private val auditLogDao: AuditLogDao,
    private val apiClient: ShellGuardTotpClient? = null
) {
    suspend fun persistImportedTokens(
        tokens: List<ParsedTotpItem>,
        destination: ImportDestination,
        policy: ConflictPolicy,
        ownerUuid: String = "local",
        huKey: String? = null,
        sessionToken: String? = null
    ): ImportResult {
        var added = 0
        var updated = 0
        var skipped = 0

        val existingItems = totpItemDao.getAllTotpItems(ownerUuid)

        // Maps to quickly find duplicates
        val secretMap = existingItems.associateBy { it.secret }
        val titleUsernameMap = existingItems.associateBy { "${it.title}|${it.username}" }

        val itemsToUpsert = mutableListOf<Pair<com.clawstack.shellguard.totp.data.local.entities.TotpItemEntity, Boolean>>()

        for (token in tokens) {
            val duplicateBySecret = secretMap[token.secret]
            val duplicateByName = titleUsernameMap["${token.title}|${token.username}"]

            val existing = duplicateBySecret ?: duplicateByName

            if (existing != null) {
                when (policy) {
                    ConflictPolicy.SKIP_DUPLICATES -> {
                        skipped++
                        continue
                    }
                    ConflictPolicy.OVERWRITE_EXISTING -> {
                        val updatedEntity = existing.copy(
                            title = token.title,
                            username = token.username,
                            category = token.category ?: existing.category,
                            localUpdatedAt = System.currentTimeMillis(),
                            syncState = if (destination == ImportDestination.REMOTE_GATEWAY) "PENDING_SYNC" else existing.syncState
                        )
                        itemsToUpsert.add(updatedEntity to true)
                        updated++
                    }
                    ConflictPolicy.KEEP_BOTH -> {
                        val newEntity = token.toEntity(
                            id = UUID.randomUUID().toString(),
                            ownerUuid = ownerUuid,
                            isLocalOnly = destination == ImportDestination.LOCAL_VAULT,
                            syncState = if (destination == ImportDestination.REMOTE_GATEWAY) "PENDING_SYNC" else "SYNCED",
                            customTitle = "${token.title} (Imported)"
                        )
                        itemsToUpsert.add(newEntity to false)
                        added++
                    }
                }
            } else {
                val newEntity = token.toEntity(
                    id = token.id.ifBlank { UUID.randomUUID().toString() },
                    ownerUuid = ownerUuid,
                    isLocalOnly = destination == ImportDestination.LOCAL_VAULT,
                    syncState = if (destination == ImportDestination.REMOTE_GATEWAY) "PENDING_SYNC" else "SYNCED"
                )
                itemsToUpsert.add(newEntity to false)
                added++
            }
        }

        if (itemsToUpsert.isNotEmpty()) {
            totpItemDao.upsertItems(itemsToUpsert.map { it.first })
        }

        if (destination == ImportDestination.REMOTE_GATEWAY && apiClient != null && huKey != null && sessionToken != null) {
            val shellKey = ShellCryptionEngine.deriveShellKey(huKey, ownerUuid)

            for ((item, isUpdate) in itemsToUpsert.filter { !it.first.isLocalOnly && it.first.syncState == "PENDING_SYNC" }) {
                try {
                    val encryptedTotpSecret = ShellCryptionEngine.encryptField(
                        plaintext = item.secret,
                        shellKey = shellKey,
                        table = "vault_pearls_totp",
                        recordId = item.id
                    )

                    val request = CreateVaultItemRequest(
                        title = item.title,
                        username = item.username,
                        category = item.category,
                        totp_secret = encryptedTotpSecret,
                        type = "totp"
                    )

                    val result = if (isUpdate && item.remoteUpdatedAt != null) {
                        apiClient.updateVaultItem(sessionToken, item.id, request)
                    } else {
                        apiClient.createVaultItem(sessionToken, request)
                    }

                    if (result.isSuccess) {
                        val dto = result.getOrNull()
                        if (dto != null) {
                            val newId = dto.id
                            if (newId != item.id) {
                                // If server generated a new ID, update the local record to match.
                                val updatedItem = item.copy(id = newId, syncState = "SYNCED")
                                totpItemDao.deleteById(item.id)
                                totpItemDao.upsertItem(updatedItem)
                            } else {
                                totpItemDao.updateItem(item.copy(syncState = "SYNCED"))
                            }
                        } else {
                            totpItemDao.updateItem(item.copy(syncState = "SYNCED"))
                        }
                    }
                } catch (e: Exception) {
                    // Log or handle remote failure, item remains PENDING_SYNC
                }
            }
        }

        auditLogDao.insertEvent(
            AuditLogEntity(
                eventType = "IMPORT_SUCCESS",
                detail = "Imported ${tokens.size} tokens. Added: $added, Updated: $updated, Skipped: $skipped. Destination: $destination."
            )
        )

        return ImportResult(added, updated, skipped)
    }
}
