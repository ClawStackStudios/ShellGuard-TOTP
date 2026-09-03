---
roadmap_version: 2.2.0
last_updated: 2026-09-01
current_position: "Phase 6 Complete (v0.0.0.2 Launch Ready) — Transitioning to Phase 7: Architectural Refactor (v0.0.1.0)"
statistics:
  description: "Deterministic build roadmap for ShellGuard-TOTP Android Authenticator application. Engineered strictly in synergistic 2-task phases where Task A delivers core functionality and Task B delivers the corresponding UI/UX component."
  features_completed: "██████████ 100%"
  features_in_progress: "░░░░░░░░░░ 0%"
---

# Master Project Roadmap — ShellGuard-TOTP Android

### 🏷️ Work-Driven Versioning Policy: `MAJOR.MINOR.PATCH.REVISION` (`X.Y.Z.N`)
- **Baseline**: `v0.0.0.2 (Build 6)` (Launch Foundation: Phases 1–6).
- **No Forced Targets**: Versions evolve naturally from the work done rather than arbitrary artificial goals.
  - **`MAJOR` (`X.0.0.0`)**: Fundamental structural overhauls or full 1.0 public launch.
  - **`MINOR` (`X.Y.0.0`)**: Substantial new subsystems or large feature groups.
  - **`PATCH` (`X.Y.Z.0`)**: Self-contained 2-task phase deliverables or targeted bug fixes (e.g. `0.0.1.0`, `0.0.2.0`).
  - **`REVISION` (`X.Y.Z.N`)**: Small patches, hotfixes, CI adjustments, or iterative task increments (e.g. `0.0.0.2`, `0.0.0.3`).
  - **`versionCode` (`Build N+1`)**: Monotonically increments (+1) on every Google Play release upload.

---

Systemic Design Rule: This roadmap follows a deterministic structure built on the principle: "Build features around security, not security around features." Each Phase contains strictly 2 paired tasks: **Task A (Core Functionality / Security Engine)** followed immediately by **Task B (Corresponding UI Component / Interactive State)**.

---

## Phase 1: Cryptographic Engine & Live Countdown Display [Baseline: v0.0.0.1 (Build 3)]

> Phase Feature Set Overview:
> Delivers mathematical parity with ShellGuard's ShellCryption (HKDF-SHA256 + AES-GCM-256 with AAD binding) and a thread-safe RFC 6238 TOTP computation engine (HMAC-SHA1/256/512, 6/8 digits), paired immediately with the reactive 1-second ticker, color-transitioning progress ring, and unit test suites.

- [x]  **Task 01: [Functionality] Thread-Safe RFC 6238 TOTP Engine, ShellCryption HKDF & Comprehensive Unit Tests**

Description: Create the core TOTP generation logic in `com.clawstack.shellguard.totp.engine` that implements RFC 6238, using the Kotlin Time library (`kotlin.time.Duration`, `kotlin.time.TimeSource`) for counter synchronization. Implement the standard TOTP algorithm using stored secret keys from the Room database. The engine generates current 6-digit or 8-digit codes based on system time, supporting SHA-1, SHA-256, and SHA-512 hashing algorithms with 30s/60s time steps in a thread-safe, zero-allocation manner in hot loops. Implement `ShellCryptionEngine` providing HKDF-SHA256 key derivation (`ikm = huKey`, `salt = userUuid`, `info = "clawchives-shellcryption-v1"`) and AES-GCM-256 with strict AAD binding (`vault_pearls_totp:{id}`). Create `TotpEngineTest.kt` and `ShellCryptionEngineTest.kt` in `test/` verifying RFC 6238 vectors and AAD tamper detection.

> Success Criteria: Unit tests prove 100% cryptographic compatibility with web client envelopes. `TotpEngine` passes all RFC 6238 reference test vectors across SHA-1, SHA-256, and SHA-512 with thread safety.

- [x]  **Task 02: [UI Component] Dynamic Reef Modernist Theme Engine, Reactive Ticker & Animated Progress Ring**

Description: Implement `ui/theme/Color.kt`, `Theme.kt`, and `Type.kt` matching the Reef Modernist design system (`DarkBgBase` `#0F1419`, `DarkBgSurface` `#171C21`, `DarkTextMain` `#DEE3EA`, `DarkBorderSubtle` `#3D484E`). Implement `ThemeAccent` enum with 6 curated palettes (`REEF_DEFAULT`, `CYAN_VENT`, `PURPLE_SHELL`, `EMERALD_TRENCH`, `AMBER_FLARE`, `MONOCHROME`) and `LocalShellGuardColors` `staticCompositionLocalOf` for dynamic Compose color inheritance. Build `TotpTicker` reactive Flow (emitting 1-second ticks synchronized with Kotlin Time) and construct the `TotpCountdownRing` Canvas component. The ring visually depletes counter-clockwise over the 30s period, smoothly animating colors from secondary accent (30s–11s) → `CoralOrange` (10s–6s) → `LobsterRed` (5s–0s). Build the `TotpCard` composable displaying formatted split digits (`123 456`) with spring bounce interactions.

