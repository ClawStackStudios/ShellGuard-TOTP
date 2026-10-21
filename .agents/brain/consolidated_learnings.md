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

## Automated Bot PR Forensic Verification
**Pattern: Tree-Hash & Byte Diff Auditing for Automated Patches**
- Automated bots (e.g. Sentinel, Dependabot, AI PR agents) can create commits that appear to resolve security vulnerabilities in their title and description, but contain empty tree changes (`0 files changed`).
- Before accepting or branching from automated security PRs:
  - Run `git show --stat <commit>` and `git diff <base>...<head>` to verify actual changed code bytes.
  - Verify tree hash divergence: `git rev-parse HEAD^{tree}` vs `git rev-parse <base>^{tree}`.
- *Rationale:* Prevents false-sense-of-security closures where vulnerabilities remain unpatched despite merged "fix" commits.

## Sensitive Key Masking & IME Dictionary Protection (CWE-359)
**Pattern: Keyboard Hardening for Cryptographic Secrets in Jetpack Compose**
- In Android Jetpack Compose, sensitive credential inputs (Base32 2FA secrets, master passwords, recovery keys) must not only be masked visually but also isolated from IME dictionary recording.
- Apply two-layer protection:
  1. **Visual Obfuscation**: Apply `PasswordVisualTransformation()` by default, paired with an accessible visibility toggle `IconButton` that toggles `VisualTransformation.None`.
  2. **IME Dictionary Hardening**: Explicitly supply `KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)` to instruct the soft keyboard (e.g. Gboard) not to cache cryptographic seed characters in predictive learning dictionaries.
- *Rationale:* Eliminates CWE-359 privacy leakage where sensitive keys appear in future keyboard autocomplete suggestions.

## Physical Device Coordinate Resolution via UIAutomator
**Pattern: Deterministic Hardware Coordinates via XML Tree Hierarchy**
- Avoid estimating touch coordinates from scaled image screenshots or relative proportions, especially on screens using `.imePadding()` or dynamic scroll containers.
- Extract deterministic bounding boxes:
  ```bash
  adb shell uiautomator dump /sdcard/window_dump.xml
  # Parse [x1,y1][x2,y2] bounds and calculate center: x=(x1+x2)//2, y=(y1+y2)//2
  ```
- In Android ash shell, characters like `(`, `)`, `&`, `;`, `<`, `>`, `*`, `|` trigger syntax errors in `input text`. Replace whitespace with `%s` and escape shell metacharacters.
- *Rationale:* Ensures 100% reliable automated UI testing without flakiness across varying display resolutions or keyboard states.

## Multi-Document Release Synchronization Protocol
**Pattern: The 8-Point Documentation Walk**
- Every completed phase or release milestone touches a distributed documentation mesh across the repository:
  1. `app/build.gradle.kts`: Monotonic `versionCode` and `versionName`.
  2. `ROADMAP.md`: Check off completed tasks (`[x]`), update `current_position`, `features_completed: %`, and re-align monotonic downstream build projections (`Phase X ➔ Build Y`).
  3. `project/meta-prompt-ai-studio.md`: Re-index upcoming stage headers and prompts to match adjusted build tags.
  4. `CHANGELOG.md` (Root): Detailed Keep a Changelog entry for public inspection.
  5. `RELEASE-vX.Y.Z.N.md`: Release trigger file for GitHub Actions CI.
  6. `RELEASE-PLAY.md`: Store notes strictly <500 characters.
  7. `README.md`: Version badge, test oracle count, and feature bullets.
  8. Storage/Crypto Specs: Room schema version, DAO updates, Keystore/Vault zeroization methods.
- *Rationale:* Eliminates documentation rot and ensures release pipelines, developer roadmap, and public notes remain synchronized.

## Dual-Device Multi-Form-Factor Verification (Phone + Tablet)
**Pattern: Cross-Form-Factor UI Integrity Testing**
- Testing mobile Compose interfaces solely on a phone screen obscures tablet regressions (e.g. over-stretched text fields, distorted dialog bounds, off-center buttons).
- Target both attached devices over ADB TLS:
  - Phone: Google Pixel (`1080x1920`)
  - Tablet: Nexus 7 (`1200x1920`)
- Inspect window sizes via `wm size` and capture side-by-side screencaps to verify responsive layout behavior before declaring UI tasks complete.
- *Rationale:* Guarantees production visual quality across varied display densities and aspect ratios.

