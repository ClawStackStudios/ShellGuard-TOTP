package com.clawstack.shellguard.totp.data.migration

import com.clawstack.shellguard.totp.data.local.entities.TotpItemEntity
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Encodes TotpItemEntity records into Google Authenticator otpauth-migration offline protocol URIs.
 * Uses a lightweight, manual protobuf binary serialization to avoid heavy plugin dependencies.
 */
object GoogleAuthMigrationEncoder {

    /**
     * Encodes a list of TOTP items into a list of otpauth-migration URIs.
     * Chunks items to a maximum of 10 per URI to ensure QR codes are small enough for reliable scanning.
     */
    fun encodeToMigrationUris(items: List<TotpItemEntity>, maxPerChunk: Int = 10): List<String> {
        if (items.isEmpty()) return emptyList()

        return items.chunked(maxPerChunk).map { chunk ->
            val payloadBytes = encodeMigrationPayload(chunk)
            val base64Payload = base64Encode(payloadBytes)
            val urlEncodedPayload = URLEncoder.encode(base64Payload, StandardCharsets.UTF_8.name())
            "otpauth-migration://offline?data=${urlEncodedPayload}"
        }
    }

    private fun base64Encode(bytes: ByteArray): String {
        return try {
            val clazz = Class.forName("android.util.Base64")
            clazz.getMethod("encodeToString", ByteArray::class.java, Int::class.java)
                .invoke(null, bytes, 2) as String // 2 = NO_WRAP
        } catch (e: Throwable) {
            try {
                val encoder = Class.forName("java.util.Base64").getMethod("getEncoder").invoke(null)
                encoder.javaClass.getMethod("encodeToString", ByteArray::class.java).invoke(encoder, bytes) as String
            } catch (ex: Throwable) {
                throw Exception("Base64 encode failed")
            }
        }
    }

    private fun encodeMigrationPayload(items: List<TotpItemEntity>): ByteArray {
        val out = ByteArrayOutputStream()

        for (item in items) {
            val otpParamBytes = encodeOtpParameters(item)
            writeVarint(out, (1 shl 3) or 2) // Field 1: otp_parameters (repeated message)
            writeVarint(out, otpParamBytes.size)
            out.write(otpParamBytes)
        }

        writeVarint(out, (2 shl 3) or 0) // Field 2: version (int32)
        writeVarint(out, 1) // Version 1

        writeVarint(out, (3 shl 3) or 0) // Field 3: batch_size (int32)
        writeVarint(out, items.size)

        writeVarint(out, (4 shl 3) or 0) // Field 4: batch_index (int32)
        writeVarint(out, 0)

        writeVarint(out, (5 shl 3) or 0) // Field 5: batch_id (int32)
        writeVarint(out, items.hashCode() and 0x7FFFFFFF) // Positive randomish ID

        return out.toByteArray()
    }

    private fun encodeOtpParameters(item: TotpItemEntity): ByteArray {
        val out = ByteArrayOutputStream()

        // Field 1: secret (bytes)
        val secretBytes = base32Decode(item.secret.replace(" ", "").replace("-", "").uppercase())
        writeVarint(out, (1 shl 3) or 2)
        writeVarint(out, secretBytes.size)
        out.write(secretBytes)

        // Field 2: name (string)
        val nameStr = if (item.username.isNullOrBlank()) item.title else "${item.title}:${item.username}"
        val nameBytes = nameStr.toByteArray(StandardCharsets.UTF_8)
        writeVarint(out, (2 shl 3) or 2)
        writeVarint(out, nameBytes.size)
        out.write(nameBytes)

        // Field 3: issuer (string)
        val issuerBytes = item.title.toByteArray(StandardCharsets.UTF_8)
        writeVarint(out, (3 shl 3) or 2)
        writeVarint(out, issuerBytes.size)
        out.write(issuerBytes)

        // Field 4: algorithm (Algorithm enum) -> 1=SHA1, 2=SHA256, 3=SHA512, 4=MD5
        val algInt = when (item.algorithm.uppercase()) {
            "SHA256", "HMACSHA256" -> 2
            "SHA512", "HMACSHA512" -> 3
            else -> 1 // Default to SHA1
        }
        writeVarint(out, (4 shl 3) or 0)
        writeVarint(out, algInt)

        // Field 5: digits (DigitCount enum) -> 1=SIX, 2=EIGHT
        val digitsInt = if (item.digits == 8) 2 else 1
        writeVarint(out, (5 shl 3) or 0)
        writeVarint(out, digitsInt)

        // Field 6: type (OtpType enum) -> 1=HOTP, 2=TOTP
        writeVarint(out, (6 shl 3) or 0)
        writeVarint(out, 2) // We only support TOTP currently

        return out.toByteArray()
    }

    private fun writeVarint(out: ByteArrayOutputStream, value: Int) {
        var v = value
        while ((v and 0xFFFFFF80.toInt()) != 0) {
            out.write((v and 0x7F) or 0x80)
            v = v ushr 7
        }
        out.write(v and 0x7F)
    }

    private fun base32Decode(base32: String): ByteArray {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val cleanInput = base32.trim().uppercase().replace("=", "")
        val output = ByteArrayOutputStream()

        var buffer = 0
        var bitsLeft = 0

        for (char in cleanInput) {
            val valIndex = alphabet.indexOf(char)
            if (valIndex < 0) continue

            buffer = (buffer shl 5) or valIndex
            bitsLeft += 5

            if (bitsLeft >= 8) {
                output.write((buffer shr (bitsLeft - 8)) and 0xFF)
                bitsLeft -= 8
            }
        }
        return output.toByteArray()
    }
}