> Success Criteria: Composable renders a smooth 60fps countdown ring with animated color transitions. Numbers and rings update synchronously on 30s interval boundaries without frame drops or memory allocations. Theme supports dynamic Compose inheritance across dark/light and custom accents.

---

## Phase 2: Encrypted Local Persistence & Authenticator Dashboard [Baseline: v0.0.0.1 (Build 3)]

> Phase Feature Set Overview:
> Establishes the offline-first data layer using AndroidX Room with SQLCipher whole-database encryption initialized in the Application class, paired immediately with the complete Authenticator list screen, Material 3 Empty State view, live search bar, Pod category filter chips, and gesture controls (Swipe-to-delete, Long-press edit).

- [x]  **Task 03: [Functionality] AndroidX Room with SQLCipher Secure Persistence & Cache DAO**

Description: Set up AndroidX Room with SQLCipher (`net.zetetic:sqlcipher-android`) for secure whole-database encrypted persistence, initialized properly within the Application class. Define `TotpItemEntity` for storing TOTP secrets (account name, username, secret key, issuer, algorithm, period, digits, category, `is_local_only`, `sync_state`), `SyncMetadataEntity`, and `AppConfig`. Implement `TotpItemDao` for CRUD, reactive Flow queries (`observeAllTotpItems`), keyword search, `getPendingSyncItems`, and delta pruning of obsolete remote items while strictly protecting local-only entries (`is_local_only = 1`). Implement `ShellGuardTotpDatabase` with SQLCipher `SupportFactory`.

> Success Criteria: Room database opens successfully with SQLCipher passphrase derived from KeyStore. CRUD queries execute on IO dispatchers. Database survives app process restarts and serves cached tokens offline.

- [x]  **Task 04: [UI Component] Reef Modernist Authenticator List Screen, Empty State & Gestures**

Description: Develop a Material 3 'Empty State' screen (`TotpEmptyState.kt`) for the main dashboard featuring the minimalist 3D locked shell illustration (`R.drawable.ic_locked_shell`) that prompts the user to add their first 2FA code if the database is empty. Implement `TotpListScreen` with search bar, horizontal scrollable Pod category chips (`PodFilterChips.kt`), card list, settings navigation action, and one-tap clipboard copy with haptic feedback (`HapticFeedbackType.LongPress`) and auto-clearing toast. Wrap cards in `SwipeToDismissBox` (swipe-to-delete with red trash bin action) and tap-and-hold (long-press) to edit.

> Success Criteria: Screen displays cached 2FA accounts streamed from Room DB, and renders `TotpEmptyState` when 0 items exist. Search queries filter items instantly. Swipe-to-delete removes items from Room DB, and long-press triggers edit navigation.

---

## Phase 3: Vault Onboarding & Biometric Security Lifecycle [Baseline: v0.0.0.1 (Build 3)]

> Phase Feature Set Overview:
> Delivers the hardware-backed Android KeyStore biometric cryptographic binding, app-level `FLAG_SECURE` window protection, and inactivity auto-lock observer, paired immediately with the 3-step "Hatch New Vault" first-run onboarding wizard and the biometric `LockScreen` / `LoginScreen`.

- [x]  **Task 05: [Functionality] Hardware KeyStore Biometric Sealing, FLAG_SECURE & Inactivity Auto-Lock**

Description: Implement `AndroidKeyStoreHelper` generating hardware-backed AES-256-GCM secret keys with `setUserAuthenticationRequired(true)` and `setInvalidatedByBiometricEnrollment(true)`. Implement `ClawCrypto` for `hu-`/`lb-` key SHA-256 hashing. Apply `FLAG_SECURE` to `MainActivity.window` to prevent screenshot leaks and task-switcher recents thumbnail snooping. Implement `AppLifecycleObserver` (using `ProcessLifecycleOwner`) to track app backgrounding and enforce configurable auto-lock timeouts (e.g. immediately or 1 minute after backgrounding).

> Success Criteria: Hardware KeyStore key initializes securely. Android OS blocks screenshots and renders blank recents previews in the app switcher. Leaving the app and returning after timeout triggers the lock state.

- [x]  **Task 06: [UI Component] "Hatch New Vault" Onboarding Wizard & Biometric LockScreen**

Description: Implement `HatchVaultScreen.kt` (**"Hatch New Vault" Initial Launch Onboarding Wizard**): Step 1 (Welcome & Mode Selector: 4–8 digit PIN vs Master Password), Step 2 (Protection setup with confirmation match + skippable Biometric Switch Card), Step 3 (Success orientation). Implement `LockScreen.kt` / `LoginScreen.kt` integrating `androidx.biometric.BiometricPrompt` with `AndroidKeyStoreHelper.getBiometricCipher()` crypto-object wrapping, handling success, error, and PIN/Master Key fallback. Update `TotpNavHost.kt` with dynamic `startDestination` routing (`HatchVault` vs `LockScreen` vs `CodeList`).

