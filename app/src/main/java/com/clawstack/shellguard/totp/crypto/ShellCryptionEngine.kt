package com.clawstack.shellguard.totp.crypto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Serializable
data class ShellCryptionEnvelope(
    val v: Int = 1,
    val alg: String = "AES-GCM-256",
    val iv: String,
    val ct: String,
    val aad: String
)

/**
 * ShellCryption Engine providing 100% cryptographic parity with the ShellGuard Web Vault and API.
 * Uses HKDF-SHA256 key derivation and AES-GCM-256 authenticated encryption with strict AAD binding.
 */
object ShellCryptionEngine {
    private const val HKDF_ALGORITHM = "HmacSHA256"
    private const val HKDF_INFO_STRING = "clawchives-shellcryption-v1"
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Derives a 256-bit AES key from the master human key and user UUID using HKDF-SHA256.
     * ikm = huKey.toByteArray(), salt = userUuid.toByteArray(), info = "clawchives-shellcryption-v1", length = 32
     */
    fun deriveShellKey(huKey: String, userUuid: String): SecretKeySpec {
        val ikm = huKey.toByteArray(StandardCharsets.UTF_8)
        val salt = userUuid.toByteArray(StandardCharsets.UTF_8)
        val info = HKDF_INFO_STRING.toByteArray(StandardCharsets.UTF_8)

        val prk = hkdfExtract(salt, ikm)
        val okm = hkdfExpand(prk, info, 32)

        return SecretKeySpec(okm, "AES")
    }

    /**
     * Decrypts a ShellCryption JSON envelope string using the derived key and expected AAD.
     * Enforces strict AAD verification (e.g. "vault_pearls_totp:<recordId>") to prevent substitution attacks.
     */
    fun decryptField(
        encryptedJson: String,
        shellKey: SecretKeySpec,
        table: String,
        recordId: String
    ): String {
        val trimmed = encryptedJson.trim()
        if (trimmed.isBlank()) return ""

        val envelope = try {
            json.decodeFromString<ShellCryptionEnvelope>(trimmed)
        } catch (e: Exception) {
            // Not a valid JSON envelope, return as raw fallback
            return trimmed
        }

        val expectedAad = "$table:$recordId"
        require(envelope.aad == expectedAad) {
            "AAD mismatch! Expected '$expectedAad' but found '${envelope.aad}'. Possible substitution attack."
        }

        val iv = base64Decode(envelope.iv)
        val ciphertextWithTag = base64Decode(envelope.ct)

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, shellKey, spec)
        cipher.updateAAD(envelope.aad.toByteArray(StandardCharsets.UTF_8))

        val decryptedBytes = cipher.doFinal(ciphertextWithTag)
        return String(decryptedBytes, StandardCharsets.UTF_8)
    }

    /**
     * Encrypts a plaintext TOTP seed into a ShellCryption envelope.
     */
    fun encryptField(
        plaintext: String,
        shellKey: SecretKeySpec,
        table: String,
        recordId: String
    ): String {
        val secureRandom = SecureRandom()
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        val aadString = "$table:$recordId"
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, shellKey, spec)
        cipher.updateAAD(aadString.toByteArray(StandardCharsets.UTF_8))

        val ciphertextWithTag = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))

        val envelope = ShellCryptionEnvelope(
            v = 1,
            alg = "AES-GCM-256",
            iv = base64Encode(iv),
            ct = base64Encode(ciphertextWithTag),
            aad = aadString
        )

        return json.encodeToString(ShellCryptionEnvelope.serializer(), envelope)
    }

    private fun hkdfExtract(salt: ByteArray, ikm: ByteArray): ByteArray {
        val mac = Mac.getInstance(HKDF_ALGORITHM)
        val actualSalt = if (salt.isEmpty()) ByteArray(32) else salt
        mac.init(SecretKeySpec(actualSalt, HKDF_ALGORITHM))
        return mac.doFinal(ikm)
    }

    private fun hkdfExpand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        val mac = Mac.getInstance(HKDF_ALGORITHM)
        mac.init(SecretKeySpec(prk, HKDF_ALGORITHM))

        val result = ByteArray(length)
        var previousT = ByteArray(0)
        var offset = 0
        var blockIndex: Byte = 1

        while (offset < length) {
            mac.update(previousT)
            mac.update(info)
            mac.update(blockIndex)
            previousT = mac.doFinal()

            val toCopy = minOf(previousT.size, length - offset)
            System.arraycopy(previousT, 0, result, offset, toCopy)
            offset += toCopy
            blockIndex++
        }
        return result
    }

    private fun base64Encode(bytes: ByteArray): String {
        return java.util.Base64.getEncoder().encodeToString(bytes)
    }

    private fun base64Decode(str: String): ByteArray {
        return java.util.Base64.getDecoder().decode(str.trim())
    }
}
