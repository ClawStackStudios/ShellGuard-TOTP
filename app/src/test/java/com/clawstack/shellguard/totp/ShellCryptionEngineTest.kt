package com.clawstack.shellguard.totp

import com.clawstack.shellguard.totp.crypto.ShellCryptionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ShellCryptionEngineTest {

    @Test
    fun testHkdfKeyDerivationAndEncryptionRoundtrip() {
        val huKey = "human-master-passphrase-reef-1234"
        val userUuid = "usr_9988aabbccddee"
        val table = "vault_pearls_totp"
        val recordId = "item_123"
        val secretTotp = "JBSWY3DPEHPK3PXP"

        val derivedKey = ShellCryptionEngine.deriveShellKey(huKey, userUuid)
        assertNotNull(derivedKey)
        assertEquals("AES", derivedKey.algorithm)
        assertEquals(32, derivedKey.encoded.size) // 256-bit

        val encryptedEnvelopeJson = ShellCryptionEngine.encryptField(secretTotp, derivedKey, table, recordId)
        val decryptedSecret = ShellCryptionEngine.decryptField(encryptedEnvelopeJson, derivedKey, table, recordId)

        assertEquals(secretTotp, decryptedSecret)
    }

    @Test
    fun testAadMismatchRejection() {
        val huKey = "master-key"
        val userUuid = "uuid-1"
        val table = "vault_pearls_totp"
        val recordId = "item_123"
        val wrongRecordId = "item_999"

        val key = ShellCryptionEngine.deriveShellKey(huKey, userUuid)
        val encryptedEnvelope = ShellCryptionEngine.encryptField("SECRET_DATA", key, table, recordId)

        // Attempting to decrypt with mismatching AAD record id must fail
        assertThrows(IllegalArgumentException::class.java) {
            ShellCryptionEngine.decryptField(encryptedEnvelope, key, table, wrongRecordId)
        }
    }
}