> Success Criteria: Fresh installs launch into the Hatch Vault wizard to set a 4–8 digit PIN. Biometrics can be skipped or enabled. Once hatched, cold launches display the LockScreen with biometric prompt before revealing codes.

---

## Phase 4: CameraX QR Scanner & Multi-Format Backup Engine [Baseline: v0.0.0.1 (Build 3)]

> Phase Feature Set Overview:
> Delivers URI parsing for standard `otpauth://totp/...` URIs, Google Authenticator migration payloads, and the encrypted JSON backup/restore engine, paired immediately with the live CameraX preview scanner, scan from gallery image picker, and manual secret entry form.

- [x]  **Task 07: [Functionality] TotpUriParser, Migration Payloads & BackupManager Engine**

Description: Implement `TotpUriParser` parsing `otpauth://totp/...` URIs (extracting secret, issuer, label, username, algorithm, period, digits) and raw Base32 strings. Implement `BackupManager` for encrypted JSON backup export and import (`shellguard-totp-backup-v1`) with SHA-256 integrity checksums and `ShellCryptionEngine` validation. Support standard unencrypted backup export/import for cross-app interoperability (e.g. Aegis / Bitwarden JSON).

> Success Criteria: URI parser extracts all 2FA parameters accurately. `BackupManager` exports and imports encrypted backups, failing cleanly if a file is tampered with or corrupted.

- [x]  **Task 08: [UI Component] CameraX Live Scanner, Gallery QR Picker & Manual Entry Screen**

Description: Implement `QrScannerScreen.kt` using CameraX (`1.5.0`) and ML Kit Barcode Scanning (`17.3.0`) with a composable camera preview, targeted scanner reticle, flashlight toggle, and a "Scan from Image / Gallery" file picker fallback. Implement `AddSecretScreen.kt` for manual secret entry (title, username, Base32 key, category, algorithm, period, digits).

> Success Criteria: Camera scanner detects and parses 2FA QR codes within 500ms and pre-fills account creation. Gallery picker allows scanning QR codes from saved screenshots. Manual entry screen validates Base32 secrets and saves to Room DB.

---

## Phase 5: Self-Hosted Server Gateway & Bidirectional Delta Sync [Baseline: v0.0.0.1 (Build 3)]

> Phase Feature Set Overview:
> Delivers Ktor HTTP/HTTPS client with local LAN/VPN cleartext transport, two-way bidirectional sync (`POST /api/vault` upstream push + `GET /api/vault` downstream pull), and WorkManager background sync, paired immediately with the interactive Spotlight guided tour, ClawStack Gateway screen, and Settings screen.

- [x]  **Task 09: [Functionality] Two-Way Bidirectional Sync, Ktor OkHttp Client & WorkManager**

Description: Configure Android Cleartext & VPN Network Security (`network_security_config.xml` & `AndroidManifest.xml`) permitting cleartext HTTP on local LANs (`192.168.x.x`, `10.x.x.x`) and Tailscale / WireGuard VPN mesh IPs (`100.64.0.0/10`). Implement `ShellGuardTotpClient` and `ApiClient` (Ktor OkHttp with `ConnectionSpec.CLEARTEXT` and `ConnectionSpec.MODERN_TLS`, self-signed cert trust, and VPN routing). Implement `AuthRepository` with `Mutex` synchronization lock. Implement `TotpRepository.syncRemoteVault()` supporting two-way bidirectional sync: pushes local/pending items to `POST /api/vault` as `password` items with encrypted `totp_secret`, and pulls remote pearls to merge into Room DB. Register `TotpSyncWorker` using AndroidX WorkManager for 15-minute periodic background delta sync.

> Success Criteria: App connects over HTTP, HTTPS, LAN, and Tailscale VPN without cleartext errors. Local 2FA codes push to the server and appear in the ShellGuard Web UI with live countdown rings. Remote vault items sync down to Android. WorkManager executes background sync cleanly.

- [x]  **Task 10: [UI Component] Interactive Spotlight Guided Tour, Server Gateway & Settings with Theme Picker**

Description: Implement `SpotlightOverlay.kt` (**Interactive Spotlight Guided Tour**): dims and blurs background, punches circular cutout over topbar Settings icon with tooltip pill and centered `[ Skip Tutorial ]` button, transitioning to spotlight the "Connect to Server" button inside Settings. Implement `GatewayScreen.kt` & `GatewayViewModel.kt` (faithful 1:1 port of ClawStack Gateway: protocol/host/port segment bar, animated port width, key file dropzone + paste ShellKey view, and warning card). Implement `SettingsScreen.kt` (Appearance & Theme Accents Card with 6 curated palette swatches + mode toggles, server sync status card, "Sync Now", "Disconnect", offline codes count & filter, encrypted backup pickers, and biometric toggle).

> Success Criteria: First launch after Hatch Vault presents the Spotlight overlay guiding the user to Settings. Gateway connects and authenticates with the server. Settings screen allows selecting theme accent palettes with live preview, manual sync, disconnecting, and toggling biometrics.

