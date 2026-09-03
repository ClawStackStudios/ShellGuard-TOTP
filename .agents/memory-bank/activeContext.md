# ShellGuard TOTP — Active Context

## Sliding Window of Events
1. [2026-09-02] Release v0.0.1.0 Cloud Build Passed & Cleaned: Cloud CI on tag `v0.0.1.0` completed successfully, producing signed `.aab` and `.apk` release artifacts. Deleted obsolete `RELEASE-v0.0.0.2.md`. Codified headless KeyStore and Robolectric SQLite testing invariants in `AGENTS.md` and `android-keystore-cold-restart-testing` skill. Aligned `project/meta-prompt-ai-studio.md` Stage 11 numbering directly with `ROADMAP.md` Phase 10. Local test suite verified 100% green across all 79 unit tests.
2. [2026-09-02] Release v0.0.1.0 (Build 7) Retagged & Pushed: Diagnosed headless CI test failures (AndroidKeyStore NoSuchAlgorithmException and SQLiteConnectionNatives UnsatisfiedLinkError). Added HMAC fallback in AndroidKeyStoreHelper, explicit FrameworkSQLiteOpenHelperFactory in Robolectric tests, -XX:-UsePerfData in gradle.properties, and redirected test java.io.tmpdir to build/tmp. Re-ran full test suite locally (79/79 passing 100% green). Committed under ff344a3, force-updated tag v0.0.1.0, and pushed to origin to trigger passing GitHub Actions release build.
3. [2026-09-02] Release v0.0.1.0 (Build 7) Tagged & Pushed to GitHub: Completed Phase 9 (Vault Security Education, Enlarged Spotlight Tour & Empty Vault Landing) + One-Way Mirror Sync grouped architecture. Fixed two test regressions in `LocalModeUnlockAndVaultTest` and `BackupManagerTest` prior to push. Created `RELEASE-v0.0.1.0.md`, updated `RELEASE-PLAY.md` with grouped separation highlights, bumped `versionCode = 7` & `versionName = 0.0.1.0`, merged to `main`, and pushed annotated tag `v0.0.1.0`.
4. [2026-09-02] Phase 15 Planned & Written to Docs: Designed ClawKey (`hu-`) vault creation mode as Phase 15 (Tasks 29 & 30). Appended full spec to ROADMAP.md and Stage 16 execution prompt to meta-prompt-ai-studio.md. Key design decisions: `ClawKeyInputForm` dual-tab (Paste / Upload .json), `ClawKeyValidator` as single source of truth (`startsWith("hu-") && length == 67`), CLAWKEY routes through existing password KDF pathway, pre-import duplicate resolution by normalized secret+title fingerprint, export stamping via `vaultMode.name`, LockScreen CLAWKEY branch.
5. [2026-09-02] Phase 9 Implementation & Hardware Key Orchestration: Implemented VaultSecurityScreen with zero-knowledge educational cards, interactive PIN/Password selector, and biometric toggle. Refactored SpotlightOverlay with +18dp breathable cutouts, fluid spring geometry, and stacked actions. Updated AndroidKeyStoreHelper and AuthRepository to generate instant hardware keys (sg_totp_pin_wrapper, sg_totp_password_wrapper) based on selected protection mode. Code is ready; waiting for user to build and test locally before committing.
6. [2026-09-01] Version Alignment & UI Polish: Downgraded Phase 7 version bump to v0.0.1.0 following semantic-versioning rules. Re-aligned future Roadmap phases (9-14) sequentially. Updated SettingsScreen copy to remove references to pushing upstream, and locked SwipeableTotpCard deletion gestures strictly to local codes. Permanently encoded One-Way Sync and Cross-Repo Execution Sequence rules into AGENTS.md via continuous learning proposal.
7. [2026-09-01] Architectural Refactor (One-Way Mirror Sync & Unified Export): Shifted application from bidirectional to One-Way Mirror sync. All items created on Android default to Local Codes. Exposed grouped streams (Local Vault vs Synced) in TotpViewModel and redesigned TotpListScreen to remove category chips and Snackbar filters. Modified BackupManager to strictly export Local Codes to sgtotp.bak. Authored compatibility_layer.md in ShellGuard web repo. Version bumped to v0.0.1.0 (Build 7).
8. [2026-08-31] Release v0.0.0.2 Re-Release & CI Hardening: Fixed Gradle Configuration Cache drops via unconditional AGP signing block in app/build.gradle.kts; resolved Robolectric headless test stalls (manual Looper pump + advanceTimeBy) and cross-test database state pollution in LocalModeUnlockAndVaultTest; created top-level CHANGELOG.md and updated learning skills (Build 5).
9. [2026-08-31] Release v0.0.0.2 (Build 4) Committed & Pushed: Completed Phase 7 (First-Run Intake & Habitat Import). Added Brand Hero Welcome Screen, SAF multi-vault ingestion, .sgtotp.bak backup support, Key Rotation at Import Time, Zero-Knowledge Physical Storage Guidance, BoringSSL error sanitization, Steam Guard 5-char generator, and tightened .agents/rules/zero-knowledge-migration.md. Pushed feat/stage-8-intake-wizard to origin.
10. [2026-08-31] Proprietary `sgtotp.bak` Format & Dynamic PIN/Password/Biometric Intake Detection: Implemented support for ShellGuard TOTP proprietary backup format `sgtotp.bak` (`BackupEnvelope` format, protectionMode metadata, pinLength, isBiometricEnabled). Enhanced `MultiVaultBackupPreValidator` and `IntakeWelcomeScreen` with automatic PIN/Password mode detection, dynamic numeric numpad keyboard (`KeyboardType.NumberPassword`), resilient multi-salt key derivation, and user-friendly error messages replacing raw OpenSSL BAD_DECRYPT crashes.

