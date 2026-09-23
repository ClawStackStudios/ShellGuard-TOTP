# 🦞 ShellGuard TOTP — Release v0.0.2.3 (Build 16)

> **Phase 12: Security Suite, Panic Purge, Audit Logging & Web Server v0.0.2.3 Sync Parity**: versionCode 16 (`versionName = "0.0.2.3"`). Delivers advanced vault security controls (tap-to-reveal tokens, screen security toggle, emergency panic purge trigger integration), a local append-only security Audit Log recording cryptographic and access events, and complete protocol re-alignment with ShellGuard Web Server v0.0.2.3 (Build 25) restoring dynamic TOTP URI parsing during remote synchronization. Install over previous builds in place; vault data is preserved.

## *Phase 12: Security Suite, Panic Purge, Security Audit Logging & Web Server v0.0.2.3 Sync Parity*

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

Welcome to **v0.0.2.3** of **ShellGuard-TOTP** — **Phase 12: Security Suite, Panic Purge, Audit Logging & Web Server v0.0.2.3 Sync Parity (Build 16)**. This major release implements Tasks 23, 24, and 24b, establishing deep system security enhancements and repairing the remote synchronization bridge with the updated ShellGuard Web Server v0.0.2.3 (Build 25).

Vaults gain granular defense controls including configurable Tap-to-Reveal timeouts, dynamic `FLAG_SECURE` window management with user-facing risk disclosures, an emergency broadcast panic wipe receiver, and an append-only Room-backed security audit log. Concurrently, the remote sync intake pipeline has been upgraded with `TotpUriParser` to dynamically ingest RFC 6238 Key URIs carrying custom hash algorithms (SHA256/512), 8 digits, and custom intervals. Pre-flight verification passed 100% green across all 107 automated unit and Robolectric tests, verified live on both phone (Google Pixel) and tablet (Nexus 7) form factors.

---

## 💎 Key Themes & Highlights

### 🛡️ 1. Security Suite & Granular Privacy Controls (Tasks 23 & 24)

* **`SecurityPreferenceController`**: Encrypted SharedPreferences management for device-level security options (`allowScreenshots`, tap-to-reveal timeout duration, panic purge enabled).
* **Dynamic `FLAG_SECURE` Protection**: `MainActivity` reactively toggles window anti-snoop protection. Screen capture prevention is strictly enforced when the vault is locked or backgrounded regardless of setting, while allowing users an opt-in toggle with an explicit risk confirmation dialog when unlocked.
* **Tap-to-Reveal Timeout Selector**: Configure sensitive token visibility duration (`10s`, `30s`, `60s`) with automatic re-masking.

### 🚨 2. Emergency Panic Purge (`PanicTriggerReceiver`) (Task 23)

* **Broadcast-Triggered Self-Destruction**: Registered `ACTION_PANIC_WIPE` receiver listening for emergency device broadcasts.
* **Complete Sanitization**: Atomically destroys Android KeyStore hardware wrapper keys, purges the `EncryptedDeviceVault`, drops Room database tables, and clears all cached preferences within milliseconds.

### 📈 3. Room-Backed Security Audit Trail (Tasks 23 & 24)

* **Append-Only Event Store**: Upgraded `ShellGuardTotpDatabase` to schema version 2 with `AuditLogEntity` and `AuditLogDao`.
* **Reactive Timeline**: Live tracking of key cryptographic operations: vault unlocked, biometric pass/fail, backup exported/restored, and manual/QR secret additions.
* **Sub-Screen Experience**: `SettingsAuditLogScreen.kt` provides real-time search filtering, formatted timestamps, status chips, export share intents, and clear log dialogs.

### 🔄 4. Web Server v0.0.2.3 Dynamic TOTP Sync Parity (Task 24b)

* **Dynamic Key URI Parsing**: Decrypted seeds from Web Server v0.0.2.3 are parsed via `TotpUriParser.parse()`, seamlessly normalizing raw Base32 keys as well as `otpauth://totp/...` URIs. Dynamic parameters—including HMAC-SHA256, 8-digit codes, and 60-second time steps—are preserved without Base32 decoding errors.
* **Delta Fast-Filter**: Added `isContentIdentical()` fast-filter in `TotpRepository.syncRemoteVault` comparing candidate entity fields before running Room write transactions, eliminating database churn on server responses with null `updated_at`.

---

## 🧪 Verification Record

* **107 unit and Robolectric tests** passing 100% green (`./gradlew testDebugUnitTest`).
* **Clean build verification**: `./gradlew assembleDebug` succeeded.
* **Pre-flight invariants**: `targetSdk = 36`, `sqlcipher 4.6.1` (16 KB-aligned ELF segments), `jniLibs.useLegacyPackaging = false`, dynamic theming with `LocalShellGuardColors`.
* **Physical Device Walkthrough (ADB TLS)**:
  - **Google Pixel (Phone, 1080x1920)**: Verified PIN unlock, Settings navigation, Security sub-screen, and real-time audit logging.
  - **Nexus 7 (Tablet, 1200x1920)**: Verified first-run vault hatching, PIN unlock, and widescreen Settings layout fidelity.

---

## 🛡️ Security Posture — Hardened

Android KeyStore hardware backing, SQLCipher AES-256 at rest (Database v2), emergency panic purge broadcast, Room security audit logging, dynamic `FLAG_SECURE` window shield, zero-knowledge sync transport, Android 16 (API 36) + 16 KB page-size kernel ready.

---

*ClawStack Mobile Studios©™ — Build features around security, not security around features.*