---

## Phase 6: Final Polish — Adaptive App Icon, Splash Screen & Release Hardening

> Phase Feature Set Overview:
> Delivers ProGuard/R8 release minification rules, backup exclusions, release signing configurations, and semantic versioning, paired immediately with the vector adaptive launcher icon, Android 12+ SplashScreen theme, and edge-to-edge system bar styling.

- [x]  **Task 11: [Functionality] ProGuard/R8 Hardening, Backup Rules & Release Configuration**

Description: Create `proguard-rules.pro` with keep rules for SQLCipher, Ktor OkHttp, Kotlinx Serialization, and Room entities to prevent release build obfuscation crashes. Configure `res/xml/backup_rules.xml` and `data_extraction_rules.xml` to exclude encrypted Room databases and KeyStore preferences from unencrypted Android Cloud backup. Set release signing and versioning in `build.gradle.kts`.

> Success Criteria: Release APK compiles with R8 minification enabled without runtime reflection crashes or serialization issues. Cloud backups do not leak encrypted database files.

- [x]  **Task 12: [UI Component] Adaptive Launcher Icon, Android 12+ Splash Screen & Edge-to-Edge Polish**

Description: Create `ic_launcher_background.xml` (solid `#030712`) and `ic_launcher_foreground.xml` (ShellGuard shield vector with `#E4048A` → `#EC4899` → `#06B6D4` gradient and clam pearl within 72dp safe zone). Add `androidx.core:core-splashscreen:1.0.1` and configure `Theme.App.Starting` in styles/themes. Call `installSplashScreen()` in `MainActivity.kt`. Apply edge-to-edge transparent system navigation and status bar styling.

> Success Criteria: App icon renders crisply on Android launchers across all adaptive icon masks (circle, squircle, rounded square). App launches with smooth Android 12+ splash animation and edge-to-edge dark theme.

---

------------------ Next Horizon (Post-Launch Expansion) ------------------


## Phase 7: Architectural Refactor — One-Way Mirror Sync, Grouped Dashboard & Unified Export [v0.0.1.0 (Build 7) — Milestone 1]

> Phase Feature Set Overview:
> Shifts the application from two-way bidirectional sync to a strict One-Way Mirror sync pattern. Codes added on Android remain exclusively local. The dashboard UI is overhauled to present Grouped Sections (Local Codes vs Remote Codes) eliminating cognitive load from the old Snackbar filter. Finally, imports/exports are simplified to strictly handle Local items using a unified `sgtotp.bak` format shared with the ShellGuard Web Server.

- [ ]  Task 13: [Functionality] One-Way Sync Engine, Grouped Repository & Unified Export Schema
Description: Overhaul `TotpRepository` to only pull down remote codes as read-only mirror items. Remove upstream pushes completely. Modify `TotpItemDao` and `TotpViewModel` to expose explicitly grouped streams (Local vs Synced). Revamp `BackupManager` to only export local codes, adopting the canonical `sgtotp.bak` backup schema that aligns perfectly with the ShellGuard web server import pipeline. Add a `compatibility_layer.md` doc to the ShellGuard repo to detail this integration.

> Success Criteria: Application pulls remote items correctly without ever pushing. ViewModels group the codes natively. Exports produce valid `sgtotp.bak` JSON structures representing only local elements.

- [ ]  Task 14: [UI Component] Grouped Authenticator Dashboard & Simplified Import Flow
Description: Refactor `TotpListScreen.kt` to present a unified vertically-scrollable list with clear sticky-headers/dividers grouping "📱 Local Vault" at the top and "☁️ Synced from ShellGuard" below. Remove the old connection Snackbar and top-bar filter chips for local/synced. Update the Add Secret and QR Scanner flows to strictly save to local vault.

> Success Criteria: Dashboard renders two distinct grouped sections clearly. Creating new items automatically appends them to the Local Vault group.

---
## Phase 8: Welcoming First-Run Wizard & "Import Habitat" Intake Flow [v0.0.0.2 (Build 4)]

> Phase Feature Set Overview:
> Delivers a completely reimagined, welcoming user-first application intake flow. Features the official ShellGuard launcher shield hero branding, immediate "Import Habitat" one-tap backup restoration supporting ShellGuard, Bitwarden, and Aegis JSON backups via Android Storage Access Framework (SAF), and seamless forward navigation to vault protection.

- [x]  Task 15: [Functionality] First-Run Intake Engine, Multi-Vault Backup Pre-Validator & Dynamic Route State

Description: Implement the first-run intake orchestrator in `com.clawstack.shellguard.totp.ui.onboarding`. Provide reactive state management for onboarding flow progression (`INTAKE_WELCOME`, `IMPORTING_HABITAT`, `SECURITY_SETUP`, `COMPLETED`). Integrate Android Storage Access Framework (SAF) `OpenDocument` file parser to inspect and pre-validate imported `.json` / `.shellguard` backup envelopes before vault database creation. Automatically detect schema type (ShellGuard Habitat `shellguard-totp-backup-v1`, Bitwarden Vault `items[].login.totp`, Bitwarden Authenticator, Aegis, 2FAS) and prepare the unlock cipher / sanitizer state for password/PIN input.

