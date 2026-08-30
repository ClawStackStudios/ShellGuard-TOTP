# Changelog

All notable changes to the ShellGuard TOTP project will be documented in this file.

## [1.9.1] - 2026-08-30
### Fixed
- **Android KeyStore Caller-Provided IV Exception in EncryptedDeviceVault**:
  - Resolved `java.security.InvalidAlgorithmParameterException: Caller-provided IV not permitted` on Android KeyStore AES-256-GCM cipher initialization during `EncryptedDeviceVault.encrypt`.
  - Refactored `EncryptedDeviceVault.encrypt` to allow the Android KeyStore provider to generate the cryptographically random 12-byte IV during `cipher.init(Cipher.ENCRYPT_MODE, key)`, retrieving `cipher.iv` directly for the sealed envelope (`IV:CIPHERTEXT`).
  - Replaced inline `<aapt:attr>` gradient in splash theme icon with clean static vector `@drawable/ic_splash_icon.xml`, resolving inflation exceptions on API levels <31.
  - Safeguarded `EncryptedDeviceVault.getOrCreateDatabasePassphrase` against encryption runtime failures with graceful in-memory fallback.
  - Streamlined SQLCipher native library loading in `ShellGuardTotpApp.initializeSecurityFoundation`.
### Added
- **`MainActivityLaunchTest` and `EncryptedDeviceVaultTest` Test Extensions**:
  - Comprehensive Robolectric tests covering `MainActivity` cold launch, scenario lifecycles, background/foreground transitions, corrupted preference recovery, splash screen vector inflation, and Android KeyStore IV generation round-trips (100% test pass across 43 tests).

## [1.9.0] - 2026-08-30
### Added
- **Dynamic Theme Engine & Curated Accent Palettes**:
  - Implemented `ThemeAccent` enum featuring 6 marine-inspired color palettes: `REEF_DEFAULT` (Lobster Red & Claw Cyan), `CYAN_VENT` (Bioluminescent Cyan & Emerald), `PURPLE_SHELL` (Abyssal Purple & Rose), `EMERALD_TRENCH` (Emerald & Mint), `AMBER_FLARE` (Coral Amber & Gold), and `MONOCHROME` (Silver Pearl & Slate).
  - Created `ShellGuardCustomColors` and `LocalShellGuardColors` dynamic composition local provider with safe fallback tokens.
  - Refactored `Color.kt` to introduce canonical brand constants, dual-mode tokens (Abyssal Dark vs Ocean Mist Light), and accessible semantic naming.
  - Updated `TotpCard`, `TotpCountdownRing`, and `SwipeableTotpCard` to consume dynamic theme tokens for background surfaces, subtle carapace borders, warning thresholds, and danger states.
  - Added interactive Accent Palette selector with dual-row gradient swatches in `SettingsScreen`.
  - Added persistent preference handling in `AuthRepository` with full unit test coverage in `AuthVaultModeRepositoryTest`.

## [1.8.0] - 2026-08-30
### Added
- **Hardware-Backed Room Encryption At Rest (`EncryptedDeviceVault`)**:
  - Created `EncryptedDeviceVault` module utilizing Android KeyStore AES-256-GCM hardware key isolation for cryptographic key management.
  - Automatically derives and securely persists a high-entropy 256-bit database passphrase using Android KeyStore hardware keys for SQLCipher `SupportOpenHelperFactory`.
  - Encrypted all local Room database entries at rest on disk (`shellguard_totp_encrypted.db`).
  - Sensitive session credentials (`saved_raw_key`, `saved_token`, `pref_vault_secret_hash`) are fully encrypted at rest inside `EncryptedDeviceVault` rather than plain SharedPreferences, with automatic transparent migration.
- **Strict Transport & Payload Zero-Knowledge Encryption**:
  - Upstream secret creation and sync payloads are encrypted end-to-end with `ShellCryptionEngine` using HKDF-SHA256 and AES-GCM-256 with strict AAD binding before transmission over the wire.
  - Remote authentication uses SHA-256 key hashing (`ClawCrypto.hashHumanKey`), never exposing raw human keys to the server over the wire.
  - Added unit test suite `EncryptedDeviceVaultTest.kt` verifying encryption round-trips, IND-CPA IV randomness, and secure storage persistence.

