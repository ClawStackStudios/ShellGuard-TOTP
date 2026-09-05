# Consolidated Learnings & Durable Patterns

## Test Oracle & Architectural Invariant Synchronization
**Pattern: Continuous Test Fixture Auditing**
- Whenever an architectural rule or data filtering logic changes (e.g. One-Way Mirror Sync excluding remote codes from backup exports, or Grouped Dashboard replacing filter chips), inspect all test fixtures in `app/src/test`.
- Audit both unit test expectations (`assertEquals`) and Compose UI test semantic finders (`onNodeWithTag`, `onNodeWithText`).
- *Rationale:* Prevents silent latent regressions from passing undetected until headless CI execution.

## Local Execution Environment & Java JBR Mapping
**Pattern: Android Studio JBR Java Runtime Provisioning**
- In environments where global `java` or `gradlew` is missing, configure:
  ```bash
  export JAVA_HOME="/config/Applications/android-studio/jbr"
  export PATH="$JAVA_HOME/bin:$PATH"
  ```
- Generate Gradle wrapper locally via cached binaries in `/config/.gradle/wrapper/dists/`.

## ClawKey Identity & Ingestion Deduplication
**Pattern: Pre-DAO Normalized Fingerprint Deduplication**
- SQLite `@Insert(onConflict = OnConflictStrategy.REPLACE)` conflicts solely on the primary key UUID.
- For backup imports and account ingestion, compute normalized fingerprint:
  `fingerprint = secret.uppercase().replace(" ", "").replace("-", "") + "_" + title.trim().lowercase()`
- Filter incoming entities against existing records before database insertion to prevent duplicate records.

## Containerized Android Gradle Testing & Launch Invariants
**Pattern: JBR JVM Flags for Virtualized/Containerized Environments**
- In Linux containerized development environments, Gradle and test worker JVMs can crash with `SIGBUS` in `PerfLongVariant::sample()+0x1b` or run into temp directory permission locks.
- Always apply container JVM isolation flags:
  ```bash
  export GRADLE_OPTS="-XX:-UsePerfData -Djava.io.tmpdir=$PWD/app/build/tmp"
  ```
- Also configure in `app/build.gradle.kts` test options:
  ```kotlin
  testOptions {
    unitTests.all { test ->
      test.jvmArgs("-XX:-UsePerfData")
      val testTmpDir = File(layout.buildDirectory.get().asFile, "tmp")
      testTmpDir.mkdirs()
      test.systemProperty("java.io.tmpdir", testTmpDir.absolutePath)
    }
  }
  ```
- *Rationale:* Completely bypasses the hotspot shared memory performance data file and isolates temp file operations to the build directory.

## One-Way Mirror Sync Defense-in-Depth
**Pattern: Multi-Layer Read-Only Invariants for Remote Mirrored Records**
- When an application acts as a one-way downstream mirror of a remote server:
  1. **Database Layer**: Implement conditional deletion queries (e.g. `TotpItemDao.deleteByIdIfLocal`) with SQL `WHERE id = :id AND (is_local_only = 1 OR owner_uuid = 'local')`. This atomically prevents deletion of remote synced records without requiring a pre-fetch read.
  2. **Domain / ViewModel Layer**: Reject local edits and deletions when `!item.isLocalOnly && item.ownerUuid != "local"`.
  3. **UI Layer**: Disable swipe-to-dismiss, hide edit modals, remove delete confirmation triggers, and display an explicit "Read-only" badge.
  4. **Client-Side Delta Decryption**: Snapshot local records (`getRemoteItemsOnce`) and compare remote `updated_at` timestamps against local entries; skip expensive AES-GCM-256 authenticated decryption for unchanged records while ensuring accurate pruning for server-deleted items.
- *Rationale:* Eliminates data corruption, prevents local modification of server-owned records, and minimizes battery/CPU consumption during synchronization.

## Google Play Console Release Invariants & Pre-Flight Gate
**Pattern: Release Invariant Checkpoints**
- Prior to creating release tags or uploading bundles:
  1. **Target SDK**: Mandate API 36 (`targetSdk = 36`, Android 16) per Google Play requirements.
  2. **16 KB Page Alignment**: Enforce SQLCipher 4.6.1+ and `packaging { jniLibs { useLegacyPackaging = false } }` to ensure uncompressed, page-aligned native `.so` binaries.
  3. **Monotonic Version Code**: Ensure `versionCode` strictly increments (+1) monotonically per upload (e.g. Build 7 ➔ Build 8).
  4. **Release Notes Character Limit**: Keep localized `<en-US>` release notes strictly under 500 characters in `RELEASE-PLAY.md`.
  5. **Strict Release Document**: Maintain `RELEASE-vX.Y.Z.N.md` at repo checkout root as the immutable source of truth for the CI pipeline.
- *Rationale:* Guarantees zero upload rejection on Play Console and prevents CI failure due to missing release notes.

## Robolectric Asynchronous Room DAO Testing
**Pattern: Looper Flushing & SQLite Polling**
- Testing asynchronous Room operations driven by coroutines on `Dispatchers.IO` in Robolectric requires advancing the main looper:
  ```kotlin
  shadowOf(Looper.getMainLooper()).idle()
  ```
- Pair with short polling loops (e.g. 20-50ms delays) inspecting in-memory SQLite tables to ensure IO continuations finish persisting state before making assertions.
- *Rationale:* Eliminates race conditions between Compose UI event handlers and Room SQLite transactions in headless test runs.

## Physical Device ADB Capture & Screencap Invariants
**Pattern: Display Wake & Coordinate Alignment**
- Physical Android devices subjected to screen capture via `adb exec-out screencap -p` must be explicitly awakened before capture:
  ```bash
  adb shell input keyevent KEYCODE_WAKEUP
  ```
- In Compose UI flows driven via `adb shell input tap`, active soft keyboards alter root coordinates; send `KEYCODE_BACK` to dismiss soft input before calculating tap coordinates or dumping hierarchy via `uiautomator`.
- *Rationale:* Prevents 0-byte or black screen captures resulting from aggressive display sleep timeouts, and eliminates touch miss-clicks on obscured targets.

## Google Play Console Listing Content Formatting
**Pattern: Zero-Markdown Plain-Text Normalization**
- Google Play Console full description fields (4,000 char max) reject markdown tags (no `*`, `**`, `#`, or backticks), displaying them verbatim to users.
- Format all store listings using pure plain text with standard Unicode bullets (`•`), blank line delimiters, and clean uppercase headers (e.g., `CORE FEATURES`).
- *Rationale:* Ensures professional visual rendering and zero formatting leakage on the Play Store web and mobile client.

## CI/CD Selective Runner Allocation & Chained Mirroring
**Pattern: Multi-Trigger Gating for Heavy Android Pipelines**
- Android compilation (`bundleRelease`, `assembleRelease`) and signing workloads consume heavy CI runner minutes.
- Gate compilation jobs with granular commit/ref checks:
  ```yaml
  if: startsWith(github.ref, 'refs/tags/v') || github.event_name == 'workflow_dispatch' || contains(github.event.head_commit.message, '--release')
  ```
- Chain documentation mirroring jobs downstream using `needs: [release]` paired with `if: always() && ...` so that documentation edits (`RELEASE-v*.md`) mirror independently without compiling binaries, while full releases wait for successful compilation before mirroring release notes.
- *Rationale:* Conserves GitHub Actions runner quota while maintaining automated continuous documentation parity.

