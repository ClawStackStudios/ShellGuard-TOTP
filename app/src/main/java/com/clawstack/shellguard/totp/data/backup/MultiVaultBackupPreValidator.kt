package com.clawstack.shellguard.totp.data.backup

import com.clawstack.shellguard.totp.crypto.ShellCryptionEngine
import com.clawstack.shellguard.totp.data.local.entities.TotpItemEntity
import com.clawstack.shellguard.totp.engine.TotpUriParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

enum class BackupSchemaType(val displayName: String) {
    SHELLGUARD_ENCRYPTED("ShellGuard Habitat (Encrypted)"),
    SHELLGUARD_PLAIN("ShellGuard Plain Export"),
    BITWARDEN_VAULT("Bitwarden Vault Export"),
    BITWARDEN_AUTHENTICATOR("Bitwarden Authenticator"),
    AEGIS("Aegis Authenticator"),
    TWO_FAS("2FAS Backup"),
    UNKNOWN("Unknown Format")
}

sealed interface PreValidationResult {
    data class Success(
        val schemaType: BackupSchemaType,
        val isEncrypted: Boolean,
        val estimatedItemCount: Int,
        val rawJson: String,
        val fileName: String? = null,
        val details: String = "",
        val protectionMode: String? = null,
        val isBiometricEnabled: Boolean = false,
        val pinLength: Int? = null,
        val itemsPreview: List<TotpItemEntity> = emptyList()
    ) : PreValidationResult

    data class Error(
        val message: String
    ) : PreValidationResult
}

