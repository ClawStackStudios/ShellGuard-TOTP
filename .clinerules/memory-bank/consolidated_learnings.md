# Consolidated Learnings (Cline)

## Behavioral Rules
- **Workspace Scope & Destructive Ops**: codified as an active rule —
  `.clinerules/rules/workspace-scope-and-destructive-ops.md`. Stay inside the
  project workspace when exploring config; destructive/system operations require
  audit-first numbers, tiered risk-labeled proposals, exact-scope execution, and
  pause-on-interrupt.

## Environment
- Java: `JAVA_HOME=/config/Applications/android-studio/jbr`; tests via `./gradlew testDebugUnitTest --no-daemon`.
- **Container Gradle exports (required)**: `GRADLE_OPTS="-XX:-UsePerfData -Djava.io.tmpdir=$PWD/app/build/tmp"` — prevents launcher JVM SIGBUS and KSP tmpdir failures; `/tmp` is ephemeral per shell (redirect logs into `app/build/`).

## Compose Animation on Plain JVM
- **Pattern: Advancing MonotonicFrameClock**: `Animatable.animateTo` needs a `MonotonicFrameClock` context element. On JVM tests supply `object : MonotonicFrameClock, CoroutineContext.Element { override val key get() = MonotonicFrameClock; withFrameNanos = onFrame(t += 16ms) }`. A zero-delta clock hangs springs forever (dt=0 never converges).

## Inherited Durable Patterns (validated by Antigravity sessions)
- **Test Oracle Audit**: layout/data-filter changes require auditing `app/src/test` fixtures (semantic tags, entity flags) before push.
- **Pre-DAO Fingerprint Dedup**: `normalizedSecret + "_" + normalizedTitle` filter before `upsertItems()`.
- **Stale XML trap**: JUnit XML in `app/build/test-results` persists across runs — verify `timestamp=` before diagnosing failures.