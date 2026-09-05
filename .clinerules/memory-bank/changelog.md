# Changelog (Cline)

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