## [1.7.0] - 2026-08-30
### Added
- **Dynamic Local Mode vs Server Connected Mode Segregation**:
  - **Category / Pod Filter Chips Refinement**:
    - When running in Local Standalone Mode (no active server connection in Settings), the `☁️ Synced` and `📱 Local Only` fixed filter chips are omitted from the UI. Only `All Accounts` and dynamic custom user categories are displayed.
    - When a server connection is active, `☁️ Synced` and `📱 Local Only` chips become visible, enabling quick filtering between remote-synced accounts and local device tokens.
  - **Local vs Remote Scoped Secret Creation**:
    - In Local Mode, adding a new 2FA token (manual entry or QR code scan) stores the item locally with `isLocalOnly = true` and `ownerUuid = 'local'`.
    - In Server Connected Mode, added tokens are automatically tagged with the user's session UUID and `isLocalOnly = false`, queueing them for upstream bidirectional delta sync.
    - Disconnecting from the server seamlessly transitions the app back to pure Local Mode, re-enabling local-only additions and resetting sync-specific filters.
  - **Context-Aware Visual Cues & Status**:
    - `TotpListScreen` displays `🔒 Local Mode` when disconnected, switching dynamically to `🟢 Synced` or `🟡 Offline Cache` when connected to a server.
    - `AddSecretScreen` presents a subtle mode banner informing the user whether the token will remain purely local or sync to their configured server.
### Changed
- **Blank Vault on Hatching / Creation**:
  - Removed automatic seeding of mock authentication accounts (`GitHub`, `ClawChives Vault`, `Google Workspace`) from `MainActivity.kt` and `TotpViewModel.kt`.
  - Newly created or hatched vaults start with an empty database, presenting the `TotpEmptyState` view prompting the user to scan a 2FA QR code or add Base32 keys manually.

## [1.6.0] - 2026-08-30
### Added
- **Vector Adaptive Launcher Icon**:
  - `ic_launcher_background.xml`: Deep Abyssal `#030712` canvas vector drawable.
  - `ic_launcher_foreground.xml`: Bioluminescent ShellGuard shield with `#E4048A` → `#EC4899` → `#06B6D4` linear gradient, pearl core, and specular highlight centered within the 72dp safe zone.
  - `ic_launcher.xml` and `ic_launcher_round.xml` in `res/mipmap-anydpi-v26/` referencing the adaptive background and foreground layers.
- **Android 12+ Splash Screen Integration**:
  - Integrated `androidx.core:core-splashscreen:1.0.1`.
  - Configured `Theme.App.Starting` with `#030712` background and animated icon drawable in `res/values/themes.xml`.
  - Initialized `installSplashScreen()` in `MainActivity.kt` before `super.onCreate(savedInstanceState)`.
  - Updated `AndroidManifest.xml` application and activity theme attributes to `Theme.App.Starting`.
- **ProGuard / R8 Hardening & Release Rules**:
  - Configured `proguard-rules.pro` with keep rules for SQLCipher, Ktor OkHttp engine, Kotlinx Serialization, Room database entities and DAOs, AndroidX Biometrics, and Security Crypto.
- **Cloud Backup Exclusion Rules**:
  - Configured `res/xml/backup_rules.xml` and `res/xml/data_extraction_rules.xml` to exclude encrypted Room databases (`shellguard_totp.db*`) and KeyStore preferences from unencrypted Android cloud backup while enabling direct device-to-device encrypted migration.

## [1.5.0] - 2026-08-30
### Added
- **Bidirectional Delta Sync**: Implemented full two-way synchronization in `TotpRepository`:
  - Upstream push for `PENDING_SYNC` items with `ShellCryptionEngine` HKDF-SHA256 key derivation and AES-GCM-256 encryption (`vault_pearls_totp:<recordId>` AAD).
  - Downstream pull of remote pearls, HKDF key derivation, and authenticated AES-GCM decryption into Room.
  - Automatic pruning of remote records deleted on the server while preserving local-only records.
- **Mutex Sync Locking**: Added `withSyncLock` in `AuthRepository` and wrapped periodic background synchronization in `TotpSyncWorker` to prevent race conditions with active UI sessions.
- **Interactive Spotlight Tour**: Created `SpotlightOverlay` with `BlendMode.Clear` circular cutout punching, animated cyan pulsating glow, full touch interception, and step tooltips guiding the user from Dashboard Settings to Server Gateway connection.
- **Local Storage & Offline Codes Management**: Added an offline codes card in `SettingsScreen` showing live counts of cached tokens and one-tap dashboard filtering.

## [1.4.0] - 2026-08-30
### Added
- **Staggered Entrance Animation**: Implemented `StaggeredAnimatedItem` for 2FA token cards on `TotpListScreen` load with subtle staggered delays, alpha fade-in, and bouncy spring slide-up physics.
- **Smooth Item Removal & Reordering**: Added `Modifier.animateItem()` with keyed items in `LazyColumn` for smooth collapse and spring relocation transitions when items are deleted or rearranged.
- **Swipe-to-Delete Confirmation**: Integrated a Material 3 confirmation `AlertDialog` when swiping 2FA account cards in the TOTP list.
  - Displays the targeted account name, an explicit warning explaining that OTP codes will no longer be generated, a destructive "Delete" action button, and a "Cancel" button.
  - Automatically resets the swiped card to its settled position on cancellation or dismissal.
