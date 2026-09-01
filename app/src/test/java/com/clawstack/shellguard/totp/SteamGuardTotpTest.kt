package com.clawstack.shellguard.totp

import com.clawstack.shellguard.totp.engine.HashAlgorithm
import com.clawstack.shellguard.totp.engine.TotpEngine
import com.clawstack.shellguard.totp.engine.TotpUriParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SteamGuardTotpTest {

    private val steamChars = "23456789BCDFGHJKMNPQRTVWXY"
    private val sampleSecret = "HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ"

    @Test
    fun testSteamGuardAlphanumericCharacterSet() {
        val timestamp = 1700000000000L // arbitrary fixed timestamp
        val code = TotpEngine.generateSteamGuardCode(sampleSecret, timestamp)

        assertEquals("Steam code must be exactly 5 characters", 5, code.length)
        assertTrue("Every character must exist in the Steam Guard Base32 alphabet: $code",
            code.all { it in steamChars }
        )
    }

    @Test
    fun testSteamGuardDeterministicGeneration() {
        val t0 = 1700000000000L
        val code1 = TotpEngine.generateSteamGuardCode(sampleSecret, t0)
        val code2 = TotpEngine.generateSteamGuardCode(sampleSecret, t0 + 10000L) // same 30s window

        assertEquals("Codes within the same 30s window must match", code1, code2)

        val tNextWindow = t0 + 31000L // next 30s window
        val codeNext = TotpEngine.generateSteamGuardCode(sampleSecret, tNextWindow)

        assertEquals(5, codeNext.length)
        assertTrue(codeNext.all { it in steamChars })
    }

    @Test
    fun testTotpEngineAutoRoutesSteamAlgorithm() {
        val timestamp = 1700000000000L

        // Route via HashAlgorithm.STEAM
        val codeViaAlg = TotpEngine.generateTotp(
            secretBase32 = sampleSecret,
            timestampMillis = timestamp,
            algorithm = HashAlgorithm.STEAM
        )
        assertEquals(5, codeViaAlg.length)
        assertTrue(codeViaAlg.all { it in steamChars })

        // Route via digits = 5
        val codeViaDigits = TotpEngine.generateTotp(
            secretBase32 = sampleSecret,
            timestampMillis = timestamp,
            digits = 5
        )
        assertEquals(codeViaAlg, codeViaDigits)
    }

    @Test
    fun testSteamGuardSecretFormattingAndEdgeCases() {
        // Lowercase with spaces and hyphens
        val formattedSecret = "hxdm-vjec-jjws-rb3h-wizr-4ifu-gftm-xboz"
        val timestamp = 1700000000000L
        val codeFormatted = TotpEngine.generateSteamGuardCode(formattedSecret, timestamp)
        val codeClean = TotpEngine.generateSteamGuardCode(sampleSecret, timestamp)

        assertEquals(codeClean, codeFormatted)

        // Blank secret fallback
        val emptyCode = TotpEngine.generateSteamGuardCode("", timestamp)
        assertEquals("-----", emptyCode)

        val blankCode = TotpEngine.generateSteamGuardCode("   ", timestamp)
        assertEquals("-----", blankCode)
    }

    @Test
    fun testSteamUriParsingIntegration() {
        // Direct secret in steam:// scheme
        val uri1 = "steam://HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ"
        val parsed1 = TotpUriParser.parse(uri1)
        assertNotNull(parsed1)
        assertEquals("Steam", parsed1!!.title)
        assertEquals("HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ", parsed1.secret)
        assertEquals("STEAM", parsed1.algorithm)
        assertEquals(5, parsed1.digits)

        // Standard otpauth with algorithm=STEAM
        val uri2 = "otpauth://totp/Steam:lucas?secret=HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ&issuer=Steam&algorithm=STEAM"
        val parsed2 = TotpUriParser.parse(uri2)
        assertNotNull(parsed2)
        assertEquals("Steam", parsed2!!.title)
        assertEquals("lucas", parsed2.username)
        assertEquals("STEAM", parsed2.algorithm)
        assertEquals(5, parsed2.digits)
    }
}
