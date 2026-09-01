package com.clawstack.shellguard.totp

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.clawstack.shellguard.totp.crypto.ShellCryptionEngine
import com.clawstack.shellguard.totp.data.backup.BackupEnvelope
import com.clawstack.shellguard.totp.data.backup.BackupItemDto
import com.clawstack.shellguard.totp.data.backup.BackupSchemaType
import com.clawstack.shellguard.totp.data.backup.MultiVaultBackupPreValidator
import com.clawstack.shellguard.totp.data.backup.PreValidationResult
import com.clawstack.shellguard.totp.data.local.ShellGuardTotpDatabase
import com.clawstack.shellguard.totp.data.repository.AuthRepository
import com.clawstack.shellguard.totp.data.repository.VaultProtectionMode
import com.clawstack.shellguard.totp.ui.onboarding.IntakeStep
import com.clawstack.shellguard.totp.ui.onboarding.IntakeViewModel
import com.clawstack.shellguard.totp.ui.screens.onboarding.IntakeWelcomeScreen
import com.clawstack.shellguard.totp.ui.theme.ShellGuardTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import android.os.Looper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = ShellGuardTotpApp::class, qualifiers = "w1000dp-h2000dp")
class IntakeOnboardingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var app: ShellGuardTotpApp
    private lateinit var database: ShellGuardTotpDatabase
    private lateinit var authRepository: AuthRepository
    private lateinit var intakeViewModel: IntakeViewModel

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<ShellGuardTotpApp>()
        app = context as ShellGuardTotpApp
        database = app.database
        authRepository = app.authRepository
        intakeViewModel = IntakeViewModel(app)
    }

    @After
    fun tearDown() {
        try {
            val clearMethod = androidx.lifecycle.ViewModel::class.java.getDeclaredMethod("clear")
            clearMethod.isAccessible = true
            clearMethod.invoke(intakeViewModel)
        } catch (e: Exception) {
            // Ignore
        }
        database.close()
    }

    // ── 1. MultiVaultBackupPreValidator Tests ─────────────────────────

    @Test
    fun testPreValidateShellGuardEncryptedHabitat() {
        val ownerUuid = "local"
        val rawKey = "hu-secret-key-123"
        val dtos = listOf(
            BackupItemDto(
                id = "item-1",
                ownerUuid = ownerUuid,
                title = "GitHub",
                username = "dev",
                secret = "JBSWY3DPEHPK3PXP"
            )
        )
        val plainJson = json.encodeToString(dtos)
        val checksum = MessageDigest.getInstance("SHA-256")
            .digest(plainJson.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

        val shellKey = ShellCryptionEngine.deriveShellKey(rawKey, ownerUuid)
        val encryptedEnvelopeJson = ShellCryptionEngine.encryptField(
            plaintext = plainJson,
            shellKey = shellKey,
            table = "totp_backup",
            recordId = ownerUuid
        )

        val envelope = BackupEnvelope(
            version = 1,
            type = "shellguard-totp-backup-v1",
            createdAt = System.currentTimeMillis(),
            ownerUuid = ownerUuid,
            itemCount = dtos.size,
            checksumSha256 = checksum,
            encryptedEnvelopeJson = encryptedEnvelopeJson
        )
        val envelopeJson = json.encodeToString(envelope)

        val result = MultiVaultBackupPreValidator.validateString(envelopeJson, "my_backup.shellguard")
        assertTrue(result is PreValidationResult.Success)
        val success = result as PreValidationResult.Success
        assertEquals(BackupSchemaType.SHELLGUARD_ENCRYPTED, success.schemaType)
        assertTrue(success.isEncrypted)
        assertEquals(1, success.estimatedItemCount)

        // Decrypt with correct key
        val decryptResult = MultiVaultBackupPreValidator.decryptShellGuardBackup(envelopeJson, rawKey, "local")
        assertTrue(decryptResult.isSuccess)
        val items = decryptResult.getOrThrow()
        assertEquals(1, items.size)
        assertEquals("GitHub", items[0].title)
        assertEquals("JBSWY3DPEHPK3PXP", items[0].secret)

        // Decrypt with wrong key
        val badDecrypt = MultiVaultBackupPreValidator.decryptShellGuardBackup(envelopeJson, "wrong-key", "local")
        assertTrue(badDecrypt.isFailure)
    }

    @Test
    fun testPreValidateBitwardenPlainVaultWithZeroKnowledgeSanitization() {
        val bitwardenJson = """
            {
              "encrypted": false,
              "folders": [
                { "id": "f-1", "name": "Work Infrastructure" },
                { "id": "f-2", "name": "Personal" }
              ],
              "items": [
                {
                  "id": "bw-1",
                  "folderId": "f-1",
                  "type": 1,
                  "name": "AWS Console",
                  "notes": "SUPER_SECRET_NOTE_MUST_BE_DISCARDED",
                  "login": {
                    "username": "admin@clawstack.io",
                    "password": "SUPER_SECRET_PASSWORD_MUST_BE_DISCARDED",
                    "totp": "otpauth://totp/AWS:admin@clawstack.io?secret=JBSWY3DPEHPK3PXP&issuer=AWS&algorithm=SHA1&digits=6&period=30"
                  }
                },
                {
                  "id": "bw-2",
                  "folderId": "f-2",
                  "type": 1,
                  "name": "Steam Gaming",
                  "login": {
                    "username": "gamer_lucas",
                    "password": "PASSWORD_DISCARDED",
                    "totp": "steam://HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ"
                  }
                },
                {
                  "id": "bw-3",
                  "folderId": "f-2",
                  "type": 1,
                  "name": "Webmail No 2FA",
                  "login": {
                    "username": "no_2fa@webmail.com",
                    "password": "PASSWORD_DISCARDED",
                    "totp": null
                  }
                }
              ]
            }
        """.trimIndent()

        val result = MultiVaultBackupPreValidator.validateString(bitwardenJson, "bitwarden_export.json")
        assertTrue(result is PreValidationResult.Success)
        val success = result as PreValidationResult.Success
        assertEquals(BackupSchemaType.BITWARDEN_VAULT, success.schemaType)
        assertFalse(success.isEncrypted)
        assertEquals(2, success.estimatedItemCount) // bw-3 is omitted because totp is null

        val items = success.itemsPreview
        assertEquals(2, items.size)

        val aws = items.first { it.title == "AWS" || it.title == "AWS Console" }
        assertEquals("JBSWY3DPEHPK3PXP", aws.secret)
        assertEquals("Work Infrastructure", aws.category)

        val steam = items.first { it.title == "Steam" || it.title == "Steam Gaming" }
        assertEquals("HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ", steam.secret)
        assertEquals("STEAM", steam.algorithm)
        assertEquals(5, steam.digits)
        assertEquals("Personal", steam.category)
    }

    @Test
    fun testPreValidateBitwardenAuthenticatorFlatList() {
        val authListJson = """
            [
              {
                "issuer": "Google",
                "name": "user@gmail.com",
                "key": "KRSXG5CTMVRXEZLUKN2XAZLS",
                "algorithm": "SHA1",
                "digits": 6,
                "period": 30
              },
              {
                "issuer": "Cloudflare",
                "name": "ops@clawstack.com",
                "key": "JBSWY3DPEHPK3PXP",
                "algorithm": "SHA256",
                "digits": 8,
                "period": 30
              }
            ]
        """.trimIndent()

        val result = MultiVaultBackupPreValidator.validateString(authListJson, "bitwarden_authenticator.json")
        assertTrue(result is PreValidationResult.Success)
        val success = result as PreValidationResult.Success
        assertEquals(BackupSchemaType.BITWARDEN_AUTHENTICATOR, success.schemaType)
        assertFalse(success.isEncrypted)
        assertEquals(2, success.estimatedItemCount)
        assertEquals("Google", success.itemsPreview[0].title)
        assertEquals("KRSXG5CTMVRXEZLUKN2XAZLS", success.itemsPreview[0].secret)
        assertEquals("Cloudflare", success.itemsPreview[1].title)
        assertEquals("SHA256", success.itemsPreview[1].algorithm)
        assertEquals(8, success.itemsPreview[1].digits)
    }

    @Test
    fun testPreValidateAegisAnd2FAS() {
        val aegisJson = """
            {
              "version": 1,
              "db": {
                "version": 1,
                "entries": [
                  {
                    "type": "totp",
                    "name": "alice@github.com",
                    "issuer": "GitHub",
                    "group": "Development",
                    "info": {
                      "secret": "JBSWY3DPEHPK3PXP",
                      "algo": "SHA1",
                      "digits": 6,
                      "period": 30
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        val aegisResult = MultiVaultBackupPreValidator.validateString(aegisJson)
        assertTrue(aegisResult is PreValidationResult.Success)
        assertEquals(BackupSchemaType.AEGIS, (aegisResult as PreValidationResult.Success).schemaType)
        assertEquals(1, aegisResult.estimatedItemCount)
        assertEquals("GitHub", aegisResult.itemsPreview[0].title)
        assertEquals("Development", aegisResult.itemsPreview[0].category)

        val twoFasJson = """
            {
              "services": [
                {
                  "name": "Discord",
                  "secret": "JBSWY3DPEHPK3PXP",
                  "otp": {
                    "account": "discord_user",
                    "digits": 6,
                    "period": 30,
                    "algorithm": "SHA1"
                  },
                  "group": { "name": "Social" }
                }
              ]
            }
        """.trimIndent()

        val twoFasResult = MultiVaultBackupPreValidator.validateString(twoFasJson)
        assertTrue(twoFasResult is PreValidationResult.Success)
        assertEquals(BackupSchemaType.TWO_FAS, (twoFasResult as PreValidationResult.Success).schemaType)
        assertEquals(1, twoFasResult.estimatedItemCount)
        assertEquals("Discord", twoFasResult.itemsPreview[0].title)
    }

    @Test
    fun testPreValidateInvalidCorruptedJsonReturnsError() {
        val corrupted = "{ this is not valid json : 123 "
        val result = MultiVaultBackupPreValidator.validateString(corrupted)
        assertTrue(result is PreValidationResult.Error)
    }

    // ── 2. IntakeViewModel State Machine & Hatching Tests ─────────────

    @Test
    fun testIntakeViewModelPinProtectionValidationAndHatching() = runBlocking {
        // Set PIN mode and test secret validation
        intakeViewModel.onProtectionModeChanged(isPin = true)
        intakeViewModel.onPasswordChanged("1234")
        intakeViewModel.onConfirmSecretChanged("1234")

        val state = intakeViewModel.uiState.value
        assertTrue(state.isSecretValid)
        assertFalse(state.isSecretMismatch)

        // Hatch & Import
        var completed = false
        intakeViewModel.hatchAndImportVault {
            completed = true
        }

        // Flush pending main-thread coroutine tasks (viewModelScope.launch runs on main dispatcher)
        // and wait for Dispatchers.IO context switch to finish.
        composeTestRule.waitUntil(5000) { completed }
        assertEquals(IntakeStep.COMPLETED, intakeViewModel.uiState.value.step)
        assertTrue(authRepository.isVaultHatched.first())
        assertEquals(VaultProtectionMode.PIN, authRepository.vaultMode.first())
    }

    @Test
    fun testIntakeViewModelMasterPasswordProtectionValidation() {
        intakeViewModel.onProtectionModeChanged(isPin = false)
        intakeViewModel.onPasswordChanged("super-strong-passphrase")
        intakeViewModel.onConfirmSecretChanged("different-passphrase")

        val state = intakeViewModel.uiState.value
        assertFalse(state.isSecretValid)
        assertTrue(state.isSecretMismatch)

        intakeViewModel.onConfirmSecretChanged("super-strong-passphrase")
        val matchedState = intakeViewModel.uiState.value
        assertTrue(matchedState.isSecretValid)
        assertFalse(matchedState.isSecretMismatch)
    }

    // ── 3. IntakeWelcomeScreen Compose UI Tests ───────────────────────

    @Test
    fun testIntakeWelcomeScreenRendersHeroHeaderAndActions() {
        var navigatedToFresh = false
        var intakeCompleted = false

        composeTestRule.setContent {
            ShellGuardTheme {
                IntakeWelcomeScreen(
                    viewModel = intakeViewModel,
                    onNavigateToFreshVault = { navigatedToFresh = true },
                    onIntakeCompleted = { intakeCompleted = true }
                )
            }
        }

        // Verify Hero Header Title and Subtitle
        composeTestRule.onNodeWithText("ShellGuard TOTP").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your zero-knowledge privacy fortress for two-factor authentication").assertIsDisplayed()

        // Verify Security Badges
        composeTestRule.onNodeWithText("Zero-Knowledge").assertIsDisplayed()
        composeTestRule.onNodeWithText("KeyStore Sealed").assertIsDisplayed()
        composeTestRule.onNodeWithText("100% Offline").assertIsDisplayed()

        // Verify Import Habitat Button
        composeTestRule.onNodeWithTag("import_habitat_button").assertIsDisplayed()

        // Verify Forward Arrow FAB
        composeTestRule.onNodeWithTag("fresh_vault_forward_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("fresh_vault_forward_button").performClick()
        assertTrue("Clicking forward FAB should trigger fresh vault navigation", navigatedToFresh)
    }
}
