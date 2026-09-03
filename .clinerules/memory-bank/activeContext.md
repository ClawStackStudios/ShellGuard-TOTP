# ShellGuard TOTP — Active Context

## Current Focus
**Phase 10: Expandable Floating Actions Speed Dial (QR, Image & Manual) [v0.0.1.2 (Build 9)]** — branch `feat/phase-10-speed-dial`.

## Sliding Window of Events (10)
1. [2026-09-02] **Phase 10 implemented & verified**: Tasks 19 & 20 complete on `feat/phase-10-speed-dial`. 86/86 unit tests green, `assembleDebug` produces `app-debug.apk`. New files: `scanner/ImageQrDecoder.kt`, `ui/components/SpeedDialState.kt`, `ui/components/ExpandableSpeedDialFab.kt`, tests `ImageQrDecoderTest`, `SpeedDialStateTest`. `QrScannerScreen` refactored onto shared `ImageQrDecoder`; dual FABs + empty-state `ScannerFab` replaced by speed dial (`ScannerFab.kt` deleted as dead code). Test oracle updated: `scan_qr_fab` → `speed_dial_fab` in `LocalModeUnlockAndVaultTest`.
2. [2026-09-02] Cline memory bank initialized at `.clinerules/memory-bank/` (distinct from Antigravity's `.agents/memory-bank/`).
3. [2026-09-02] Context mapped: `TotpListScreen` dual FABs (Add ~L128, Scan QR ~L145) + `ScannerFab` (L313) superseded by speed dial; `QrScannerScreen` L113–137 inline gallery decode extracted into `ImageQrDecoder`.
4. [2026-09-02] Phase 10 spec confirmed: Task 19 (`ImageQrDecoder` + `SpeedDialState`) + Task 20 (`ExpandableSpeedDialFab.kt`, 45° morph, scrim, 3 staggered pills).

## Decisions
- Refactored (not duplicated) `QrScannerScreen`'s inline image decode into shared `ImageQrDecoder` — approved in plan.
- Speed dial "🖼️ Scan image" pill hosts SAF `GetContent("image/*")` on `TotpListScreen`; decode → `viewModel.importScannedUri()` with success/invalid Toasts.
- Camera permission delegated to `QrScannerScreen` (owner of CameraX flow) — speed dial only handles expand/collapse/back/scrim.
- `ScannerFab.kt` deleted (zero remaining references).

## Next Steps
1. **User live-run gate**: install `app-debug.apk` on device/emulator; verify FAB morph, scrim, pills, image decode (container has no device).
2. Release prep when approved: bump `versionCode = 9` / `versionName = "0.0.1.2"`, release notes, tag `v0.0.1.2`.
3. Phase 11: Categorized Settings Hub (v0.0.2.0).