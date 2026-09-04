package com.clawstack.shellguard.totp

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.clawstack.shellguard.totp.data.local.ShellGuardTotpDatabase
import com.clawstack.shellguard.totp.data.local.entities.TotpItemEntity
import com.clawstack.shellguard.totp.data.repository.AuthRepository
import com.clawstack.shellguard.totp.data.repository.TotpRepository
import com.clawstack.shellguard.totp.ui.viewmodels.TotpViewModel
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import android.os.Looper
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = ShellGuardTotpApp::class)
class OneWaySyncAndReadOnlyProtectionTest {

    private lateinit var context: Context
    private lateinit var database: ShellGuardTotpDatabase
    private lateinit var authRepository: AuthRepository
    private lateinit var totpRepository: TotpRepository
    private lateinit var totpViewModel: TotpViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<ShellGuardTotpApp>()
        val app = context as ShellGuardTotpApp
        database = app.database
        authRepository = app.authRepository
        totpRepository = app.totpRepository
        totpViewModel = TotpViewModel(app)

        runBlocking {
            database.totpItemDao().clearVault("local")
            database.totpItemDao().clearVault("user-remote-123")
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            database.totpItemDao().clearVault("local")
            database.totpItemDao().clearVault("user-remote-123")
        }
    }

    @Test
    fun testRemoteSyncedItemCannotBeEditedLocally() = runBlocking {
        val remoteId = UUID.randomUUID().toString()
        val remoteItem = TotpItemEntity(
            id = remoteId,
            ownerUuid = "user-remote-123",
            title = "Remote Cloud Service",
            username = "admin@cloud.com",
            category = "Infrastructure",
            secret = "JBSWY3DPEHPK3PXP",
            isLocalOnly = false,
            syncState = "SYNCED"
        )
        database.totpItemDao().upsertItem(remoteItem)

        // Attempt local update through ViewModel
        totpViewModel.updateItemDetails(
            id = remoteId,
            title = "Hacked Title",
            username = "hacked@cloud.com",
            category = "Hacked"
        )

        // Let coroutine run
        shadowOf(Looper.getMainLooper()).idle()

        // Verify record in DB was untouched
        val itemInDb = database.totpItemDao().getItemById(remoteId)
        assertNotNull(itemInDb)
        assertEquals("Remote Cloud Service", itemInDb?.title)
        assertEquals("admin@cloud.com", itemInDb?.username)
        assertEquals("Infrastructure", itemInDb?.category)
    }

    @Test
    fun testRemoteSyncedItemCannotBeDeletedLocally() = runBlocking {
        val remoteId = UUID.randomUUID().toString()
        val remoteItem = TotpItemEntity(
            id = remoteId,
            ownerUuid = "user-remote-123",
            title = "Protected Remote Item",
            secret = "JBSWY3DPEHPK3PXP",
            isLocalOnly = false,
            syncState = "SYNCED"
        )
        database.totpItemDao().upsertItem(remoteItem)

        // Attempt deletion via ID
        totpViewModel.deleteItem(remoteId)
        shadowOf(Looper.getMainLooper()).idle()

        assertNotNull(database.totpItemDao().getItemById(remoteId))

        // Attempt deletion via Entity
        totpViewModel.deleteItem(remoteItem)
        shadowOf(Looper.getMainLooper()).idle()

        assertNotNull(database.totpItemDao().getItemById(remoteId))
    }

    @Test
    fun testLocalItemCanBeEditedAndDeleted() = runBlocking {
        val localId = UUID.randomUUID().toString()
        val localItem = TotpItemEntity(
            id = localId,
            ownerUuid = "local",
            title = "My Local Key",
            username = "me@local.org",
            category = "Personal",
            secret = "JBSWY3DPEHPK3PXP",
            isLocalOnly = true,
            syncState = "LOCAL"
        )
        database.totpItemDao().upsertItem(localItem)

        // Edit
        totpViewModel.updateItemDetails(
            id = localId,
            title = "My Local Key Updated",
            username = "me_new@local.org",
            category = "Work"
        )
        var updated: TotpItemEntity? = null
        for (i in 1..25) {
            shadowOf(Looper.getMainLooper()).idle()
            kotlinx.coroutines.delay(20)
            updated = database.totpItemDao().getItemById(localId)
            if (updated?.title == "My Local Key Updated") break
        }

        assertNotNull(updated)
        assertEquals("My Local Key Updated", updated?.title)

        // Delete
        totpViewModel.deleteItem(localId)
        for (i in 1..25) {
            shadowOf(Looper.getMainLooper()).idle()
            kotlinx.coroutines.delay(20)
            if (database.totpItemDao().getItemById(localId) == null) break
        }

        assertNull(database.totpItemDao().getItemById(localId))
    }

    @Test
    fun testManualSecretAddedIsStrictlyLocalOnly() = runBlocking {
        totpViewModel.addManualSecret(
            title = "GitHub Personal",
            username = "dev@example.com",
            category = "Dev",
            secret = "JBSWY3DPEHPK3PXP"
        )
        var added: TotpItemEntity? = null
        for (i in 1..25) {
            shadowOf(Looper.getMainLooper()).idle()
            kotlinx.coroutines.delay(20)
            val items = database.totpItemDao().observeAllTotpItems("local").first()
            added = items.firstOrNull { it.title == "GitHub Personal" }
            if (added != null) break
        }
        assertNotNull(added)
        assertTrue(added!!.isLocalOnly)
        assertEquals("LOCAL", added.syncState)
    }
}
