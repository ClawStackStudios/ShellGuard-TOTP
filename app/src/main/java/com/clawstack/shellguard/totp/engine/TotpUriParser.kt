package com.clawstack.shellguard.totp.engine

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class ParsedTotpUri(
    val title: String,
    val username: String?,
    val secret: String,
    val issuer: String?,
    val algorithm: String = "SHA1",
    val digits: Int = 6,
    val period: Int = 30
)

/**
 * Parses Key URI Format (RFC 6238 / Google Authenticator standard):
 * otpauth://totp/Issuer:account@domain?secret=JBSWY3DPEHPK3PXP&issuer=Issuer&algorithm=SHA1&digits=6&period=30
 * And Steam Guard URIs:
 * steam://<secret> or steam://totp/Steam:account?secret=...
 */
object TotpUriParser {

    fun parse(rawUriString: String): ParsedTotpUri? {
        val trimmed = rawUriString.trim()
        if (trimmed.startsWith("otpauth://", ignoreCase = true) || trimmed.startsWith("steam://", ignoreCase = true)) {
            return parseUri(trimmed)
        }

        // If it looks like a URL or has query delimiters but lacks otpauth/steam scheme, do not treat as raw secret
        if (trimmed.contains("://") || trimmed.contains("?") || trimmed.contains("&")) {
            return null
        }

        // Raw base32 secret fallback (typical Base32 OTP secret is 16, 20, 24, 26, 32, 40, 64 characters, uppercase)
        val isUppercaseSecret = trimmed == trimmed.uppercase()
        val clean = trimmed.replace(" ", "").replace("-", "").uppercase()
        val isBase32 = isUppercaseSecret &&
                clean.length in 16..64 &&
                clean.all { it in "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567=" }

        if (isBase32) {
            return ParsedTotpUri(
                title = "Imported 2FA",
                username = null,
                secret = clean,
                issuer = null
            )
        }

        return null
    }

    private fun parseUri(rawUri: String): ParsedTotpUri? {
        return try {
            val isSteamScheme = rawUri.startsWith("steam://", ignoreCase = true)
            val schemeLength = if (isSteamScheme) "steam://".length else "otpauth://".length
            val withoutScheme = rawUri.substring(schemeLength)

            // Handle direct raw secret in steam:// e.g. steam://HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ
            if (isSteamScheme && !withoutScheme.contains('/') && !withoutScheme.contains('?') && !withoutScheme.contains('=')) {
                val cleanSecret = withoutScheme.replace(" ", "").replace("-", "").uppercase()
                if (cleanSecret.isNotBlank()) {
                    return ParsedTotpUri(
                        title = "Steam",
                        username = null,
                        secret = cleanSecret,
                        issuer = "Steam",
                        algorithm = "STEAM",
                        digits = 5,
                        period = 30
                    )
                }
            }

            val questionIdx = withoutScheme.indexOf('?')
            val typeAndLabel = if (questionIdx != -1) withoutScheme.substring(0, questionIdx) else withoutScheme
            val queryString = if (questionIdx != -1) withoutScheme.substring(questionIdx + 1) else ""

            val slashIdx = typeAndLabel.indexOf('/')
            val type = if (slashIdx != -1) typeAndLabel.substring(0, slashIdx).lowercase() else if (isSteamScheme) "steam" else "totp"
            if (type != "totp" && type != "hotp" && type != "steam") return null

            val labelRaw = if (slashIdx != -1) typeAndLabel.substring(slashIdx + 1) else if (isSteamScheme && !typeAndLabel.contains(':') && !typeAndLabel.contains('?')) "" else typeAndLabel
            val decodedLabel = try {
                URLDecoder.decode(labelRaw, StandardCharsets.UTF_8.name())
            } catch (e: Exception) {
                labelRaw
            }

            // Parse Query parameters
            val params = mutableMapOf<String, String>()
            if (queryString.isNotEmpty()) {
                queryString.split("&").forEach { pair ->
                    val eqIdx = pair.indexOf('=')
                    if (eqIdx != -1) {
                        val key = try {
                            URLDecoder.decode(pair.substring(0, eqIdx), StandardCharsets.UTF_8.name()).lowercase()
                        } catch (e: Exception) {
                            pair.substring(0, eqIdx).lowercase()
                        }
                        val value = try {
                            URLDecoder.decode(pair.substring(eqIdx + 1), StandardCharsets.UTF_8.name())
                        } catch (e: Exception) {
                            pair.substring(eqIdx + 1)
                        }
                        params[key] = value
                    }
                }
            }

            // If secret is not in params and isSteamScheme, typeAndLabel might be the secret itself
            val secretParam = params["secret"] ?: if (isSteamScheme && decodedLabel.isNotBlank() && !decodedLabel.contains(':')) decodedLabel else null
            val secret = secretParam?.replace(" ", "")?.replace("-", "")?.uppercase()
            if (secret.isNullOrBlank()) return null

            // Label format: "Issuer:account" or "account"
            val parts = decodedLabel.split(":", limit = 2)
            val pathIssuer = if (parts.size > 1) parts[0].trim() else null
            val pathAccount = if (parts.size > 1) parts[1].trim() else parts[0].trim()

            val queryIssuer = params["issuer"]?.trim()
            val isSteam = isSteamScheme || type == "steam" || params["algorithm"]?.uppercase()?.trim() == "STEAM" || queryIssuer.equals("Steam", ignoreCase = true) || pathIssuer.equals("Steam", ignoreCase = true)

            val defaultIssuer = if (isSteam) "Steam" else null
            val finalIssuer = queryIssuer?.ifBlank { null } ?: pathIssuer?.ifBlank { null } ?: defaultIssuer

            val title = finalIssuer ?: if (pathAccount.isNotBlank()) pathAccount else if (isSteam) "Steam" else "2FA Account"
            val username = if (pathAccount.isNotBlank() && pathAccount != finalIssuer && pathAccount != secret) pathAccount else null

            val rawAlg = params["algorithm"]?.uppercase()?.trim() ?: if (isSteam) "STEAM" else "SHA1"
            val algorithm = when (rawAlg) {
                "SHA256", "HMACSHA256" -> "SHA256"
                "SHA512", "HMACSHA512" -> "SHA512"
                "STEAM" -> "STEAM"
                else -> if (isSteam) "STEAM" else "SHA1"
            }

            val defaultDigits = if (algorithm == "STEAM") 5 else 6
            val digits = params["digits"]?.toIntOrNull() ?: defaultDigits
            val period = params["period"]?.toIntOrNull() ?: 30

            ParsedTotpUri(
                title = title,
                username = username,
                secret = secret,
                issuer = finalIssuer,
                algorithm = algorithm,
                digits = digits,
                period = period
            )
        } catch (e: Exception) {
            null
        }
    }
}
