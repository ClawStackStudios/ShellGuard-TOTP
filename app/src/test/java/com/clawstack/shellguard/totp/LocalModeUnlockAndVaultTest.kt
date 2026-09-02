package com.clawstack.shellguard.totp

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.clawstack.shellguard.totp.data.local.ShellGuardTotpDatabase
import com.clawstack.shellguard.totp.data.local.entities.TotpItemEntity
import com.clawstack.shellguard.totp.data.repository.AuthRepository
import com.clawstack.shellguard.totp.data.repository.TotpRepository
import com.clawstack.shellguard.totp.data.repository.VaultProtectionMode
import com.clawstack.shellguard.totp.ui.screens.LockScreen
import com.clawstack.shellguard.totp.ui.screens.TotpListScreen
import com.clawstack.shellguard.totp.ui.viewmodels.AuthViewModel
import com.clawstack.shellguard.totp.ui.viewmodels.TotpViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import android.os.Looper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = ShellGuardTotpApp::class, qualifiers = "w1000dp-h2000dp")
class LocalModeUnlockAndVaultTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: ShellGuardTotpDatabase
    private lateinit var authRepository: AuthRepository
    private lateinit var totpRepository: TotpRepository
    private lateinit var authViewModel: AuthViewModel
    private lateinit var totpViewModel: TotpViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<ShellGuardTotpApp>()
        val app = context as ShellGuardTotpApp
        database = app.database
        authRepository = app.authRepository
        totpRepository = app.totpRepository
        runBlocking {
            database.totpItemDao().clearVault("local")
        }
        authViewModel = AuthViewModel(app)
        totpViewModel = TotpViewModel(app)
        // Dismiss spotlight guided tour so it does not obscure underlying screen elements in UI tests
        authRepository.setGuidedTourCompleted(true)
    }

    @After
    fun tearDown() {
        // We omit database.close() here because closing the Room database while
        // TotpViewModel still has active Flow subscriptions (via stateIn) causes Room
        // to throw a JobCancellationException which crashes the Robolectric test.
        // Since it's an in-memory database, it will safely be garbage collected.
    }

    @Test
    fun testLocalModeHatchWithPinUnlock() {
        // 1. Hatch vault in PIN mode with secret "5678" in short runBlocking
        runBlocking {
            database.totpItemDao().clearVault("local")
            authViewModel.hatchVault("5678", isPin = true, enableBiometrics = false)
            assertTrue(authViewModel.isVaultHatched.first())
            assertEquals(VaultProtectionMode.PIN, authViewModel.vaultMode.first())

            // Lock vault
            authRepository.lockVault()
            assertTrue(authRepository.isLocked.first())
        }

        // 2. Render LockScreen and unlock with PIN (looper is free to pump)
        var unlocked = false
        composeTestRule.setContent {
            LockScreen(
                vaultMode = VaultProtectionMode.PIN,
                isBiometricEnabled = false,
                onUnlockWithSecret = { pin ->
                    val success = authViewModel.unlockWithSecret(pin)
                    if (success) unlocked = true
                    success
                },
                onUnlockSuccess = {},
                onNavigateToGateway = {},
            )
        }

        composeTestRule.onNodeWithTag("unlock_pin_input").performScrollTo().performTextReplacement("5678")
        composeTestRule.onNodeWithTag("unlock_pin_submit").performScrollTo().performClick()

        assertTrue("Unlock with PIN should succeed", unlocked)
        runBlocking {
            assertFalse("Vault should no longer be locked", authRepository.isLocked.first())
        }
    }

    @Test
    fun testLocalModeVaultLandingRendersEmptyStateAfterHatch() {
        // 1. Hatch vault in short runBlocking and ensure clean table
        runBlocking {
            database.totpItemDao().clearVault("local")
            authViewModel.hatchVault("5678", isPin = true, enableBiometrics = false)
            assertTrue(authViewModel.isVaultHatched.first())
        }

        // 2. Render TotpListScreen (looper is free to pump)
        composeTestRule.setContent {
            TotpListScreen(
                viewModel = totpViewModel,
                onAddSecretClick = {},
                onScanQrClick = {},
                onSettingsClick = {}
            )
        }

        // 3. Verify Empty state renders and animations settle
        //    (Manual polling: Compose test clock stalls in headless Robolectric CI,
        //     so we pump the Looper ourselves instead of using waitUntil.)
        var retries = 0
        while (composeTestRule.onAllNodesWithTag("totp_empty_state").fetchSemanticsNodes().isEmpty() && retries < 50) {
            Thread.sleep(100)
            shadowOf(Looper.getMainLooper()).idle()
            composeTestRule.mainClock.advanceTimeBy(200)
            retries++
        }
        composeTestRule.mainClock.advanceTimeBy(1000)

        composeTestRule.onNodeWithTag("totp_empty_state").assertIsDisplayed()
        composeTestRule.onNodeWithText("No 2FA Codes Yet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("scan_qr_fab").assertIsDisplayed()
    }

    @Test
    fun testLocalModeVaultWithLocalItemsRendersAndFiltersWithoutCrash() {
        // 1. Hatch vault and insert items in short runBlocking
        runBlocking {
            authViewModel.hatchVault("1122", isPin = true, enableBiometrics = false)

            val item1 = TotpItemEntity(
                id = "local-uuid-1",
                ownerUuid = "local",
                title = "GitHub Personal",
                username = "octocat",
                category = "DevOps",
                secret = "JBSWY3DPEHPK3PXP",
                isLocalOnly = true,
                syncState = "LOCAL"
            )
            val item2 = TotpItemEntity(
                id = "local-uuid-2",
                ownerUuid = "local",
                title = "ProtonMail",
                username = "sec@proton.me",
                category = "Email",
                secret = "HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ",
                isLocalOnly = true,
                syncState = "LOCAL"
            )
            database.totpItemDao().upsertItem(item1)
            database.totpItemDao().upsertItem(item2)
        }

        // 2. Render TotpListScreen (looper is free to pump)
        composeTestRule.setContent {
            TotpListScreen(
                viewModel = totpViewModel,
                onAddSecretClick = {},
                onScanQrClick = {},
                onSettingsClick = {}
            )
        }

        // 3. Verify items display and animations settle
        //    (Manual polling: same headless Robolectric CI workaround.)
        var retries = 0
        while (composeTestRule.onAllNodesWithText("GitHub Personal").fetchSemanticsNodes().isEmpty() && retries < 50) {
            Thread.sleep(100)
            shadowOf(Looper.getMainLooper()).idle()
            composeTestRule.mainClock.advanceTimeBy(200)
            retries++
        }
        composeTestRule.mainClock.advanceTimeBy(1000)

        composeTestRule.onNodeWithText("GitHub Personal").assertIsDisplayed()
        composeTestRule.onNodeWithText("ProtonMail").assertIsDisplayed()
        composeTestRule.onNodeWithTag("filter_chip_all").assertIsDisplayed()
    }
}
