# 🦞 ShellGuard TOTP — Release v0.0.3.0 (Build 17)

> **Phase 13 & 13.5: Advanced Import/Export, Migration Polish, Google Authenticator Multi-QR & OWASP Step-Up Auth**: versionCode 17 (`versionName = "0.0.3.0"`). Delivers a unified multi-vault ingestion engine (Bitwarden Vault, Bitwarden Auth, Aegis, 2FAS) with zero-knowledge volatile RAM sanitization, Google Authenticator 10-token QR migration export carousels via pure Kotlin protobuf and ZXing, hardware-backed step-up authentication under OWASP MASVS-AUTH-1 (BiometricPrompt + Master PIN/Password fallback challenge dialog) with dynamic `FLAG_SECURE` window protection, offline-first YAGNI sync (`PENDING_SYNC`), native SQLCipher 16 KB debug symbol tables (`ndk.debugSymbolLevel = "SYMBOL_TABLE"`), and 100% SSoT documentation synchronization. Install over previous builds in place; vault data is preserved.

## *Phase 13 & 13.5: Advanced Import/Export, Google Authenticator Multi-QR & OWASP MASVS-AUTH-1 Step-Up Security*

```text
███████╗██╗   ██╗███████╗██╗     ██╗              ██████╗   ██╗   ██╗   █████╗    ██████╗     ██████╗ 
██╔════╝██║   ██║██╔════╝██║     ██║              ██╔═══╝   ██║   ██║  ██╔══██╗  ██╔══██╗    ██╔══██╗
███████╗███████║█████╗   ██║     ██║              ██║ ███╗  ██║   ██║  ███████║  ██████╔╝    ██║   ██║
╚════██║██╔══██║██╔══╝   ██║     ██║              ██║   ██║  ██║   ██║  ██╔══██║  ██╔══██╗    ██║   ██║
███████║██║   ██║███████╗███████╗  ╚██████╔╝╚██████╝  ██║   ██║  ██║   ██║   ██████╔╝
╚══════╝╚═╝  ╚═╝╚══════╝╚══════╝╚══════╝    ╚═╝   ╚═╝   ╚═╝   ╚═╝   ╚═╝   ╚═╝
                                                  ~ **ClawStack Mobile Studios©™** ~
```

---

## 🚀 The Core Summary

Welcome to **v0.0.3.0** of **ShellGuard-TOTP** — **Phase 13 & 13.5: Advanced Import/Export, Migration Polish & OWASP Step-Up Auth (Build 17)**. This milestone implements Tasks 25, 25b, 26, and 26.5, expanding ShellGuard-TOTP's interoperability footprint to support frictionless migration from major industry authenticators while reinforcing our zero-knowledge boundary defense.

Users can now seamlessly migrate accounts from Bitwarden (Vault & Authenticator JSON), Aegis, and 2FAS backups with zero credential leakage—foreign passwords and personal notes are purged in volatile RAM before ever reaching disk. Vaults can also be exported to Google Authenticator using pure Kotlin protobuf varint serialization, rendering interactive 10-token QR code carousels via ZXing. Under OWASP MASVS-AUTH-1, sensitive export surfaces are fortified with mandatory step-up authentication across all device configurations (`BiometricPrompt` when available, falling back to a Master PIN/Password challenge dialog when biometrics are absent or disabled), wrapped in dynamic `FLAG_SECURE` window protections.

---

## 💎 Key Themes & Highlights

### 📥 1. Unified Multi-Format Intake Engine & Volatile RAM Sanitizer (Task 25)

* **Multi-Format Ingestion**: `MultiFormatMigrationEngine` normalizes incoming backups from Bitwarden Vault JSON, Bitwarden Authenticator JSON, Aegis JSON, and 2FAS JSON into standard `ParsedTotpItem` models.
* **Zero-Knowledge RAM Sanitization**: Strips passwords, secure notes, card data, and non-2FA metadata in volatile RAM, guaranteeing 0% credential leakage into local persistence.
* **Dual Vault Persister**: `MigrationPersister` provides selectable conflict resolution policies: `SKIP_DUPLICATES` (fingerprint deduplication), `OVERWRITE_EXISTING`, and `KEEP_BOTH`.