## 🤝 Next Agent Handoff
**Current State**:
- Working tree is clean on branch `main`.
- Release `v0.0.1.0` (Build 7) is published and passing on GitHub Actions.
- All 79 local tests are passing 100% green (`./gradlew testDebugUnitTest`).
- Obsolete `RELEASE-v0.0.0.2.md` has been pruned.

**Immediate Directive for Next Agent**:
Begin implementation of **Phase 10: Expandable Floating Actions Speed Dial (QR, Image & Manual) [v0.0.1.2 (Build 9)]**:
1. **Branching**: Start on a fresh branch off `main`: `git checkout -b feat/phase-10-speed-dial`.
2. **Roadmap Reference**: Open [`ROADMAP.md`](ROADMAP.md#phase-10-expandable-floating-actions-speed-dial-qr-image--manual-v0012-build-9) and review **Task 19** and **Task 20**.
3. **Execution Prompt**: Read Stage 11 in [`project/meta-prompt-ai-studio.md`](project/meta-prompt-ai-studio.md#stage-11-phase-10-prompt--expandable-floating-actions-speed-dial-qr-image--manual-v0012-build-9).
4. **Task Breakdown**:
   - **Task 19: [Functionality] Image QR Decoder Pipeline & Expandable FAB Interaction Controller**:
     - Implement `ImageQrDecoder` using Google ML Kit Barcode Scanning on URI bitmap streams (`InputImage.fromFilePath` / `fromBitmap`).
     - Implement `SpeedDialState` controller managing expand/collapse transitions, back-handler interception, outside touch scrim dismissals, and permission requests.
   - **Task 20: [UI Component] Animated Speed Dial FAB & Elevated Action Pills**:
     - Implement `ExpandableSpeedDialFab.kt` on `TotpListScreen.kt`:
       - Main FAB with smooth 45-degree rotation morphing from `+` to `✕`.
       - Background scrim with subtle dark alpha dimming dismissible by tapping anywhere outside.
       - Elevated Action Pills (staggered slide-and-fade entrance):
         1. `[ 📷 Scan QR code ]` (Navigates to CameraX preview scanner).
         2. `[ 🖼️ Scan image ]` (Launches SAF image gallery picker to decode screenshot QR).
         3. `[ ✏️ Enter manually ]` (Navigates to manual secret entry form).
5. **Local Verification**: Run `./gradlew testDebugUnitTest` using:
   ```bash
   export JAVA_HOME="/config/Applications/android-studio/jbr"
   export PATH="$JAVA_HOME/bin:$PATH"
   ./gradlew testDebugUnitTest --no-daemon
   ```

