package com.clawstack.shellguard.totp.engine

import com.clawstack.shellguard.totp.crypto.ClawCrypto
import java.nio.ByteBuffer
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds

/**
 * Supported HMAC Hash Algorithms for TOTP generation (RFC 6238).
 */
enum class HashAlgorithm(val hmacName: String) {
    SHA1("HmacSHA1"),
    SHA256("HmacSHA256"),
    SHA512("HmacSHA512");

    companion object {
        fun fromString(value: String?): HashAlgorithm {
            return when (value?.uppercase()?.trim()) {
                "SHA256", "HMAC-SHA256", "HMACSHA256" -> SHA256
                "SHA512", "HMAC-SHA512", "HMACSHA512" -> SHA512
                else -> SHA1
            }
        }
    }
}

/**
 * Thread-safe RFC 6238 (TOTP) and RFC 4226 (HOTP) computation engine.
 */
object TotpEngine {
    const val DEFAULT_TIME_STEP_SECONDS = 30L
    const val DEFAULT_DIGITS = 6

    /**
     * Computes the current TOTP numeric code for a given Base32 secret.
     *
     * @param secretBase32 Base32 encoded secret key or raw ASCII secret if indicated.
     * @param timestampMillis Current epoch time in milliseconds.
     * @param timeStepSeconds Time step window in seconds (standard: 30s).
     * @param digits Number of output digits (standard: 6).
     * @param algorithm HMAC algorithm (standard: SHA1).
     * @return Zero-padded OTP code string.
     */
    fun generateTotp(
        secretBase32: String,
        timestampMillis: Long = System.currentTimeMillis(),
        timeStepSeconds: Long = DEFAULT_TIME_STEP_SECONDS,
        digits: Int = DEFAULT_DIGITS,
        algorithm: HashAlgorithm = HashAlgorithm.SHA1
    ): String {
        val cleanSecret = secretBase32.replace(" ", "").replace("-", "").uppercase()
        if (cleanSecret.isBlank()) return "------"

        val counter = (timestampMillis / 1000L) / timeStepSeconds
        return generateHotp(cleanSecret, counter, digits, algorithm)
    }

    /**
     * Computes HOTP code given a counter value (RFC 4226 Section 5.3 & 5.4).
     */
    fun generateHotp(
        secretBase32: String,
        counter: Long,
        digits: Int = DEFAULT_DIGITS,
        algorithm: HashAlgorithm = HashAlgorithm.SHA1
    ): String {
        return try {
            val secretBytes = Base32Decoder.decode(secretBase32)
            val counterBuffer = ByteBuffer.allocate(8).putLong(counter).array()

            val hmacHash = ClawCrypto.hmac(algorithm.hmacName, secretBytes, counterBuffer)

            // Dynamic Truncation (RFC 4226 Section 5.3)
            val offset = (hmacHash.last().toInt() and 0x0F)
            val binaryCode = ((hmacHash[offset].toInt() and 0x7F) shl 24) or
                    ((hmacHash[offset + 1].toInt() and 0xFF) shl 16) or
                    ((hmacHash[offset + 2].toInt() and 0xFF) shl 8) or
                    (hmacHash[offset + 3].toInt() and 0xFF)

            val modulus = 10.0.pow(digits.toDouble()).toInt()
            val otpInt = binaryCode % modulus
            "%0${digits}d".format(otpInt)
        } catch (e: Exception) {
            "------"
        }
    }

    /**
     * Computes remaining whole seconds in the current cycle (e.g., 30 down to 1).
     */
    fun getRemainingSeconds(
        timestampMillis: Long = System.currentTimeMillis(),
        timeStepSeconds: Long = DEFAULT_TIME_STEP_SECONDS
    ): Int {
        val elapsedSeconds = (timestampMillis / 1000L) % timeStepSeconds
        return (timeStepSeconds - elapsedSeconds).toInt()
    }

    /**
     * Computes remaining progress ratio (1.0f down to 0.0f) in current TOTP cycle.
     */
    fun getProgressRatio(
        timestampMillis: Long = System.currentTimeMillis(),
        timeStepSeconds: Long = DEFAULT_TIME_STEP_SECONDS
    ): Float {
        val remaining = getRemainingSeconds(timestampMillis, timeStepSeconds)
        return remaining.toFloat() / timeStepSeconds.toFloat()
    }
}

/**
 * Base32 Decoder supporting RFC 4648 standard alphabet.
 */
object Base32Decoder {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun decode(base32String: String): ByteArray {
        val clean = base32String.trim().replace("=", "").replace(" ", "").replace("-", "").uppercase()
        if (clean.isEmpty()) return ByteArray(0)

        val output = mutableListOf<Byte>()
        var buffer = 0
        var bitsLeft = 0

        for (char in clean) {
            val charValue = ALPHABET.indexOf(char)
            if (charValue < 0) continue // ignore illegal characters

            buffer = (buffer shl 5) or charValue
            bitsLeft += 5

            if (bitsLeft >= 8) {
                bitsLeft -= 8
                output.add(((buffer shr bitsLeft) and 0xFF).toByte())
            }
        }

        return output.toByteArray()
    }
}