> Success Criteria: Intake engine pre-validates selected backup files within 100ms, detects corrupted or unsupported files with clear user feedback, and sets up dynamic navigation routes without corrupting local database state.

- [x]  Task 16: [UI Component] Brand Hero Welcome Screen & "Import Habitat" File Picker Launcher

Description: Implement `IntakeWelcomeScreen.kt` (**First-Run Brand Hero Screen**):
- **Hero Header**: Displays the high-resolution vector ShellGuard launcher shield (`ic_launcher_foreground`) with glowing ambient backdrop.
- **App Introduction**: Clean typography introducing ShellGuard-TOTP (*"Your zero-knowledge privacy fortress for two-factor authentication"*).
- **Import Habitat Button**: Prominent primary button `[ 📥 Import Habitat / Vault ]` with file icon that launches the system file picker (`rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument())`) supporting ShellGuard backups, Bitwarden JSON exports, and Aegis files.
- **Decryption / Migration Prompt Sheet**: Modal bottom sheet prompting for Master Password/PIN for encrypted habitats, or displaying token count summary for Bitwarden imports with passwords/notes stripped.
- **Fresh Vault Forward Navigation**: An elegant floating arrow icon button (`Icons.AutoMirrored.Filled.ArrowForward`) in the bottom right corner with a subtle breathing pulse that transitions fresh users to the Vault Security screen.

> Success Criteria: First launch presents a polished, welcoming brand hero screen. Tapping "Import Habitat" launches the native file picker, parses valid backups/Bitwarden exports, and moves directly to password unlock or import preview. Tapping the forward arrow navigates smoothly to Vault Security.


---

## Phase 9: Vault Security Education, Enlarged Spotlight Tour & Empty Vault Landing [v0.0.1.1 (Build 8) — Milestone 1]

> Phase Feature Set Overview:
> Delivers the educational Vault Security configuration screen explaining ShellGuard's zero-knowledge protection options, paired with an enhanced Spotlight Tour featuring enlarged, breathable circular cutouts that comfortably frame UI targets without crowding.

- [x]  Task 17: [Functionality] Android KeyStore Protection Orchestrator & Spotlight Geometry Engine

Description: Refactor `AndroidKeyStoreHelper` and `AuthRepository` to support unified protection configuration (PIN vs Password vs Biometrics) with instant hardware key generation. Update `SpotlightOverlay.kt` geometry calculation engine to calculate enlarged, comfortable circular cutouts with configurable padding (+16dp to +20dp radial offset beyond target bounds) and smooth spring easing.

> Success Criteria: Protection orchestrator binds selected PIN/Password and Biometrics into Android KeyStore AES-256-GCM hardware keys. Spotlight cutout geometry computes comfortable, non-cramped bounding circles with 60fps canvas clipping.

- [x]  Task 18: [UI Component] Vault Security Orientation Screen & Enhanced Spotlight Overlay

Description: Implement `VaultSecurityScreen.kt` and update `SpotlightOverlay.kt`:
- **Vault Security Orientation**: Educational screen explaining zero-knowledge encryption, hardware KeyStore isolation, and offline autonomy.
- **Protection Selector**: Interactive radio cards for `[ 🔢 PIN Code (4–8 digits) ]` vs `[ 🔑 Master Password ]` with real-time strength meter and confirmation input.
- **Biometric Switch Card**: Sleek switch card to enable fingerprint/face unlock.
- **Enlarged Spotlight Guided Tour**: Enhanced `SpotlightOverlay` with enlarged spotlight radii, glowing cyan halo borders, breathable tooltips (*"To add a new server to sync to, open the settings menu"*), and centered `[ Skip Tutorial ]` button.
- **Empty Vault Landing**: Seamless transition landing the user directly into the empty local vault dashboard (`TotpEmptyState`).

> Success Criteria: Users configure security with full educational context. Spotlight tour highlights Settings and Gateway buttons with spacious, easy-to-track cutouts, ending on the pristine local vault.

---

## Phase 10: Expandable Floating Actions Speed Dial (QR, Image & Manual) [v0.0.1.2 (Build 9)]

> Phase Feature Set Overview:
> Delivers an expandable speed dial Floating Action Button (FAB) on the main vault dashboard, providing one-tap access to CameraX live scanning, Gallery image QR decoding, and manual Base32 secret entry with animated icon pills and scrim dismissal.

- [x]  Task 19: [Functionality] Image QR Decoder Pipeline & Expandable FAB Interaction Controller

Description: Implement `ImageQrDecoder` using Google ML Kit Barcode Scanning on URI bitmap streams. Implement `SpeedDialState` controller managing expand/collapse transitions, back-handler interception, outside touch scrim dismissals, and permission requests.

> Success Criteria: ML Kit barcode scanning decodes QR codes from high-res bitmap files in <200ms. SpeedDial controller coordinates animated expansion states without UI jank.

