package com.clawstack.shellguard.totp.data.preferences

import com.clawstack.shellguard.totp.ui.theme.AppThemeMode
import com.clawstack.shellguard.totp.ui.theme.ThemeAccent

/**
 * Phase 11 / Task 21 — Structured user preference stores.
 *
 * Appearance governs how entries are RENDERED on the dashboard;
 * Behavior governs how the dashboard REACTS to interaction.
 * Both are immutable snapshots exposed via [androidx.lifecycle] StateFlows on
 * AuthRepository and persisted to SharedPreferences (`shellguard_auth_prefs`).
 */

enum class EntryViewMode { NORMAL, COMPACT }

enum class IssuerDisplayMode { ISSUER_AND_ACCOUNT, ISSUER_ONLY, ACCOUNT_ONLY }

enum class SearchScope { ALL, LOCAL_ONLY, REMOTE_ONLY }

data class AppearancePreferences(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val themeAccent: ThemeAccent = ThemeAccent.REEF_DEFAULT,
    val viewMode: EntryViewMode = EntryViewMode.NORMAL,
    val showIcons: Boolean = true,
    val showNextCode: Boolean = false,
    val expireBlinkIndicator: Boolean = false,
    val digitGrouping: Boolean = true,
    val issuerDisplayMode: IssuerDisplayMode = IssuerDisplayMode.ISSUER_AND_ACCOUNT,
    val hiddenGroups: Set<String> = emptySet()
)

data class BehaviorPreferences(
    val focusSearchOnStart: Boolean = false,
    val searchScope: SearchScope = SearchScope.ALL,
    val minimizeOnCopy: Boolean = false,
    val copyOnTap: Boolean = true,
    val hapticFeedback: Boolean = true,
    val multiselectGroups: Boolean = false,
    val highlightTokensOnTap: Boolean = false,
    val freezeTokensOnTap: Boolean = false
)
