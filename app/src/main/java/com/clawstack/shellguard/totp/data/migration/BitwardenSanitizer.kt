package com.clawstack.shellguard.totp.data.migration

import com.clawstack.shellguard.totp.engine.TotpUriParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

/**
 * Zero-Knowledge RAM Sanitizer for Bitwarden Password Manager and Bitwarden Authenticator exports.
 *
 * CRITICAL SECURITY INVARIANT:
 * Strips login.password, notes, credit cards, identities, and custom sensitive fields entirely in memory.
 * Extracts exclusively valid TOTP/HOTP/Steam secrets and their associated non-credential metadata (issuer, account name, folder).
 */
object BitwardenSanitizer {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Sanitizes and parses a Bitwarden Password Manager Vault JSON export.
     */
    fun sanitizeBitwardenVault(jsonString: String): List<ParsedTotpItem> {
        val rootElement = try {
            json.parseToJsonElement(jsonString)
        } catch (e: Exception) {
            return emptyList()
        }

        if (rootElement !is JsonObject) return emptyList()

        // 1. Build Folder Map (folderId -> folderName)
        val folderMap = mutableMapOf<String, String>()
        rootElement["folders"]?.jsonArray?.forEach { folderElem ->
            if (folderElem is JsonObject) {
                val id = folderElem["id"]?.jsonPrimitive?.contentOrNull
                val name = folderElem["name"]?.jsonPrimitive?.contentOrNull
                if (!id.isNullOrBlank() && !name.isNullOrBlank()) {
                    folderMap[id] = name
                }
            }
        }

        // 2. Stream & Sanitize Items
        val itemsArray = rootElement["items"]?.jsonArray ?: return emptyList()
        val parsedItems = mutableListOf<ParsedTotpItem>()

        for (itemElem in itemsArray) {
            if (itemElem !is JsonObject) continue

            val loginObj = itemElem["login"]
            if (loginObj !is JsonObject) continue

            val rawTotp = loginObj["totp"]?.jsonPrimitive?.contentOrNull?.trim()
            if (rawTotp.isNullOrBlank()) continue

            val itemName = itemElem["name"]?.jsonPrimitive?.contentOrNull?.trim() ?: "2FA Account"
            val loginUsername = loginObj["username"]?.jsonPrimitive?.contentOrNull?.trim()
            val folderId = itemElem["folderId"]?.jsonPrimitive?.contentOrNull
            val category = if (folderId != null) folderMap[folderId] ?: "General" else "General"

            val parsedFromUri = TotpUriParser.parse(rawTotp)
            if (parsedFromUri != null) {
                val title = if (itemName.isNotBlank() && itemName != "Imported 2FA") itemName else parsedFromUri.title
                val username = parsedFromUri.username ?: loginUsername
                val issuer = parsedFromUri.issuer ?: if (itemName.isNotBlank()) itemName else null

                parsedItems.add(
                    ParsedTotpItem(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        username = username,
                        secret = parsedFromUri.secret,
                        issuer = issuer,
                        category = category,
                        algorithm = parsedFromUri.algorithm,
                        digits = parsedFromUri.digits,
                        period = parsedFromUri.period,
                        schemaSource = BackupSchemaType.BITWARDEN_VAULT
                    )
                )
            } else {
                // Fallback: Check if rawTotp is a valid Base32 secret string
                val cleanSecret = rawTotp.replace(" ", "").replace("-", "").uppercase()
                if (isValidBase32(cleanSecret)) {
                    parsedItems.add(
                        ParsedTotpItem(
                            id = UUID.randomUUID().toString(),
                            title = itemName,
                            username = loginUsername,
                            secret = cleanSecret,
                            issuer = itemName,
                            category = category,
                            algorithm = "SHA1",
                            digits = 6,
                            period = 30,
                            schemaSource = BackupSchemaType.BITWARDEN_VAULT
                        )
                    )
                }
            }
        }

        return parsedItems
    }

