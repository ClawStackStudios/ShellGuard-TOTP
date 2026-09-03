# Skill: Android Container Gradle Testing (ShellGuard-TOTP)

> Validated 2026-09-02 during Phase 10. Apply whenever running Gradle builds/tests
> in this containerized JBR environment.

## 1. Required Gradle Environment Exports
`gradle.properties` `org.gradle.jvmargs` does **not** cover the Gradle launcher JVM
under `--no-daemon`. Without these, expect launcher `SIGBUS` (JBR 21, libc
`PerfLongVariant::sample`) and KSP `ExceptionInInitializerError` when `/tmp` fills:

```bash
export JAVA_HOME="/config/Applications/android-studio/jbr"
export PATH="$JAVA_HOME/bin:$PATH"
export GRADLE_OPTS="-XX:-UsePerfData -Djava.io.tmpdir=$PWD/app/build/tmp"
```

## 2. Ephemeral /tmp — Redirect All Logs Into the Workspace
`/tmp` is isolated per shell session: files written there (nohup logs, jstack
attach sockets) vanish or are unreachable from later commands. Always redirect
build/test logs into `app/build/` and read them from there:
```bash
./gradlew testDebugUnitTest --no-daemon > app/build/sg-test.log 2>&1
```

## 3. Plain-JVM Compose Animation Tests — Advancing Frame Clock
`Animatable.animateTo` requires a `MonotonicFrameClock` context element. On the
plain JVM supply one that **advances time** (16 ms/frame):
```kotlin
private class ImmediateFrameClock : MonotonicFrameClock, CoroutineContext.Element {
    private val t = AtomicLong(0L)
    override val key: CoroutineContext.Key<*> get() = MonotonicFrameClock
    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R =
        onFrame(t.addAndGet(16_000_000L))
}
// usage: withContext(ImmediateFrameClock()) { state.expand() }
```
- A **zero-delta clock (`onFrame(0L)` every frame) hangs springs forever** — dt=0
  never converges; the test task stalls silently. This cost a 20-minute hung run.
- A `MonotonicFrameClock` is an interface (not fun interface): implement it with
  `object/class X : MonotonicFrameClock, CoroutineContext.Element` and
  `override val key get() = MonotonicFrameClock`.

## 4. Stale JUnit XML Trap
`app/build/test-results/testDebugUnitTest/TEST-*.xml` persists across runs. Before
diagnosing a failure, verify `timestamp="..."` in the XML matches the run you just
executed — otherwise you'll debug an already-fixed failure.

## Reference Witnesses
- Phase 10 commit `65b0c1d` (`SpeedDialStateTest.kt` — ImmediateFrameClock pattern).
- Hung-run diagnosis: `jstack` is also blocked by per-process `/tmp`; SIGQUIT the
  worker or reason from test boundaries instead.