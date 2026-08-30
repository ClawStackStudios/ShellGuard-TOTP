package com.clawstack.shellguard.totp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.clawstack.shellguard.totp.data.repository.VaultProtectionMode
import com.clawstack.shellguard.totp.ui.screens.LockScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LockScreenDynamicUnlockTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testLockScreenPinModeDisplaysCorrectLabelsAndUnlocks() {
        var unlockedWithSecret = ""
        var unlockSuccessCalled = false

        composeTestRule.setContent {
            LockScreen(
                vaultMode = VaultProtectionMode.PIN,
                isBiometricEnabled = false,
                onUnlockWithSecret = { input ->
                    unlockedWithSecret = input
                    val matches = input == "1234"
                    if (matches) unlockSuccessCalled = true
                    matches
                },
                onUnlockSuccess = {
                    unlockSuccessCalled = true
                },
                onNavigateToGateway = {}
            )
        }

        // Verify PIN mode indicators
        composeTestRule.onNodeWithText("PIN Protected").assertIsDisplayed()
        composeTestRule.onNodeWithText("ENTER VAULT PIN").assertIsDisplayed()

        // Submit button initially disabled
        composeTestRule.onNodeWithTag("unlock_pin_submit").performScrollTo().assertIsNotEnabled()

        // Enter PIN via performTextReplacement
        composeTestRule.onNodeWithTag("unlock_pin_input").performScrollTo().performTextReplacement("1234")

        // Submit button enabled and clicked
        composeTestRule.onNodeWithTag("unlock_pin_submit").performScrollTo().assertIsEnabled()
        composeTestRule.onNodeWithTag("unlock_pin_submit").performClick()

        assertEquals("1234", unlockedWithSecret)
        assertTrue(unlockSuccessCalled)
    }

    @Test
    fun testLockScreenPasswordModeDisplaysCorrectLabelsAndUnlocks() {
        var unlockedWithSecret = ""
        var unlockSuccessCalled = false

        composeTestRule.setContent {
            LockScreen(
                vaultMode = VaultProtectionMode.PASSWORD,
                isBiometricEnabled = false,
                onUnlockWithSecret = { input ->
                    unlockedWithSecret = input
                    val matches = input == "MySecretPassword123"
                    if (matches) unlockSuccessCalled = true
                    matches
                },
                onUnlockSuccess = {
                    unlockSuccessCalled = true
                },
                onNavigateToGateway = {}
            )
        }

        // Verify Password mode indicators
        composeTestRule.onNodeWithText("Master Password Protected").assertIsDisplayed()
        composeTestRule.onNodeWithText("ENTER MASTER PASSWORD").assertIsDisplayed()

        // Submit button initially disabled
        composeTestRule.onNodeWithTag("unlock_password_submit").performScrollTo().assertIsNotEnabled()

        // Enter Password via performTextReplacement
        composeTestRule.onNodeWithTag("unlock_password_input").performScrollTo().performTextReplacement("MySecretPassword123")

        // Submit button enabled and clicked
        composeTestRule.onNodeWithTag("unlock_password_submit").performScrollTo().assertIsEnabled()
        composeTestRule.onNodeWithTag("unlock_password_submit").performClick()

        assertEquals("MySecretPassword123", unlockedWithSecret)
        assertTrue(unlockSuccessCalled)
    }

    @Test
    fun testLockScreenShowsErrorMessageOnIncorrectSecret() {
        composeTestRule.setContent {
            LockScreen(
                vaultMode = VaultProtectionMode.PIN,
                isBiometricEnabled = false,
                onUnlockWithSecret = { input ->
                    input == "9999"
                },
                onUnlockSuccess = {},
                onNavigateToGateway = {}
            )
        }

        composeTestRule.onNodeWithTag("unlock_pin_input").performScrollTo().performTextReplacement("0000")
        composeTestRule.onNodeWithTag("unlock_pin_submit").performScrollTo().performClick()

        composeTestRule.onNodeWithText("Incorrect PIN code. Please try again.").assertIsDisplayed()
    }
}
