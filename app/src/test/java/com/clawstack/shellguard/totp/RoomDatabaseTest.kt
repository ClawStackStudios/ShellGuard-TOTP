package com.clawstack.shellguard.totp

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.clawstack.shellguard.totp.data.local.ShellGuardTotpDatabase
import com.clawstack.shellguard.totp.data.local.entities.TotpItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomDatabaseTest {

    private lateinit var database: ShellGuardTotpDatabase
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = ShellGuardTotpDatabase.createInMemory(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInsertAndRetrieveTotpItem() = runBlocking {
        val dao = database.totpItemDao()
        val item = TotpItemEntity(
            id = "test-item-1",
            ownerUuid = "local",
            title = "GitHub",
            username = "octocat@github.com",
            category = "Developer",
            secret = "JBSWY3DPEHPK3PXP",
            algorithm = "SHA1",
            digits = 6,
            period = 30,
            isLocalOnly = true
        )

        dao.upsertItem(item)

        val fetched = dao.getItemById("test-item-1")
        assertNotNull(fetched)
        assertEquals("GitHub", fetched?.title)
        assertEquals("Developer", fetched?.category)
        assertEquals(true, fetched?.isLocalOnly)
    }

    @Test
    fun testSearchTotpItems() = runBlocking {
        val dao = database.totpItemDao()
        val item1 = TotpItemEntity(
            id = "item-gh",
            ownerUuid = "local",
            title = "GitHub Enterprise",
            username = "dev@company.com",
            category = "Work",
            secret = "JBSWY3DPEHPK3PXP"
        )
        val item2 = TotpItemEntity(
            id = "item-aws",
            ownerUuid = "local",
            title = "Amazon Web Services",
            username = "infra@company.com",
            category = "Cloud",
            secret = "JBSWY3DPEHPK3PXP"
        )

        dao.upsertItems(listOf(item1, item2))

        val searchResults = dao.searchTotpItems("local", "GitHub").first()
        assertEquals(1, searchResults.size)
        assertEquals("GitHub Enterprise", searchResults[0].title)

        val searchByUsername = dao.searchTotpItems("local", "infra").first()
        assertEquals(1, searchByUsername.size)
        assertEquals("Amazon Web Services", searchByUsername[0].title)
    }

    @Test
    fun testPruneDeletedRemoteItemsPreservesLocalOnly() = runBlocking {
        val dao = database.totpItemDao()

        // Remote item that was deleted on server
        val remoteItem1 = TotpItemEntity(
            id = "remote-deleted",
            ownerUuid = "local",
            title = "Old Remote Service",
            secret = "JBSWY3DPEHPK3PXP",
            isLocalOnly = false
        )

        // Remote item that still exists
        val remoteItem2 = TotpItemEntity(
            id = "remote-active",
            ownerUuid = "local",
            title = "Active Remote Service",
            secret = "JBSWY3DPEHPK3PXP",
            isLocalOnly = false
        )

        // Local-only scanned item
        val localOnlyItem = TotpItemEntity(
            id = "local-scanned",
            ownerUuid = "local",
            title = "My Bank Offline",
            secret = "JBSWY3DPEHPK3PXP",
            isLocalOnly = true
        )

        dao.upsertItems(listOf(remoteItem1, remoteItem2, localOnlyItem))

        // Active server remote IDs only contains remote-active
        dao.pruneDeletedRemoteItems("local", listOf("remote-active"))

        assertNull(dao.getItemById("remote-deleted"))
        assertNotNull(dao.getItemById("remote-active"))
        assertNotNull(dao.getItemById("local-scanned"))
    }
}
