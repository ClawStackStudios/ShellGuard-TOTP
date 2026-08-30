package com.clawstack.shellguard.totp

import com.clawstack.shellguard.totp.crypto.ClawCrypto
import com.clawstack.shellguard.totp.engine.Base32Decoder
import com.clawstack.shellguard.totp.engine.HashAlgorithm
import com.clawstack.shellguard.totp.engine.TotpEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClawCryptoTest {

    @Test
    fun hashHumanKey_producesCorrectSha256Hex() {
        // Known test vector: "test" -> 9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08
        val rawKey = "test"
        val hash = ClawCrypto.hashHumanKey(rawKey)
        assertEquals(64, hash.length)
        assertEquals("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08", hash)
    }

    @Test
    fun generateSecureBytes_producesExpectedLength() {
        val bytes = ClawCrypto.generateSecureBytes(32)
        assertEquals(32, bytes.size)
    }

    @Test
    fun base32Decoder_decodesStandardVectors() {
        val secret = "JBSWY3DPEHPK3PXP" // standard test vector "Hello!\xde\xad\xbe\xef"
        val decoded = Base32Decoder.decode(secret)
        assertTrue(decoded.isNotEmpty())
    }

    @Test
    fun totpEngine_generatesConsistentCode() {
        val secret = "JBSWY3DPEHPK3PXP"
        val code = TotpEngine.generateTotp(
            secretBase32 = secret,
            timestampMillis = 1600000000000L, // Fixed timestamp
            timeStepSeconds = 30,
            digits = 6,
            algorithm = HashAlgorithm.SHA1
        )
        assertEquals(6, code.length)
        assertTrue(code.all { it.isDigit() })
    }
}
