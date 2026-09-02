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


