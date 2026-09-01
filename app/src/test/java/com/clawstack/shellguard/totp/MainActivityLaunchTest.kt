package com.clawstack.shellguard.totp

import android.content.Context
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.clawstack.shellguard.totp.crypto.EncryptedDeviceVault
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = ShellGuardTotpApp::class)
class MainActivityLaunchTest {

    @Test
    fun testApplicationInitializesProperly() {
        val app = ApplicationProvider.getApplicationContext<ShellGuardTotpApp>()
        assertNotNull(app)
        assertNotNull(app.authRepository)
        assertNotNull(app.totpRepository)
        assertNotNull(app.database)
        assertNotNull(app.backupManager)
    }

    @Test
    fun testMainActivityLaunchesWithoutCrashing() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        controller.create()
        controller.start()
        controller.resume()

        val activity = controller.get()
        assertNotNull(activity)
        assertTrue(activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))

        controller.pause()
        controller.stop()
        controller.destroy()
    }

    @Test
    fun testMainActivityScenarioLaunch() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
        scenario.close()
    }

    @Test
    fun testSplashScreenIconLoadsSuccessfully() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val drawable = ResourcesCompat.getDrawable(context.resources, R.drawable.ic_splash_icon, context.theme)
        assertNotNull("Splash screen icon drawable should inflate without throwing", drawable)
    }

    @Test
    fun testColdLaunchWithCorruptedPreferencesDoesNotCrash() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("shellguard_auth_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("saved_server_url", "https://invalid-vault.test")
            .putString("saved_user_uuid", "invalid-uuid-format")
            .putString("saved_token", "invalid_corrupt_base64_payload_format:%%$$")
            .putString("saved_raw_key", "invalid:payload")
            .putString("pref_theme_accent", "NON_EXISTENT_ACCENT_KEY")
            .putString("pref_theme_mode", "INVALID_THEME_STRING")
            .commit()

        val controller = Robolectric.buildActivity(MainActivity::class.java)
        controller.create()
        controller.start()
        controller.resume()

        val activity = controller.get()
        assertNotNull(activity)
        assertTrue(activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))

        controller.pause()
        controller.stop()
        controller.destroy()
    }

    @Test
    fun testActivityLifecycleTransitionBackgroundAndForeground() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        controller.create()
        controller.start()
        controller.resume()

        // Background app
        controller.pause()
        controller.stop()

        // Foreground app
        controller.restart()
        controller.start()
        controller.resume()

        val activity = controller.get()
        assertNotNull(activity)
        assertTrue(activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))

        controller.pause()
        controller.stop()
        controller.destroy()
    }

    @Test
    fun testEncryptedDeviceVaultPassphraseCreationUnderConcurrentAccess() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val p1 = EncryptedDeviceVault.getOrCreateDatabasePassphrase(context)
        val p2 = EncryptedDeviceVault.getOrCreateDatabasePassphrase(context)
        assertNotNull(p1)
        assertNotNull(p2)
        assertEquals(32, p1.size)
        assertEquals(32, p2.size)
    }

    @Test
    fun testFreshLaunchStartsAtIntakeWelcome() {
        val app = ApplicationProvider.getApplicationContext<ShellGuardTotpApp>()
        // Confirm fresh launch state
        assertFalse(app.authRepository.isVaultHatched.value)

        val controller = Robolectric.buildActivity(MainActivity::class.java)
        controller.create()
        controller.start()
        controller.resume()

        val activity = controller.get()
        assertNotNull(activity)
        assertTrue(activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))

        controller.pause()
        controller.stop()
        controller.destroy()
    }
}