- **Auth Form Back Navigation**: Added a prominent 'Back' arrow icon button (`Icons.AutoMirrored.Filled.ArrowBack`) to the top left of the Authenticator Gateway auth login screen and the master login screen.
  - Allows users to smoothly return to the Settings screen (or previous destination) if they choose not to enter a key or PIN.
  - Wired navigation stack popbacks through `TotpNavHost`.

## [1.3.0] - 2026-08-30
### Added
- Implemented `VaultProtectionMode` enum (`PIN` vs `PASSWORD`) across `AuthRepository` with SharedPreferences persistence and reactive `StateFlow`.
- Refactored `LockScreen` to be dynamically aware of the user's saved authentication method:
  - Automatically switches between numeric PIN entry (4–8 digits with `KeyboardType.NumberPassword`) and Master Password entry (`KeyboardType.Password`).
  - Dynamic visual badge and status ("PIN Protected" vs "Master Password Protected").
  - Seamless fallback between Biometric authentication and the user's specific secondary authentication method.
- Updated `HatchVaultScreen` onboarding flow to initialize vault protection with selected mode.
- Added Vault Protection Method management in `SettingsScreen` with dialog to change between PIN Code and Master Password.
- Added unit and Robolectric tests in `LockScreenDynamicUnlockTest` and `AuthVaultModeRepositoryTest` ensuring 100% test pass rate.

## [1.2.0] - 2026-08-30
### Added
- Implemented `AppThemeMode` (DARK, LIGHT, SYSTEM) with SharedPreferences persistence and reactive `StateFlow` propagation via `AuthRepository` and `AuthViewModel`.
- Defined high-contrast accessible `ShellGuardLightColorScheme` adhering to Reef Modernist Mobile design.
- Implemented "Appearance & Accessibility" theme switcher card with `ThemeOptionTile` in `SettingsScreen`.
- Refactored `TotpCard`, `PodFilterChips`, `TotpCountdownRing`, `TotpEmptyState`, `ClipboardToastPill`, and `AddSecretScreen` to strictly utilize `MaterialTheme.colorScheme` for dynamic theming across the entire application.

## [1.1.0] - 2026-08-30
### Added
- Implemented `TotpUriParser` RFC 6238 Key URI parser extracting secret, issuer, username, algorithm (SHA1, SHA256, SHA512), digits (6, 8), and period (30, 60), with raw Base32 secret fallback.
- Implemented `BackupManager` with ShellCrypted AES-GCM-256 encrypted JSON export/import (`shellguard-totp-backup-v1`) and plain unencrypted JSON portability export/import with SHA-256 integrity checksum verification.
- Implemented `QrScannerScreen` with CameraX preview, ML Kit barcode scanning, targeted scanner reticle overlay, torch/flashlight toggle, and image gallery picker (`ActivityResultContracts.GetContent()`).
- Implemented `AddSecretScreen` with real-time Base32 character validation, token parameter selection (SHA1/SHA256/SHA512, 6/8 digits, 30s/60s interval), and Room persistence.
- Implemented `ScannerFab` adhering to Section 6 of `DESIGN.md`.
- Added unit and Robolectric tests in `TotpUriParserTest`, `BackupManagerTest`, and `Phase4ScreensTest`.

## [1.0.0] - 2026-08-30
### Added
- Implemented `ShellGuardTotpClient` and `ApiClient` using Ktor HTTP Client with custom headers and error handling.
- Implemented `AuthRepository` with thread-safe `Mutex` session management and token caching.
- Implemented `TotpRepository` delta synchronization with ShellCryption HKDF key derivation and AES-GCM-256 decryption.
- Implemented `TotpSyncWorker` periodic background synchronization via WorkManager.
- Implemented 1:1 ClawStack `GatewayScreen` with animated segmented URL host/port input and JSON identity file dropzone.
- Implemented `SettingsScreen` with live vault connectivity, sync trigger, biometric toggle, and backup actions.
- Implemented `LockScreen` with AndroidX `BiometricPrompt` and Android KeyStore integration.
- Implemented `HatchVaultScreen` 3-step vault onboarding wizard.
- Implemented `SwipeableTotpCard` and `combinedClickable` with tap-to-copy and long-press account editing.
