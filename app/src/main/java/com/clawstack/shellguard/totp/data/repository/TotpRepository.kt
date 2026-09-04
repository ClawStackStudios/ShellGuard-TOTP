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
     * One-Way Mirror Delta Synchronization:
     * 1. Downstream Pull: Fetches remote vault pearls, decrypts seeds, and upserts them locally with pruning.
     * Remote items are strictly read-only mirrors.
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

            // 2. Downstream Pull: Fetch remote pearls
            val remotePearls = client.fetchVault(token).getOrThrow()

            // 3. Client-side delta filter: skip re-decryption for pearls whose
            //    remote `updated_at` stamp matches the local mirror (CPU/IO saving —
            //    unchanged items are neither decrypted nor re-upserted).
            val existingRemoteByUpdatedAt = totpItemDao.getRemoteItemsOnce(userUuid)
                .associate { it.id to it.remoteUpdatedAt }
            val totpPearls = remotePearls.filter { !it.totp_secret.isNullOrBlank() }
            val unchangedRemoteIds = totpPearls
                .filter { existingRemoteByUpdatedAt[it.id] == it.updated_at }
                .map { it.id }
            val changedPearls = totpPearls.filter { existingRemoteByUpdatedAt[it.id] != it.updated_at }

            // 4. Decrypt seeds and map to Room entities (changed/new pearls only)
            val entities = changedPearls.mapNotNull { pearl ->
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

            // 5. Upsert into Room DB and prune deleted remote items.
            //    Pruning spans ALL known remote ids (unchanged + changed) so rows are
            //    only removed when they disappeared server-side, never for merely
            //    unchanged mirrors.
            if (entities.isNotEmpty()) {
                totpItemDao.upsertItems(entities)
            }
            val remoteIds = unchangedRemoteIds + entities.map { it.id }
            totpItemDao.pruneDeletedRemoteItems(userUuid, remoteIds)

            // 6. Update sync metadata
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
