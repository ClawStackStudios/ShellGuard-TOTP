package com.clawstack.shellguard.totp

import com.clawstack.shellguard.totp.crypto.ShellCryptionEngine
import com.clawstack.shellguard.totp.data.backup.BackupItemDto
import com.clawstack.shellguard.totp.data.backup.BackupSchemaType
import com.clawstack.shellguard.totp.data.backup.MultiVaultBackupPreValidator
import com.clawstack.shellguard.totp.data.backup.PreValidationResult
import com.clawstack.shellguard.totp.data.migration.AegisTwoFasParser
import com.clawstack.shellguard.totp.data.migration.BitwardenSanitizer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class IntakeEngineTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    // ── 1. ShellGuard Encrypted Habitat Pre-Validation ────────────────────────

    @Test
    fun testPreValidateShellGuardEncryptedBackup() {
        val encryptedJson = """
        {
            "version": 1,
            "type": "shellguard-totp-backup-v1",
            "ownerUuid": "user-uuid-1234",
            "itemCount": 5,
            "createdAt": 1700000000000,
            "checksumSha256": "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
            "encryptedEnvelopeJson": "{\"iv\":\"...\",\"ciphertext\":\"...\",\"authTag\":\"...\"}"
        }
        """.trimIndent()

        val result = MultiVaultBackupPreValidator.validateString(encryptedJson, "my_backup.json")
        assertTrue(result is PreValidationResult.Success)
        val success = result as PreValidationResult.Success
        assertEquals(BackupSchemaType.SHELLGUARD_ENCRYPTED, success.schemaType)
        assertTrue(success.isEncrypted)
        assertEquals(5, success.estimatedItemCount)
        assertEquals("my_backup.json", success.fileName)
        assertTrue(success.details.contains("5 tokens"))
    }

    // ── 2. ShellGuard Plain Backup Pre-Validation ─────────────────────────────

    @Test
    fun testPreValidateShellGuardPlainBackup() {
        val plainExportJson = """
        {
            "version": 1,
            "format": "shellguard-totp-plain-export-v1",
            "exportedAt": 1700000000000,
            "items": [
                {
                    "id": "item-1",
                    "title": "GitHub",
                    "username": "lucas@clawstack.com",
                    "category": "Infrastructure",
                    "secret": "JBSWY3DPEHPK3PXP",
                    "algorithm": "SHA1",
                    "digits": 6,
                    "period": 30
                },
                {
                    "id": "item-2",
                    "title": "AWS IAM",
                    "username": "admin",
                    "category": "Cloud",
                    "secret": "KRSXG5CTMVRXEZLUKN2XAZLS",
                    "algorithm": "SHA256",
                    "digits": 8,
                    "period": 60
                }
            ]
        }
        """.trimIndent()

        val result = MultiVaultBackupPreValidator.validateString(plainExportJson)
        assertTrue(result is PreValidationResult.Success)
        val success = result as PreValidationResult.Success
        assertEquals(BackupSchemaType.SHELLGUARD_PLAIN, success.schemaType)
        assertFalse(success.isEncrypted)
        assertEquals(2, success.estimatedItemCount)
        assertEquals(2, success.itemsPreview.size)
        assertEquals("GitHub", success.itemsPreview[0].title)
        assertEquals("JBSWY3DPEHPK3PXP", success.itemsPreview[0].secret)
        assertEquals("AWS IAM", success.itemsPreview[1].title)
        assertEquals("SHA256", success.itemsPreview[1].algorithm)
        assertEquals(8, success.itemsPreview[1].digits)
    }

    // ── 3. Bitwarden Password Manager Vault Pre-Validation & Sanitization ─────

    @Test
    fun testBitwardenVaultSanitizationZeroKnowledgeExclusion() {
        // Vault containing 2 items with TOTP (1 standard, 1 steam) and 2 non-TOTP items (password only, secure note)
        val bitwardenVaultJson = """
        {
            "encrypted": false,
            "folders": [
                { "id": "folder-infra-uuid", "name": "Work Infrastructure" },
                { "id": "folder-finance-uuid", "name": "Personal Finance" }
            ],
            "items": [
                {
                    "id": "bw-item-1",
                    "folderId": "folder-infra-uuid",
                    "type": 1,
                    "name": "GitHub Enterprise",
                    "notes": "HIGHLY SENSITIVE RECOVERY SEED NOTES - MUST BE STRIPPED",
                    "login": {
                        "username": "lucas@clawstack.com",
                        "password": "SUPER_SECRET_PLAINTEXT_PASSWORD_MUST_BE_PURGED_12345",
                        "totp": "otpauth://totp/GitHub:lucas@clawstack.com?secret=JBSWY3DPEHPK3PXP&issuer=GitHub&algorithm=SHA1&digits=6&period=30",
                        "uris": [{ "uri": "https://github.com" }]
                    }
                },
                {
                    "id": "bw-item-2",
                    "folderId": null,
                    "type": 1,
                    "name": "Steam Gaming",
                    "notes": "Steam trade url and credit card notes",
                    "login": {
                        "username": "gamer_lucas",
                        "password": "GAMING_PASSWORD_MUST_BE_PURGED",
                        "totp": "steam://HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ"
                    }
                },
                {
                    "id": "bw-item-3-no-totp",
                    "folderId": "folder-finance-uuid",
                    "type": 1,
                    "name": "Bank Portal",
                    "notes": "Account number 99999",
                    "login": {
                        "username": "lucas_bank",
                        "password": "BANK_PASSWORD_NEVER_SAVED",
                        "totp": null
                    }
                },
                {
                    "id": "bw-item-4-secure-note",
                    "folderId": null,
                    "type": 2,
                    "name": "Server SSH Private Key Note",
                    "notes": "-----BEGIN OPENSSH PRIVATE KEY-----..."
                }
            ]
        }
        """.trimIndent()

        // 1. Test via PreValidator
        val result = MultiVaultBackupPreValidator.validateString(bitwardenVaultJson)
        assertTrue(result is PreValidationResult.Success)
        val success = result as PreValidationResult.Success
        assertEquals(BackupSchemaType.BITWARDEN_VAULT, success.schemaType)
        assertFalse(success.isEncrypted)
        assertEquals(2, success.estimatedItemCount) // Exactly 2 items with TOTP!

        val items = success.itemsPreview
        assertEquals(2, items.size)

        // Item 1: GitHub Enterprise
        val item1 = items[0]
        assertEquals("GitHub Enterprise", item1.title)
        assertEquals("lucas@clawstack.com", item1.username)
        assertEquals("Work Infrastructure", item1.category) // Mapped from folderId!
        assertEquals("JBSWY3DPEHPK3PXP", item1.secret)
        assertEquals("SHA1", item1.algorithm)
        assertEquals(6, item1.digits)

        // Item 2: Steam
        val item2 = items[1]
        assertEquals("Steam Gaming", item2.title)
        assertEquals("gamer_lucas", item2.username)
        assertEquals("General", item2.category) // folderId was null -> resolved to "General"
        assertEquals("HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ", item2.secret)
        assertEquals("STEAM", item2.algorithm)
        assertEquals(5, item2.digits)

        // 2. Direct BitwardenSanitizer verification
        val parsedSanitized = BitwardenSanitizer.sanitizeBitwardenVault(bitwardenVaultJson)
        assertEquals(2, parsedSanitized.size)
        assertEquals("Work Infrastructure", parsedSanitized[0].category)
        assertEquals("General", parsedSanitized[1].category)

        // 3. Verify zero leakage in serialized DTOs / Entities
        val serializedEntityList = json.encodeToString(items)
        assertFalse(serializedEntityList.contains("SUPER_SECRET_PLAINTEXT_PASSWORD"))
        assertFalse(serializedEntityList.contains("GAMING_PASSWORD"))
        assertFalse(serializedEntityList.contains("BANK_PASSWORD"))
        assertFalse(serializedEntityList.contains("HIGHLY SENSITIVE RECOVERY SEED"))
        assertFalse(serializedEntityList.contains("OPENSSH PRIVATE KEY"))
    }

    // ── 4. Bitwarden Authenticator Pre-Validation ─────────────────────────────

    @Test
    fun testBitwardenAuthenticatorFlatList() {
        val bwAuthJson = """
        [
            {
                "issuer": "Amazon Web Services",
                "name": "admin@clawstack.io",
                "key": "JBSWY3DPEHPK3PXP",
                "algorithm": "SHA1",
                "digits": 6,
                "period": 30
            },
            {
                "issuer": "Google Cloud",
                "name": "infra@clawstack.io",
                "key": "KRSXG5CTMVRXEZLUKN2XAZLS",
                "algorithm": "SHA256",
                "digits": 8,
                "period": 60
            }
        ]
        """.trimIndent()

        val result = MultiVaultBackupPreValidator.validateString(bwAuthJson)
        assertTrue(result is PreValidationResult.Success)
        val success = result as PreValidationResult.Success
        assertEquals(BackupSchemaType.BITWARDEN_AUTHENTICATOR, success.schemaType)
        assertFalse(success.isEncrypted)
        assertEquals(2, success.estimatedItemCount)
        assertEquals("Amazon Web Services", success.itemsPreview[0].title)
        assertEquals("admin@clawstack.io", success.itemsPreview[0].username)
        assertEquals("JBSWY3DPEHPK3PXP", success.itemsPreview[0].secret)
        assertEquals("Google Cloud", success.itemsPreview[1].title)
        assertEquals("SHA256", success.itemsPreview[1].algorithm)
        assertEquals(8, success.itemsPreview[1].digits)

        val directParsed = BitwardenSanitizer.sanitizeBitwardenAuthenticator(bwAuthJson)
        assertEquals(2, directParsed.size)
        assertEquals("Amazon Web Services", directParsed[0].title)
    }

    // ── 5. Aegis and 2FAS Pre-Validation ──────────────────────────────────────

    @Test
    fun testAegisAuthenticatorPlainBackup() {
        val aegisJson = """
        {
            "version": 1,
            "header": { "slots": null, "params": null },
            "db": {
                "version": 1,
                "entries": [
                    {
                        "type": "totp",
                        "uuid": "aegis-uuid-1",
                        "name": "lucas@proton.me",
                        "issuer": "ProtonMail",
                        "group": "Personal Communications",
                        "info": {
                            "secret": "JBSWY3DPEHPK3PXP",
                            "algo": "SHA256",
                            "digits": 6,
                            "period": 30
                        }
                    },
                    {
                        "type": "steam",
                        "uuid": "aegis-uuid-2",
                        "name": "SteamAccount",
                        "issuer": "Steam",
                        "group": "Gaming",
                        "info": {
                            "secret": "HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ",
                            "algo": "SHA1",
                            "digits": 5,
                            "period": 30
                        }
                    }
                ]
            }
        }
        """.trimIndent()

        val result = MultiVaultBackupPreValidator.validateString(aegisJson)
        assertTrue(result is PreValidationResult.Success)
        val success = result as PreValidationResult.Success
        assertEquals(BackupSchemaType.AEGIS, success.schemaType)
        assertFalse(success.isEncrypted)
        assertEquals(2, success.estimatedItemCount)
        assertEquals("ProtonMail", success.itemsPreview[0].title)
        assertEquals("lucas@proton.me", success.itemsPreview[0].username)
        assertEquals("Personal Communications", success.itemsPreview[0].category)
        assertEquals("SHA256", success.itemsPreview[0].algorithm)

        // Aegis steam entry
        assertEquals("Steam", success.itemsPreview[1].title)
        assertEquals("STEAM", success.itemsPreview[1].algorithm)
        assertEquals(5, success.itemsPreview[1].digits)

        val directParsed = AegisTwoFasParser.parseAegis(aegisJson)
        assertEquals(2, directParsed.size)
    }

    @Test
    fun testTwoFasBackupFormat() {
        val twoFasJson = """
        {
            "schemaVersion": 4,
            "appVersion": 401,
            "groups": [
                { "id": "grp-1", "name": "DevOps Pod" }
            ],
            "services": [
                {
                    "name": "Cloudflare",
                    "groupId": "grp-1",
                    "otp": {
                        "account": "admin@clawstack.io",
                        "issuer": "Cloudflare",
                        "secret": "JBSWY3DPEHPK3PXP",
                        "digits": 6,
                        "period": 30,
                        "algorithm": "SHA1"
                    }
                },
                {
                    "name": "Steam Guard",
                    "groupId": null,
                    "serviceType": "STEAM",
                    "secret": "HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ",
                    "otp": {
                        "account": "lucas",
                        "digits": 5,
                        "period": 30
                    }
                }
            ]
        }
        """.trimIndent()

        val result = MultiVaultBackupPreValidator.validateString(twoFasJson)
        assertTrue(result is PreValidationResult.Success)
        val success = result as PreValidationResult.Success
        assertEquals(BackupSchemaType.TWO_FAS, success.schemaType)
        assertFalse(success.isEncrypted)
        assertEquals(2, success.estimatedItemCount)
        assertEquals("Cloudflare", success.itemsPreview[0].title)
        assertEquals("DevOps Pod", success.itemsPreview[0].category)
        assertEquals("Steam Guard", success.itemsPreview[1].title)
        assertEquals("STEAM", success.itemsPreview[1].algorithm)
        assertEquals(5, success.itemsPreview[1].digits)

        val directParsed = AegisTwoFasParser.parseTwoFas(twoFasJson)
        assertEquals(2, directParsed.size)
    }

    // ── 6. Error Handling & Corrupted JSON Rejection ─────────────────────────

    @Test
    fun testCorruptedOrInvalidJsonRejection() {
        // 1. Empty string
        val emptyResult = MultiVaultBackupPreValidator.validateString("")
        assertTrue(emptyResult is PreValidationResult.Error)
        assertEquals("The selected file is empty.", (emptyResult as PreValidationResult.Error).message)

        // 2. Syntax corrupted JSON
        val malformedResult = MultiVaultBackupPreValidator.validateString("{ \"items\": [ { invalid json")
        assertTrue(malformedResult is PreValidationResult.Error)
        assertTrue((malformedResult as PreValidationResult.Error).message.contains("Invalid JSON structure"))

        // 3. Unrecognized object schema
        val unknownResult = MultiVaultBackupPreValidator.validateString("{ \"foo\": \"bar\", \"count\": 42 }")
        assertTrue(unknownResult is PreValidationResult.Error)
        assertTrue((unknownResult as PreValidationResult.Error).message.contains("Unrecognized JSON backup schema"))

        // 4. Empty array
        val emptyArrayResult = MultiVaultBackupPreValidator.validateString("[]")
        assertTrue(emptyArrayResult is PreValidationResult.Error)
        assertEquals("The JSON array contains 0 items.", (emptyArrayResult as PreValidationResult.Error).message)

        // 5. InputStream validation
        val stream = ByteArrayInputStream("malformed stream content".toByteArray(StandardCharsets.UTF_8))
        val streamResult = MultiVaultBackupPreValidator.validate(stream, "bad.json")
        assertTrue(streamResult is PreValidationResult.Error)
    }

    // ── 7. Encrypted Backup Decryption with Correct vs Incorrect Password ─────

    @Test
    fun testDecryptShellGuardBackupCorrectAndIncorrectPassword() {
        val ownerUuid = "habitat-owner-user"
        val correctPassword = "MySecretVaultMasterPassword123!"
        val incorrectPassword = "WrongPassword456!"

        val itemsToBackup = listOf(
            BackupItemDto(
                id = "item-alpha",
                title = "Proton Mail",
                username = "lucas@pm.me",
                category = "Privacy",
                secret = "JBSWY3DPEHPK3PXP",
                algorithm = "SHA256",
                digits = 6,
                period = 30
            ),
            BackupItemDto(
                id = "item-beta",
                title = "Tailscale Admin",
                username = "admin",
                category = "VPN",
                secret = "KRSXG5CTMVRXEZLUKN2XAZLS",
                algorithm = "SHA1",
                digits = 6,
                period = 30
            )
        )

        val plainItemsJson = json.encodeToString(itemsToBackup)
        val checksumSha256 = computeSha256(plainItemsJson)

        val shellKey = ShellCryptionEngine.deriveShellKey(correctPassword, ownerUuid)
        val encryptedEnvelope = ShellCryptionEngine.encryptField(
            plainText = plainItemsJson,
            shellKey = shellKey,
            table = "totp_backup",
            recordId = ownerUuid
        )

        val validBackupEnvelopeJson = """
        {
            "version": 1,
            "type": "shellguard-totp-backup-v1",
            "ownerUuid": "$ownerUuid",
            "itemCount": 2,
            "createdAt": 1700000000000,
            "checksumSha256": "$checksumSha256",
            "encryptedEnvelopeJson": ${json.encodeToString(encryptedEnvelope)}
        }
        """.trimIndent()

        // 1. Decrypt with CORRECT password -> SUCCESS
        val successResult = MultiVaultBackupPreValidator.decryptShellGuardBackup(
            rawJson = validBackupEnvelopeJson,
            passwordOrKey = correctPassword,
            targetOwnerUuid = "local"
        )
        assertTrue(successResult.isSuccess)
        val decryptedItems = successResult.getOrNull()
        assertNotNull(decryptedItems)
        assertEquals(2, decryptedItems!!.size)
        assertEquals("Proton Mail", decryptedItems[0].title)
        assertEquals("JBSWY3DPEHPK3PXP", decryptedItems[0].secret)
        assertEquals("SHA256", decryptedItems[0].algorithm)
        assertEquals("Tailscale Admin", decryptedItems[1].title)

        // 2. Decrypt with INCORRECT password -> FAILURE
        val failResult = MultiVaultBackupPreValidator.decryptShellGuardBackup(
            rawJson = validBackupEnvelopeJson,
            passwordOrKey = incorrectPassword,
            targetOwnerUuid = "local"
        )
        assertTrue(failResult.isFailure)
        val exception = failResult.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(
            "Error message must be user-friendly without exposing raw OpenSSL C++ crashes",
            exception!!.message!!.contains("Incorrect PIN or Master Password")
        )
    }

    // ── 7. Proprietary .sgtotp.bak Format & PIN Code Detection ────────────────

    @Test
    fun testPreValidateSgtotpBakWithPinCodeAndBiometrics() {
        val pin = "1234"
        val ownerUuid = "local"
        val dtos = listOf(
            BackupItemDto(
                id = "item-pin-1",
                ownerUuid = ownerUuid,
                title = "Steam Authenticator",
                username = "lucas",
                secret = "HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ",
                algorithm = "STEAM",
                digits = 5
            )
        )
        val plainJson = json.encodeToString(dtos)
        val checksumSha256 = computeSha256(plainJson)
        val shellKey = ShellCryptionEngine.deriveShellKey(pin, ownerUuid)
        val encryptedEnvelopeJson = ShellCryptionEngine.encryptField(
            plaintext = plainJson,
            shellKey = shellKey,
            table = "totp_backup",
            recordId = ownerUuid
        )

        val sgtotpBakJson = """
        {
            "version": 1,
            "type": "shellguard-totp-backup-v1",
            "format": "sgtotp.bak",
            "protectionMode": "PIN",
            "isBiometricEnabled": true,
            "pinLength": 4,
            "createdAt": 1700000000000,
            "ownerUuid": "$ownerUuid",
            "itemCount": 1,
            "checksumSha256": "$checksumSha256",
            "encryptedEnvelopeJson": ${json.encodeToString(encryptedEnvelopeJson)}
        }
        """.trimIndent()

        // 1. Validate format detection
        val result = MultiVaultBackupPreValidator.validateString(sgtotpBakJson, "my_backup.sgtotp.bak")
        assertTrue(result is PreValidationResult.Success)
        val success = result as PreValidationResult.Success
        assertEquals(BackupSchemaType.SHELLGUARD_ENCRYPTED, success.schemaType)
        assertTrue(success.isEncrypted)
        assertEquals("PIN", success.protectionMode)
        assertTrue(success.isBiometricEnabled)
        assertEquals(4, success.pinLength)
        assertEquals(1, success.estimatedItemCount)

        // 2. Decrypt with PIN
        val decryptResult = MultiVaultBackupPreValidator.decryptShellGuardBackup(
            rawJson = sgtotpBakJson,
            passwordOrKey = pin,
            targetOwnerUuid = "local"
        )
        assertTrue(decryptResult.isSuccess)
        val items = decryptResult.getOrThrow()
        assertEquals(1, items.size)
        assertEquals("Steam Authenticator", items[0].title)
        assertEquals("STEAM", items[0].algorithm)
        assertEquals(5, items[0].digits)

        // 3. Decrypt with wrong PIN -> returns clean user-friendly error
        val badResult = MultiVaultBackupPreValidator.decryptShellGuardBackup(
            rawJson = sgtotpBakJson,
            passwordOrKey = "9999",
            targetOwnerUuid = "local"
        )
        assertTrue(badResult.isFailure)
        assertEquals(
            "Incorrect PIN or Master Password. Please check your secret and try again.",
            badResult.exceptionOrNull()?.message
        )
    }

    private fun computeSha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