## Runner-Native Android SDK Provisioning & CLI Release Observability
**Pattern: Zero-Dependency CI SDK Setup & Terminal Build Monitoring**
- Third-party GitHub Actions wrappers like `android-actions/setup-android@v3` fail when upstream SDK packages are removed (e.g. `sdkmanager tools`).
- Modern GitHub Actions `ubuntu-latest` runners already pre-install the Android SDK at `/usr/local/lib/android/sdk` (`ANDROID_HOME`). Always use native runners with:
  `yes | sdkmanager --licenses || true`
- Monitor, diagnose, and verify cloud releases directly from the shell using GitHub CLI:
  - `gh run list -L 5` (track queued/running workflows)
  - `gh run view <run-id> --log-failed` (instant terminal stacktrace on CI failures)
  - `gh release view <tag>` (confirm published release assets and download URLs)
- *Rationale:* Eliminates CI breakage from unmaintained third-party actions and removes the need to leave the terminal to inspect build failures.

## Conventional Commits & Automated Changelog Invariants (Keep a Changelog 1.1.0)
**Pattern: Deterministic Commit-to-Changelog Grouping**
- Keep a Changelog 1.1.0 requires 6 canonical categories: `Added`, `Changed`, `Deprecated`, `Removed`, `Fixed`, and `Security`.
- Map commit types deterministically:
  - `feat` ➔ `Added`
  - `refactor`, `perf` ➔ `Changed`
  - `revert` ➔ `Removed`
  - `fix` ➔ `Fixed`
  - `fix(security)`, `fix(crypto)`, CWE remediations ➔ `Security`
  - `chore`, `docs`, `style`, `test`, `ci`, `build` ➔ Excluded from public user changelogs (or grouped into Maintenance).
- Every version heading must strictly use `## [X.Y.Z.N] - YYYY-MM-DD` and maintain comparison anchors at the footer (`[X.Y.Z.N]: https://.../compare/vPREV...vX.Y.Z.N`).
- *Rationale:* Eliminates arbitrary section naming drift and enables automated tools like `git-cliff` to parse history without configuration errors.

## Conventional Commits & Two-Layer Attribution Harmonization
**Pattern: Standards-Compliant AI Pair Attribution**
- In human/AI pair programming systems requiring two-layer accountability (`User:` intent / `AI:` implementation), maintain standard Conventional Commit line-1 headers:
  ```
  <type>(<scope>): <summary>

  User: <intent, system design, architectural decision, or glue provided>
  AI: <concrete implementation, functions, refactors, or tests generated>
  ```
- Breaking changes must be declared with `!` after the type/scope (`feat(api)!: ...`) or in a footer `BREAKING CHANGE: <explanation>`.
- *Rationale:* Concurrently satisfies strict Git linters (commitlint, git-cliff, standard-version) while enforcing human-AI accountability standards.

## Release Notes Multi-Audience Triad
**Pattern: Three-Tier Synchronized Release Artifacts**
- Every software release targets three distinct consumers with differing format constraints:
  1. **Public GitHub Release**: Markdown formatted with category headers (`Added`, `Fixed`, `Security`), contributor mentions, and compare links.
  2. **Internal Milestone Document (`RELEASE-vX.Y.Z.N.md`)**: Comprehensive technical dossier including architectural summary, highlights, pre-flight test gate counts (e.g. 107/107 tests green), and schema/dependency deltas.
  3. **Google Play Console (`RELEASE-PLAY.md`)**: Plain text strictly under 500 characters within `<en-US>` tags, with zero markdown syntax.
- *Rationale:* Prevents Play Store binary upload rejections while giving developers deep technical context and public users clean change summaries.

## Deterministic Documentation Invariants & The 8-Point Root Mesh
**Pattern: The Docs Bow to Code SSoT Law**
- Documentation is a compiled projection of physical codebase truth. If code and docs disagree, docs are defective by definition.
- Synchronize across the 8-point root mesh on every release or architectural milestone:
  1. `app/build.gradle.kts`: Monotonic `versionCode` and semantic `versionName`.
  2. `ROADMAP.md`: Check off completed tasks, update `current_position`, preserve downstream monotonic build numbers.
  3. `CHANGELOG.md`: Keep a Changelog 1.1.0 with compare link anchors.
  4. `RELEASE-vX.Y.Z.N.md`: Milestone technical summary and test oracle count.
  5. `RELEASE-PLAY.md`: `<en-US>` block (<500 chars, zero markdown tags).
  6. `README.md`: Version badges, test count badges, tech stack, and documentation index.
  7. `project/*.md` & `ARCHITECTURE.md`: Room schemas, KeyStore aliases, wire contracts.
  8. `SECURITY.md`: Supported versions table, cryptographic threat model, and SLAs.
  9. `.agents/memory-bank/`: Sliding active context and progress metrics.
