package com.clawstack.shellguard.totp

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextReplacement
import com.clawstack.shellguard.totp.ui.screens.AddSecretScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Phase4ScreensTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testAddSecretScreenValidationAndSubmission() {
        composeTestRule.mainClock.autoAdvance = false
        var savedTitle = ""
        var savedSecret = ""
        var savedAlgorithm = ""

        composeTestRule.setContent {
            AddSecretScreen(
                onSaveSecret = { title, _, _, secret, algorithm, _, _ ->
                    savedTitle = title
                    savedSecret = secret
                    savedAlgorithm = algorithm
                },
                onBackClick = {}
            )
        }

        composeTestRule.mainClock.advanceTimeBy(300)

        // Initially disabled because title and secret are empty
        composeTestRule.onNodeWithTag("save_secret_button").assertIsNotEnabled()

        // Enter Title
        composeTestRule.onNodeWithTag("add_secret_title_input").performTextReplacement("GitHub Enterprise")
        composeTestRule.mainClock.advanceTimeBy(300)

        // Enter Valid Base32 Secret
        composeTestRule.onNodeWithTag("add_secret_key_input").performTextReplacement("JBSWY3DPEHPK3PXP")
        composeTestRule.mainClock.advanceTimeBy(300)

        // Button should now be enabled
        composeTestRule.onNodeWithTag("save_secret_button").assertIsEnabled()
    }
}