### 🔄 2. Google Authenticator Multi-QR Migration Carousel (Tasks 25 & 26)

* **Pure Kotlin Protobuf Serialization**: `GoogleAuthMigrationEncoder` implements Google Authenticator `otpauth-migration://offline?data=...` payload encoding using varint wire formats with zero heavy protoc runtime dependencies.
* **10-Token Chunking**: Automatically partitions large vaults into manageable 10-token batches for optimal camera scannability.
* **ZXing Barcode Rendering**: High-contrast, scannable 2D QR bitmaps wrapped in padded white containers for instant optical recognition.

### 🔒 3. OWASP MASVS-AUTH-1 Step-Up Authentication & Privacy Shield (Task 26.5)

* **Mandatory Step-Up Challenge**: Exporting plaintext secrets or viewing migration QR carousels strictly enforces step-up verification.
* **Biometric + PIN Fallback Pairing**: Challenges AndroidX `BiometricPrompt` when biometric hardware is enrolled and enabled; strictly falls back to a Master PIN / Password challenge `AlertDialog` validating against `authViewModel.unlockWithSecret()` when biometrics are unavailable or disabled in settings. Never fails open.
* **Dynamic `FLAG_SECURE` Window Shield**: `GoogleAuthExportViewerDialog` and step-up challenge dialogs enforce `FLAG_SECURE`, preventing screenshots or thumbnail leakage in the Android Recents menu.

### ⚡ 4. Offline-First YAGNI Sync & Summary Audit Logging (Task 26.5)

* **Offline-First Data Flow**: Bulk imports insert records directly into local SQLCipher Room storage with `syncState = PENDING_SYNC`. Network replication is delegated asynchronously to `TotpSyncWorker`, eliminating UI thread blocking and network failure vulnerabilities.
* **Compact Audit Log**: Consolidates mass import operations into a single structured summary row in `AuditLogEntity`, preventing database bloat during 200+ account migrations.

### 📦 5. Native Debug Symbol Tables & Packaging (Task 25b)

* **Play Console Symbol Resolution**: Configured `ndk { debugSymbolLevel = "SYMBOL_TABLE" }` in `app/build.gradle.kts` to package `libsqlcipher.so` native debug symbol tables directly inside release App Bundles (.aab).
* **16 KB Memory Page-Size Compliance**: Native binaries fully aligned to 16 KB segment boundaries for Android 15+ (API 35/36).

---

## 🧪 Verification Record

* **107 unit and Robolectric tests** passing 100% green (`./gradlew testDebugUnitTest`).
* **Clean build compilation**: `./gradlew assembleDebug` succeeded.
* **Deterministic doc audit**: `scripts/audit_docs_drift.py` returned `0` (100% compliance).
* **Cloud CI Verification Gate**: Verified in GitHub Actions runner `39015197453` (`ubuntu-latest`).
* **Packaging invariants**: `targetSdk = 36`, `sqlcipher 4.6.1` (16 KB page-aligned), `jniLibs.useLegacyPackaging = false`, dynamic theming with `LocalShellGuardColors`.

---

## 🛡️ Security Posture — Hardened

Android KeyStore StrongBox keys, SQLCipher AES-256 at rest (Room Database v2), OWASP MASVS-AUTH-1 step-up authentication with Master PIN fallback dialog, emergency panic wipe broadcast receiver, append-only security audit trail, dynamic `FLAG_SECURE` window shields, zero-knowledge volatile RAM sanitization, offline-first background synchronization, Android 16 (API 36) + 16 KB page-size kernel ready.

---

*ClawStack Mobile Studios©™ — Build features around security, not security around features.*
