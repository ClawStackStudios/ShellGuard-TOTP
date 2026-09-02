package com.clawstack.shellguard.totp

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.clawstack.shellguard.totp.data.backup.BackupManager
import com.clawstack.shellguard.totp.data.local.ShellGuardTotpDatabase
import com.clawstack.shellguard.totp.data.local.entities.TotpItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupManagerTest {

    private lateinit var database: ShellGuardTotpDatabase
    private lateinit var backupManager: BackupManager

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ShellGuardTotpDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        backupManager = BackupManager(database.totpItemDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testExportAndImportEncryptedBackup() = runBlocking {
        val dao = database.totpItemDao()
        val ownerUuid = "user-123"
        val masterKey = "hu-claw-super-secret-key-12345678"

        val item1 = TotpItemEntity(
            id = "item-1",
            ownerUuid = ownerUuid,
            title = "GitHub",
            username = "octocat",
            category = "Dev",
            secret = "JBSWY3DPEHPK3PXP",
            isLocalOnly = true,
            syncState = "LOCAL"
        )
        val item2 = TotpItemEntity(
            id = "item-2",
            ownerUuid = ownerUuid,
            title = "Google",
            username = "security@google.com",
            category = "Work",
            secret = "KVKFKRCPNZQUYMLX",
            isLocalOnly = true,
            syncState = "PENDING_SYNC"
        )
        dao.upsertItems(listOf(item1, item2))

        // 1. Export
        val outputStream = ByteArrayOutputStream()
        val exportResult = backupManager.exportEncryptedBackup(outputStream, masterKey, ownerUuid)
        assertTrue(exportResult.isSuccess)
        assertEquals(2, exportResult.getOrNull())

        val backupData = outputStream.toByteArray()
        assertTrue(backupData.isNotEmpty())

        // 2. Clear Database
        dao.clearVault(ownerUuid)
        assertEquals(0, dao.getItemCount(ownerUuid))

        // 3. Restore
        val inputStream = ByteArrayInputStream(backupData)
        val importResult = backupManager.importEncryptedBackup(inputStream, masterKey, ownerUuid)
        assertTrue(importResult.isSuccess)
        assertEquals(2, importResult.getOrNull())

        val restoredItems = dao.observeAllTotpItems(ownerUuid).first()
        assertEquals(2, restoredItems.size)
        assertTrue(restoredItems.any { it.title == "GitHub" && it.secret == "JBSWY3DPEHPK3PXP" })
        assertTrue(restoredItems.any { it.title == "Google" && it.secret == "KVKFKRCPNZQUYMLX" })
    }

    @Test
    fun testExportAndImportPlainJsonBackup() = runBlocking {
        val dao = database.totpItemDao()
        val ownerUuid = "user-plain"

        val item = TotpItemEntity(
            id = "plain-1",
            ownerUuid = ownerUuid,
            title = "AWS IAM",
            username = "admin",
            category = "Cloud",
            secret = "JBSWY3DPEHPK3PXP",
            algorithm = "SHA256",
            digits = 6,
            period = 30,
            isLocalOnly = true,
            syncState = "PENDING_SYNC"
        )
        dao.upsertItem(item)

        // 1. Export unencrypted
        val outputStream = ByteArrayOutputStream()
        val exportResult = backupManager.exportPlainJsonBackup(outputStream, ownerUuid)
        assertTrue(exportResult.isSuccess)
        assertEquals(1, exportResult.getOrNull())

        val plainJsonData = outputStream.toByteArray()
        assertTrue(plainJsonData.isNotEmpty())

        // 2. Clear DB
        dao.clearVault(ownerUuid)
        assertEquals(0, dao.getItemCount(ownerUuid))

        // 3. Import unencrypted
        val inputStream = ByteArrayInputStream(plainJsonData)
        val importResult = backupManager.importPlainJsonBackup(inputStream, ownerUuid)
        assertTrue(importResult.isSuccess)
        assertEquals(1, importResult.getOrNull())

        val restored = dao.observeAllTotpItems(ownerUuid).first()
        assertEquals(1, restored.size)
        assertEquals("AWS IAM", restored[0].title)
        assertEquals("SHA256", restored[0].algorithm)
    }

    @Test
    fun testRemoteCodesAreExcludedFromExport() = runBlocking {
        val dao = database.totpItemDao()
        val ownerUuid = "user-test"
        val masterKey = "hu-secret-key-12345678"

        // 1 Local code and 2 Remote codes
        val localItem = TotpItemEntity(
            id = "local-1",
            ownerUuid = ownerUuid,
            title = "My Local Token",
            secret = "JBSWY3DPEHPK3PXP",
            isLocalOnly = true,
            syncState = "LOCAL"
        )
        val remoteItem1 = TotpItemEntity(
            id = "remote-1",
            ownerUuid = ownerUuid,
            title = "Remote Mirror Token 1",
            secret = "KVKFKRCPNZQUYMLX",
            isLocalOnly = false,
            syncState = "SYNCED"
        )
        val remoteItem2 = TotpItemEntity(
            id = "remote-2",
            ownerUuid = ownerUuid,
            title = "Remote Mirror Token 2",
            secret = "HXDMVJECJJWSRB3H",
            isLocalOnly = false,
            syncState = "SYNCED"
        )
        dao.upsertItems(listOf(localItem, remoteItem1, remoteItem2))

        val outputStream = ByteArrayOutputStream()
        val exportResult = backupManager.exportEncryptedBackup(outputStream, masterKey, ownerUuid)
        assertTrue(exportResult.isSuccess)
        // Only the 1 local item should be exported
        assertEquals(1, exportResult.getOrNull())
    }
}
