package com.clawstack.shellguard.totp.data.repository

import com.clawstack.shellguard.totp.crypto.ClawCrypto
import com.clawstack.shellguard.totp.crypto.ShellCryptionEngine
import com.clawstack.shellguard.totp.data.local.dao.SyncMetadataDao
import com.clawstack.shellguard.totp.data.local.dao.TotpItemDao
import com.clawstack.shellguard.totp.data.local.entities.SyncMetadataEntity
import com.clawstack.shellguard.totp.data.local.entities.TotpItemEntity
import com.clawstack.shellguard.totp.data.remote.ApiClient
import com.clawstack.shellguard.totp.data.remote.models.CreateVaultItemRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TotpRepository(
    private val totpItemDao: TotpItemDao,
    private val syncMetadataDao: SyncMetadataDao? = null
) {

    fun observeAllItems(ownerUuid: String = "local"): Flow<List<TotpItemEntity>> {
        return totpItemDao.observeAllTotpItems(ownerUuid)
    }

    fun observeItemsByPod(ownerUuid: String = "local", category: String): Flow<List<TotpItemEntity>> {
        return totpItemDao.observeTotpItemsByPod(ownerUuid, category)
    }

    fun searchItems(ownerUuid: String = "local", query: String): Flow<List<TotpItemEntity>> {
        return totpItemDao.searchTotpItems(ownerUuid, query)
    }

    suspend fun getItemById(id: String): TotpItemEntity? = withContext(Dispatchers.IO) {
        totpItemDao.getItemById(id)
    }

    suspend fun upsertItem(item: TotpItemEntity) = withContext(Dispatchers.IO) {
        totpItemDao.upsertItem(item)
    }

    suspend fun upsertItems(items: List<TotpItemEntity>) = withContext(Dispatchers.IO) {
        totpItemDao.upsertItems(items)
    }

    suspend fun deleteItem(id: String) = withContext(Dispatchers.IO) {
        totpItemDao.deleteById(id)
    }

    suspend fun clearVault(ownerUuid: String = "local") = withContext(Dispatchers.IO) {
        totpItemDao.clearVault(ownerUuid)
    }

    /**
     * Bidirectional Delta Synchronization:
     * 1. Upstream Push: Encrypts and posts locally created/pending TOTP items to the remote ShellGuard server.
     * 2. Downstream Pull: Fetches remote vault pearls, decrypts seeds, and upserts them locally with pruning.
     */
    suspend fun syncRemoteVault(serverUrl: String, rawHuKey: String, userUuid: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val client = ApiClient.getClient(serverUrl)

            // 1. Ensure active session token
            var token = ApiClient.authToken
            if (token == null) {
                val keyHash = ClawCrypto.hashHumanKey(rawHuKey)
                val session = client.authenticate(keyHash).getOrThrow()
                token = session.token
                ApiClient.updateAuthToken(token)
            }

            val itemKey = ShellCryptionEngine.deriveShellKey(rawHuKey, userUuid)

            // 2. Upstream Push: Check for pending local items to sync to remote server
            val pendingItems = totpItemDao.getPendingSyncItems(userUuid)
            for (pending in pendingItems) {
                try {
                    val encryptedSecret = ShellCryptionEngine.encryptField(
                        plaintext = "",
                        shellKey = itemKey,
                        table = "vault_pearls",
                        recordId = pending.id
                    )
                    val encryptedTotp = ShellCryptionEngine.encryptField(
                        plaintext = pending.secret,
                        shellKey = itemKey,
                        table = "vault_pearls_totp",
                        recordId = pending.id
                    )

                    val createReq = CreateVaultItemRequest(
                        title = pending.title,
                        username = pending.username,
                        category = pending.category,
                        secret = encryptedSecret,
                        totp_secret = encryptedTotp,
                        type = "password"
                    )

                    val remotePearl = client.createVaultItem(token, createReq).getOrNull()
                    if (remotePearl != null) {
                        if (remotePearl.id != pending.id) {
                            totpItemDao.deleteById(pending.id)
                        }
                        totpItemDao.upsertItem(
                            pending.copy(
                                id = remotePearl.id,
                                ownerUuid = userUuid,
                                isLocalOnly = false,
                                syncState = "SYNCED",
                                remoteUpdatedAt = remotePearl.updated_at,
                                localUpdatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                } catch (ignored: Exception) {
                    // Retain pending state for next retry
                }
            }

            // 3. Downstream Pull: Fetch remote pearls
            val remotePearls = client.fetchVault(token).getOrThrow()

            // 4. Filter items that have a non-empty totp_secret
            val totpPearls = remotePearls.filter { !it.totp_secret.isNullOrBlank() }

            // 5. Decrypt seeds and map to Room entities
            val entities = totpPearls.mapNotNull { pearl ->
                try {
                    val decryptedSeed = ShellCryptionEngine.decryptField(
                        encryptedJson = pearl.totp_secret!!,
                        shellKey = itemKey,
                        table = "vault_pearls_totp",
                        recordId = pearl.id
                    )
                    TotpItemEntity(
                        id = pearl.id,
                        ownerUuid = userUuid,
                        title = pearl.title,
                        username = pearl.username,
                        category = pearl.category,
                        secret = decryptedSeed.replace(" ", "").replace("-", "").uppercase(),
                        isLocalOnly = false,
                        syncState = "SYNCED",
                        remoteUpdatedAt = pearl.updated_at,
                        localUpdatedAt = System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    null // Skip corrupted envelopes or items belonging to other keys
                }
            }

            // 6. Upsert into Room DB and prune deleted remote items
            if (entities.isNotEmpty()) {
                totpItemDao.upsertItems(entities)
            }
            val remoteIds = entities.map { it.id }
            totpItemDao.pruneDeletedRemoteItems(userUuid, remoteIds)

            // 7. Update sync metadata
            val count = totpItemDao.getItemCount(userUuid)
            syncMetadataDao?.updateMetadata(
                SyncMetadataEntity(
                    id = 1,
                    serverUrl = serverUrl,
                    ownerUuid = userUuid,
                    lastSyncTimestamp = System.currentTimeMillis(),
                    lastSyncStatus = "SUCCESS",
                    itemCount = count,
                    lastErrorMessage = null
                )
            )

            entities.size
        }.onFailure { ex ->
            syncMetadataDao?.updateMetadata(
                SyncMetadataEntity(
                    id = 1,
                    serverUrl = serverUrl,
                    ownerUuid = userUuid,
                    lastSyncTimestamp = System.currentTimeMillis(),
                    lastSyncStatus = "FAILED",
                    itemCount = 0,
                    lastErrorMessage = ex.message
                )
            )
        }
    }
}
