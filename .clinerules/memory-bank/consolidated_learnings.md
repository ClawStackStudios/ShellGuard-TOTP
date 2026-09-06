# Consolidated Learnings (Cline)

## Behavioral Rules
- **Nullable-Timestamp Delta Sync (CRITICAL pattern)**: In any "skip if unchanged" comparison, a null/missing remote stamp must classify as CHANGED — never compare nullable-to-nullable (`null == null` silently swallows new records when reconciliation is delete-only, since prune can't insert). Implemented in `TotpRepository.classifyDeltaPearls` (v0.0.1.2 regression fix, 2026-09-04).
- **Sync testability seam**: extract delta/classification logic into pure `internal companion` functions on the repository — the ApiClient singleton can't be faked, but pure functions test on plain JVM. `syncRemoteVault` shipped untested and regressed; the delta path now has direct coverage (`DeltaSyncClassificationTest`).

## Workspace Scope & Destructive Ops
- codified as an active rule —
  `.clinerules/rules/workspace-scope-and-destructive-ops.md`. Stay inside the
  project workspace when exploring config; destructive/system operations require
  audit-first numbers, tiered risk-labeled proposals, exact-scope execution, and
  pause-on-interrupt.

## Environment
- Java: `JAVA_HOME=/config/Applications/android-studio/jbr`; tests via `./gradlew testDebugUnitTest --no-daemon`.
- **Container Gradle exports (required)**: `GRADLE_OPTS="-XX:-UsePerfData -Djava.io.tmpdir=$PWD/app/build/tmp"` — prevents launcher JVM SIGBUS and KSP tmpdir failures; `/tmp` is ephemeral per shell (redirect logs into `app/build/`).

## Compose Animation on Plain JVM
- **Pattern: Advancing MonotonicFrameClock**: `Animatable.animateTo` needs a `MonotonicFrameClock` context element. On JVM tests supply `object : MonotonicFrameClock, CoroutineContext.Element { override val key get() = MonotonicFrameClock; withFrameNanos = onFrame(t += 16ms) }`. A zero-delta clock hangs springs forever (dt=0 never converges).

## ADB Device UI Automation
**Skill:** `.agents/skills/adb-ui-input/SKILL.md`
- `input text` APPENDS; clear = `keyevent 123` + `keyevent 67`×N, then screenshot-verify empty before typing. Keyboard (dismiss: `keyevent 4`) covers bottom-third buttons — prefer tapping the visible submit over `keyevent 66`. Every `adb install -r` re-locks a PIN vault. Screenshot-verify every step; never act on assumed field state.

## Theme-Aware Compose Surfaces
**Rule:** `.clinerules/rules/theme-aware-compose-surfaces.md`
- Root cause of all three light-mode defects (Build 12–14): hardcoded appearance. Never hardcode White/Black — resolve via colorScheme; `isAppearanceLightStatusBars` follows the APP's effective theme via `DisposableEffect`, not the system; scrims/sheets behind dynamic content must be full-screen; draw `background`+`border` BEFORE `clip` (border-before-clip). Verify every theme change with a live two-mode sweep.

## Release Hotfix Retag Flow
**Skill:** RELEASE-PLAY.md appendix
- Hotfix under same `versionName`: bump `versionCode` only → push → `git tag -d` local + `git push origin :refs/tags/X` (delete FIRST — stale tag = pipeline runs old commit) → re-tag → verify `gh run list` + release assets + on-device APK. Proven Build 13→14.
- Phase headings carry build *projections* only — re-point them at release time; never renumber phases to chase versionCode (codified in ROADMAP.md 2026-09-06 amendment).


## Inherited Durable Patterns (validated by Antigravity sessions)
- **Test Oracle Audit**: layout/data-filter changes require auditing `app/src/test` fixtures (semantic tags, entity flags) before push.
- **Pre-DAO Fingerprint Dedup**: `normalizedSecret + "_" + normalizedTitle` filter before `upsertItems()`.
- **Stale XML trap**: JUnit XML in `app/build/test-results` persists across runs — verify `timestamp=` before diagnosing failures.