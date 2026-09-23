package com.clawstack.shellguard.totp

import com.clawstack.shellguard.totp.data.local.entities.TotpItemEntity
import com.clawstack.shellguard.totp.data.remote.models.PearlDto
import com.clawstack.shellguard.totp.data.repository.TotpRepository
import com.clawstack.shellguard.totp.engine.TotpUriParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 12 / Task 24b: Test Oracle for TotpRepository and Web Server v0.0.2.3 Sync Parity.
 * Verifies dynamic RFC 6238 TOTP URI decoding (SHA256, 8 digits, custom periods),
 * content-delta fast comparison, and delta classification resilience against null updated_at.
 */
class TotpRepositoryTest {

    @Test
    fun syncRawBase32Pearl_storesCorrectSecret() {
        val rawSeed = "JBSWY3DPEHPK3PXP"
        val parsed = TotpUriParser.parse(rawSeed)
        assertNotNull("Raw Base32 must be parsed", parsed)
        assertEquals("JBSWY3DPEHPK3PXP", parsed!!.secret)
        assertEquals("SHA1", parsed.algorithm)
        assertEquals(6, parsed.digits)
        assertEquals(30, parsed.period)
    }

    @Test
    fun syncOtpauthUriSha256_8digit_populatesFields() {
        val uri = "otpauth://totp/Acme:alice@example.com?secret=JBSWY3DPEHPK3PXP&algorithm=SHA256&digits=8&period=30"
        val parsed = TotpUriParser.parse(uri)
        assertNotNull("otpauth URI with SHA256/8-digits must be parsed", parsed)
        assertEquals("JBSWY3DPEHPK3PXP", parsed!!.secret)
        assertEquals("SHA256", parsed.algorithm)
        assertEquals(8, parsed.digits)
        assertEquals(30, parsed.period)
        assertEquals("Acme", parsed.title)
        assertEquals("alice@example.com", parsed.username)
    }

    @Test
    fun syncOtpauthUri_custom60sPeriod_populatesField() {
        val uri = "otpauth://totp/Server:bob?secret=JBSWY3DPEHPK3PXP&period=60"
        val parsed = TotpUriParser.parse(uri)
        assertNotNull("otpauth URI with custom 60s period must be parsed", parsed)
        assertEquals("JBSWY3DPEHPK3PXP", parsed!!.secret)
        assertEquals("SHA1", parsed.algorithm)
        assertEquals(6, parsed.digits)
        assertEquals(60, parsed.period)
    }

    @Test
    fun isContentIdentical_differentiatesDifferences() {
        val local = TotpItemEntity(
            id = "pearl-1",
            ownerUuid = "user-123",
            title = "Test Service",
            username = "user",
            category = "Work",
            secret = "JBSWY3DPEHPK3PXP",
            algorithm = "SHA256",
            digits = 8,
            period = 60,
            isLocalOnly = false,
            syncState = "SYNCED",
            remoteUpdatedAt = null,
            localUpdatedAt = 1000L
        )

        // Same content with different localUpdatedAt should be identical
        val sameContent = local.copy(localUpdatedAt = 5000L)
        assertTrue(TotpRepository.isContentIdentical(local, sameContent))

        // Different secret must NOT be identical
        val diffSecret = local.copy(secret = "OTHERSECRET23456")
        assertFalse(TotpRepository.isContentIdentical(local, diffSecret))

        // Different algorithm must NOT be identical
        val diffAlgo = local.copy(algorithm = "SHA1")
        assertFalse(TotpRepository.isContentIdentical(local, diffAlgo))

        // Different digits must NOT be identical
        val diffDigits = local.copy(digits = 6)
        assertFalse(TotpRepository.isContentIdentical(local, diffDigits))

        // Different period must NOT be identical
        val diffPeriod = local.copy(period = 30)
        assertFalse(TotpRepository.isContentIdentical(local, diffPeriod))

        // Different title must NOT be identical
        val diffTitle = local.copy(title = "Updated Title")
        assertFalse(TotpRepository.isContentIdentical(local, diffTitle))
    }

    @Test
    fun classifyDeltaPearls_nullUpdatedAt_alwaysMarksChanged() {
        val localSnapshot = mapOf("pearl-null" to null)
        val pearl = PearlDto(
            id = "pearl-null",
            owner_uuid = "user-123",
            title = "Service",
            totp_secret = "secret",
            updated_at = null
        )

        val (unchanged, changed) = TotpRepository.classifyDeltaPearls(localSnapshot, listOf(pearl))
        assertTrue("Null updated_at must not be classified as unchanged", unchanged.isEmpty())
        assertEquals(1, changed.size)
        assertEquals("pearl-null", changed[0].id)
    }
}
