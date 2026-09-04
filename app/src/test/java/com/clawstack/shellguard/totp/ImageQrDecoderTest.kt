package com.clawstack.shellguard.totp

import androidx.test.core.app.ApplicationProvider
import com.clawstack.shellguard.totp.scanner.ImageQrDecoder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import android.net.Uri

/**
 * Phase 10 / Task 19 — Image QR Decoder pipeline tests.
 */
@RunWith(RobolectricTestRunner::class)
class ImageQrDecoderTest {

    @Test
    fun decodeWithUnresolvableUriReturnsFailure() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val bogusUri = Uri.parse("content://nonexistent-provider/ghost_image.png")

        var received: ImageQrDecoder.DecodeResult? = null
        ImageQrDecoder.decode(context, bogusUri) { received = it }

        // Wait for the async result (fromFilePath failure path is synchronous).
        var waits = 0
        while (received == null && waits < 50) {
            Thread.sleep(20)
            waits++
        }

        assertTrue("Decoder must always produce a result", received != null)
        assertTrue(
            "Unresolvable URI must yield Failure",
            received is ImageQrDecoder.DecodeResult.Failure
        )
    }

    @Test
    fun decodeResultSealedHierarchyIsExhaustive() {
        val results = listOf(
            ImageQrDecoder.DecodeResult.Success("otpauth://totp/Test?secret=ABC"),
            ImageQrDecoder.DecodeResult.NoBarcodeFound,
            ImageQrDecoder.DecodeResult.Failure("boom")
        )
        assertEquals(3, results.size)
        assertTrue(results[0] is ImageQrDecoder.DecodeResult.Success)
        assertEquals("otpauth://totp/Test?secret=ABC", (results[0] as ImageQrDecoder.DecodeResult.Success).rawValue)
    }
}
