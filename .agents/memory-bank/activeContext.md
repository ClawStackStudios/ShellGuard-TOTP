# ShellGuard TOTP — Active Context

## Sliding Window of Events
1. [2026-09-03] Release v0.0.1.2 (Build 8) Harmonization & Pre-Flight Gate Verification: Verified full pre-flight verification gate (90/90 unit & Robolectric tests passing 100% green, assembleDebug clean build successful). Harmonized RELEASE-v0.0.1.2.md, CHANGELOG.md, and memory bank documents with the 90 tests milestone, One-Way Sync optimizations (TotpItemDao.deleteByIdIfLocal, client-side delta sync skipping unchanged decryption, pull-to-refresh integration), and Build 8 alignment for Play Console release.
2. [2026-09-02] One-Way Sync Hardening & Pull-to-Refresh Dashboard Alignment: Verified and codified strict one-way mirror sync invariants. Updated TotpSyncWorker to 6-hour periodic background schedule (ExistingPeriodicWorkPolicy.UPDATE). Implemented Compose Material 3 PullToRefreshBox on TotpListScreen backed by refreshRemoteVault in TotpViewModel. Completely locked remote synced codes to read-only on device (disabled edit modal, disabled delete confirmation, disabled swipe-to-dismiss, added visual 'Read-only' indicator to TotpCard). Fixed misleading AddSecretScreen banner copy from 'Token will sync to connected server' to local storage banner. Changed IntakeViewModel import syncState from PENDING_SYNC to LOCAL. Authored OneWaySyncAndReadOnlyProtectionTest verifying all protections (90/90 tests passing 100% green). Streamed and verified updated APK live on physical Pixel 8.
3. [2026-09-02] Phase 10 Speed Dial Implemented & Release v0.0.1.2 (Build 8) Prepped: Implemented ExpandableSpeedDialFab with 45-degree morph (+ to ✕), dark scrim, and 3 staggered pills (Scan QR / Scan image / Enter manually). Extracted unified ImageQrDecoder with Google ML Kit for gallery QR screenshot decoding. Refactored QrScannerScreen onto shared decoder and removed dead ScannerFab.kt. Updated test oracle in LocalModeUnlockAndVaultTest. Verified 86/86 unit tests green and assembleDebug successful. Prepped RELEASE-v0.0.1.2.md, updated RELEASE-PLAY.md, bumped versionCode = 8 & versionName = "0.0.1.2", and aligned ROADMAP.md.
4. [2026-09-02] Release v0.0.1.0 Cloud Build Passed & Cleaned: Cloud CI on tag `v0.0.1.0` completed successfully, producing signed `.aab` and `.apk` release artifacts. Deleted obsolete `RELEASE-v0.0.0.2.md`. Codified headless KeyStore and Robolectric SQLite testing invariants in `AGENTS.md` and `android-keystore-cold-restart-testing` skill. Aligned `project/meta-prompt-ai-studio.md` Stage 11 numbering directly with `ROADMAP.md` Phase 10. Local test suite verified 100% green across all 79 unit tests.
5. [2026-09-02] Release v0.0.1.0 (Build 7) Retagged & Pushed: Diagnosed headless CI test failures (AndroidKeyStore NoSuchAlgorithmException and SQLiteConnectionNatives UnsatisfiedLinkError). Added HMAC fallback in AndroidKeyStoreHelper, explicit FrameworkSQLiteOpenHelperFactory in Robolectric tests, -XX:-UsePerfData in gradle.properties, and redirected test java.io.tmpdir to build/tmp. Re-ran full test suite locally (79/79 passing 100% green). Committed under ff344a3, force-updated tag v0.0.1.0, and pushed to origin to trigger passing GitHub Actions release build.
6. [2026-09-02] Release v0.0.1.0 (Build 7) Tagged & Pushed to GitHub: Completed Phase 9 (Vault Security Education, Enlarged Spotlight Tour & Empty Vault Landing) + One-Way Mirror Sync grouped architecture. Fixed two test regressions in `LocalModeUnlockAndVaultTest` and `BackupManagerTest` prior to push. Created `RELEASE-v0.0.1.0.md`, updated `RELEASE-PLAY.md` with grouped separation highlights, bumped `versionCode = 7` & `versionName = 0.0.1.0`, merged to `main`, and pushed annotated tag `v0.0.1.0`.
7. [2026-09-02] Phase 15 Planned & Written to Docs: Designed ClawKey (`hu-`) vault creation mode as Phase 15 (Tasks 29 & 30). Appended full spec to ROADMAP.md and Stage 16 execution prompt to meta-prompt-ai-studio.md. Key design decisions: `ClawKeyInputForm` dual-tab (Paste / Upload .json), `ClawKeyValidator` as single source of truth (`startsWith("hu-") && length == 67`), CLAWKEY routes through existing password KDF pathway, pre-import duplicate resolution by normalized secret+title fingerprint, export stamping via `vaultMode.name`, LockScreen CLAWKEY branch.
8. [2026-09-02] Phase 9 Implementation & Hardware Key Orchestration: Implemented VaultSecurityScreen with zero-knowledge educational cards, interactive PIN/Password selector, and biometric toggle. Refactored SpotlightOverlay with +18dp breathable cutouts, fluid spring geometry, and stacked actions. Updated AndroidKeyStoreHelper and AuthRepository to generate instant hardware keys (sg_totp_pin_wrapper, sg_totp_password_wrapper) based on selected protection mode. Code is ready; waiting for user to build and test locally before committing.
9. [2026-09-01] Version Alignment & UI Polish: Downgraded Phase 7 version bump to v0.0.1.0 following semantic-versioning rules. Re-aligned future Roadmap phases (9-14) sequentially. Updated SettingsScreen copy to remove references to pushing upstream, and locked SwipeableTotpCard deletion gestures strictly to local codes. Permanently encoded One-Way Sync and Cross-Repo Execution Sequence rules into AGENTS.md via continuous learning proposal.
10. [2026-09-01] Architectural Refactor (One-Way Mirror Sync & Unified Export): Shifted application from bidirectional to One-Way Mirror sync. All items created on Android default to Local Codes. Exposed grouped streams (Local Vault vs Synced) in TotpViewModel and redesigned TotpListScreen to remove category chips and Snackbar filters. Modified BackupManager to strictly export Local Codes to sgtotp.bak. Authored compatibility_layer.md in ShellGuard web repo. Version bumped to v0.0.1.0 (Build 7).

