# ShellGuard TOTP — Progress

## Completed Features
- ✅ **Grouped Dashboard & Unified Architecture**: Separated local and remote items into distinct UI groups ("📱 Local Vault" vs "☁️ Synced from ShellGuard"). Enforced local-only creation constraint for all new codes. Refactored BackupManager to exclusively export/import the canonical `sgtotp.bak` format for Local items.
- ✅ **RFC 6238 TOTP Engine**: HMAC-SHA1, HMAC-SHA256, HMAC-SHA512 with custom digits (6/8) and periods (30s/60s).
- ✅ **Live Epoch Ticker**: 1-second interval Flow emitting real-time countdown seconds and progress ratio.
- ✅ **SQLCipher Encrypted Database**: AES-256 Room persistence with `TotpItemDao` and `SyncMetadataDao`.
- ✅ **Remote API Client & One-Way Mirror Sync**: Ktor HTTP client, `ShellGuardTotpClient` API integration, `AuthRepository` with thread-safe `Mutex` and `withSyncLock`, `TotpRepository` one-way sync (downstream pull & decryption, read-only remote mirroring), and `TotpSyncWorker` periodic background execution.
- ✅ **Spotlight Guided Tour**: `SpotlightOverlay` with `BlendMode.Clear` circular cutout punching, animated cyan glowing rings, touch blocking, and contextual tooltip pills guiding through setup.
- ✅ **Settings Screen Management**: Live server connection card, sync controls, local storage & offline codes dashboard filtering, biometric settings, and encrypted JSON export/restore.
- ✅ **CameraX QR Code Scanner**: Live CameraX preview with ML Kit barcode scanner and fallback URI parser.
- ✅ **1:1 ClawStack Gateway Screen**: Segmented protocol/host/port bar, file upload dropzone, and paste view.
- ✅ **Staggered Entrance & Smooth List Motion**: Subtle staggered slide-up and fade-in entrance animation on load with `Modifier.animateItem()` list placement and removal animations.
- ✅ **Swipe-to-Delete Confirmation**: Material 3 confirmation modal dialog on swipe-to-delete with clear warning, destructive action button, and cancel reset.
- ✅ **Auth Form Top Navigation**: Back arrow icon button in the top left of Gateway and Auth Login screens returning to Settings/previous screen when key is not entered.
- ✅ **Dynamic Vault Protection & Lock Screen**: Dynamic awareness of saved unlock method (4–8 digit numeric PIN vs alphanumeric Master Password), KeyStore AES-256 key generation, and AndroidX `BiometricPrompt` integration.
- ✅ **Encrypted Backup & Restore**: Sealed JSON envelope export and import with SHA-256 integrity validation.
- ✅ **Dynamic Theming & 6 Curated Accent Palettes**: Dual-mode theme system (Abyssal Dark vs Ocean Mist Light) paired with 6 bioluminescent marine accent palettes (`REEF_DEFAULT`, `CYAN_VENT`, `PURPLE_SHELL`, `EMERALD_TRENCH`, `AMBER_FLARE`, `MONOCHROME`), injected dynamically via `LocalShellGuardColors` and custom `ShellGuardCustomColors`.
- ✅ **Adaptive Launcher Icon & Android 12+ Splash Screen**: Custom vector adaptive icon layers (`ic_launcher_background.xml` and `ic_launcher_foreground.xml`) with bioluminescent shield and clam pearl, `Theme.App.Starting` via `androidx.core:core-splashscreen:1.0.1`, and `installSplashScreen()` initialization.
- ✅ **Release Hardening & Cloud Backup Rules**: ProGuard/R8 rules for SQLCipher, Ktor, OkHttp, Kotlinx Serialization, and Room entities. `backup_rules.xml` and `data_extraction_rules.xml` excluding encrypted Room databases and KeyStore preferences from cloud backups.
- ✅ **Local Mode vs Server Synced Mode Segregation**: Dedicated offline space hiding 'Synced'/'Local Only' filter chips when disconnected, automatic local-only item scoping, and server-synced secret creation when connected with seamless transition back on disconnect.
- ✅ **EncryptedDeviceVault & Full Encryption At Rest**: Android KeyStore hardware-backed AES-256-GCM master key management. Automatic SQLCipher 256-bit database key derivation ensuring all Room tables, indices, and data blocks are encrypted at rest on disk. Zero plaintext credential persistence for raw keys and session tokens.
- ✅ **Zero-Knowledge Wire & Transport Security**: End-to-end payload encryption via `ShellCryptionEngine` (HKDF-SHA256 and AES-GCM-256 authenticated encryption with strict AAD binding), SHA-256 key hashing for authentication, and secure bearer token management.
- ✅ **Automated Unit & Robolectric Tests**: 100% test pass rate across 46 test cases covering crypto, EncryptedDeviceVault, TOTP engine, Room, dynamic lock screen, backup, and complete `MainActivityLaunchTest` launch lifecycle verification.
- ✅ **16 KB Memory Page Size Support**: SQLCipher 4.6.1 integration and uncompressed JNI packaging (`useLegacyPackaging = false`) for Android 15+ compatibility.
- ✅ **Automated Cloud CI/CD Release Pipeline**: Unified `.github/workflows/release.yml` with dual triggers (`--release vX.Y.Z.N` commit flag and `v*` tag push), strict `RELEASE-vX.Y.Z.N.md` notes resolution, dual-artifact compilation & signing (`app-release.aab` + `app-release.apk`), and automated GitHub Release publishing.
- ✅ **First-Run Brand Hero Welcome & "Import Habitat" Engine (Task 14)**: Welcoming brand hero onboarding screen (`IntakeWelcomeScreen`) with bioluminescent glowing shield aura, SAF OpenDocument file picker launcher, multi-vault pre-validation engine (`MultiVaultBackupPreValidator`) supporting ShellGuard Encrypted Habitats, Bitwarden Vaults (with zero-knowledge in-RAM password/note stripping), Bitwarden Authenticator, Aegis, and 2FAS, encrypted habitat password unlock modal bottom sheet, token summary confirmation badge, and smooth breathing pulse forward navigation.
- ✅ **Vault Security Orientation & Hardware KeyStore Isolation (Tasks 17 & 18)**: Educational zero-knowledge orientation screen (`VaultSecurityScreen`), interactive PIN/Password protection selector with a live 4-tier password entropy meter, biometric toggle card, dedicated Android KeyStore AES-256-GCM hardware key generators (`KEY_ALIAS_PIN_WRAPPER`, `KEY_ALIAS_PASSWORD_WRAPPER`), and smooth landing onto the pristine empty vault dashboard (`TotpEmptyState`).
- ✅ **Physics-Based Spring Spotlight Tour (Tasks 17 & 18)**: Upgraded `SpotlightOverlay` with `Spring.DampingRatioMediumBouncy` animated geometry, density-aware +18dp radial breathing offset, pulsing cyan halos, and stacked action controls featuring a centered `[ Skip Tutorial ]` button.
- ✅ **Headless CI & Test Suite Hardening**: Fixed test regressions and KeyStore headless fallback in `AndroidKeyStoreHelper`, wired `FrameworkSQLiteOpenHelperFactory` into Robolectric test runs, added container flags `-XX:-UsePerfData`, and verified 100% green pass across all 79 unit tests.
- ✅ **Milestone 1 / Release v0.0.1.0 (Build 7)**: Tagged, verified, signed, and published to GitHub Releases.
- ✅ **Phase 10: Expandable Floating Actions Speed Dial (QR, Image & Manual) [v0.0.1.2 (Build 8)]**:
  - `ImageQrDecoder.kt`: Shared high-throughput ML Kit barcode decoding engine on URI bitmap streams.
  - `SpeedDialState.kt`: Expand/collapse state machine, back-handler interception, outside-touch scrim dismissal.
  - `ExpandableSpeedDialFab.kt`: Fluid 45° morph (+ to ✕), dark alpha dimming scrim, and 3 staggered elevated action pills (CameraX live scan, SAF gallery image decode, manual Base32 entry).
  - One-Way Sync Hardening: `TotpItemDao.deleteByIdIfLocal` conditional delete, client-side delta sync skipping unchanged decryption, pull-to-refresh integration.
  - 90/90 unit and Robolectric tests passing 100% green.

## What's Next
- ⏳ **Phase 11: Categorized Settings Hub & Appearance/Behavior Customization [v0.0.2.0 (Build 10) — Milestone 2]**:
  - Task 21: [Functionality] Preferences Store Architecture & Entry Formatting Engine.
  - Task 22: [UI Component] Categorized Settings Hub (`SettingsMetaScreen`), Appearance & Behavior Sub-screens.
- ⏳ **Phase 12: Security Suite, Panic Purge & Security Audit Logging [v0.0.2.1 (Build 11)]**
- ⏳ **Phase 13: Advanced Import/Export, Bitwarden Migration & Google Authenticator Multi-QR [v0.0.3.0 (Build 12)]**
- ⏳ **Phase 14: Home Screen Interactive Glance Widgets & Icon Pack Manager [v0.1.0.0 (Build 13)]**
- ⏳ **Phase 15: Sovereign ClawKey (`hu-`) Vault Creation & Import Integration [v0.1.1.0 (Build 14)]**
