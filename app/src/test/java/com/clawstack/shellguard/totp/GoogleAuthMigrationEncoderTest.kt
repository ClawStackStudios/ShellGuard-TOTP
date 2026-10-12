package com.clawstack.shellguard.totp

import com.clawstack.shellguard.totp.data.local.entities.TotpItemEntity
import com.clawstack.shellguard.totp.data.migration.GoogleAuthMigrationEncoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class GoogleAuthMigrationEncoderTest {

    @Test
    fun `test single token encoding`() {
        val item = TotpItemEntity(
            id = "1",
            ownerUuid = "local",
            title = "GitHub",
            username = "lucas@example.com",
            category = "General",
            secret = "JBSWY3DPEHPK3PXP",
            algorithm = "SHA1",
            digits = 6,
            period = 30,
            isLocalOnly = true,
            syncState = "SYNCED",
            remoteUpdatedAt = null,
            localUpdatedAt = 0L
        )

        val uris = GoogleAuthMigrationEncoder.encodeToMigrationUris(listOf(item))
        assertEquals(1, uris.size)
        assertTrue(uris[0].startsWith("otpauth-migration://offline?data="))
    }

    @Test
    fun `test chunking batches into 10 tokens max`() {
        val items = (1..25).map {
            TotpItemEntity(
                id = it.toString(),
                ownerUuid = "local",
                title = "TestService${it}",
                username = "user${it}",
                category = "General",
                secret = "JBSWY3DPEHPK3PXP",
                algorithm = "SHA1",
                digits = 6,
                period = 30,
                isLocalOnly = true,
                syncState = "SYNCED",
                remoteUpdatedAt = null,
                localUpdatedAt = 0L
            )
        }

        val uris = GoogleAuthMigrationEncoder.encodeToMigrationUris(items)
        assertEquals(3, uris.size)
    }
}
