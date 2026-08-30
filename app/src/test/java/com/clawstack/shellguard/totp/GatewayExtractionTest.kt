package com.clawstack.shellguard.totp

import com.clawstack.shellguard.totp.ui.screens.cleanAndExtractKey
import org.junit.Assert.assertEquals
import org.junit.Test

class GatewayExtractionTest {

    @Test
    fun testExtractFromRawHuKey() {
        val raw = "hu-abyssal-test-key-999"
        assertEquals("hu-abyssal-test-key-999", cleanAndExtractKey(raw))
    }

    @Test
    fun testExtractFromRawLbKey() {
        val raw = "lb-lobster-test-key-123"
        assertEquals("lb-lobster-test-key-123", cleanAndExtractKey(raw))
    }

    @Test
    fun testExtractFromJsonFile() {
        val json = """
            {
                "version": 1,
                "token": "hu-exported-token-file-555"
            }
        """.trimIndent()
        assertEquals("hu-exported-token-file-555", cleanAndExtractKey(json))
    }
}
