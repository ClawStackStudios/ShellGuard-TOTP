package com.clawstack.shellguard.totp

import com.clawstack.shellguard.totp.engine.TotpUriParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TotpUriParserTest {

    @Test
    fun testParseStandardOtpauthUri() {
        val uri = "otpauth://totp/GitHub:octocat?secret=JBSWY3DPEHPK3PXP&issuer=GitHub&algorithm=SHA1&digits=6&period=30"
        val parsed = TotpUriParser.parse(uri)

        assertNotNull(parsed)
        assertEquals("GitHub", parsed?.title)
        assertEquals("octocat", parsed?.username)
        assertEquals("JBSWY3DPEHPK3PXP", parsed?.secret)
        assertEquals("GitHub", parsed?.issuer)
        assertEquals("SHA1", parsed?.algorithm)
        assertEquals(6, parsed?.digits)
        assertEquals(30, parsed?.period)
    }

    @Test
    fun testParseCustomAlgorithmAndDigitsUri() {
        val uri = "otpauth://totp/EnterpriseVault:admin@corp.io?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&issuer=EnterpriseVault&algorithm=SHA256&digits=8&period=60"
        val parsed = TotpUriParser.parse(uri)

        assertNotNull(parsed)
        assertEquals("EnterpriseVault", parsed?.title)
        assertEquals("admin@corp.io", parsed?.username)
        assertEquals("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", parsed?.secret)
        assertEquals("EnterpriseVault", parsed?.issuer)
        assertEquals("SHA256", parsed?.algorithm)
        assertEquals(8, parsed?.digits)
        assertEquals(60, parsed?.period)
    }

    @Test
    fun testParseRawBase32Secret() {
        val rawSecret = "JBSW Y3DP EHPK 3PXP"
        val parsed = TotpUriParser.parse(rawSecret)

        assertNotNull(parsed)
        assertEquals("JBSWY3DPEHPK3PXP", parsed?.secret)
        assertEquals("Imported 2FA", parsed?.title)
    }

    @Test
    fun testParseDashSeparatedBase32Secret() {
        val rawSecret = "JBSW-Y3DP-EHPK-3PXP"
        val parsed = TotpUriParser.parse(rawSecret)

        assertNotNull(parsed)
        assertEquals("JBSWY3DPEHPK3PXP", parsed?.secret)
        assertEquals("Imported 2FA", parsed?.title)
    }

    @Test
    fun testParseInvalidString() {
        val invalid = "not-a-valid-uri-or-secret"
        val parsed = TotpUriParser.parse(invalid)

        assertNull(parsed)
    }

    @Test
    fun testParseSteamUriDirectSecret() {
        val uri = "steam://HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ"
        val parsed = TotpUriParser.parse(uri)

        assertNotNull(parsed)
        assertEquals("Steam", parsed?.title)
        assertEquals("Steam", parsed?.issuer)
        assertEquals("HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ", parsed?.secret)
        assertEquals("STEAM", parsed?.algorithm)
        assertEquals(5, parsed?.digits)
        assertEquals(30, parsed?.period)
    }

    @Test
    fun testParseSteamUriWithLabelAndAccount() {
        val uri = "steam://Steam:gamer_lucas?secret=HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ"
        val parsed = TotpUriParser.parse(uri)

        assertNotNull(parsed)
        assertEquals("Steam", parsed?.title)
        assertEquals("gamer_lucas", parsed?.username)
        assertEquals("Steam", parsed?.issuer)
        assertEquals("HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ", parsed?.secret)
        assertEquals("STEAM", parsed?.algorithm)
        assertEquals(5, parsed?.digits)
    }

    @Test
    fun testParseOtpauthSteamAlgorithm() {
        val uri = "otpauth://totp/Steam:gamer_lucas?secret=HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ&issuer=Steam&algorithm=STEAM"
        val parsed = TotpUriParser.parse(uri)

        assertNotNull(parsed)
        assertEquals("Steam", parsed?.title)
        assertEquals("gamer_lucas", parsed?.username)
        assertEquals("STEAM", parsed?.algorithm)
        assertEquals(5, parsed?.digits)
    }
}

