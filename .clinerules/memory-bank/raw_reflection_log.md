# Raw Reflection Log

---
Date: 2026-09-06
TaskRef: "/learn — codify light-mode polish + ADB automation learnings (approved by Lucas)"

Learnings:
- Created `.agents/skills/adb-ui-input/SKILL.md` (Skill): ADB UI input cheatsheet — `input text` appends; clear = keyevent 123 + 67×N then screenshot-verify empty; keyboard (keyevent 4 to dismiss) covers bottom-third buttons; prefer tapping visible submit over keyevent 66; every `adb install -r` re-locks the PIN vault. Cost this session: ~10 round-trips on PIN entry alone.
- Created `.clinerules/rules/theme-aware-compose-surfaces.md` (Rule): root cause of all 3 light-mode defects was hardcoded appearance — (a) Type.kt static dark tokens, (b) status-bar icons following system theme, (c) scrim confined to FAB slot; plus modifier-order lesson (background+border BEFORE clip).
- Extended `RELEASE-PLAY.md` appendix (Skill): hotfix retag flow under unchanged versionName — proven twice (Build 13→14). Always delete remote tag before re-pushing or pipeline may run stale commit.
- FAB white-in-light-mode regression CLOSED: was stale-build mix on device, not a code defect; final Build 14 sweep confirmed pink FAB + correct scrim both modes.

Successes:
- Pipeline retag + on-device APK verification loop executed cleanly twice; full two-mode live sweep passed.

Consolidation: 3 items transferred to consolidated_learnings.md; learning_proposal.md consumed and deleted.

Handoff_Package_Prepared: false
---

---
Date: 2026-09-04
TaskRef: "/learn — codify hotfix session behaviors (approved by Lucas)"

Learnings:
- Created `.clinerules/workflows/android-device-adb-verification.md` (Skill): on-device UI verification loop + black-frame triage (display suspend ≠ FLAG_SECURE; debug builds exempt via !BuildConfig.DEBUG).
- Updated `.clinerules/workflows/version-update.md`: added Step 5 (5-anchor all-or-nothing release sync; ROADMAP Phase headings historical) and Step 6 (push + gh CLI CI/artifact verification chain).
- Deliberately NOT codified: nullable-timestamp delta invariant (already in consolidated_learnings) — avoid redundant rules.

Handoff_Package_Prepared: false
---

---
Date: 2026-09-04
TaskRef: "v0.0.1.3 hotfix release shipped — sync regression fix + card polish + Task 24 spec expansion"

Learnings:
- RELEASE SHIPPED: commit 3e70fa9 pushed to origin/main + tag v0.0.1.3. Cloud Release Pipeline passed in 3m46s (run 33939042244): signed artifacts published — shellguard-totp-v0.0.1.3.aab (35.2MB) + .apk (59.6MB) on GitHub Release "ShellGuard TOTP v0.0.1.3". versionCode 9 / versionName 0.0.1.3 per version-update protocol REVISION increment rule.
- gh CLI available at /config/.local/bin/gh and authenticated — `gh run list/watch/view` + `gh release view` beats raw API curl for CI verification.
- Full verification stack this session: pure-function unit tests (6 delta cases) → full suite 96/96 → assembleDebug → on-device ADB screencap verification → CI green. Every layer agreed; no orphans.

Successes:
- Hotfix executed end-to-end in one session: diagnose → fix → test → device-verify → UI tweak → device-verify → spec expansion → version sync across 5 anchors → gate → commit → tag → push → CI-verified release.
- Task 24 FLAG_SECURE toggle spec expanded with live-session context before implementation debt could accumulate.

Handoff_Package_Prepared: true
---

---
---
Date: 2026-09-04
TaskRef: "Diagnose & fix v0.0.1.2 remote sync regression (no codes syncing from server)"

Handoff_Context:
- Session state: ~60% used
- Active work: compare v0.0.1.0 vs v0.0.1.2 sync behavior; found + fixed root cause
- Pending decisions: whether server SHOULD populate updated_at (server-side fix option); whether to cut v0.0.1.3 release with this fix
- Unresolved issues: on-device sync verification (Lucas's Pixel 8); server-side confirmation that updated_at is null/missing in vault responses (inferred, not captured)

Learnings:
- NULL-VS-NULL DELTA TRAP: any "skip if unchanged" comparison against a nullable remote field must treat null remote stamps as always-changed. `localNull == remoteNull` silently swallows new records when reconciliation is delete-only (prune can't insert). Generalizable to every mirror/delta sync design.
- The regression shipped because syncRemoteVault had zero direct test coverage — read-only tests inserted entities straight into Room. Extracting the classification into a pure `internal companion` function enabled plain-JVM regression tests without faking the ApiClient singleton.
- Test-verification trap: long gradle runs (~13min) exceed the 300s tool timeout; partial XML results from prior runs persist in app/build/test-results and TOTAL counts can look "done" while the executor is still mid-suite. Always confirm the target suite's own XML exists and grep BUILD SUCCESSFUL before declaring green.

Successes:
- Diff-driven bisection (git diff v0.0.1.0 v0.0.1.2 on data/ + viewmodels) isolated the culprit to the 3 changed sync functions within minutes despite no logs.
- Fix is self-healing: devices stuck with null-stamped mirror rows re-sync on next pull; no migration needed.

Improvements_Identified_For_Consolidation:
- Pattern: nullable-timestamp delta classification (null remote stamp ⇒ changed).
- Pattern: extract pure classification functions from repository sync methods for testability.
- Environment: background + poll long gradle runs; verify per-suite XML freshness (stale XML trap recurrence).

Handoff_Package_Prepared: true
---

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
---
Date: 2026-09-05
TaskRef: "Phase 11.5 Settings Continuity — corpus weave (ROADMAP v2.5.0, meta-prompt Stage 12.5)"

Learnings:
- Settings parity audit method: diff legacy release tag settings UI (git show v0.0.1.3:<file>) against new hub screens, then cross-check each control against roadmap phases BEFORE deciding anything is "lost" — 4 of 6 lost cards were already scheduled (Phases 12/13/14); only Theme (Task 22 spec drift) and Server & Sync (true orphan) needed corpus work.
- Versioning: renumbering unshipped future phases is safe (no tags/releases reference them); must simultaneously update ROADMAP headers, meta-prompt stage headers + inline prompt blocks, and next-stop pointers in the previous release record.
- Structural contracts belong in ui-ux-design-system.md (route maps, invariants); DESIGN.md gets a superseded-section pointer note, not a rewrite.

Successes:
- Interlude C pattern (honest gap record) keeps the deterministic roadmap trustworthy while inserting a revision phase.
