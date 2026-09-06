# Changelog (Cline)

## [session-5] - 2026-09-05
### Released
- **v0.0.2.1 (Build 11) — Phase 11.5 Settings Continuity** prepped on `feat/phase-11.5-settings-continuity`: Tasks 22b (verify-only)/22c (Appearance theme section + SettingsServerSyncScreen + tour step 2 migration)/22d (Import & Export SAF screen) implemented and verified live on Pixel sailfish (hub nav, export/restore roundtrip, theme cold-restart persistence, tour cutout).

### Fixed
- **Gateway back-button clipping**: circular back button inset 10dp + border drawn outside `CircleShape` clip (top arc no longer shaved). Commit e469a1e.
- **Missing route/screen in Task 22d commit**: `SettingsImportExportScreen.kt` was silently untracked and the nav-graph insertion had no-opped (python replace without assert) — committed and grep-verified (commit 25560c6).

### Learned
- python `str.replace` without `assert` silently no-ops — always assert anchors (bit twice now: nav graph, screen file).
- Pre-flight flake: `IntakeOnboardingTest.testIntakeViewModelPinProtectionValidationAndHatching` failed on Robolectric timing poll once, passed clean on isolated re-run — treat single timing-poll failures as flake candidates before diagnosing.

## [session-4] - 2026-09-05
### Released
- **v0.0.2.0 (Build 10) — Milestone 2**: Phase 11 (Tasks 21/22) shipped. Tag pushed, Release Pipeline green (run 33981002398), signed .aab/.apk on GitHub Release. Verified live on new test device (original Pixel sailfish/LineageOS); Hub + sub-screen screenshots captured into store-assets and README refreshed (slot 5 hub + new row 3; test count 101+).

## [session-3] - 2026-09-04 (v0.0.1.3 Hotfix Release)
### Fixed
- Remote sync regression + TotpCard badge wrap (see [session-2] below; shipped in v0.0.1.3).

### Released
- **v0.0.1.3 (Build 9) Hotfix** — commit 3e70fa9, tag pushed, Release Pipeline passed 3m46s (run 33939042244); signed `shellguard-totp-v0.0.1.3.aab` (35.2MB) + `.apk` (59.6MB) on GitHub Release. Version synced across all 5 anchors (versionCode 9, CHANGELOG, RELEASE-v0.0.1.3.md, RELEASE-PLAY.md, README badge).

### Added
- **Task 24 "Screen security toggle" (FLAG_SECURE) spec fully expanded** in ROADMAP.md + project/meta-prompt-ai-studio.md: `allowScreenshots` DataStore pref (default false), opt-in confirm dialog, StateFlow-driven immediate window flag apply, forced FLAG_SECURE when vault locked/backgrounded, debug exemption preserved, black-screencap QA note, `SCREEN_SECURITY_CHANGED` audit event, Task 23 controller dependency note.
- `/learn` codifications (approved by Lucas): new workflow `android-device-adb-verification.md`; version-update.md Steps 5–6 (release anchor checklist + gh CI verification). Proposal artifact in `.clinerules/learning-proposals/2026-09-04-hotfix-session.md`.
- Memory bank updates: progress.md refreshed to post-release state, techContext.md device/CI tooling section.

## [session-2] - 2026-09-04
### Fixed
- **CRITICAL — Remote Sync Regression (v0.0.1.2)**: Delta filter in `TotpRepository.syncRemoteVault` compared nullable local `remoteUpdatedAt` against nullable remote `updated_at`; null==null classified all pearls (and especially fresh installs) as "unchanged" → never decrypted/upserted, prune-only reconciliation couldn't insert → zero codes synced with silent success. Extracted `classifyDeltaPearls` (unchanged requires existing local row + non-null equal stamps; null stamps always sync — self-healing).

### Added
- `DeltaSyncClassificationTest` (6 regression cases including the exact repro) — first direct coverage of the sync delta path; suite now 96/96 green + assembleDebug verified.

## [session-1] - 2026-09-02
### Added
- Initialized Cline memory bank at `.clinerules/memory-bank/` (7 core files), seeded from Antigravity's `.agents/memory-bank/` + codebase mapping. Distinct from Antigravity's bank — that directory remains untouched.
- **Phase 10 implementation** (branch `feat/phase-10-speed-dial`):
  - `scanner/ImageQrDecoder.kt` — shared ML Kit image QR pipeline (Task 19).
  - `ui/components/SpeedDialState.kt` — expand/collapse interaction controller (Task 19).
  - `ui/components/ExpandableSpeedDialFab.kt` — animated FAB, scrim, 3 staggered pills (Task 20).
  - Tests: `ImageQrDecoderTest` (2), `SpeedDialStateTest` (5).

### Changed
- `QrScannerScreen` refactored onto shared `ImageQrDecoder` (removed inline duplicate).
- `TotpListScreen` dual FABs replaced by `ExpandableSpeedDialFab`; new image-decode routing via `importScannedUri`.

### Removed
- `ui/components/ScannerFab.kt` (dead code after speed dial integration).

### Verified
- 86/86 unit tests green; `assembleDebug` successful (67 MB `app-debug.apk`).
- On-device live run pending (no emulator in container) — user gate.

### Environment Learnings
- Gradle launcher JVM SIGBUS in container → export `GRADLE_OPTS="-XX:-UsePerfData -Djava.io.tmpdir=$PWD/app/build/tmp"` (gradle.properties jvmargs don't cover the launcher under --no-daemon).
- `/tmp` is ephemeral per shell session — redirect build logs into `app/build/`.
- Compose `Animatable` on plain JVM requires a MonotonicFrameClock; a zero-delta frame clock makes spring animations spin forever (hang). Advance 16ms/frame.