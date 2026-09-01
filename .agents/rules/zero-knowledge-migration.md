---
name: zero-knowledge-migration
description: Strict security and architectural invariants for third-party 2FA vault ingestion, memory isolation, Steam Guard translation, conflict resolution, and dual persistence routing.
---

# 🛡️ Rule: Zero-Knowledge Vault Migration & Ingestion

## Core Mandate
ShellGuard is a sovereign, zero-knowledge two-factor authentication vault. When ingesting external archives (Bitwarden Password Manager, Bitwarden Authenticator, Aegis, 2FAS, Google Authenticator), the application must enforce strict memory isolation, format parity, deterministic duplicate handling, and dual-vault routing.

---

## 🔒 1. Zero-Knowledge Memory Isolation
- **Volatile Purging**: Non-TOTP credentials (passwords, master passwords, credit cards, secure notes, personal fields) found within third-party vault exports MUST be strictly purged in volatile memory.
- **Zero Storage Leakage**: Non-TOTP data MUST NEVER be written to Room database tables, encrypted cache files, temporary files on disk, or logcat outputs.
- **Immediate Stream Release**: Files read via Android Storage Access Framework (SAF) `ContentResolver` must be closed and their in-memory buffers zeroized immediately after TOTP extraction.

---

## 🎮 2. Steam Guard 2FA Support
- **Alphanumeric Alphabet Parity**: Steam Guard 2FA utilizes a custom 26-character Base32 alphanumeric translation table (`23456789BCDFGHJKMNPQRTVWXY`).
- **Format Routing**: The URI parser (`TotpUriParser`) must detect `steam://` URIs and route them to `SteamTotpGenerator` to generate valid 5-character alphanumeric codes rather than failing numeric modulo validation.

---

## ⚔️ 3. Deterministic Conflict & Duplicate Policy
Every batch import flow must provide an explicit conflict resolution policy before database commit:
1. `SKIP_DUPLICATES` *(Default)*: Skips imported items if an exact secret or `title + username` already exists in the vault.
2. `OVERWRITE_EXISTING`: Updates the existing card's metadata (category/title/username) without creating duplicate cards.
3. `KEEP_BOTH`: Assigns a new UUID and appends a discriminator (e.g. `GitHub (Imported)`).

---

## 🔀 4. Dual-Pathway Persistence Routing
Every import operation must explicitly route sanitized items to the destination selected by the user:
- **Local Vault Pathway**: Batch inserted directly into Room SQLCipher with `is_local_only = true` and `sync_state = "LOCAL"`.
- **Remote Gateway Pathway**: Encrypted with `ShellCryptionEngine` (`huKey` + `userUuid` + AAD `vault_pearls_totp:{id}`) and pushed upstream to the self-hosted server gateway via `POST /api/vault`.

---

## 📜 5. Post-Commit Security Hooks
Upon successful completion of any batch migration:
1. **Audit Log Emission**: Immediately append an immutable record to `security_audit_logs` via `AuditLogDao`:
   `[IMPORT_SUCCESS]`: `"Imported X accounts (Y updated, Z skipped) ➔ Destination: [Local | Remote]"`
2. **Automated Backup Trigger**: If `Automatic Backups` is enabled in settings, invoke `BackupManager` to create an encrypted JSON backup snapshot.

---

## 📦 6. Proprietary Backup Formats (`.sgtotp.bak` & `.sgbak`)
- **Canonical Format**: The proprietary backup envelope format for ShellGuard TOTP is `.sgtotp.bak` (`format = "sgtotp.bak"`).
- **Ecosystem Parity**: Native `.sgbak` (ShellGuard Core) and standard JSON backup envelopes must always be supported.
- **Embedded Metadata**: Backup envelopes must serialize `protectionMode = "PIN" | "PASSWORD"`, `pinLength: Int?`, and `isBiometricEnabled: Boolean` to allow downstream clients to pre-configure appropriate unlock forms.
- **SAF Registration**: Document pickers and SAF launchers must register broad MIME filters: `arrayOf("*/*", "application/octet-stream", "application/json")`.

---

## 🔄 7. Sovereign Key Rotation at Import Time
- **Key Decoupling**: Decrypting an encrypted backup archive unlocks data using the historical backup key ($K_{backup}$).
- **Explicit User Choice**: The UI must present two explicit pathways upon decryption:
  1. `REUSE_SECRET` *(1-Tap Fast Track)*: Uses verified $K_{backup}$ directly to seal the local Android KeyStore vault.
  2. `ROTATE_KEY` *(Key Rotation)*: Generates a new master key ($K_{new}$) from a user-selected PIN or Master Password, encrypting all tokens under $K_{new}$ and immediately discarding $K_{backup}$ from memory.

---

## 🛡️ 8. BoringSSL / OpenSSL Cipher Error Interception
- **Exception Sanitization**: Low-level C++ BoringSSL or Java AEAD exceptions (`OPENSSL_INTERNAL:BAD_DECRYPT`, `AEADBadTagException`) MUST NEVER be surfaced directly to the user.
- **Multi-Salt Fallback**: Derivation logic must attempt all compatible salts (`ownerUuid`, `"local"`, default) before declaring failure.
- **Actionable User Feedback**: On unrecoverable tag failure, present clear UI feedback: *"Incorrect PIN or Master Password. Please check your secret and try again."*

---

## 📱 9. Dynamic Input Presentation & Physical Storage Advisory
- **Adaptive Keypad Presentation**: When PIN mode is detected in backup metadata, forms must automatically present `KeyboardType.NumberPassword` (numeric numpad) while maintaining a segmented toggle `[ 🔢 PIN Code ] [ 🔑 Password ]`.
- **Zero-Knowledge Physical Safe Storage Notice**: First-launch welcome and vault setup screens MUST prominently display a security notice instructing users to write down and physically secure their PIN or Master Password, explicitly warning that lost zero-knowledge secrets seal the vault forever without recovery backdoors.