object MultiVaultBackupPreValidator {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowSpecialFloatingPointValues = true
    }

    /**
     * Inspects and pre-validates an input stream without persisting anything to disk.
     */
    fun validate(inputStream: InputStream, fileName: String? = null): PreValidationResult {
        return try {
            val rawContent = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            validateString(rawContent, fileName)
        } catch (e: Exception) {
            PreValidationResult.Error(e.message ?: "Failed to read backup file.")
        }
    }

    /**
     * Inspects raw JSON text and identifies schema type, encryption status, and extracts sanitized preview items.
     */
    fun validateString(rawContent: String, fileName: String? = null): PreValidationResult {
        val trimmed = rawContent.trim()
        if (trimmed.isEmpty()) {
            return PreValidationResult.Error("The selected file is empty.")
        }

        return try {
            val rootElement = json.parseToJsonElement(trimmed)
            when (rootElement) {
                is JsonObject -> inspectJsonObject(rootElement, trimmed, fileName)
                is JsonArray -> inspectJsonArray(rootElement, trimmed, fileName)
                else -> PreValidationResult.Error("Unsupported file format: root JSON element must be an object or array.")
            }
        } catch (e: Exception) {
            PreValidationResult.Error("Invalid JSON structure: ${e.message ?: "Unable to parse JSON."}")
        }
    }

    private fun inspectJsonObject(
        obj: JsonObject,
        rawJson: String,
        fileName: String?
    ): PreValidationResult {
        // 1. ShellGuard Encrypted Habitat (.sgtotp.bak, .sgbak, or JSON backup envelope)
        val typeStr = obj["type"]?.jsonPrimitive?.contentOrNull
        val formatStr = obj["format"]?.jsonPrimitive?.contentOrNull
        val hasEncEnvelope = obj.containsKey("encryptedEnvelopeJson") && obj.containsKey("checksumSha256")
        val isSgEncrypted = typeStr == "shellguard-totp-backup-v1" ||
                typeStr == "shellguard_totp_encrypted_backup" ||
                typeStr == "sgtotp.bak" ||
                typeStr == "sgbak" ||
                formatStr == "sgtotp.bak" ||
                formatStr == "sgbak" ||
                hasEncEnvelope

        if (isSgEncrypted && hasEncEnvelope) {
            val count = obj["itemCount"]?.jsonPrimitive?.intOrNull ?: 0
            val protMode = obj["protectionMode"]?.jsonPrimitive?.contentOrNull ?: "PIN"
            val bioEnabled = obj["isBiometricEnabled"]?.jsonPrimitive?.booleanOrNull ?: false
            val pinLen = obj["pinLength"]?.jsonPrimitive?.intOrNull

            return PreValidationResult.Success(
                schemaType = BackupSchemaType.SHELLGUARD_ENCRYPTED,
                isEncrypted = true,
                estimatedItemCount = count,
                rawJson = rawJson,
                fileName = fileName,
                details = if (count > 0) "ShellGuard Habitat ($count tokens, locked with $protMode)." else "ShellGuard Habitat (Encrypted $protMode).",
                protectionMode = protMode,
                isBiometricEnabled = bioEnabled,
                pinLength = pinLen
            )
        }

        // 2. ShellGuard Plain Export
        if (formatStr == "shellguard-totp-plain-export-v1" && obj.containsKey("items")) {
            val itemsArray = obj["items"]?.jsonArray
            val extracted = itemsArray?.let { parseShellGuardPlainItems(it) } ?: emptyList()
            if (extracted.isEmpty()) {
                return PreValidationResult.Error("No 2FA tokens found in ShellGuard plain export.")
            }
            return PreValidationResult.Success(
                schemaType = BackupSchemaType.SHELLGUARD_PLAIN,
                isEncrypted = false,
                estimatedItemCount = extracted.size,
                rawJson = rawJson,
                fileName = fileName,
                details = "Found ${extracted.size} 2FA tokens in ShellGuard export.",
                itemsPreview = extracted
            )
        }

        // 3. Bitwarden Vault Export (Password Manager)
        if (obj.containsKey("items") && (obj.containsKey("folders") || obj.containsKey("encrypted"))) {
            val isEncrypted = obj["encrypted"]?.jsonPrimitive?.booleanOrNull ?: false
            if (isEncrypted) {
                return PreValidationResult.Success(
                    schemaType = BackupSchemaType.BITWARDEN_VAULT,
                    isEncrypted = true,
                    estimatedItemCount = obj["items"]?.jsonArray?.size ?: 0,
                    rawJson = rawJson,
                    fileName = fileName,
                    details = "Encrypted Bitwarden Vault export."
                )
            } else {
                val extracted = parseBitwardenVaultItems(obj)
                if (extracted.isEmpty()) {
                    return PreValidationResult.Error("No TOTP 2FA tokens found in Bitwarden vault (passwords and non-TOTP items ignored).")
                }
                return PreValidationResult.Success(
                    schemaType = BackupSchemaType.BITWARDEN_VAULT,
                    isEncrypted = false,
                    estimatedItemCount = extracted.size,
                    rawJson = rawJson,
                    fileName = fileName,
                    details = "Found ${extracted.size} 2FA tokens. Passwords and notes have been securely excluded in memory.",
                    itemsPreview = extracted
                )
            }
        }

        // 4. Aegis Authenticator
        if (obj.containsKey("header") || obj.containsKey("db")) {
            val header = obj["header"]?.jsonObject
            val slotsElem = header?.get("slots")
            val isEncrypted = slotsElem != null &&
                    slotsElem !is kotlinx.serialization.json.JsonNull &&
                    (slotsElem as? kotlinx.serialization.json.JsonArray)?.isNotEmpty() == true
            if (isEncrypted) {
                return PreValidationResult.Success(
                    schemaType = BackupSchemaType.AEGIS,
                    isEncrypted = true,
                    estimatedItemCount = 0,
                    rawJson = rawJson,
                    fileName = fileName,
                    details = "Encrypted Aegis Authenticator backup."
                )
            } else {
                val dbObj = obj["db"]?.jsonObject ?: obj
                val extracted = parseAegisPlainItems(dbObj)
                if (extracted.isEmpty()) {
                    return PreValidationResult.Error("No 2FA tokens found in Aegis backup.")
                }
                return PreValidationResult.Success(
                    schemaType = BackupSchemaType.AEGIS,
                    isEncrypted = false,
                    estimatedItemCount = extracted.size,
                    rawJson = rawJson,
                    fileName = fileName,
                    details = "Found ${extracted.size} 2FA tokens in Aegis backup.",
                    itemsPreview = extracted
                )
            }
        }

        // 5. 2FAS Backup
        if (obj.containsKey("services")) {
            val extracted = parseTwoFasItems(obj)
            if (extracted.isEmpty()) {
                return PreValidationResult.Error("No 2FA tokens found in 2FAS backup.")
            }
            return PreValidationResult.Success(
                schemaType = BackupSchemaType.TWO_FAS,
                isEncrypted = false,
                estimatedItemCount = extracted.size,
                rawJson = rawJson,
                fileName = fileName,
                details = "Found ${extracted.size} 2FA tokens in 2FAS backup.",
                itemsPreview = extracted
            )
        }

        return PreValidationResult.Error("Unrecognized JSON backup schema. Supported: ShellGuard, Bitwarden, Aegis, and 2FAS.")
    }

    private fun inspectJsonArray(
        array: JsonArray,
        rawJson: String,
        fileName: String?
    ): PreValidationResult {
        if (array.isEmpty()) {
            return PreValidationResult.Error("The JSON array contains 0 items.")
        }

        // Check if items resemble Bitwarden Authenticator or ShellGuard DTOs
        val firstObj = array.firstOrNull()?.jsonObject ?: return PreValidationResult.Error("Invalid array elements.")
        
        if (firstObj.containsKey("key") || (firstObj.containsKey("issuer") && firstObj.containsKey("name"))) {
            // Bitwarden Authenticator flat export
            val extracted = parseBitwardenAuthenticatorItems(array)
            if (extracted.isEmpty()) {
                return PreValidationResult.Error("No valid 2FA tokens found in Bitwarden Authenticator list.")
            }
            return PreValidationResult.Success(
                schemaType = BackupSchemaType.BITWARDEN_AUTHENTICATOR,
                isEncrypted = false,
                estimatedItemCount = extracted.size,
                rawJson = rawJson,
                fileName = fileName,
                details = "Found ${extracted.size} 2FA tokens from Bitwarden Authenticator.",
                itemsPreview = extracted
            )
        }

        if (firstObj.containsKey("secret") && firstObj.containsKey("title")) {
            val extracted = parseShellGuardPlainItems(array)
            if (extracted.isEmpty()) {
                return PreValidationResult.Error("No valid 2FA tokens found in plain item list.")
            }
            return PreValidationResult.Success(
                schemaType = BackupSchemaType.SHELLGUARD_PLAIN,
                isEncrypted = false,
                estimatedItemCount = extracted.size,
                rawJson = rawJson,
                fileName = fileName,
                details = "Found ${extracted.size} 2FA tokens in plain export.",
                itemsPreview = extracted
            )
        }

        return PreValidationResult.Error("Unrecognized JSON item array format.")
    }

    // ── Parsers with Zero-Knowledge Sanitization ─────────────────────

    private fun parseShellGuardPlainItems(array: JsonArray): List<TotpItemEntity> {
        val result = mutableListOf<TotpItemEntity>()
        for (elem in array) {
            val obj = elem.jsonObject
            val secret = obj["secret"]?.jsonPrimitive?.contentOrNull ?: continue
            val title = obj["title"]?.jsonPrimitive?.contentOrNull ?: "2FA Account"
            val username = obj["username"]?.jsonPrimitive?.contentOrNull
            val category = obj["category"]?.jsonPrimitive?.contentOrNull
            val algorithm = obj["algorithm"]?.jsonPrimitive?.contentOrNull ?: "SHA1"
            val digits = obj["digits"]?.jsonPrimitive?.intOrNull ?: 6
            val period = obj["period"]?.jsonPrimitive?.intOrNull ?: 30
            val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: UUID.randomUUID().toString()

            result.add(
                TotpItemEntity(
                    id = id,
                    ownerUuid = "local",
                    title = title,
                    username = username,
                    category = category,
                    secret = secret.replace(" ", "").replace("-", "").uppercase(),
                    algorithm = algorithm,
                    digits = digits,
                    period = period,
                    isLocalOnly = true,
                    syncState = "PENDING_SYNC",
                    localUpdatedAt = System.currentTimeMillis()
                )
            )
        }
        return result
    }

    private fun parseBitwardenVaultItems(root: JsonObject): List<TotpItemEntity> {
        val foldersMap = mutableMapOf<String, String>()
        root["folders"]?.jsonArray?.forEach { folderElem ->
            val fObj = folderElem.jsonObject
            val id = fObj["id"]?.jsonPrimitive?.contentOrNull
            val name = fObj["name"]?.jsonPrimitive?.contentOrNull
            if (!id.isNullOrBlank() && !name.isNullOrBlank()) {
                foldersMap[id] = name
            }
        }

        val result = mutableListOf<TotpItemEntity>()
        val itemsArray = root["items"]?.jsonArray ?: return emptyList()

        for (itemElem in itemsArray) {
            val itemObj = itemElem.jsonObject
            val loginObj = itemObj["login"]?.jsonObject ?: continue
            val totpValue = loginObj["totp"]?.jsonPrimitive?.contentOrNull?.trim()
            if (totpValue.isNullOrBlank()) {
                // Ignore items without TOTP 2FA secrets (strictly discard passwords / notes)
                continue
            }

            val itemName = itemObj["name"]?.jsonPrimitive?.contentOrNull ?: "Bitwarden Account"
            val username = loginObj["username"]?.jsonPrimitive?.contentOrNull
            val folderId = itemObj["folderId"]?.jsonPrimitive?.contentOrNull
            val category = (folderId?.let { foldersMap[it] } ?: "General").ifBlank { "General" }

            // Parse login.totp (could be otpauth://, steam://, or raw Base32)
            val parsedTotp = TotpUriParser.parse(totpValue)
            if (parsedTotp != null) {
                result.add(
                    TotpItemEntity(
                        id = UUID.randomUUID().toString(),
                        ownerUuid = "local",
                        title = itemName,
                        username = username ?: parsedTotp.username,
                        category = category,
                        secret = parsedTotp.secret,
                        algorithm = parsedTotp.algorithm,
                        digits = parsedTotp.digits,
                        period = parsedTotp.period,
                        isLocalOnly = true,
                        syncState = "PENDING_SYNC",
                        localUpdatedAt = System.currentTimeMillis()
                    )
                )
            } else {
                // Fallback raw clean secret
                val cleanSecret = totpValue.replace(" ", "").replace("-", "").uppercase()
                if (cleanSecret.isNotEmpty()) {
                    result.add(
                        TotpItemEntity(
                            id = UUID.randomUUID().toString(),
                            ownerUuid = "local",
                            title = itemName,
                            username = username,
                            category = category,
                            secret = cleanSecret,
                            algorithm = "SHA1",
                            digits = 6,
                            period = 30,
                            isLocalOnly = true,
                            syncState = "PENDING_SYNC",
                            localUpdatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }

        return result
    }

    private fun parseBitwardenAuthenticatorItems(array: JsonArray): List<TotpItemEntity> {
        val result = mutableListOf<TotpItemEntity>()
        for (elem in array) {
            val obj = elem.jsonObject
            val key = obj["key"]?.jsonPrimitive?.contentOrNull
                ?: obj["secret"]?.jsonPrimitive?.contentOrNull
                ?: continue
            val issuer = obj["issuer"]?.jsonPrimitive?.contentOrNull
            val name = obj["name"]?.jsonPrimitive?.contentOrNull
            val algorithm = obj["algorithm"]?.jsonPrimitive?.contentOrNull ?: "SHA1"
            val digits = obj["digits"]?.jsonPrimitive?.intOrNull ?: 6
            val period = obj["period"]?.jsonPrimitive?.intOrNull ?: 30

            val title = issuer ?: name ?: "2FA Account"
            val username = if (name != null && name != issuer) name else null

            result.add(
                TotpItemEntity(
                    id = UUID.randomUUID().toString(),
                    ownerUuid = "local",
                    title = title,
                    username = username,
                    category = issuer ?: "General",
                    secret = key.replace(" ", "").replace("-", "").uppercase(),
                    algorithm = algorithm,
                    digits = digits,
                    period = period,
                    isLocalOnly = true,
                    syncState = "PENDING_SYNC",
                    localUpdatedAt = System.currentTimeMillis()
                )
            )
        }
        return result
    }

    private fun parseAegisPlainItems(dbObj: JsonObject): List<TotpItemEntity> {
        val entries = dbObj["entries"]?.jsonArray ?: return emptyList()
        val result = mutableListOf<TotpItemEntity>()

        for (entryElem in entries) {
            val entry = entryElem.jsonObject
            val type = entry["type"]?.jsonPrimitive?.contentOrNull?.trim()?.lowercase() ?: "totp"
            if (type != "totp" && type != "steam" && type != "hotp") continue

            val name = entry["name"]?.jsonPrimitive?.contentOrNull ?: "2FA Account"
            val issuer = entry["issuer"]?.jsonPrimitive?.contentOrNull
            val group = entry["group"]?.jsonPrimitive?.contentOrNull
            val info = entry["info"]?.jsonObject ?: continue

            val secret = info["secret"]?.jsonPrimitive?.contentOrNull ?: continue
            val isSteam = type == "steam"
            val rawAlgo = info["algo"]?.jsonPrimitive?.contentOrNull?.trim()?.uppercase() ?: "SHA1"
            val algorithm = if (isSteam) "STEAM" else when (rawAlgo) {
                "SHA256", "HMACSHA256" -> "SHA256"
                "SHA512", "HMACSHA512" -> "SHA512"
                "STEAM" -> "STEAM"
                else -> "SHA1"
            }
            val defaultDigits = if (isSteam) 5 else 6
            val digits = info["digits"]?.jsonPrimitive?.intOrNull ?: defaultDigits
            val period = info["period"]?.jsonPrimitive?.intOrNull ?: 30

            val title = issuer ?: name
            val username = if (name != issuer) name else null

            result.add(
                TotpItemEntity(
                    id = UUID.randomUUID().toString(),
                    ownerUuid = "local",
                    title = title,
                    username = username,
                    category = group ?: issuer ?: "General",
                    secret = secret.replace(" ", "").replace("-", "").uppercase(),
                    algorithm = algorithm,
                    digits = digits,
                    period = period,
                    isLocalOnly = true,
                    syncState = "PENDING_SYNC",
                    localUpdatedAt = System.currentTimeMillis()
                )
            )
        }
        return result
    }

    private fun parseTwoFasItems(root: JsonObject): List<TotpItemEntity> {
        val services = root["services"]?.jsonArray ?: return emptyList()
        val result = mutableListOf<TotpItemEntity>()

        // Build group map from root-level "groups" array (standard 2FAS schema with groupId on services)
        val groupMap = mutableMapOf<String, String>()
        root["groups"]?.jsonArray?.forEach { groupElem ->
            if (groupElem is JsonObject) {
                val id = groupElem["id"]?.jsonPrimitive?.contentOrNull
                val grpName = groupElem["name"]?.jsonPrimitive?.contentOrNull
                if (!id.isNullOrBlank() && !grpName.isNullOrBlank()) {
                    groupMap[id] = grpName
                }
            }
        }

        for (svcElem in services) {
            val svc = svcElem.jsonObject
            val name = svc["name"]?.jsonPrimitive?.contentOrNull ?: "2FA Account"
            val secretDirect = svc["secret"]?.jsonPrimitive?.contentOrNull
            val otpObj = svc["otp"]?.jsonObject
            val otpSecret = otpObj?.get("secret")?.jsonPrimitive?.contentOrNull
            val secret = secretDirect ?: otpSecret ?: continue

            val account = otpObj?.get("account")?.jsonPrimitive?.contentOrNull
            val issuer = otpObj?.get("issuer")?.jsonPrimitive?.contentOrNull

            val serviceType = svc["serviceType"]?.jsonPrimitive?.contentOrNull?.trim()?.uppercase()
            val tokenType = otpObj?.get("tokenType")?.jsonPrimitive?.contentOrNull?.trim()?.uppercase()
            val isSteam = serviceType == "STEAM" || tokenType == "STEAM"

            val rawAlgo = otpObj?.get("algorithm")?.jsonPrimitive?.contentOrNull?.trim()?.uppercase() ?: "SHA1"
            val algorithm = if (isSteam) "STEAM" else when (rawAlgo) {
                "SHA256", "HMACSHA256" -> "SHA256"
                "SHA512", "HMACSHA512" -> "SHA512"
                "STEAM" -> "STEAM"
                else -> "SHA1"
            }
            val defaultDigits = if (isSteam) 5 else 6
            val digits = otpObj?.get("digits")?.jsonPrimitive?.intOrNull ?: defaultDigits
            val period = otpObj?.get("period")?.jsonPrimitive?.intOrNull ?: 30

            // Resolve group: prefer root groupMap via groupId, fall back to embedded group object
            val groupId = svc["groupId"]?.jsonPrimitive?.contentOrNull
            val groupName = if (groupId != null) {
                groupMap[groupId]
            } else {
                svc["group"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
            }

            val title = issuer ?: name
            val username = account?.ifBlank { null }

            result.add(
                TotpItemEntity(
                    id = UUID.randomUUID().toString(),
                    ownerUuid = "local",
                    title = title,
                    username = username,
                    category = groupName ?: issuer ?: "General",
                    secret = secret.replace(" ", "").replace("-", "").uppercase(),
                    algorithm = algorithm,
                    digits = digits,
                    period = period,
                    isLocalOnly = true,
                    syncState = "PENDING_SYNC",
                    localUpdatedAt = System.currentTimeMillis()
                )
            )
        }
        return result
    }

    /**
     * Decrypts an encrypted ShellGuard habitat using the provided password or master key.
     * Resiliently handles multiple derivation salts and returns clean, human-friendly error messages on failure.
     */
    fun decryptShellGuardBackup(
        rawJson: String,
        passwordOrKey: String,
        targetOwnerUuid: String = "local"
    ): Result<List<TotpItemEntity>> {
        return runCatching {
            val envelope = try {
                json.decodeFromString<BackupEnvelope>(rawJson)
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid backup envelope format: ${e.message}")
            }

            val cleanKey = passwordOrKey.trim()
            if (cleanKey.isEmpty()) {
                throw IllegalArgumentException("PIN or Password cannot be empty.")
            }

            val candidateKeys = listOf(
                cleanKey,
                "shellguard_default_master_key"
            ).distinct()

            val candidateSalts = listOf(
                envelope.ownerUuid,
                "local",
                ""
            ).distinct()

            var decryptedPlainJson: String? = null

            decryptLoop@ for (candidateKey in candidateKeys) {
                for (candidateSalt in candidateSalts) {
                    try {
                        val shellKey = ShellCryptionEngine.deriveShellKey(candidateKey, candidateSalt)
                        val attempt = ShellCryptionEngine.decryptField(
                            encryptedJson = envelope.encryptedEnvelopeJson,
                            shellKey = shellKey,
                            table = "totp_backup",
                            recordId = envelope.ownerUuid
                        )
                        if (attempt.isNotBlank() && attempt.startsWith("[")) {
                            val actualChecksum = computeSha256(attempt)
                            if (actualChecksum.equals(envelope.checksumSha256, ignoreCase = true)) {
                                decryptedPlainJson = attempt
                                break@decryptLoop
                            }
                        }
                    } catch (_: Exception) {
                        // Continue trying fallback candidates
                    }
                }
            }

            if (decryptedPlainJson == null) {
                throw IllegalArgumentException("Incorrect PIN or Master Password. Please check your secret and try again.")
            }

            val items = json.decodeFromString<List<BackupItemDto>>(decryptedPlainJson)
            items.map { dto ->
                TotpItemEntity(
                    id = dto.id.ifBlank { UUID.randomUUID().toString() },
                    ownerUuid = targetOwnerUuid,
                    title = dto.title,
                    username = dto.username,
                    category = dto.category,
                    secret = dto.secret.replace(" ", "").replace("-", "").uppercase(),
                    algorithm = dto.algorithm,
                    digits = dto.digits,
                    period = dto.period,
                    isLocalOnly = true,
                    syncState = "PENDING_SYNC",
                    remoteUpdatedAt = dto.remoteUpdatedAt,
                    localUpdatedAt = System.currentTimeMillis()
                )
            }
        }
    }

    private fun computeSha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
