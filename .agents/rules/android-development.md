---
trigger: always_on
description: Comprehensive Android development invariants, MVI architecture, storage, security, 16 KB alignment, and headless JVM build standards for ShellGuard-TOTP.
---

# 🤖 Android Development & Architecture Invariants

> **Core Objective:** Establish immutable engineering guardrails for native Android development within the ShellGuard-TOTP codebase.

---

## 1. Platform Constraints & Headless Execution Environment

### Platform Boundaries
- **Client-Side Only**: No server runtime, no Firebase, no Google Workspace APIs. All network I/O is direct HTTP/HTTPS from the app process.
- **Single-Activity, Single-Module**: Use Compose Navigation for all screens. No Activity-per-screen patterns.
- **No NDK / C++**: Native code is strictly bundled via verified precompiled dependencies (e.g. SQLCipher).

### Subshell & Headless Build Environment Invariants
When executing Gradle, ADB, or JVM test commands in subshells or headless CI containers, never rely on default environment variables. Always explicitly export the bundled Android Studio JBR and platform tools:

```bash
export JAVA_HOME="/config/Applications/android-studio/jbr"
export PATH="$JAVA_HOME/bin:/config/Android/Sdk/platform-tools:$PATH"
export GRADLE_OPTS="-XX:-UsePerfData -Djava.io.tmpdir=$PWD/app/build/tmp"
```

- **JVM Flags**: `-XX:-UsePerfData` prevents memory crashes in containerized environments.
- **Temp Directory**: Isolating `java.io.tmpdir` to `app/build/tmp` prevents permission errors and file locks.

### Cloud CI Runner & SDK Invariants (GitHub Actions)
- **Avoid Legacy SDK Actions**: Never use third-party actions (such as `android-actions/setup-android@v3`) that invoke `sdkmanager tools`. Google has removed the legacy `tools` package from the remote SDK repository, causing exit code 1 build failures.
- **Runner-Native Android SDK**: GitHub Actions `ubuntu-latest` runners already pre-install the Android SDK at `/usr/local/lib/android/sdk` (`ANDROID_HOME`). Accept licenses directly via runner-native tooling:
  ```yaml
  - name: 🤖 Setup Android SDK
    if: steps.detect.outputs.tag != ''
    run: |
      yes | sdkmanager --licenses || true
  ```
- **CLI Release Observability**: When monitoring cloud releases or troubleshooting pipeline failures, use `gh` CLI commands:
  ```bash
  gh run list -L 5
  gh run view <run-id> --log-failed
  gh run view --job=<job-id>
  gh release view <tag>
  ```

---


## 2. Architecture & MVI Layer Boundaries

| Layer | Technology |
|---|---|
| **State** | MVI (Unidirectional Data Flow) |
| **DI** | Dagger Hilt |
| **Local DB** | Room 2.7+ (SQLCipher whole-database encryption) |
| **Sensitive Secrets** | EncryptedSharedPreferences (Jetpack Security, Android Keystore) |
| **UI Framework** | Jetpack Compose + Material 3 (no custom external widget libraries) |

### Layer Data Flow
```
UI (Compose) ──(UserIntent)──> ViewModel ──> UseCase ──> Repository ──> Data Source
     ▲                                                                       │
     └────────────────────────── StateFlow<State> ───────────────────────────┘
```

- **UI (Compose)**: Renders immutable `State`, emits `UserIntent` actions. Zero business logic.
- **ViewModel**: Exposes `StateFlow<State>`, handles `UserIntent`. No Android framework imports beyond Compose.
- **UseCase**: Single-responsibility domain operations. Pure Kotlin, no Android imports.
- **Repository**: Data abstraction layer. Manages local caching vs. remote synchronization.
- **Data Source**: Concrete I/O (Room DAOs, Ktor HTTP client, KeyStore vault).

---

## 3. Storage & Cryptographic Boundaries

- **Room Database**: All user data that benefits from relational querying, searching, and sorting (records, audit logs, caches). Encrypted at rest via SQLCipher.
- **EncryptedSharedPreferences** (Jetpack Security + Android KeyStore): Master identity keys, session tokens, and PIN hashes. Never store raw identity keys or unencrypted master secrets in Room.
- **Ephemeral State**: Every user-facing toggle or setting must be backed by persistent storage (`SharedPreferences` or `EncryptedDeviceVault`), never ephemeral Compose `remember` state alone.

---

## 4. UI, Theming, and IME Hardening