## 🤝 Next Agent Handoff
**Current State**:
- Working tree on branch `feat/phase-10-speed-dial`.
- Phase 10 (Tasks 19 & 20) fully implemented and verified.
- All 90 local unit tests are passing 100% green (`./gradlew testDebugUnitTest`).
- Release `v0.0.1.2 (Build 8)` prepped: `RELEASE-v0.0.1.2.md` aligned, `RELEASE-PLAY.md` prepended, `app/build.gradle.kts` configured to `versionCode = 8` & `versionName = "0.0.1.2"`, `ROADMAP.md` checked off.
- User notified prior to merge into `main`.

**Immediate Directive for Next Agent**:
Following merge and release of `v0.0.1.2 (Build 8)`, begin implementation of **Phase 11: Categorized Settings Hub & Appearance/Behavior Customization [v0.0.2.0 (Build 10) — Milestone 2]**:
1. **Branching**: Start on a fresh branch off `main`: `git checkout -b feat/phase-11-settings-hub`.
2. **Roadmap Reference**: Open [`ROADMAP.md`](ROADMAP.md#phase-11-categorized-settings-hub--appearancebehavior-customization-v0020-build-10--milestone-2) and review **Task 21** and **Task 22**.
3. **Execution Prompt**: Read Stage 12 in [`project/meta-prompt-ai-studio.md`](project/meta-prompt-ai-studio.md#stage-12-phase-11-prompt--categorized-settings-hub--appearancebehavior-customization-v0020-build-10--milestone-2).
4. **Task Breakdown**:
   - **Task 21: [Functionality] Preferences Store Architecture & Entry Formatting Engine**:
     - Expand `AuthRepository` and preferences store (`AppearancePreferences`, `BehaviorPreferences`).
   - **Task 22: [UI Component] Categorized Settings Hub (`SettingsMetaScreen`), Appearance & Behavior Sub-screens**:
     - Implement `SettingsMetaScreen.kt`, `SettingsAppearanceScreen.kt`, and `SettingsBehaviorScreen.kt`.
5. **Local Verification**: Run `./gradlew testDebugUnitTest` using:
   ```bash
   export JAVA_HOME="/config/Applications/android-studio/jbr"
   export PATH="$JAVA_HOME/bin:$PATH"
   export GRADLE_OPTS="-XX:-UsePerfData -Djava.io.tmpdir=$PWD/app/build/tmp"
   ./gradlew testDebugUnitTest --no-daemon
   ```

