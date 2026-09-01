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
