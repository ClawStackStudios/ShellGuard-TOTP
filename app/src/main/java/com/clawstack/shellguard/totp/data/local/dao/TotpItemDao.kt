package com.clawstack.shellguard.totp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.clawstack.shellguard.totp.data.local.entities.TotpItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TotpItemDao {
    @Query("SELECT * FROM totp_items WHERE (:ownerUuid = 'local' AND owner_uuid = 'local') OR (:ownerUuid != 'local' AND (owner_uuid = :ownerUuid OR owner_uuid = 'local')) ORDER BY title COLLATE NOCASE ASC")
    fun observeAllTotpItems(ownerUuid: String = "local"): Flow<List<TotpItemEntity>>

    @Query("SELECT * FROM totp_items WHERE ((:ownerUuid = 'local' AND owner_uuid = 'local') OR (:ownerUuid != 'local' AND (owner_uuid = :ownerUuid OR owner_uuid = 'local'))) AND category = :category ORDER BY title COLLATE NOCASE ASC")
    fun observeTotpItemsByPod(ownerUuid: String = "local", category: String): Flow<List<TotpItemEntity>>

    @Query("SELECT DISTINCT category FROM totp_items WHERE ((:ownerUuid = 'local' AND owner_uuid = 'local') OR (:ownerUuid != 'local' AND (owner_uuid = :ownerUuid OR owner_uuid = 'local'))) AND category IS NOT NULL AND category != '' ORDER BY category ASC")
    fun observeDistinctCategories(ownerUuid: String = "local"): Flow<List<String>>

    @Query("SELECT * FROM totp_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): TotpItemEntity?

    @Query("SELECT * FROM totp_items WHERE ((:ownerUuid = 'local' AND owner_uuid = 'local') OR (:ownerUuid != 'local' AND (owner_uuid = :ownerUuid OR owner_uuid = 'local'))) AND (title LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%') ORDER BY title COLLATE NOCASE ASC")
    fun searchTotpItems(ownerUuid: String = "local", query: String): Flow<List<TotpItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<TotpItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: TotpItemEntity)

    @Update
    suspend fun updateItem(item: TotpItemEntity)

    @Query("DELETE FROM totp_items WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Delta sync reconciliation: Deletes remote items that no longer exist on the server,
     * while strictly preserving user items created locally on device (is_local_only = 1).
     */
    @Query("DELETE FROM totp_items WHERE owner_uuid = :ownerUuid AND is_local_only = 0 AND id NOT IN (:activeRemoteIds)")
    suspend fun pruneDeletedRemoteItems(ownerUuid: String = "local", activeRemoteIds: List<String>)

    @Query("DELETE FROM totp_items WHERE owner_uuid = :ownerUuid")
    suspend fun clearVault(ownerUuid: String = "local")

    @Query("SELECT * FROM totp_items WHERE sync_state = 'PENDING_SYNC' OR (is_local_only = 1 AND sync_state != 'SYNCED')")
    suspend fun getAllPendingSyncItems(): List<TotpItemEntity>

    @Query("SELECT * FROM totp_items WHERE (owner_uuid = :ownerUuid OR owner_uuid = 'local') AND (sync_state = 'PENDING_SYNC' OR (is_local_only = 1 AND sync_state != 'SYNCED'))")
    suspend fun getPendingSyncItems(ownerUuid: String): List<TotpItemEntity>

    @Query("SELECT COUNT(*) FROM totp_items WHERE owner_uuid = :ownerUuid")
    suspend fun getItemCount(ownerUuid: String = "local"): Int
}
