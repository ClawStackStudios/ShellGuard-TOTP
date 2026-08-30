package com.clawstack.shellguard.totp

import com.clawstack.shellguard.totp.engine.Base32Decoder
import com.clawstack.shellguard.totp.engine.HashAlgorithm
import com.clawstack.shellguard.totp.engine.TotpEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TotpEngineTest {

    @Test
    fun testBase32Decoder() {
        // "JBSWY3DP" is "Hello" in Base32 (RFC 4648)
        val decoded = Base32Decoder.decode("JBSWY3DP")
        val text = String(decoded)
        assertEquals("Hello", text)
    }

    @Test
    fun testTotpEngineStandardRfc6238() {
        // Standard test secret "12345678901234567890" in Base32 is "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
        val secretBase32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"

        // T0 = 59s -> step = 1
        val code1 = TotpEngine.generateTotp(
            secretBase32 = secretBase32,
            timestampMillis = 59000L,
            timeStepSeconds = 30L,
            digits = 6,
            algorithm = HashAlgorithm.SHA1
        )
        assertEquals(6, code1.length)
        assertTrue(code1.all { it.isDigit() })

        // T1 = 1111111109s (RFC 6238 Test Vector: 07081804 for 8 digits)
        val codeSha1_8digits = TotpEngine.generateTotp(
            secretBase32 = secretBase32,
            timestampMillis = 1111111109000L,
            timeStepSeconds = 30L,
            digits = 8,
            algorithm = HashAlgorithm.SHA1
        )
        assertEquals("07081804", codeSha1_8digits)
    }

    @Test
    fun testRemainingSecondsCalculation() {
        // At 59s with 30s step, elapsed = 29s, remaining = 1s
        val remaining = TotpEngine.getRemainingSeconds(59000L, 30L)
        assertEquals(1, remaining)

        // At 60s with 30s step, elapsed = 0s, remaining = 30s
        val remainingStart = TotpEngine.getRemainingSeconds(60000L, 30L)
        assertEquals(30, remainingStart)
    }
}
