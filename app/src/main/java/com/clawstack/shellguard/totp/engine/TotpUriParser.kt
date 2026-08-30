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
 */
object TotpUriParser {

    fun parse(rawUriString: String): ParsedTotpUri? {
        val trimmed = rawUriString.trim()
        if (trimmed.startsWith("otpauth://", ignoreCase = true)) {
            return parseOtpAuthUri(trimmed)
        }

        // If it looks like a URL or has query delimiters but lacks otpauth scheme, do not treat as raw secret
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

    private fun parseOtpAuthUri(rawUri: String): ParsedTotpUri? {
        return try {
            val withoutScheme = rawUri.substring("otpauth://".length)
            val questionIdx = withoutScheme.indexOf('?')
            val typeAndLabel = if (questionIdx != -1) withoutScheme.substring(0, questionIdx) else withoutScheme
            val queryString = if (questionIdx != -1) withoutScheme.substring(questionIdx + 1) else ""

            val slashIdx = typeAndLabel.indexOf('/')
            val type = if (slashIdx != -1) typeAndLabel.substring(0, slashIdx).lowercase() else "totp"
            if (type != "totp" && type != "hotp") return null

            val labelRaw = if (slashIdx != -1) typeAndLabel.substring(slashIdx + 1) else typeAndLabel
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

            val secret = params["secret"]?.replace(" ", "")?.replace("-", "")?.uppercase()
            if (secret.isNullOrBlank()) return null

            // Label format: "Issuer:account" or "account"
            val parts = decodedLabel.split(":", limit = 2)
            val pathIssuer = if (parts.size > 1) parts[0].trim() else null
            val pathAccount = if (parts.size > 1) parts[1].trim() else parts[0].trim()

            val queryIssuer = params["issuer"]?.trim()
            val finalIssuer = queryIssuer?.ifBlank { null } ?: pathIssuer?.ifBlank { null }

            val title = finalIssuer ?: if (pathAccount.isNotBlank()) pathAccount else "2FA Account"
            val username = if (finalIssuer != null && pathAccount.isNotBlank() && pathAccount != finalIssuer) pathAccount else null

            val rawAlg = params["algorithm"]?.uppercase()?.trim() ?: "SHA1"
            val algorithm = when (rawAlg) {
                "SHA256", "HMACSHA256" -> "SHA256"
                "SHA512", "HMACSHA512" -> "SHA512"
                else -> "SHA1"
            }

            val digits = params["digits"]?.toIntOrNull() ?: 6
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