- [x]  Task 20: [UI Component] Animated Speed Dial FAB & Elevated Action Pills

Description: Implement `ExpandableSpeedDialFab.kt` on `TotpListScreen.kt`:
- **Main FAB**: Smooth 45-degree rotation morphing from `+` to `✕`.
- **Background Scrim**: Subtle dark alpha dimming dismissible by tapping anywhere outside.
- **Elevated Action Pills** (staggered slide-and-fade entrance from bottom to top):
  1. `[ 📷 Scan QR code ]` (Navigates to live CameraX preview scanner)
  2. `[ 🖼️ Scan image ]` (Launches SAF image gallery picker to decode screenshot QR)
  3. `[ ✏️ Enter manually ]` (Navigates to manual secret entry form)

> Success Criteria: FAB expands with smooth 60fps spring animations, presents clear pill buttons with high contrast, and navigates seamlessly to each intake modality.

---

## Phase 11: Categorized Settings Hub & Appearance/Behavior Customization [v0.0.2.0 (Build 10) — Milestone 2]

> Phase Feature Set Overview:
> Transforms the monolithic settings page into a modular multi-tier Settings Hub with dedicated sub-screens for Appearance styling (theme modes, dynamic colors, digit grouping, entry view modes) and Application Behavior (search focus, minimize on copy, token tap highlights, freeze on tap).

- [ ]  Task 21: [Functionality] Preferences Store Architecture & Entry Formatting Engine

Description: Expand `AuthRepository` and `DataStore` / `SharedPreferences` to manage structured preferences for `AppearancePreferences` (view mode, show icons, show next code, expire blink indicator, digit grouping, issuer/account display rules) and `BehaviorPreferences` (search focus on start, search scope, minimize on copy, haptic feedback, multiselect categories, highlight tokens on tap, freeze tokens on tap).

> Success Criteria: All user customization preferences persist reliably across app recreation and update reactive `StateFlow` streams synchronously.

- [ ]  Task 22: [UI Component] Categorized Settings Hub (`SettingsMetaScreen`), Appearance & Behavior Sub-screens

Description: Implement modular Settings navigation:
- **`SettingsMetaScreen.kt`**: Master category list with icons and descriptive subtitles:
  - 🎨 **Appearance** (`Adjust theme, language, and other appearance settings`)
  - ⚡ **Behavior** (`Customize behavior when interacting with entry list`)
  - 📦 **Icon packs** (`Manage and import icon packs`)
  - 🔐 **Security** (`Configure encryption, biometric unlock, auto lock`)
  - ☁️ **Backups** (`Automatic backups & Android cloud backup system`)
  - 🛠️ **Import & Export** (`Import from Aegis/Bitwarden/Google, export vault`)
  - 📈 **Audit log** (`Security event audit trail`)
- **`SettingsAppearanceScreen.kt`**: Theme mode, Dynamic Colors toggle, Language, View mode (Normal/Compact), Show icons toggle, Show next code toggle, Expiration indicator toggle, Digit grouping selector, Account name visibility rules, Group manager, Reset usage counts.
- **`SettingsBehaviorScreen.kt`**: Focus search on start, Search behavior selector, Minimize on copy toggle, Copy tokens to clipboard policy, Haptic feedback toggle, Multiselect groups toggle, Highlight tokens on tap toggle, Freeze tokens on tap toggle.

> Success Criteria: Settings menu presents clean, native Material 3 category lists with fluid sub-screen navigation and instant live preview updates.

---

## Phase 12: Security Suite, Panic Purge & Security Audit Logging [v0.0.2.1 (Build 11)]

> Phase Feature Set Overview:
> Delivers advanced vault security controls (tap-to-reveal tokens, screen security toggle, emergency panic purge trigger integration) and a local append-only security Audit Log recording important cryptographic and access events.

- [ ]  Task 23: [Functionality] Security Preference Controller, Panic Trigger Handler & Room Audit Log DAO

Description: Implement `AuditLogDao` and `AuditLogEntity` in Room to record security events (vault unlocked, biometric failed, backup created, secret added, panic triggered). Implement `PanicTriggerReceiver` (supporting broadcast/intent triggers to wipe encryption keys and purge Room DB on emergency). Implement configurable Tap-to-Reveal timeout timer (default 30s).

> Success Criteria: Audit events are safely recorded with millisecond timestamps. Panic trigger securely wipes all local encryption keys and databases within 50ms.

- [ ]  Task 24: [UI Component] Security Sub-screen (Tap-to-Reveal, Screen Security, Panic Purge) & Audit Log Screen

Description: Implement:
- **`SettingsSecurityScreen.kt`**: Encryption status tile, Screen security toggle (`FLAG_SECURE`), Tap to reveal codes toggle with configurable timeout duration, Delete vault on panic trigger toggle.
- **`SettingsAuditLogScreen.kt`**: Chronological event list with status chips (Unlock, Export, Failed Attempt, Sync), search filter, export audit log action, and empty state illustration (`No reported events`).

