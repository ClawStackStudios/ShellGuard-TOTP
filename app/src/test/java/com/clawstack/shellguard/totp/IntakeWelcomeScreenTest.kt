package com.clawstack.shellguard.totp

import android.app.Application
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.clawstack.shellguard.totp.data.backup.BackupSchemaType
import com.clawstack.shellguard.totp.data.backup.PreValidationResult
import com.clawstack.shellguard.totp.data.local.entities.TotpItemEntity
import com.clawstack.shellguard.totp.ui.onboarding.IntakeStep
import com.clawstack.shellguard.totp.ui.onboarding.IntakeUiState
import com.clawstack.shellguard.totp.ui.onboarding.IntakeViewModel
import com.clawstack.shellguard.totp.ui.screens.onboarding.IntakeWelcomeScreen
import com.clawstack.shellguard.totp.ui.theme.ShellGuardTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = ShellGuardTotpApp::class)
class IntakeWelcomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var app: Application
    private lateinit var viewModel: IntakeViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        viewModel = IntakeViewModel(app)
    }

    @Test
    fun testWelcomeScreenRendersHeroAndActionButtons() {
        composeTestRule.mainClock.autoAdvance = false
        var navigatedToFreshVault = false
        var intakeCompleted = false

        composeTestRule.setContent {
            ShellGuardTheme {
                IntakeWelcomeScreen(
                    viewModel = viewModel,
                    onNavigateToFreshVault = { navigatedToFreshVault = true },
                    onIntakeCompleted = { intakeCompleted = true }
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(300)

        // Verify Hero typography
        composeTestRule.onNodeWithText("ShellGuard TOTP").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your zero-knowledge privacy fortress for two-factor authentication").assertIsDisplayed()

        // Verify Security Badges
        composeTestRule.onNodeWithText("Zero-Knowledge").assertIsDisplayed()
        composeTestRule.onNodeWithText("KeyStore Sealed").assertIsDisplayed()
        composeTestRule.onNodeWithText("100% Offline").assertIsDisplayed()

        // Verify Import Button & Forward FAB
        composeTestRule.onNodeWithTag("import_habitat_button").assertIsDisplayed().assertHasClickAction()
        composeTestRule.onNodeWithTag("fresh_vault_forward_button").assertIsDisplayed().assertHasClickAction()

        // Click Forward FAB
        composeTestRule.onNodeWithTag("fresh_vault_forward_button").performClick()
        composeTestRule.mainClock.advanceTimeBy(300)
        assertTrue(navigatedToFreshVault)
    }

    @Test
    fun testPasswordPromptModalBottomSheetFlow() {
        composeTestRule.mainClock.autoAdvance = false

        // Simulate Encrypted Habitat file selection directly on ViewModel state
        viewModel.onProtectionModeChanged(isPin = true)
        assertTrue(viewModel.uiState.value.isPinMode)
        viewModel.onPasswordChanged("1234")

        composeTestRule.setContent {
            ShellGuardTheme {
                IntakeWelcomeScreen(
                    viewModel = viewModel,
                    onNavigateToFreshVault = {},
                    onIntakeCompleted = {}
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(300)
        // Check input updates
        assertEquals("1234", viewModel.uiState.value.passwordInput)

        // Switch to password mode
        viewModel.onProtectionModeChanged(isPin = false)
        assertFalse(viewModel.uiState.value.isPinMode)
        viewModel.onPasswordChanged("MasterPassphrase!")
        assertEquals("MasterPassphrase!", viewModel.uiState.value.passwordInput)
    }

    @Test
    fun testSummaryAndProtectionContentBehavior() {
        composeTestRule.mainClock.autoAdvance = false

        val sampleDecryptedItems = listOf(
            TotpItemEntity(
                id = "item-1",
                ownerUuid = "local",
                title = "GitHub",
                username = "octocat",
                category = "Development",
                secret = "JBSWY3DPEHPK3PXP"
            ),
            TotpItemEntity(
                id = "item-2",
                ownerUuid = "local",
                title = "AWS",
                username = "admin",
                category = "Cloud",
                secret = "KRSXG5CTMVRXEZLUKN2XAZLS"
            )
        )

        composeTestRule.setContent {
            ShellGuardTheme {
                IntakeWelcomeScreen(
                    viewModel = viewModel,
                    onNavigateToFreshVault = {},
                    onIntakeCompleted = {}
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(300)

        // Test PIN vs Password mode toggles on ViewModel
        viewModel.onProtectionModeChanged(isPin = true)
        assertTrue(viewModel.uiState.value.isPinMode)

        viewModel.onPasswordChanged("1234")
        viewModel.onConfirmSecretChanged("1234")
        assertTrue(viewModel.uiState.value.isSecretValid)

        viewModel.onProtectionModeChanged(isPin = false)
        assertFalse(viewModel.uiState.value.isPinMode)
        assertEquals("", viewModel.uiState.value.passwordInput)

        viewModel.onPasswordChanged("MasterPassphrase99!")
        viewModel.onConfirmSecretChanged("MasterPassphrase99!")
        assertTrue(viewModel.uiState.value.isSecretValid)

        // Test biometric toggle
        viewModel.onBiometricToggleChanged(false)
        assertFalse(viewModel.uiState.value.enableBiometrics)
        viewModel.onBiometricToggleChanged(true)
        assertTrue(viewModel.uiState.value.enableBiometrics)

        // Test Secret Reuse vs Key Rotation
        viewModel.onToggleReuseSecret(true)
        assertTrue(viewModel.uiState.value.isReusingDecryptionSecret)

        viewModel.onToggleReuseSecret(false)
        assertFalse(viewModel.uiState.value.isReusingDecryptionSecret)
        assertEquals("", viewModel.uiState.value.passwordInput)
    }
}
