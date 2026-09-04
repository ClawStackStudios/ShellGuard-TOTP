package com.clawstack.shellguard.totp.scanner

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

/**
 * Task 19 — Image QR Decoder Pipeline.
 *
 * Decodes QR (and other 2FA) barcodes from gallery/screenshot image URIs using
 * Google ML Kit Barcode Scanning on [InputImage.fromFilePath] streams.
 *
 * This is the single shared pipeline for image QR decoding: used by the
 * speed dial's "Scan image" pill on the vault dashboard and by the gallery
 * picker inside [com.clawstack.shellguard.totp.ui.screens.QrScannerScreen].
 */
object ImageQrDecoder {

    /** Outcome of a decode attempt, mirroring ML Kit's success/empty/failure triad. */
    sealed interface DecodeResult {
        /** A barcode with a non-blank raw value was found. */
        data class Success(val rawValue: String) : DecodeResult

        /** The image was readable but contained no decodable 2FA barcode. */
        data object NoBarcodeFound : DecodeResult

        /** The image could not be opened or analyzed. */
        data class Failure(val message: String) : DecodeResult
    }

    /**
     * Decodes the first barcode with a non-blank raw value from [imageUri].
     * Callbacks are invoked on ML Kit's main-thread executor, matching the
     * existing TaskList-style asynchronous pattern used in the live scanner.
     */
    fun decode(
        context: Context,
        imageUri: Uri,
        onResult: (DecodeResult) -> Unit
    ) {
        val inputImage = try {
            InputImage.fromFilePath(context, imageUri)
        } catch (e: Exception) {
            onResult(DecodeResult.Failure("Error opening image: ${e.message}"))
            return
        }

        val scanner = BarcodeScanning.getClient()
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val firstBarcode = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                onResult(
                    if (firstBarcode?.rawValue != null) {
                        DecodeResult.Success(firstBarcode.rawValue!!)
                    } else {
                        DecodeResult.NoBarcodeFound
                    }
                )
            }
            .addOnFailureListener { e ->
                onResult(DecodeResult.Failure("Failed to analyze image: ${e.message}"))
            }
    }
}
