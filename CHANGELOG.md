# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
- No unreleased changes yet.

## [0.0.2.1] - 2026-09-05 (Build 12) — Phase 11.5: Settings Continuity
> **Build 12 re-upload (versionCode 12, versionName unchanged)** — fixes the light-mode readability regression found during on-device testing before Play Console upload.

### Fixed
- **CRITICAL — Light Mode Typography (Ocean Mist)**: `ShellGuardTypography` baked hard-coded dark-mode text colors (`TextPearl` ≈ white, `TextMuted`, `ClawCyan`) into every headline/title/body style. Any `Text()` without an explicit color inherited white text on light backgrounds — nearly unreadable across the whole app in Ocean Mist mode. Typography is now a theme-aware factory (`shellGuardTypography(colors)`) resolving all style colors from the active `ShellGuardCustomColors` palette; dark mode rendering is pixel-identical to before.
- **Accent Swatch Border**: `SettingsControls.kt` selected-accent swatch border used 30% white (invisible in light mode); now uses `colorScheme.outline` at 60% so it reads in both modes.

### Added
- **Server & Sync Sub-screen** (Task 22c): new ☁️ hub category hosting connection status, manual **Sync Now**, **Connect → Gateway** navigation, and **Disconnect** with confirmation dialog; legacy `SettingsScreen.kt` deleted and its route removed.
- **Import & Export Sub-screen** (Task 22d): SAF-based export of an encrypted `.sgtotp.bak` backup (CreateDocument) and vault restore (OpenDocument), wired through `AuthViewModel.exportVaultBackup`/`importVaultBackup`.
- **Appearance Theme Section** (Task 22c): theme mode tiles (Dark/Light/System) extracted to `SettingsControls.kt`; v0.0.1.3 theme parity restored into the hub's Appearance section.
- **Gateway Back-Button Polish**: circular back button inset 10dp from the status-bar boundary with the border stroke drawn outside the `CircleShape` clip — no more top-arc clipping.

### Changed
- **Spotlight Tour Step 2 Re-homed**: the `tourStep == 2` cutout (`settings_connect_button`) migrated from the deleted legacy screen to `SettingsServerSyncScreen`; onboarding continuity preserved.
- **AuthViewModel Surface**: gained `currentSession`/`logout`/`tourStep`/`setGuidedTourCompleted` plus export/import backup wrappers — secret/key resolution stays ViewModel-first, never in UI.
- **Release Verification**: Tasks 22b/22c/22d verified live on device (Pixel sailfish/LineageOS); nav graph, tour cutout, export/restore roundtrip, theme persistence across cold restart. 101/101 unit tests green.

## [0.0.2.0] - 2026-09-05 (Build 10) — Milestone 2
### Added
- **Categorized Settings Hub**: `SettingsMetaScreen` presenting 7 preference categories (🎨 Appearance, ⚡ Behavior, 📦 Icon packs, 🔐 Security, ☁️ Backups, 🛠️ Import & Export, 📈 Audit log) with descriptive subtitles and Reef Modernist cards; categories shipping in later phases route to honest in-app placeholder screens.
- **Appearance Sub-screen**: View mode (Normal/Compact), issuer icons, next-code preview, expiration blink indicator, issuer/account display rules (Both/Issuer/Account), digit grouping, and Group Manager hidden-groups — all live `StateFlow`-backed.
- **Behavior Sub-screen**: Search scope (All/Local/Synced), focus search on start, minimize on copy, copy on tap, haptic feedback, multiselect groups, highlight & freeze tokens on tap.
- **Preferences Store Architecture**: Structured `AppearancePreferences` / `BehaviorPreferences` stores with corrupt-value fallback, persistence across process recreation, and 14 setters; `UserPreferencesStoreTest` (5 cases) brings the suite to 101/101.

### Changed
- **Dashboard Preference Consumption**: Digit grouping & haptic feedback drive `TotpCard` rendering and interaction; search scope & hidden groups filter dashboard streams reactively; focus-search-on-start via `FocusRequester`.
- **Settings Entry Point**: Dashboard ⚙️ now opens the categorized hub; legacy server/backup/biometric settings remain reachable from placeholder screens.

## [0.0.1.3] - 2026-09-04 (Build 9) — Hotfix
### Fixed
- **CRITICAL — Remote Sync Regression**: v0.0.1.2's delta sync classified pearls with a null/missing server `updated_at` stamp as "unchanged" (null-to-null comparison), silently skipping decryption and upsert so remote codes never synced while reporting success. Extracted `TotpRepository.classifyDeltaPearls`: a pearl is skipped only when a local mirror row exists AND the remote stamp is non-null AND equal — null stamps always sync, and devices stuck on the broken build self-heal on the next pull.
- **Synced Card Badge Wrap**: Removed the wrapping "Read-only" text pill from `TotpCard` (rendered as "Rea/d-on/ly" and inflated card height); the pink cloud icon alone now denotes synced read-only codes (full meaning preserved in `contentDescription` and the "☁️ Synced from ShellGuard" group header). Card renders at standard single-line height.

