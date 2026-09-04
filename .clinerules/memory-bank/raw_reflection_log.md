# Raw Reflection Log

---
Date: 2026-09-02
TaskRef: "One-Way Mirror Sync Hardening, Read-Only Protections & Pull-to-Refresh Dashboard Alignment"

Learnings:
- Compose Material 3 `PullToRefreshBox` integrates smoothly with ViewModel `isSyncing` StateFlow and provides native drag-to-refresh ergonomics across both populated and empty lists via `.verticalScroll(rememberScrollState())`.
- WorkManager periodic task scheduling intervals must be scheduled with `ExistingPeriodicWorkPolicy.UPDATE` so that when interval cadence changes (e.g. 15 minutes ➔ 6 hours), existing scheduled jobs are automatically updated without requiring full app re-install.
- Guarding against accidental remote code modifications requires defense-in-depth:
  1. UI Layer: omit/disable edit modals, disable delete buttons, disable swipe-to-dismiss, and add explicit "Read-only" badge text on the card.
  2. ViewModel/Domain Layer: strictly check `!item.isLocalOnly && item.ownerUuid != "local"` and reject update/delete operations.
  3. Copy/Text Layer: remove any copy implying upstream pushing (e.g., "Token will sync to connected server" replaced with "Added tokens are stored encrypted on this device only").
- Robolectric coroutine verification for async Room DAO calls requires flushing the Android Main Looper (`shadowOf(Looper.getMainLooper()).idle()`) and polling the in-memory SQLite state to ensure Dispatchers.IO continuations resume and persist before assertions run.

Difficulties:
- Identifying obsolete copy from early development phases that implied bidirectional sync: resolved by auditing all strings, banners, and overlays.

Successes:
- 90/90 unit and Robolectric tests passing 100% green.
- `assembleDebug` compiled cleanly.
- Updated APK streamed live to connected physical Pixel 8 and confirmed running.
- Invariants confirmed: 6-hour background sync, dashboard pull-to-refresh, strict read-only remote codes, and local-only backup exports.

Improvements_Identified_For_Consolidation:
- Defensive read-only mirror invariants (UI + ViewModel + DAO).
- Robolectric Room asynchronous coroutine looper idling pattern.
---
Date: 2026-09-02
TaskRef: "Release v0.0.1.2 (Build 9) Preparation & Phase 10 Speed Dial Verification"

Learnings:
- Gradle launcher JVM SIGBUS under container runtime: export `GRADLE_OPTS="-XX:-UsePerfData -Djava.io.tmpdir=$PWD/app/build/tmp"` ensures launcher process does not crash before task execution.
- Image QR decoding via Google ML Kit Barcode Scanning can be deduplicated across both live camera fallback and dashboard speed dial gallery picker via a centralized `ImageQrDecoder` component.
- Updating UI interaction components requires a test oracle audit: replacing dual FABs with `ExpandableSpeedDialFab` required updating `scan_qr_fab` ➔ `speed_dial_fab` in `LocalModeUnlockAndVaultTest`.

Difficulties:
- MonotonicFrameClock on plain JVM Robolectric tests requires explicit clock advancements or coroutine scheduling to prevent indefinite loops during spring physics calculations.

Successes:
- 86/86 unit and Robolectric tests passing 100% green.
- Clean `assembleDebug` compilation.
- Prepped `RELEASE-v0.0.1.2.md` and prepended `<en-US>` release notes (<500 chars) to `RELEASE-PLAY.md`.
- Monotonically bumped `versionCode = 9` and `versionName = "0.0.1.2"`.

Improvements_Identified_For_Consolidation:
- Container Gradle launcher JVM flags (`GRADLE_OPTS`) pattern.
---

---
Date: 2026-09-02
TaskRef: "Release v0.0.1.0 Preparation, Test Suite Alignment & Invariant Codification"

Learnings:
- Discovered Android Studio JBR OpenJDK 21 is available at `/config/Applications/android-studio/jbr/bin/java`.
- Cached Gradle distributions exist at `/config/.gradle/wrapper/dists/gradle-9.3.1-bin/.../bin/gradle`.
- Pre-flight test boundary audit caught two test failures before GitHub CI execution:
  1. `LocalModeUnlockAndVaultTest` still asserted `filter_chip_all` which was removed in the grouped dashboard refactor.
  2. `BackupManagerTest` had `isLocalOnly = false` on an item during export test, but One-Way Mirror Sync strictly excludes remote codes from `.sgtotp.bak` exports.
- Single source of truth validator `ClawKeyValidator` (`startsWith("hu-") && length == 67`) prevents validation drift between vault creation, lock screen, and import flows.
- Pre-DAO normalized fingerprint deduplication (`secret` + `title`) prevents SQLite `REPLACE` UUID collision blindspots during backup ingestion.

Difficulties:
- Identifying why `android describe` was reporting `gradlew not found`: required mapping Java JBR and cached Gradle binary paths in the container.

Successes:
- Caught and resolved test regressions before pushing to GitHub.
- Release `v0.0.1.0 (Build 7)` tagged, committed, and pushed cleanly to remote.
- Continuous learning invariants codified in `AGENTS.md` and `systemPatterns.md`.

Improvements_Identified_For_Consolidation:
- Test Oracle Synchronization Invariant when architectural rules shift.
- Pre-DAO normalized fingerprint deduplication pattern.
---