    /**
     * Sanitizes and parses a standalone Bitwarden Authenticator JSON export.
     */
    fun sanitizeBitwardenAuthenticator(jsonString: String): List<ParsedTotpItem> {
        val rootElement = try {
            json.parseToJsonElement(jsonString)
        } catch (e: Exception) {
            return emptyList()
        }

        val itemsArray: JsonArray = when (rootElement) {
            is JsonArray -> rootElement
            is JsonObject -> rootElement["items"]?.jsonArray ?: return emptyList()
            else -> return emptyList()
        }

        val parsedItems = mutableListOf<ParsedTotpItem>()

        for (itemElem in itemsArray) {
            if (itemElem !is JsonObject) continue

            val rawKey = itemElem["key"]?.jsonPrimitive?.contentOrNull?.trim()
                ?: itemElem["secret"]?.jsonPrimitive?.contentOrNull?.trim()
                ?: itemElem["totp"]?.jsonPrimitive?.contentOrNull?.trim()

            if (rawKey.isNullOrBlank()) continue

            val issuer = itemElem["issuer"]?.jsonPrimitive?.contentOrNull?.trim()
            val name = itemElem["name"]?.jsonPrimitive?.contentOrNull?.trim()
                ?: itemElem["title"]?.jsonPrimitive?.contentOrNull?.trim()

            val rawAlg = itemElem["algorithm"]?.jsonPrimitive?.contentOrNull?.trim()?.uppercase() ?: "SHA1"
            val digits = itemElem["digits"]?.jsonPrimitive?.intOrNull ?: 6
            val period = itemElem["period"]?.jsonPrimitive?.intOrNull ?: 30

            val parsedFromUri = TotpUriParser.parse(rawKey)
            if (parsedFromUri != null) {
                val finalTitle = issuer ?: parsedFromUri.issuer ?: name ?: parsedFromUri.title
                val finalUsername = parsedFromUri.username ?: if (name != null && name != issuer) name else null

                parsedItems.add(
                    ParsedTotpItem(
                        id = UUID.randomUUID().toString(),
                        title = finalTitle,
                        username = finalUsername,
                        secret = parsedFromUri.secret,
                        issuer = issuer ?: parsedFromUri.issuer,
                        category = "General",
                        algorithm = parsedFromUri.algorithm,
                        digits = parsedFromUri.digits,
                        period = parsedFromUri.period,
                        schemaSource = BackupSchemaType.BITWARDEN_AUTHENTICATOR
                    )
                )
            } else {
                val cleanSecret = rawKey.replace(" ", "").replace("-", "").uppercase()
                if (isValidBase32(cleanSecret)) {
                    val finalTitle = issuer ?: (name ?: "2FA Account")
                    val finalUsername = if (!issuer.isNullOrBlank() && !name.isNullOrBlank() && name != issuer) name else null
                    val algorithm = when (rawAlg) {
                        "SHA256", "HMACSHA256" -> "SHA256"
                        "SHA512", "HMACSHA512" -> "SHA512"
                        "STEAM" -> "STEAM"
                        else -> "SHA1"
                    }
                    val finalDigits = if (algorithm == "STEAM") 5 else digits

                    parsedItems.add(
                        ParsedTotpItem(
                            id = UUID.randomUUID().toString(),
                            title = finalTitle,
                            username = finalUsername,
                            secret = cleanSecret,
                            issuer = issuer,
                            category = "General",
                            algorithm = algorithm,
                            digits = finalDigits,
                            period = period,
                            schemaSource = BackupSchemaType.BITWARDEN_AUTHENTICATOR
                        )
                    )
                }
            }
        }

        return parsedItems
    }

    /**
     * Checks whether the JSON root structure matches Bitwarden Password Manager Vault format.
     */
    fun isBitwardenVault(jsonElement: JsonElement): Boolean {
        if (jsonElement !is JsonObject) return false
        val hasFolders = jsonElement.containsKey("folders") && jsonElement["folders"] is JsonArray
        val hasItems = jsonElement.containsKey("items") && jsonElement["items"] is JsonArray
        val hasEncryptedFlag = jsonElement.containsKey("encrypted")

        return (hasFolders && hasItems) || (hasEncryptedFlag && hasItems)
    }

    /**
     * Checks whether the JSON root structure matches Bitwarden Authenticator format.
     */
    fun isBitwardenAuthenticator(jsonElement: JsonElement): Boolean {
        return when (jsonElement) {
            is JsonArray -> {
                if (jsonElement.isEmpty()) false
                else {
                    val first = jsonElement[0]
                    if (first is JsonObject) {
                        (first.containsKey("key") || first.containsKey("secret")) &&
                                (first.containsKey("issuer") || first.containsKey("name") || first.containsKey("algorithm")) &&
                                !first.containsKey("ownerUuid") &&
                                !first.containsKey("syncState")
                    } else false
                }
            }
            is JsonObject -> {
                val items = jsonElement["items"]
                if (items is JsonArray && items.isNotEmpty()) {
                    val first = items[0]
                    first is JsonObject && first.containsKey("key") && !jsonElement.containsKey("folders")
                } else false
            }
            else -> false
        }
    }

    private fun isValidBase32(secret: String): Boolean {
        if (secret.length < 8 || secret.length > 128) return false
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567="
        return secret.all { it in alphabet }
    }
}
