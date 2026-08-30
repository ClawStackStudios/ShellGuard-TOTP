package com.clawstack.shellguard.totp

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.clawstack.shellguard.totp.data.repository.AuthRepository
import com.clawstack.shellguard.totp.data.repository.VaultProtectionMode
import com.clawstack.shellguard.totp.ui.theme.AppThemeMode
import com.clawstack.shellguard.totp.ui.theme.ThemeAccent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthVaultModeRepositoryTest {

    private lateinit var context: Context
    private lateinit var authRepository: AuthRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clear prefs before test
        context.getSharedPreferences("shellguard_auth_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        authRepository = AuthRepository(context)
    }

    @Test
    fun testVaultModeDefaultsToPinAndUpdates() {
        assertEquals(VaultProtectionMode.PIN, authRepository.vaultMode.value)

        authRepository.updateVaultSecret("123456", isPin = false)
        assertEquals(VaultProtectionMode.PASSWORD, authRepository.vaultMode.value)

        authRepository.updateVaultSecret("9876", isPin = true)
        assertEquals(VaultProtectionMode.PIN, authRepository.vaultMode.value)
    }

    @Test
    fun testUnlockWithPinAndWithPassword() {
        // Set PIN
        authRepository.updateVaultSecret("4321", isPin = true)
        assertEquals(VaultProtectionMode.PIN, authRepository.vaultMode.value)
        assertTrue(authRepository.unlockWithSecret("4321"))
        assertFalse(authRepository.unlockWithSecret("0000"))

        // Update to Master Password
        authRepository.updateVaultSecret("SuperSecurePassword999", isPin = false)
        assertEquals(VaultProtectionMode.PASSWORD, authRepository.vaultMode.value)
        assertTrue(authRepository.unlockWithSecret("SuperSecurePassword999"))
        assertFalse(authRepository.unlockWithSecret("WrongPassword"))
    }

    @Test
    fun testThemeModeSwitching() {
        assertEquals(AppThemeMode.DARK, authRepository.themeMode.value)

        authRepository.setThemeMode(AppThemeMode.LIGHT)
        assertEquals(AppThemeMode.LIGHT, authRepository.themeMode.value)

        authRepository.setThemeMode(AppThemeMode.SYSTEM)
        assertEquals(AppThemeMode.SYSTEM, authRepository.themeMode.value)
    }

    @Test
    fun testThemeAccentSwitching() {
        assertEquals(ThemeAccent.REEF_DEFAULT, authRepository.themeAccent.value)

        authRepository.setThemeAccent(ThemeAccent.CYAN_VENT)
        assertEquals(ThemeAccent.CYAN_VENT, authRepository.themeAccent.value)

        authRepository.setThemeAccent(ThemeAccent.PURPLE_SHELL)
        assertEquals(ThemeAccent.PURPLE_SHELL, authRepository.themeAccent.value)

        authRepository.setThemeAccent(ThemeAccent.EMERALD_TRENCH)
        assertEquals(ThemeAccent.EMERALD_TRENCH, authRepository.themeAccent.value)

        authRepository.setThemeAccent(ThemeAccent.AMBER_FLARE)
        assertEquals(ThemeAccent.AMBER_FLARE, authRepository.themeAccent.value)

        authRepository.setThemeAccent(ThemeAccent.MONOCHROME)
        assertEquals(ThemeAccent.MONOCHROME, authRepository.themeAccent.value)
    }
}
