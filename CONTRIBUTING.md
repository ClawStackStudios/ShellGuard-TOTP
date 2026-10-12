# 🛡️ Contributing to ShellGuard-TOTP

Thank you for contributing to **ShellGuard-TOTP**! This guide outlines the development environment, architectural principles, verification workflows, and git standards required to maintain the stability and security of the application.

---

## 🧭 Core Philosophy & Invariants

ShellGuard-TOTP is a **sovereign, zero-knowledge, offline-first 2FA authenticator** designed to pair with self-hosted ShellGuard servers or operate entirely standalone.

1. **Security Precedes Features**: We build features around security, not security around features.
2. **Zero Plaintext at Rest**: All database tables, indices, and data blocks are encrypted at rest with whole-database SQLCipher (AES-256). Sensitive identity keys and master secrets are managed strictly through the hardware-backed Android KeyStore (TEE / StrongBox).
3. **Zero External Telemetry**: We never embed third-party crash reporting, analytics, or tracking SDKs (no Firebase, Sentry, Datadog). System security events are logged on-device to an encrypted Room audit log (`AuditLogDao`) under the user's sovereign control.
4. **Strict One-Way Mirror Sync**: Remote server connections are read-only mirrors. The Android client never pushes modifications to remote codes, and local codes (`is_local_only = 1`) are strictly isolated and never sent upstream.
5. **16 KB Page-Size Kernel Alignment**: All native dependencies (SQLCipher 4.6.1+) and packaging rules are compiled for 16 KB page-size compatibility on Android 15+ (API 35/36).

---

## 💻 Development Prerequisites

- **Language**: Kotlin 2.0+
- **JDK**: Java 21 (Bundled Android Studio JBR recommended)
- **Target SDK**: Android 15 (API 35) / Android 16 (API 36 preview compatible)
- **Minimum SDK**: Android 7.0 (API 24)

### Headless & Subshell Environment Setup
When running Gradle or ADB commands in terminal subshells or containerized CI environments, always export the bundled JBR and container-safe JVM options:

```bash
export JAVA_HOME="/path/to/android-studio/jbr"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
export GRADLE_OPTS="-XX:-UsePerfData -Djava.io.tmpdir=$PWD/app/build/tmp"
```
* `-XX:-UsePerfData` prevents JVM memory mapping crashes in containerized environments.
* `-Djava.io.tmpdir` confines temporary build artifacts within the project tree to prevent permission locks.

---

## 🏛️ Architecture Overview

The codebase is built on **Single-Activity, Single-Module** architecture using Jetpack Compose and Unidirectional Data Flow (MVI):

```
UI (Compose) ──(UserIntent)──> ViewModel ──> Repository ──> Data Source (Room/KeyStore)
     ▲                                                                  │
     └────────────────────────── StateFlow<State> ──────────────────────┘
```

- **UI Layer (`ui/`)**: Declarative Jetpack Compose Material 3 screens. Strictly renders immutable state and emits actions. No direct business logic.
- **ViewModels (`ui/viewmodels/`)**: Exposes state via reactive `StateFlow` and handles incoming user intents.
- **Dependency Injection**: **Application-Scoped Lazy DI**. Singletons (`database`, `authRepository`, `totpRepository`, `backupManager`, `securityPreferenceController`) are lazily initialized in `ShellGuardTotpApp.kt`. We intentionally avoid Dagger/Hilt to maintain fast builds and zero annotation processor overhead.
- **Engine Layer (`engine/`)**: Pure cryptographic and RFC 6238 TOTP calculations (HMAC-SHA1, SHA256, SHA512, Steam Guard, rolling progress ticker).
- **Storage Layer (`data/`)**: Room 2.7+ encrypted via SQLCipher open helper factory, backed by hardware-derived AES keys in `AndroidKeyStoreHelper`.

---

## 🛡️ Input Hardening & CWE-359 Invariants

All screens accepting cryptographic secrets (Base32 keys, master passwords, PINs) MUST apply:
1. `PasswordVisualTransformation()` paired with a toggleable eye visibility icon.
2. `KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)` to prevent predictive dictionary learning and third-party keyboard telemetry caching.
3. `.imePadding()` and `.verticalScroll(rememberScrollState())` to prevent the soft keyboard from obscuring input fields.

---

## 🔄 The 3-Gate Verification Loop

We stack three verification gates before any code is merged or released:

### Gate 1: Tests (100% Green Required)
Run the full unit and Robolectric headless test suite:
```bash
./gradlew testDebugUnitTest --no-configuration-cache --no-daemon
```
* Note: Headless Robolectric tests automatically use `FrameworkSQLiteOpenHelperFactory` and headless HMAC KeyStore fallbacks to ensure native binaries do not crash host JVMs.

### Gate 2: Build Compilation
Verify clean build artifact generation:
```bash
./gradlew assembleDebug
```

### Gate 3: Live Verification
Verify interactive UX, the soft keyboard, and edge-to-edge layouts on a physical device or emulator over ADB:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.clawstack.shellguard.totp/.MainActivity
```

---

## 🌿 Git Hygiene & Branching

- **Branch Isolation**: Never work directly on `main`. Create a fresh branch for every task:
  ```bash
  git checkout -b feat/<short-desc>   # For new features
  git checkout -b fix/<short-desc>    # For bug fixes
  git checkout -b docs/<short-desc>   # For documentation updates
  ```
- **Secrets Safety**: NEVER commit keystores (`*.jks`, `*.keystore`), local paths (`local.properties`), or API secrets.
- **Two-Layer Attribution Format**: Every commit message must use this structured format:
  ```
  <type>: <short summary>

  User: <the intention, architecture direction, or design provided>
  AI: <the concrete implementation, refactoring, or test coverage applied>
  ```
  Example:
  ```
  feat(sync): implement delta-only remote credential sync

  User: Optimize remote sync to skip decryption when server timestamps match.
  AI: Integrated isContentIdentical fast-filter and remoteUpdatedAt timestamp comparator in TotpRepository.kt.
  ```

---

## 📦 Releases & Versioning

- Versioning strictly follows monotonic integer `versionCode` (+1 per release) and 4-digit `versionName` (`MAJOR.MINOR.PATCH.REVISION`) per `.agents/rules/semantic-versioning.md`.
- Automated cloud builds are triggered by annotated git tags (`vX.Y.Z.N`) via `.github/workflows/release.yml`, generating both signed Google Play `.aab` bundles and universal `.apk` packages.
