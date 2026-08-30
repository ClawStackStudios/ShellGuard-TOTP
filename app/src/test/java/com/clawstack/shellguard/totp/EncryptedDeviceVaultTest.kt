package com.clawstack.shellguard.totp

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.clawstack.shellguard.totp.crypto.EncryptedDeviceVault
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
class EncryptedDeviceVaultTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testStringEncryptionDecryptionRoundTrip() {
        val sensitiveData = "claw_hu_secret_token_1234567890_!@#"
        val encryptedEnvelope = EncryptedDeviceVault.encryptString(sensitiveData)

        assertNotNull(encryptedEnvelope)
        assertTrue(encryptedEnvelope.contains(":"))
        assertNotEquals(sensitiveData, encryptedEnvelope)

        val decrypted = EncryptedDeviceVault.decryptString(encryptedEnvelope)
        assertEquals(sensitiveData, decrypted)
    }

    @Test
    fun testUniqueInitializationVectors() {
        val payload = "identical_input_secret"
        val enc1 = EncryptedDeviceVault.encryptString(payload)
        val enc2 = EncryptedDeviceVault.encryptString(payload)

        // Ensure distinct IVs and ciphertexts are produced (IND-CPA security)
        assertNotEquals(enc1, enc2)
        assertEquals(payload, EncryptedDeviceVault.decryptString(enc1))
        assertEquals(payload, EncryptedDeviceVault.decryptString(enc2))
    }

    @Test
    fun testDatabasePassphraseGenerationAndPersistence() {
        val passphrase1 = EncryptedDeviceVault.getOrCreateDatabasePassphrase(context)
        assertNotNull(passphrase1)
        assertEquals(32, passphrase1.size)

        // Subsequent call must return the exact same persisted passphrase
        val passphrase2 = EncryptedDeviceVault.getOrCreateDatabasePassphrase(context)
        assertTrue(passphrase1.contentEquals(passphrase2))
    }

    @Test
    fun testSecureStringStorageAndRemoval() {
        val key = "test_sensitive_credential"
        val value = "session_token_xyz_987"

        EncryptedDeviceVault.storeSecureString(context, key, value)
        val retrieved = EncryptedDeviceVault.getSecureString(context, key)
        assertEquals(value, retrieved)

        EncryptedDeviceVault.removeSecureString(context, key)
        val afterRemoval = EncryptedDeviceVault.getSecureString(context, key)
        assertNull(afterRemoval)
    }

    @Test
    fun testEncryptionWithoutCallerProvidedIVGeneratesValidPayload() {
        for (i in 0..10) {
            val sample = "test_key_sample_$i"
            val enc = EncryptedDeviceVault.encryptString(sample)
            val parts = enc.split(":")
            assertEquals(2, parts.size)
            assertTrue(parts[0].isNotBlank())
            assertTrue(parts[1].isNotBlank())
            val decrypted = EncryptedDeviceVault.decryptString(enc)
            assertEquals(sample, decrypted)
        }
    }
}
