package com.clawstack.shellguard.totp.data.migration

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.InputStream
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class MigrationParseResult(
    val items: List<ParsedTotpItem>,
    val schemaType: BackupSchemaType,
    val warnings: List<String>
)

object MultiFormatMigrationEngine {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parseStream(inputStream: InputStream, password: String? = null, emailForSalt: String? = null): MigrationParseResult {
        val content = inputStream.bufferedReader().use { it.readText() }
        return parseString(content, password, emailForSalt)
    }

    fun parseString(content: String, password: String? = null, emailForSalt: String? = null): MigrationParseResult {
        val rootElement = try {
            json.parseToJsonElement(content)
        } catch (e: Exception) {
            return MigrationParseResult(emptyList(), BackupSchemaType.UNKNOWN, listOf("Invalid JSON format"))
        }

        val warnings = mutableListOf<String>()

        val jsonStringToParse = if (rootElement is JsonObject && rootElement["encrypted"]?.jsonPrimitive?.booleanOrNull == true) {
            if (password.isNullOrBlank()) {
                return MigrationParseResult(emptyList(), BackupSchemaType.BITWARDEN_VAULT, listOf("Encrypted export requires a password"))
            }
            val salt = emailForSalt ?: "user@example.com"
            try {
                decryptBitwardenExport(rootElement, password, salt)
            } catch (e: Exception) {
                return MigrationParseResult(emptyList(), BackupSchemaType.BITWARDEN_VAULT, listOf("Decryption failed: ${e.message}"))
            }
        } else {
            content
        }

        val parsedRootElement = try {
            json.parseToJsonElement(jsonStringToParse)
        } catch (e: Exception) {
            return MigrationParseResult(emptyList(), BackupSchemaType.UNKNOWN, listOf("Invalid JSON format after decryption"))
        }

        val schemaType = when {
            BitwardenSanitizer.isBitwardenAuthenticator(parsedRootElement) -> BackupSchemaType.BITWARDEN_AUTHENTICATOR
            BitwardenSanitizer.isBitwardenVault(parsedRootElement) -> BackupSchemaType.BITWARDEN_VAULT
            AegisTwoFasParser.isAegis(parsedRootElement) -> BackupSchemaType.AEGIS
            AegisTwoFasParser.isTwoFas(parsedRootElement) -> BackupSchemaType.TWO_FAS
            else -> BackupSchemaType.UNKNOWN
        }

        val parsedItems = when (schemaType) {
            BackupSchemaType.BITWARDEN_AUTHENTICATOR -> BitwardenSanitizer.sanitizeBitwardenAuthenticator(jsonStringToParse)
            BackupSchemaType.BITWARDEN_VAULT -> BitwardenSanitizer.sanitizeBitwardenVault(jsonStringToParse)
            BackupSchemaType.AEGIS -> AegisTwoFasParser.parseAegis(jsonStringToParse)
            BackupSchemaType.TWO_FAS -> AegisTwoFasParser.parseTwoFas(jsonStringToParse)
            else -> {
                warnings.add("Unsupported backup format")
                emptyList()
            }
        }

        // TODO(icon-packs): Compound title normalization is ready for Phase 14 IconPackManager matching.
        val normalizedItems = parsedItems.map { normalizeCompoundTitle(it) }

        return MigrationParseResult(normalizedItems, schemaType, warnings)
    }

    private fun normalizeCompoundTitle(item: ParsedTotpItem): ParsedTotpItem {
        if (!item.username.isNullOrBlank()) {
            return item
        }

        val separators = listOf(" - ", ": ", "-", ":")
        for (sep in separators) {
            if (item.title.contains(sep)) {
                val parts = item.title.split(sep, limit = 2)
                if (parts.size == 2 && parts[1].isNotBlank()) {
                    return item.copy(
                        title = parts[0].trim(),
                        username = parts[1].trim()
                    )
                }
            }
        }
        return item
    }

    private fun decryptBitwardenExport(rootElement: JsonObject, password: String, emailAsSalt: String): String {
        val kdf = rootElement["kdf"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0 // 0 = PBKDF2 SHA256
        val kdfIterations = rootElement["kdfIterations"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 600000
        val dataString = rootElement["data"]?.jsonPrimitive?.contentOrNull ?: throw Exception("Missing data field")

        if (!dataString.startsWith("2.")) {
            throw Exception("Unsupported enc type. Only type 2 (AES-256-CBC-HMAC-SHA256) is supported.")
        }

        val parts = dataString.substring(2).split("|")
        if (parts.size != 3) throw Exception("Invalid data format")

        val iv = decodeBase64(parts[0])
        val ciphertext = decodeBase64(parts[1])
        val mac = decodeBase64(parts[2])

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")

        val spec = PBEKeySpec(password.toCharArray(), emailAsSalt.toByteArray(), kdfIterations, 512)
        val key = factory.generateSecret(spec).encoded

        val encKey = SecretKeySpec(key.copyOfRange(0, 32), "AES")
        val macKey = SecretKeySpec(key.copyOfRange(32, 64), "HmacSHA256")

        val hmac = Mac.getInstance("HmacSHA256")
        hmac.init(macKey)
        hmac.update(iv)
        val computedMac = hmac.doFinal(ciphertext)
        if (!computedMac.contentEquals(mac)) {
            throw Exception("MAC verification failed")
        }

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, encKey, IvParameterSpec(iv))
        val decrypted = cipher.doFinal(ciphertext)

        return String(decrypted, StandardCharsets.UTF_8)
    }

    private fun decodeBase64(str: String): ByteArray {
        return try {
            val clazz = Class.forName("android.util.Base64")
            clazz.getMethod("decode", String::class.java, Int::class.java).invoke(null, str, 0) as ByteArray
        } catch (e: Throwable) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                java.util.Base64.getDecoder().decode(str)
            } else {
                throw Exception("Base64 decoding failed")
            }
        }
    }
}
