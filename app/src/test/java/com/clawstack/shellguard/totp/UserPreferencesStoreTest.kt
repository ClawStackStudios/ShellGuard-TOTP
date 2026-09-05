package com.clawstack.shellguard.totp

import androidx.test.core.app.ApplicationProvider
import com.clawstack.shellguard.totp.data.preferences.EntryViewMode
import com.clawstack.shellguard.totp.data.preferences.IssuerDisplayMode
import com.clawstack.shellguard.totp.data.preferences.SearchScope
import com.clawstack.shellguard.totp.data.repository.AuthRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 11 / Task 21 — Structured preference store tests.
 * Verifies defaults, live StateFlow updates, persistence across repository
 * recreation (same app-scoped SharedPreferences), and corrupt-value fallback.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = ShellGuardTotpApp::class)
class UserPreferencesStoreTest {

    private lateinit var repository: AuthRepository

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<ShellGuardTotpApp>()
        repository = AuthRepository(app)
    }

    @Test
    fun defaultsMatchSpec() {
        val appearance = repository.appearancePrefs.value
        assertEquals(EntryViewMode.NORMAL, appearance.viewMode)
        assertTrue(appearance.showIcons)
        assertEquals(false, appearance.showNextCode)
        assertEquals(false, appearance.expireBlinkIndicator)
        assertTrue(appearance.digitGrouping)
        assertEquals(IssuerDisplayMode.ISSUER_AND_ACCOUNT, appearance.issuerDisplayMode)
        assertTrue(appearance.hiddenGroups.isEmpty())

        val behavior = repository.behaviorPrefs.value
        assertEquals(false, behavior.focusSearchOnStart)
        assertEquals(SearchScope.ALL, behavior.searchScope)
        assertEquals(false, behavior.minimizeOnCopy)
        assertTrue(behavior.copyOnTap)
        assertTrue(behavior.hapticFeedback)
        assertEquals(false, behavior.multiselectGroups)
        assertEquals(false, behavior.highlightTokensOnTap)
        assertEquals(false, behavior.freezeTokensOnTap)
    }

    @Test
    fun settersUpdateFlowsSynchronously() {
        repository.setViewMode(EntryViewMode.COMPACT)
        assertEquals(EntryViewMode.COMPACT, repository.appearancePrefs.value.viewMode)

        repository.setShowNextCode(true)
        assertTrue(repository.appearancePrefs.value.showNextCode)

        repository.setIssuerDisplayMode(IssuerDisplayMode.ISSUER_ONLY)
        assertEquals(IssuerDisplayMode.ISSUER_ONLY, repository.appearancePrefs.value.issuerDisplayMode)

        repository.setSearchScope(SearchScope.LOCAL_ONLY)
        assertEquals(SearchScope.LOCAL_ONLY, repository.behaviorPrefs.value.searchScope)

        repository.setFreezeTokensOnTap(true)
        assertTrue(repository.behaviorPrefs.value.freezeTokensOnTap)
    }

    @Test
    fun preferencesPersistAcrossRepositoryRecreation() {
        repository.setDigitGrouping(false)
        repository.setExpireBlinkIndicator(true)
        repository.setMinimizeOnCopy(true)
        repository.setGroupHidden("Work/DevOps", hidden = true)

        // Fresh repository instance over the same app-scoped SharedPreferences
        val recreated = AuthRepository(ApplicationProvider.getApplicationContext())
        assertEquals(false, recreated.appearancePrefs.value.digitGrouping)
        assertTrue(recreated.appearancePrefs.value.expireBlinkIndicator)
        assertTrue(recreated.behaviorPrefs.value.minimizeOnCopy)
        assertEquals(setOf("Work/DevOps"), recreated.appearancePrefs.value.hiddenGroups)
    }

    @Test
    fun groupManagerHidesAndRestoresGroups() {
        repository.setGroupHidden("Personal", hidden = true)
        repository.setGroupHidden("Infrastructure", hidden = true)
        assertEquals(setOf("Personal", "Infrastructure"), repository.appearancePrefs.value.hiddenGroups)

        repository.setGroupHidden("Personal", hidden = false)
        assertEquals(setOf("Infrastructure"), repository.appearancePrefs.value.hiddenGroups)
    }

    @Test
    fun corruptEnumValueFallsBackToDefault() {
        // Simulate a corrupt persisted value written directly to the store
        val app = ApplicationProvider.getApplicationContext<ShellGuardTotpApp>()
        app.getSharedPreferences("shellguard_auth_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("pref_appearance_view_mode", "NOT_A_REAL_MODE")
            .putString("pref_behavior_search_scope", "GARBAGE")
            .commit()

        val recreated = AuthRepository(app)
        assertEquals(EntryViewMode.NORMAL, recreated.appearancePrefs.value.viewMode)
        assertEquals(SearchScope.ALL, recreated.behaviorPrefs.value.searchScope)
    }
}
