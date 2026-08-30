package com.clawstack.shellguard.totp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.clawstack.shellguard.totp.ui.components.SwipeableTotpCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SwipeConfirmationAndBackButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSwipeableTotpCardRendersComponentsProperly() {
        composeTestRule.mainClock.autoAdvance = false
        var copyClicked = false
        var editClicked = false
        var deleteClicked = false

        composeTestRule.setContent {
            SwipeableTotpCard(
                title = "AWS Production",
                username = "admin@clawstack.io",
                category = "Cloud",
                code = "123 456",
                remainingSeconds = 25,
                progress = 0.8f,
                isLocalOnly = false,
                onCopy = { copyClicked = true },
                onEdit = { editClicked = true },
                onDelete = { deleteClicked = true }
            )
        }

        composeTestRule.mainClock.advanceTimeBy(500)

        // Verify account title and username are visible
        composeTestRule.onNodeWithText("AWS Production").assertIsDisplayed()
        composeTestRule.onNodeWithText("admin@clawstack.io").assertIsDisplayed()
        composeTestRule.onNodeWithText("123 456").assertIsDisplayed()
    }
}
