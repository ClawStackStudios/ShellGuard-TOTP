# 🔐 Security Policy — ShellGuard-TOTP

ShellGuard-TOTP is a high-assurance, offline-first 2FA authenticator engineered for cryptographic sovereignty and zero-knowledge privacy. We take the security of your cryptographic seeds, authentication credentials, and hardware vault storage with the utmost seriousness.

---

## 🛡️ Supported Versions

Only the latest release milestone and actively maintained branches receive security updates and patches:

| Version | Build | Android Target | Support Status |
| :--- | :--- | :--- | :--- |
| **`v0.0.3.0`** | **Build 17** | **Android 14–16 (API 24–36)** | :white_check_mark: **Actively Supported (Latest)** |
| `v0.0.2.3` | Build 16 | Android 14–16 (API 24–36) | :x: Superseded |
| `v0.0.2.1` | Build 14 | Android 14–16 (API 24–36) | :x: Superseded |
| `v0.0.2.0` | Build 10 | Android 14–16 (API 24–36) | :x: Superseded (Milestone 2) |
| `< v0.0.2.0` | Build 1–9 | Android 14–16 (API 24–36) | :x: Unsupported |

---

## 🏛️ Cryptographic Invariants & Threat Model

ShellGuard-TOTP is designed to withstand local device theft, untrusted host environments, soft-keyboard keylogging, and malicious cloud backups.

### 1. Zero-Knowledge Ephemeral Seeds
- TOTP secret keys and Base32 seeds are decrypted strictly in ephemeral memory for the millisecond duration of RFC 6238 code computation.
- Decrypted seeds are never persisted unencrypted to flash storage, logs, or system cache, and are explicitly zeroed out when cleared from memory.

### 2. At-Rest Hardware Sealing
- **SQLCipher 4.6.1 AES-256**: All Room database tables (tokens, categories, audit logs) are encrypted at rest with 256-bit AES using SQLCipher. Fully compliant with Android 15+ 16 KB memory page-size kernels.
- **Android KeyStore (StrongBox / TEE)**: Cryptographic wrapper keys and database passphrases are generated inside hardware-backed secure elements (`KEY_ALIAS_PIN_WRAPPER`, `KEY_ALIAS_PASSWORD_WRAPPER`, `KEY_ALIAS_DB_WRAPPER`), inaccessible even to root users outside of KeyStore authentication.

### 3. Screen & IME Privacy Hardening
- **`FLAG_SECURE` Enforcement**: App windows dynamically enforce `FLAG_SECURE`, preventing unauthorized screen capture, screen-recording malware, and recent task switcher snapshot leaks.
- **IME Dictionary Isolation (CWE-359)**: All secret input fields enforce `KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)` with `PasswordVisualTransformation`, stopping third-party keyboards (e.g. Gboard, SwiftKey) from caching Base32 seeds in predictive autocomplete dictionaries.

### 4. Zero-Residue Emergency Panic Wipe
- `PanicTriggerReceiver` listens for broadcast `ACTION_PANIC_WIPE` to execute an instantaneous, irreversible purge across hardware KeyStore aliases, EncryptedSharedPreferences, Room database tables, and auth preferences before terminating the app process.

### 5. Cloud Backup Exclusion
- `res/xml/backup_rules.xml` and `res/xml/data_extraction_rules.xml` explicitly exclude encrypted Room databases (`shellguard_totp.db*`) and KeyStore preferences from unencrypted Android cloud backups, preventing Google Drive extraction while supporting encrypted device-to-device transfers.

---

## 🚨 Reporting a Vulnerability

We deeply appreciate responsible disclosure. If you discover a potential security vulnerability, cryptographic weakness, or privacy issue, please report it immediately:

1. **Private Reporting (Preferred)**:
   - Open a private security advisory via [GitHub Security Advisories](https://github.com/ClawStackStudios/ShellGuard-TOTP/security/advisories/new).
   - Or email: `security@clawstack.com` with a PGP-encrypted message (or clear subject `[SECURITY] ShellGuard-TOTP Vulnerability Report`).

2. **What to Include in Your Report**:
   - Detailed description of the vulnerability and its potential impact.
   - Affected version(s) and device environment (e.g. Pixel 8, Android 16).
   - Step-by-step reproduction steps or minimal Proof-of-Concept (PoC).
   - Proposed remediation or patch if available.

### ⏱️ Response & Disclosure Timeline
- **Initial Acknowledgment**: Within **48 hours** of report receipt.
- **Triage & Assessment**: Within **5 business days**, confirming severity and scope.
- **Remediation & Patch**:
  - Critical (remote code execution, seed leakage): Within **7 days**.
  - High/Medium: Within **14–30 days**.
- **Coordinated Public Disclosure**: Once a patched release is published on GitHub and the Google Play Store, or following a mutually agreed 90-day disclosure window.

---

## 🚫 Out of Scope

The following scenarios are considered outside the threat model of ShellGuard-TOTP:
- Attacks requiring physical device decapping, laser fault injection, or laboratory chip-off forensic analysis of the Secure Element.
- Devices running compromised custom ROMs where root privileges have been granted to arbitrary third-party applications.
- Social engineering attacks targeting the end-user's master PIN, password, or biometric enrollment.
- Denial-of-Service (DoS) attacks on self-hosted ShellGuard web sync gateways.
