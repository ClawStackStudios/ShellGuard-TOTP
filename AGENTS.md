# This is the ShellGuard-TOTP project. Project rules are as follows.

ShellGuard-TOTP is a zero-knowledge, native Android authenticator for offline resilience and cryptographic synchronization with self-hosted ShellGuard / ClawStack vaults.

### Top-Level Invariables
- **Docs Bow to Code**: Code is physical ground truth (Tier 0). Docs are compiled projections. Disagreements are documentation defects.
- **Boundary Defense**: SQLCipher AES-256 at rest, KeyStore StrongBox biometrics, HKDF-SHA256 / AES-GCM-256 with strict AAD binding (`vault_pearls_totp:<id>`), and volatile RAM sanitization of foreign secrets.
- **Strict One-Way Mirror**: Remote servers act strictly as read-only mirrors. Local items (`is_local_only = 1`) never transmit upstream and survive sync pruning.
- **Cloud-First Verification Gate**: Heavy Gradle test suites and compilation run in cloud CI (`.github/workflows/ci.yml`). Local machines run instantaneous drift checks (`python3 scripts/audit_docs_drift.py`).

---

## 1. Project Meaning & Purpose

- **Audience**: Self-hosters, privacy advocates, and enterprise homelab operators needing zero-cloud, hardware-backed 2FA.
- **Differentiators**: Client-side zero-knowledge decryption, strict one-way mirror, volatile RAM sanitization (0% credential leakage), and cleartext LAN support (Unraid/TrueNAS).
- **Market Position**: Bridges closed-source cloud authenticators (Authy) and isolated offline utilities (Aegis) by pairing self-hosted multi-device synchronization with air-gapped zero-knowledge guarantees.
- **Mechanism**: HKDF-SHA256 + AES-GCM-256 AAD binding, Zetetic SQLCipher 4.6.1 (16 KB page-aligned), Android KeyStore biometric wrapper (`sg_totp_biometric_wrapper`), bullet-masked secret inputs (CWE-359), and clipboard scrubbing (30s).
- **Why This Architecture**:
  - *Client Derivation*: Prevents compromised servers from exposing vault seeds.
  - *One-Way Mirror*: Eliminates merge race conditions and prevents uploading personal OTPs to shared servers.
  - *Volatile RAM Sanitization*: Eliminates unencrypted disk traces when importing foreign backups (Bitwarden/Aegis/2FAS).
  - *Compose MVI*: Guarantees unidirectional data flow and eliminates fragment lifecycle leaks.
- **Existence Axiom**: Digital sovereignty over two-factor authentication keys.

---

## 2. Project Rules

### Immutable Invariants
1. **SSoT & Zero Deferred Docs**: When code behavior, DAOs, schemas, or routes change, update matching documentation in the same commit.
2. **Cloud-First Verification Gate**: Never run heavy Gradle builds locally. Audit locally (`python3 scripts/audit_docs_drift.py`), commit, push, and tail GitHub Actions (`gh run watch`).
3. **Storage Boundary Isolation**: Relational caches reside in SQLCipher Room; master keys and tokens reside in Android KeyStore `EncryptedSharedPreferences`. Never store raw keys in Room or plain XML.
4. **Local Vault Protection**: Manually added/scanned items default to `is_local_only = 1`. Remote sync workers must never push upstream and must preserve local items during deletion sweeps.
5. **IME Hardening (CWE-359)**: All cryptographic seed fields must apply bullet masking (`PasswordVisualTransformation`) and `KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)` to prevent predictive dictionary caching.
6. **Android 15+ 16 KB Page Alignment**: Packaging requires `jniLibs.useLegacyPackaging = false` in `app/build.gradle.kts`. All native binaries must be 16 KB boundary aligned.
7. **Cleartext LAN Permitted**: Cleartext HTTP via `res/xml/network_security_config.xml` is intentional for homelab setups. Never disable.
8. **Monotonic Versioning**: `versionCode` strictly increments (+1 integer); `versionName` tracks semantic versioning in `app/build.gradle.kts`. Releases mandate synchronizing the 9 central anchor files.

### Operational Guardrails
- **No Unprompted Mutations**: Never commit, bump versions, or push release tags without explicit developer confirmation.
- **Two-Layer Attribution**: Commit messages require Conventional Commits paired with `User:` (intent/spec) and `AI:` (concrete implementation/test) lines.
- **Safe Rebasing**: Never rebase shared branches (`main`). On feature branches, use `git push --force-with-lease`. Never use raw `git push --force`.
- **Zero Third-Party Leaks**: Prohibit analytics, tracking SDKs, and proprietary UI libraries. Native AndroidX, Compose, ML Kit, and Ktor only.

---

## 3. Project Patterns

- **MVI Unidirectional Flow**: `UI (Compose)` $\xrightarrow{\text{UserIntent}}$ `ViewModel` $\xrightarrow{\text{UseCase / Repo}}$ `StateFlow<State>`. UI contains zero business logic.
- **KeyStore Wrap & Biometric Gate**: Identity wrapped with AES-256-GCM via `AndroidKeyStoreHelper`; gated by `BiometricPrompt` with device credential fallback.
- **RAM Multi-Vault Intake Pipeline**: SAF `OpenDocument` stream $\to$ `MultiVaultBackupPreValidator` $\to$ non-TOTP credentials discarded in memory $\to$ normalized `ParsedTotpItem` hatched directly into SQLCipher.
- **Dynamic Material 3 Theming**: Bind to `MaterialTheme.colorScheme` and `LocalShellGuardColors`. Default accent is `ThemeAccent.REEF_DEFAULT` (`#E4048A`). Form roots apply `.imePadding()` and `.verticalScroll()`.
- **Cold-Restart Simulation**: Preferences and vault state must reload with 100% fidelity from disk; verified by recreating repository instances in unit tests.
- **Headless AGP In-Engine Signing**: CI decodes Base64 keystores via Python 3 and delegates signing directly to AGP (`bundletool`/`apksigner`), producing signed `.aab` and `.apk` artifacts.