> Success Criteria: Security settings enforce immediate runtime protection. Audit log renders event timeline with zero UI stutter.

---

## Phase 13: Advanced Import/Export, Bitwarden Migration & Google Authenticator Multi-QR [v0.0.3.0 (Build 12) — Milestone 3]

> Phase Feature Set Overview:
> Delivers robust multi-format vault import with specialized Bitwarden vault parsing (extracting 2FA TOTP secrets while strictly stripping passwords and secure notes in-memory), dual persistence routing (Local SQLCipher vs Remote Gateway sync), and Google Authenticator multi-account migration QR export.

- [ ]  Task 25: [Functionality] Multi-Format Import Engine (Bitwarden Vault/Auth, Aegis, 2FAS) & Dual Vault Persister

Description: Implement `MultiFormatMigrationEngine` in `data/migration`:
- **Bitwarden Vault Parsing**: Parses Bitwarden Password Manager JSON exports (`items[].login.totp` containing `otpauth://totp/...` or raw Base32 seeds, mapping `folders[]` ➔ ShellGuard Pod categories).
- **Bitwarden Authenticator Parsing**: Parses standalone Bitwarden Authenticator exports (`issuer`, `name`, `key`, `algorithm`, `digits`, `period`).
- **Zero-Knowledge Sanitizer**: Strictly strips passwords (`login.password`), credit cards, secure notes, and personal data in RAM before persistence.
- **Dual Vault Persistence Router**:
  - *Local Pathway*: Inserts sanitized TOTP entities directly into Room SQLCipher with `is_local_only = 1`.
  - *Remote Synchronized Pathway*: Converts tokens to ShellGuard Pearl DTOs, encrypts via `ShellCryptionEngine` (`huKey` + `userUuid` + AAD `vault_pearls_totp:{id}`), and pushes upstream via `POST /api/vault` (or queues with `syncState = "PENDING_SYNC"`).
- **Google Authenticator Protobuf Exporter**: Generates standard `otpauth-migration://offline?data=...` Protobuf envelopes.

> Success Criteria: Parser extracts all 2FA secrets from complex Bitwarden exports with 0% password/note leakage in memory or storage. Dual persister accurately routes to local Room DB or self-hosted server gateway.

- [ ]  Task 26: [UI Component] Import & Export Screen, Bitwarden Migration Preview Wizard & Multi-QR Viewer

Description: Implement:
- **`SettingsImportExportScreen.kt`**: Category tiles for "Import Bitwarden Vault", "Import ShellGuard Habitat", "Import Aegis / 2FAS", "Export Encrypted Vault", and "Export for Google Authenticator".
- **`BitwardenImportPreviewDialog.kt`**: Interactive migration wizard showing summary (*"Found 14 2FA tokens across 3 categories. Passwords and notes have been securely excluded"*), category mapping previews, and destination toggle: `[ 📱 Save to Local Vault Only ]` vs `[ ☁️ Save & Sync with Remote Gateway ]`.
- **`GoogleAuthExportViewerDialog.kt`**: Paged QR code carousel for multi-account migrations with account count badges and brightness boost.
- **`SettingsBackupsScreen.kt`**: Automatic backups toggle, Backup reminder toggle, and Android cloud backups toggle.

> Success Criteria: Users can select a Bitwarden export JSON file, preview extracted 2FA accounts with clear privacy assurance, choose local vs remote destination, and import seamlessly.

---

## Phase 14: Home Screen Interactive Glance Widgets & Icon Pack Manager [v0.1.0.0 (Build 13) — Open Beta Candidate]

> Phase Feature Set Overview:
> Delivers Android Glance Compose home screen widgets for fast 2FA access with live countdown progress arcs, and a custom SVG/Vector Icon Pack management engine for branded account icons.

- [ ]  Task 27: [Functionality] AndroidX Glance AppWidget Engine & Custom Issuer Icon Pack Store

Description: Integrate `androidx.glance:glance-appwidget` and `androidx.glance:glance-material3`. Implement `TotpGlanceReceiver` and `TotpGlanceWidgetService`. Implement `IconPackManager` supporting loading, parsing, and caching vector/PNG icon packs for popular web services (GitHub, Google, AWS, Microsoft, Discord).

> Success Criteria: Widget updates on 30s boundaries synchronously with system time consuming <0.1% battery/day. Icon pack manager resolves high-res brand icons dynamically by issuer name.

- [ ]  Task 28: [UI Component] Modernist Glance 2FA Widgets (2x2 & 4x2) & Icon Pack Manager Screen

Description: Implement:
- **Glance 2FA Widgets**: Compact 2x2 single-token and expanded 4x2 multi-account list with live countdown progress bars, split digits (`123 456`), and one-tap copy actions.
- **`SettingsIconPacksScreen.kt`**: Icon pack browser, import custom icon pack `.zip`, enable/disable icon packs, and icon preview grid.

> Success Criteria: Widgets render crisply on Android home screens with tap-to-copy responsiveness, and icon packs render custom brand glyphs across all account cards.