- Enforce via automated verification script `scripts/audit_docs_drift.py`.
- *Rationale:* Eliminates documentation rot and establishes documentation synchronization as an automated, non-negotiable Definition of Done.

## Root Security Policy (SECURITY.md) & Coordinated Disclosure Invariants
**Pattern: Public Cryptographic Posture & Supported Versions**
- For high-assurance security tools, `SECURITY.md` serves as a contractual baseline for threat boundaries and coordinated disclosure:
  1. **Supported Versions Alignment**: The active version in `SECURITY.md` must strictly match `versionName` in `app/build.gradle.kts`.
  2. **Cryptographic Defense Invariants**: Explicitly document active protections (SQLCipher 4.6.1 AES-256, Android KeyStore StrongBox keys, dynamic `FLAG_SECURE`, IME dictionary protection CWE-359, panic purge receiver).
  3. **Responsible Disclosure SLA**: Declare 48h acknowledgment and severity-based patch targets with private disclosure channels.
- *Rationale:* Sets clear boundaries for security researchers and prevents vulnerability reports for out-of-scope scenarios (e.g. physical decapping or rooted devices).

## Advanced Git History Engineering & Autosquash
**Pattern: Zero-Overhead Pre-PR History Polish**
- Maintain atomic Conventional Commits on local feature branches without tedious manual rebase editing:
  1. **Tag Iterative Fixes**: When amending an earlier commit during development, use `git commit --fixup <commit-hash>`.
  2. **Automated Squashing**: Run `git rebase -i --autosquash $(git merge-base HEAD main)`. Git automatically reorders and marks fixup commits.
  3. **Headless Bypass**: In containerized or non-interactive environments, pass `GIT_SEQUENCE_EDITOR=:` to run autosquash without prompting.
  4. **Safe Remote Sync**: Always push with `--force-with-lease`, never raw `--force`. Never rebase shared public branches.
- *Rationale:* Keeps git history clean, readable, and bisectable while eliminating manual rebase mistakes.

## Automated Bisect with Gradle Test Oracles
**Pattern: Binary Search Regression Hunting via Automated Exit Codes**
- When a bug or regression is detected across multiple commits, avoid manual checkout and verification cycles:
  ```bash
  git bisect start HEAD <known-good-tag>
  git bisect run ./gradlew testDebugUnitTest --no-daemon
  git bisect reset
  ```
- Gradle's exit code `0` maps to `good`, non-zero `1` maps to `bad`, allowing Git to isolate the introducing commit in O(log N) steps without human intervention.
- Always ensure the working tree is completely clean before invoking `git bisect`.
- *Rationale:* Reduces multi-hour regression debugging sessions to automated minutes and grounds regression identification in deterministic test oracle evidence.

## Cloud CI Verification Gate & Host Resource Preservation
**Pattern: Zero-Local-Load Build and Test Offloading via GitHub Actions**
- In resource-constrained or containerized environments where Gradle and Robolectric JVM test runs cause memory starvation, system lag, or CPU exhaustion, do not force local execution.
- Implement a dedicated Continuous Integration workflow (`.github/workflows/ci.yml`) on `ubuntu-latest` cloud runners with Gradle caching (`gradle/actions/setup-gradle@v4`).
- Offload the verification pipeline:
  1. Instant local drift check: `python3 scripts/audit_docs_drift.py`
  2. Commit and push branch to GitHub.
  3. Tail and monitor execution from the terminal: `gh run list -L 1` and `gh run view --log-failed`.
  4. Ensure headless debug keystores fall back safely when `debug.keystore` is not committed (`val debugKeystoreFile = file("${rootDir}/debug.keystore"); if (debugKeystoreFile.exists()) ...`).
- *Rationale:* Completely eliminates developer machine freezes, speeds up iteration, and leverages cloud compute for heavy Android compilation and multi-test execution.
