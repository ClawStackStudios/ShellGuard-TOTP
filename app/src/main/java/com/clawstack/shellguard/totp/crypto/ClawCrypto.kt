package com.clawstack.shellguard.totp.crypto

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * ShellGuard Cryptographic Core conforming to ClawStack Security Standards.
 * Handles deterministic hashing, HMAC generation for OTP engines, and secure random byte allocation.
 */
object ClawCrypto {

    private val secureRandom = SecureRandom()

    /**
     * Generates a 64-character lowercase hexadecimal SHA-256 hash string for the given raw human key.
     * Required by ClawStack key derivation protocol.
     *
     * @param rawKey The raw human-supplied passphrase/master key.
     * @return 64-character lowercase hexadecimal string representation of the SHA-256 digest.
     */
    fun hashHumanKey(rawKey: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(rawKey.toByteArray(StandardCharsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Generates a specified number of cryptographically secure random bytes.
     *
     * @param length Number of bytes to generate (defaults to 32 bytes / 256 bits).
     */
    fun generateSecureBytes(length: Int = 32): ByteArray {
        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)
        return bytes
    }

    /**
     * Computes an HMAC digest for TOTP generation using the specified algorithm (HmacSHA1, HmacSHA256, HmacSHA512).
     */
    fun hmac(algorithm: String, key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance(algorithm)
        val secretKeySpec = SecretKeySpec(key, algorithm)
        mac.init(secretKeySpec)
        return mac.doFinal(data)
    }

    /**
     * Constant-time comparison between two byte arrays to protect against timing attacks.
     */
    fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        return MessageDigest.isEqual(a, b)
    }
}
