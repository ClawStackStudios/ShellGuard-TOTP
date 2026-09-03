# Raw Reflection Log (Cline)

---
Date: 2026-09-02
TaskRef: "Phase 10 Speed Dial implementation & Cline memory bank init"

Learnings:
- Gradle launcher JVM SIGBUS under `--no-daemon` in container: `gradle.properties` jvmargs only cover forked daemons; fix via `GRADLE_OPTS="-XX:-UsePerfData -Djava.io.tmpdir=$PWD/app/build/tmp"`.
- `/tmp` ephemeral per shell — logs must go into `app/build/`.
- Compose `Animatable` on plain JVM: needs MonotonicFrameClock element; a 0-delta clock makes springs never converge → infinite frame loop hang. Use 16ms advancing clock. Frame clock interface pattern: `object X : MonotonicFrameClock, CoroutineContext.Element { override val key get() = MonotonicFrameClock }`.
- KSP `ExceptionInInitializerError` was tmpdir-full fallout, resolved by the same GRADLE_OPTS tmpdir redirect.
- `importScannedUri(rawUri): Boolean` — false = unparseable URI (parse-level), insertion is async.

Difficulties:
- 20-minute hung test run caused by zero-delta frame clock; killed worker 2011465, fixed clock, suite then passed in ~2 min.
- Stale JUnit XML files can mislead — always check `timestamp=` before diagnosing.

Successes:
- 86/86 tests green (was 79; +7 new). `assembleDebug` artifact produced.
- Shared `ImageQrDecoder` eliminated duplicated ML Kit logic between QrScannerScreen and speed dial.

Improvements_Identified_For_Consolidation:
- Frame-clock test pattern for Compose animations on JVM.
- Container Gradle env exports (PerfData/tmpdir) — consolidate.
---