---

## 4. Project Navigation

### Dynamic Cartography Directive
The agent possesses search tools and subagents. **Do not memorize or dump static file lists.** When assigned a task, dynamically map only the specific subsystem seams relevant to the task using targeted file search or research subagents.

### Architectural Subsystem Seams
- **Presentation Layer (`app/src/main/java/.../ui/`)**: Single Activity (`MainActivity`), Compose Navigation (`TotpNavHost`), ViewModels, screens, and design tokens (`theme/`). Form inputs require `.imePadding()` and bullet masking.
- **Domain & Engines (`.../engine/`, `.../data/migration/`)**: RFC 6238 HMAC-SHA1/256/512 calculation (`TotpEngine`), 1-second pulse (`TotpTicker`), and zero-knowledge RAM intake parsers (Bitwarden, Aegis, 2FAS, Google Auth).
- **Data & Persistence (`.../data/local/`, `.../data/sync/`, `.../data/backup/`)**: SQLCipher Room database, DAOs, entities (`is_local_only`), WorkManager mirror sync (`TotpSyncWorker`), and encrypted backup schema (`BackupManager`).
- **Cryptographic Core (`.../crypto/`)**: `ShellCryptionEngine` (HKDF key derivation, AES-GCM-256 AAD binding), `AndroidKeyStoreHelper`, and `EncryptedDeviceVault`.
- **Test Oracles (`app/src/test/`)**: 26 test suites (107 tests) asserting invariants (crypto, delta sync, cold-restarts, Room schemas). Tap the test oracle before concluding tasks.
- **Verification & CI**: Fast local pre-flight (`python3 scripts/audit_docs_drift.py`); cloud build runner (`.github/workflows/ci.yml`).

> [!WARNING]
> **`project/` Directory Guardrail**: The `project/` directory contains massive domain deep-specifications (~250 KB total). **DO NOT** traverse, scan, or map `project/` unless the current task explicitly requires that specific specification document. Map only what is strictly needed for the task, never because it is interesting.

---

## 5. Project Skills

Skills live in `.agents/skills/`. Inspect a skill's `SKILL.md` when operating in its domain or delegate specialized tasks to subagents:

- **`doc-automation`**: SSoT docs-to-code drift detection & 9-point release synchronization (`scripts/audit_docs_drift.py`, root docs).
- **`git-advanced-workflows`**: Clean rebasing, autosquash, worktree isolation, automated bisect via test oracle.
- **`android-16kb-sqlcipher-audit`**: 16 KB ELF segment page alignment audit & uncompressed JNI packaging (`app/build.gradle.kts`, R8).
- **`android-keystore-cold-restart-testing`**: StrongBox KeyStore validation & cold-restart test suite patterns (`crypto/`, `test/`).
- **`android-headless-signing-ci`**: Python Base64 keystore decoding & native AGP dual-signing (`.github/workflows/`, AGP).
- **`adb-ui-input`**: Android device ADB input automation & screenshot verification loops.
- **`appfunctions`**: Android AppFunctions system shortcuts & agent hooks.
- **`changelog-automation`**: Conventional Commit parsing & Keep a Changelog 1.1.0 automation (`CHANGELOG.md`).

*Forward-Looking Skills to Refine During Development*:
- **`zero-knowledge-migration`**: RAM-sanitized streaming decoders for foreign vault formats (`data/migration/`).
- **`one-way-mirror-sync`**: Read-only delta sync & local isolation reconciliation protocols (`data/sync/`).

---

## References

1. **Specifications**:
   - Technical Architecture: [`ARCHITECTURE.md`](ARCHITECTURE.md)
   - Visual & UX Design: [`DESIGN.md`](DESIGN.md)
   - Domain Deep-Specs (Read On-Demand Only): [`project/`](project/)
2. **Roadmap & Manifests**:
   - [`ROADMAP.md`](ROADMAP.md) • [`CHANGELOG.md`](CHANGELOG.md) • [`SECURITY.md`](SECURITY.md) • [`RELEASE-PLAY.md`](RELEASE-PLAY.md)
3. **Rules**:
   - [`.agents/rules/android-development.md`](.agents/rules/android-development.md) • [`.agents/rules/docs-hygiene.md`](.agents/rules/docs-hygiene.md) • [`.agents/rules/git-hygiene.md`](.agents/rules/git-hygiene.md) • [`.agents/rules/semantic-versioning.md`](.agents/rules/semantic-versioning.md)
4. **Skills**:
   - [`.agents/skills/doc-automation/SKILL.md`](.agents/skills/doc-automation/SKILL.md) • [`.agents/skills/git-advanced-workflows/SKILL.md`](.agents/skills/git-advanced-workflows/SKILL.md) • [`.agents/skills/android-16kb-sqlcipher-audit/SKILL.md`](.agents/skills/android-16kb-sqlcipher-audit/SKILL.md) • [`.agents/skills/android-keystore-cold-restart-testing/SKILL.md`](.agents/skills/android-keystore-cold-restart-testing/SKILL.md) • [`.agents/skills/android-headless-signing-ci/SKILL.md`](.agents/skills/android-headless-signing-ci/SKILL.md) • [`.agents/skills/adb-ui-input/SKILL.md`](.agents/skills/adb-ui-input/SKILL.md) • [`.agents/skills/appfunctions/SKILL.md`](.agents/skills/appfunctions/SKILL.md) • [`.agents/skills/changelog-automation/SKILL.md`](.agents/skills/changelog-automation/SKILL.md)