- **Dynamic Theming**: Never hardcode static brand color tokens (e.g. `ClawCyan`) in screen composables; bind strictly to `MaterialTheme.colorScheme` and `LocalShellGuardColors`.
- **Default Theme Accent**: The canonical default theme accent is `ThemeAccent.REEF_DEFAULT` (Reef Pink `#E4048A`).
- **Soft Keyboard & Scrolling**: All interactive form/input screens must apply `.imePadding()` and `.verticalScroll(rememberScrollState())` to prevent the soft keyboard from obscuring inputs.
- **Sensitive Key Masking & IME Protection (CWE-359)**:
  All cryptographic, seed, or secret input fields (e.g. Base32 secret keys, master passwords, PINs) MUST apply:
  - `PasswordVisualTransformation()` (paired with an accessible toggleable visibility eye icon).
  - `KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)` to prevent predictive dictionary learning and third-party keyboard telemetry caching.

---

## 5. Network & Cleartext HTTP Specification

Cleartext HTTP is **intentional** for this project to support local Unraid/TrueNAS and home lab servers where TLS is not available.
- Do NOT flag, "fix", or remove cleartext HTTP.
- Configured via `res/xml/network_security_config.xml`:
  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <network-security-config>
      <base-config cleartextTrafficPermitted="true" />
  </network-security-config>
  ```
- Referenced in `AndroidManifest.xml` via `android:networkSecurityConfig="@xml/network_security_config"`.

---

## 6. 16 KB Memory Page-Size Alignment & Packaging

Android 15+ (API 35/36) mandates 16 KB page-aligned native binaries:
- Use SQLCipher `4.6.1+` compiled with 16 KB ELF segment alignment.
- Configure `jniLibs.useLegacyPackaging = false` in `app/build.gradle.kts` to store `.so` libraries uncompressed and page-aligned inside APKs/AABs.

---

## 7. One-Way Mirror Sync & Local Code Integrity

- **Read-Only Sync**: Remote connections are strictly read-only mirrors of the ShellGuard Web Server. Upstream writes or pushes are prohibited.
- **Local Creation**: Any TOTP secret created manually or scanned via QR within the Android app is strictly a Local Code (`isLocalOnly = true`).
- **Unified Backup Integrity**: `BackupManager` exclusively exports Local Codes into the `sgtotp.bak` unified schema, skipping remote codes to prevent cross-ecosystem duplication.
- **Grouped UI Separation**: The dashboard must present Local Codes and Remote Codes in visually distinct, vertically grouped sections rather than a single list with toggle filters.
- **Dynamic URI Extraction**: `TotpRepository` delegates payload parsing through `TotpUriParser.parse()` to handle both raw Base32 seeds and `otpauth://` URIs.
- **Pre-Upsert Identity Fast-Filter (`isContentIdentical`)**: Remote items are compared against local cached entities prior to DB writes, preventing redundant re-encryption cycles.

---

## 8. ClawKey Identity & Deduplication

- **Sovereign Key Format**: The ShellGuard ClawKey format is strictly `hu-` followed by 64 hexadecimal characters (total length: 67).
- **Single Source Validator**: All ClawKey input surfaces (Vault creation, Lock screen, Settings import) must use `ClawKeyValidator.isValid()`.
- **Pre-DAO Fingerprint Deduplication**: Backup import engines must deduplicate incoming records by normalized `secret` + `title` fingerprint prior to DAO insertion, preventing duplicate UUID false negatives.

---

## 9. Robolectric & Headless KeyStore Testing Invariants

- **KeyStore Headless JVM Fallback**: KeyStore wrapper classes (`AndroidKeyStoreHelper`, `EncryptedDeviceVault`) must provide a fallback mechanism to HMAC-derived `SecretKeySpec` for headless JVM unit tests when `AndroidKeyStore` is absent.
- **Robolectric Framework SQLite Open Helper**: When configuring Room databases (`ShellGuardTotpDatabase`), always detect Robolectric via `Class.forName("org.robolectric.Robolectric")` and assign `FrameworkSQLiteOpenHelperFactory()` to prevent host `UnsatisfiedLinkError` crashes against native SQLCipher binaries.
- **Test Oracle Audit on Refactor**: Whenever an architectural rule or UI layout changes, all existing test classes in `app/src/test` MUST be audited for obsolete assertions. Never push or release without verifying that test fixtures reflect current storage and UI invariants.

---

## 10. Monotonic Versioning & Release Verification

- Dynamic binding: User-facing version labels must bind dynamically to `BuildConfig.VERSION_NAME`.
- Google Play monotonicity: Every release bundle requires a strictly incremented monotonic `versionCode` (+1).
- Pre-release gate: Full test suite (`./gradlew testDebugUnitTest`) and build compilation (`./gradlew assembleDebug`) must pass green before committing or version bumping.
