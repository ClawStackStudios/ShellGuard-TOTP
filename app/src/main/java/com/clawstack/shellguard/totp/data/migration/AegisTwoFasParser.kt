package com.clawstack.shellguard.totp.data.migration

import com.clawstack.shellguard.totp.engine.TotpUriParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

/**
 * Parser for Aegis Authenticator and 2FAS JSON backup formats.
 */
object AegisTwoFasParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Parses an Aegis Authenticator JSON backup export (plaintext or decrypted db format).
     */
    fun parseAegis(jsonString: String): List<ParsedTotpItem> {
        val rootElement = try {
            json.parseToJsonElement(jsonString)
        } catch (e: Exception) {
            return emptyList()
        }

        if (rootElement !is JsonObject) return emptyList()

        val dbObj = rootElement["db"]?.jsonObject ?: rootElement
        val entriesArray = dbObj["entries"]?.jsonArray ?: return emptyList()

        val parsedItems = mutableListOf<ParsedTotpItem>()

        for (entryElem in entriesArray) {
            if (entryElem !is JsonObject) continue

            val type = entryElem["type"]?.jsonPrimitive?.contentOrNull?.trim()?.lowercase() ?: "totp"
            if (type != "totp" && type != "steam" && type != "hotp") continue

            val infoObj = entryElem["info"]?.jsonObject
            val rawSecret = infoObj?.get("secret")?.jsonPrimitive?.contentOrNull?.trim()
            if (rawSecret.isNullOrBlank()) continue

            val issuer = entryElem["issuer"]?.jsonPrimitive?.contentOrNull?.trim()
            val name = entryElem["name"]?.jsonPrimitive?.contentOrNull?.trim()
            val group = entryElem["group"]?.jsonPrimitive?.contentOrNull?.trim()
            val category = if (!group.isNullOrBlank()) group else "General"

            val isSteam = type == "steam"
            val rawAlgo = infoObj?.get("algo")?.jsonPrimitive?.contentOrNull?.trim()?.uppercase() ?: "SHA1"
            val algorithm = if (isSteam) "STEAM" else when (rawAlgo) {
                "SHA256", "HMACSHA256" -> "SHA256"
                "SHA512", "HMACSHA512" -> "SHA512"
                "STEAM" -> "STEAM"
                else -> "SHA1"
            }

            val defaultDigits = if (isSteam || algorithm == "STEAM") 5 else 6
            val digits = infoObj?.get("digits")?.jsonPrimitive?.intOrNull ?: defaultDigits
            val period = infoObj?.get("period")?.jsonPrimitive?.intOrNull ?: 30

            val cleanSecret = rawSecret.replace(" ", "").replace("-", "").uppercase()
            if (!isValidBase32(cleanSecret)) continue

            val finalTitle = issuer?.ifBlank { null } ?: (name?.ifBlank { null } ?: "2FA Account")
            val finalUsername = if (!issuer.isNullOrBlank() && !name.isNullOrBlank() && name != issuer) name else null

            parsedItems.add(
                ParsedTotpItem(
                    id = UUID.randomUUID().toString(),
                    title = finalTitle,
                    username = finalUsername,
                    secret = cleanSecret,
                    issuer = issuer ?: finalTitle,
                    category = category,
                    algorithm = algorithm,
                    digits = digits,
                    period = period,
                    schemaSource = BackupSchemaType.AEGIS
                )
            )
        }

        return parsedItems
    }

    /**
     * Parses a 2FAS JSON backup export.
     */
    fun parseTwoFas(jsonString: String): List<ParsedTotpItem> {
        val rootElement = try {
            json.parseToJsonElement(jsonString)
        } catch (e: Exception) {
            return emptyList()
        }

        if (rootElement !is JsonObject) return emptyList()

        // 1. Build Group Map (groupId -> groupName)
        val groupMap = mutableMapOf<String, String>()
        rootElement["groups"]?.jsonArray?.forEach { groupElem ->
            if (groupElem is JsonObject) {
                val id = groupElem["id"]?.jsonPrimitive?.contentOrNull
                val name = groupElem["name"]?.jsonPrimitive?.contentOrNull
                if (!id.isNullOrBlank() && !name.isNullOrBlank()) {
                    groupMap[id] = name
                }
            }
        }

        // 2. Parse Services
        val servicesArray = rootElement["services"]?.jsonArray ?: return emptyList()
        val parsedItems = mutableListOf<ParsedTotpItem>()

        for (serviceElem in servicesArray) {
            if (serviceElem !is JsonObject) continue

            val otpObj = serviceElem["otp"]?.jsonObject
            val rawSecret = serviceElem["secret"]?.jsonPrimitive?.contentOrNull?.trim()
                ?: otpObj?.get("secret")?.jsonPrimitive?.contentOrNull?.trim()
                ?: otpObj?.get("link")?.jsonPrimitive?.contentOrNull?.trim()

            if (rawSecret.isNullOrBlank()) continue

            val serviceName = serviceElem["name"]?.jsonPrimitive?.contentOrNull?.trim()
            val otpIssuer = otpObj?.get("issuer")?.jsonPrimitive?.contentOrNull?.trim()
            val otpAccount = otpObj?.get("account")?.jsonPrimitive?.contentOrNull?.trim()

            val groupId = serviceElem["groupId"]?.jsonPrimitive?.contentOrNull
            val category = if (groupId != null) groupMap[groupId] ?: "General" else "General"

            // Check if secret is an otpauth / steam URI
            val parsedFromUri = TotpUriParser.parse(rawSecret)
            if (parsedFromUri != null) {
                val finalTitle = serviceName?.ifBlank { null } ?: (otpIssuer ?: parsedFromUri.title)
                val finalUsername = otpAccount ?: parsedFromUri.username

                parsedItems.add(
                    ParsedTotpItem(
                        id = UUID.randomUUID().toString(),
                        title = finalTitle,
                        username = finalUsername,
                        secret = parsedFromUri.secret,
                        issuer = otpIssuer ?: parsedFromUri.issuer,
                        category = category,
                        algorithm = parsedFromUri.algorithm,
                        digits = parsedFromUri.digits,
                        period = parsedFromUri.period,
                        schemaSource = BackupSchemaType.TWO_FAS
                    )
                )
            } else {
                val cleanSecret = rawSecret.replace(" ", "").replace("-", "").uppercase()
                if (!isValidBase32(cleanSecret)) continue

                val serviceType = serviceElem["serviceType"]?.jsonPrimitive?.contentOrNull?.trim()?.uppercase()
                val tokenType = otpObj?.get("tokenType")?.jsonPrimitive?.contentOrNull?.trim()?.uppercase()
                val isSteam = serviceType == "STEAM" || tokenType == "STEAM"

                val rawAlgo = otpObj?.get("algorithm")?.jsonPrimitive?.contentOrNull?.trim()?.uppercase() ?: "SHA1"
                val algorithm = if (isSteam) "STEAM" else when (rawAlgo) {
                    "SHA256", "HMACSHA256" -> "SHA256"
                    "SHA512", "HMACSHA512" -> "SHA512"
                    "STEAM" -> "STEAM"
                    else -> "SHA1"
                }

                val defaultDigits = if (isSteam || algorithm == "STEAM") 5 else 6
                val digits = otpObj?.get("digits")?.jsonPrimitive?.intOrNull ?: defaultDigits
                val period = otpObj?.get("period")?.jsonPrimitive?.intOrNull ?: 30

                val finalTitle = otpIssuer?.ifBlank { null } ?: (serviceName?.ifBlank { null } ?: "2FA Account")
                val finalUsername = otpAccount?.ifBlank { null }

                parsedItems.add(
                    ParsedTotpItem(
                        id = UUID.randomUUID().toString(),
                        title = finalTitle,
                        username = finalUsername,
                        secret = cleanSecret,
                        issuer = otpIssuer ?: finalTitle,
                        category = category,
                        algorithm = algorithm,
                        digits = digits,
                        period = period,
                        schemaSource = BackupSchemaType.TWO_FAS
                    )
                )
            }
        }

        return parsedItems
    }

    /**
     * Detects if JSON represents an Aegis Authenticator backup file.
     */
    fun isAegis(jsonElement: JsonElement): Boolean {
        if (jsonElement !is JsonObject) return false
        val hasDb = jsonElement.containsKey("db") && jsonElement["db"] is JsonObject
        val hasHeader = jsonElement.containsKey("header")
        return hasDb || (hasHeader && jsonElement.containsKey("entries"))
    }

    /**
     * Detects if JSON represents a 2FAS backup file.
     */
    fun isTwoFas(jsonElement: JsonElement): Boolean {
        if (jsonElement !is JsonObject) return false
        val hasServices = jsonElement.containsKey("services") && jsonElement["services"] is kotlinx.serialization.json.JsonArray
        val hasSchemaVersion = jsonElement.containsKey("schemaVersion") || jsonElement.containsKey("appVersion")
        return hasServices && hasSchemaVersion
    }

    private fun isValidBase32(secret: String): Boolean {
        if (secret.length < 8 || secret.length > 128) return false
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567="
        return secret.all { it in alphabet }
    }
}