### Added
- **Delta Sync Regression Suite**: Added `DeltaSyncClassificationTest` (6 cases including the exact repro) — first direct coverage of the sync delta path, bringing total passing unit tests to 96/96.

## [0.0.1.2] - 2026-09-03 (Build 8)
### Added
- **Expandable Speed Dial FAB**: Replaced static dual action buttons with `ExpandableSpeedDialFab`, providing a spring-driven 45-degree morph from `+` to `✕`, semi-transparent dark dimming scrim, and 3 staggered animated action pills.
- **High-Throughput ML Kit Image QR Decoder**: Built `ImageQrDecoder` leveraging Google ML Kit Barcode Scanning on URI bitmap streams for instant decoding of screenshot 2FA QR codes.
- **SAF Image Gallery Picker**: Integrated Storage Access Framework gallery picker directly into the speed dial for one-tap photo QR code import.
- **One-Way Sync Protection Test Suite**: Added `OneWaySyncAndReadOnlyProtectionTest` verifying remote synced items are immutable and protected against local edit or deletion, bringing total passing unit tests to 90/90.

### Changed
- **Unified Vision Pipeline**: Consolidated gallery QR decoding across the app into the shared `ImageQrDecoder` engine.
- **Speed Dial State Coordination**: Implemented `SpeedDialState` managing expand/collapse transitions, outside touch scrim dismissals, and hardware back-press interception.
- **One-Way Sync Hardening & Delta Efficiencies**: Added `TotpItemDao.deleteByIdIfLocal` to atomically guard against local deletion of synced remote items at the database level, and added client-side delta comparison in `TotpRepository.syncRemoteVault` to skip AES-GCM decryption for unchanged items.
- **Pull-to-Refresh Remote Sync**: Integrated `refreshRemoteVault` in `TotpViewModel` and wired dashboard pull-to-refresh.
- **Test Oracle Alignment**: Updated UI test semantic finders in `LocalModeUnlockAndVaultTest` to bind to `speed_dial_fab`.

### Removed
- **Legacy Components**: Cleaned up obsolete `ScannerFab.kt`.

## [0.0.1.0] - 2026-09-02
### Added
- **Vault Security Orientation**: Implemented `VaultSecurityScreen` with zero-knowledge educational cards, interactive PIN/Password protection selector, real-time password strength meter, and biometric switch card.
- **Segregated Vault Grouping**: Vertically separated local on-device TOTP tokens ("📱 Local Vault") from cloud-synchronized tokens ("☁️ Synced from ShellGuard") on the main dashboard.
- **Hardware Key Isolation**: Extended `AndroidKeyStoreHelper` and `AuthRepository` to generate hardware-backed `AES-256-GCM` wrapper keys for PIN and Password protection modes.

### Changed
- **Spotlight Guided Tour**: Enhanced `SpotlightOverlay` with fluid `Spring` easing, density-aware +18dp radial padding, pulsing cyan halos, and stacked action controls with centered Skip button.
- **One-Way Mirror Sync**: Enforced strict read-only server sync and updated `BackupManager` to exclusively export local codes into `.sgtotp.bak`.
- **Navigation Architecture**: Integrated `VaultSecurityScreen` into `TotpNavHost` and transitioned fresh onboarding directly to `TotpEmptyState`.

### Removed
- **Legacy Components**: Removed deprecated `HatchVaultScreen.kt`.

## [0.0.0.2] - 2026-09-01
### Fixed
- **Play Console Compliance**: Upgraded `targetSdk` to API 36 (Android 16) and bumped `versionCode` to 6 for Google Play Console distribution requirements.
- **CI Pipeline**: Unconditionally register the release signing configuration in `app/build.gradle.kts` to bypass Gradle Configuration Cache misses during tag release workflows (Build 5).
- **Test Stability**: Resolved Robolectric `JobCancellationException`, `ComposeTimeoutException`, and cross-test database state pollution issues during CI headless unit tests (Build 4/5).

### Added
- **Migration Engine**: Added `BitwardenSanitizer` to parse standalone Bitwarden Authenticator and Password Manager exports.

### Changed
- **UI Architecture**: Implemented `IntakeWelcomeScreen` and `IntakeViewModel` to support new user onboarding flow and JSON import processing.

## [0.0.0.1] - 2026-08-31
### Added
- **Core Base**: Initial project setup, 16 KB page size alignment for Android 15, SQLCipher 4.6.1 integration.