## Phase 15: ClawKey Vault Creation, Import Authentication & Duplicate Resolution [v0.0.1.1 (Build 9)]

> Phase Feature Set Overview:
> Introduces the ShellGuard sovereign `hu-` ClawKey as a third vault creation and import authentication method, bringing the Android identity model into parity with the ShellGuard web platform. Delivers a reusable dual-tab `ClawKeyInputForm` (Paste / Upload), consistent format validation, correct lock screen branching, proper export envelope stamping, and a pre-import duplicate resolution engine.

- [ ]  Task 29: [Functionality] ClawKey Mode Engine — VaultProtectionMode, AndroidKeyStoreHelper, AuthRepository & Duplicate Resolver

Description: Implement the full back-end plumbing for ClawKey vault mode:
- **`VaultProtectionMode`**: Add `CLAWKEY` as a fourth enum value alongside `PIN`, `PASSWORD`, `BIOMETRICS`.
- **`AndroidKeyStoreHelper.kt`**: Add `KEY_ALIAS_CLAWKEY_WRAPPER` constant and `getOrCreateClawKeySecretKey()` generator (mirrors existing PIN/Password wrapper pattern).
- **`AuthRepository.kt`**: In `hatchVault()` and `updateVaultSecret()`, add a `CLAWKEY` branch that generates the hardware KeyStore wrapper key and stores the validated `hu-` key string as the vault secret — routing through the existing password KDF pathway (no new derivation layer).
- **`ClawKeyValidator.kt`** *(new, `crypto/` package)*: Single pure-function object implementing the canonical validation rule: `key.trim().startsWith("hu-") && key.trim().length == 67`. Used by all three surfaces (vault creation, lock, import) to prevent format drift.
- **`BackupManager.kt` — Duplicate Resolution**: Before calling `totpItemDao.upsertItems()` in both `importEncryptedBackup()` and `importPlainJsonBackup()`, query existing items and compute a normalized fingerprint per record (`secret.uppercase().replace(" ","").replace("-","")` + `title.trim().lowercase()`). Skip any incoming item whose fingerprint matches an existing record. Return the count of actually inserted items (not total processed).
- **`BackupManager.kt` — Export Stamping**: `exportEncryptedBackup()` already accepts `protectionMode: String` — ensure the `SettingsScreen` caller passes `vaultMode.name` which will now correctly emit `"CLAWKEY"` for ClawKey vaults.

> Success Criteria: `CLAWKEY` mode successfully initializes hardware KeyStore key, stores the `hu-` key as vault secret, and `ClawKeyValidator` rejects any string not matching the `hu-` prefix + 67-char invariant. Duplicate imports are silently skipped with an accurate inserted-count returned.

- [ ]  Task 30: [UI Component] ClawKeyInputForm, VaultSecurityScreen Integration, LockScreen Branch & Settings Import Sheet

Description: Implement all user-facing surfaces for the ClawKey mode:
- **`ClawKeyInputForm.kt`** *(new, `ui/components/` package)*: Reusable dual-tab composable mirroring the ShellGuard web `QuickLoginModal`:
  - **"Key Paste" tab**: Monospace `OutlinedTextField` with `hu-xxxxxxxx...` placeholder. CTA button disabled until `ClawKeyValidator.isValid()` returns true. Inline error chip on invalid format.
  - **"Upload File" tab**: Android SAF `OpenDocument` launcher restricted to `application/json`. On file selected: parse `identity.token` field from JSON, run `ClawKeyValidator`. Show file name badge on success. Same CTA button pattern.
  - Both tabs call the same `onKeyResolved(huKey: String)` callback — the form's consumer never needs to know which tab the key came from.
- **`VaultSecurityScreen.kt`**: Add a third tab `[ 🗝️ ClawKey ]` to the existing PIN / Password selector. When selected, render `ClawKeyInputForm` in place of the PIN/Password input area. On `onKeyResolved`, call `onVaultHatched(huKey, isPin = false, enableBiometrics = false)`.
- **`LockScreen.kt`**: Add `VaultProtectionMode.CLAWKEY` branch: render `ClawKeyInputForm` as the unlock form. On `onKeyResolved`, call `onUnlockWithSecret(huKey)`.
- **`SettingsScreen.kt` — Import Flow**: After `MultiVaultBackupPreValidator` resolves the backup file, inspect `protectionMode`. If `"CLAWKEY"`, show a `ModalBottomSheet` hosting `ClawKeyInputForm` before calling `importEncryptedBackup()`. On wrong key, surface a sanitized error: *"Invalid ClawKey — vault could not be decrypted."*

> Success Criteria: A user can create a vault secured by their `hu-` ClawKey (paste or upload), be correctly prompted for their ClawKey on app lock/restart, export a `.sgtotp.bak` with `protectionMode = "CLAWKEY"` stamped in the envelope, and re-import that backup on another device using only their ClawKey. Duplicate TOTP entries are silently skipped during all import flows.
