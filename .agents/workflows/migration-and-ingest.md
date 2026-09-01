---
description: Operational workflow for executing zero-knowledge 2FA vault migrations, schema sanitization, and dual persistence routing.
---

# 📥 Vault Migration & Ingestion Workflow

> **Use When:** Implementing or testing third-party vault imports (Bitwarden Password Manager, Bitwarden Authenticator, Aegis, 2FAS, Google Authenticator) in ShellGuard-TOTP.  
> **Pair With:** `.agents/rules/zero-knowledge-migration.md` and `.agents/rules/lobsterized-philosophy.md`.

---

## 🔍 Step 1: Storage Access Framework (SAF) & Format Detection
1. Launch system file picker via `ActivityResultContracts.OpenDocument()`.
2. Inspect JSON root structure to identify format:
   - **Bitwarden Vault**: Contains `items` array with `login.totp` and `folders` array.
   - **Bitwarden Authenticator**: Flat JSON array with `issuer`, `name`, `key`, `algorithm`.
   - **Aegis / 2FAS / ShellGuard**: Structured schema envelope (`version`, `header`, `db`).
3. If encrypted (`encrypted: true`), prompt user for decryption password and derive key via PBKDF2/AES-256-CBC in RAM.

---

## 🧹 Step 2: Zero-Knowledge In-Memory Sanitization
1. Extract 2FA seeds: parse standard `otpauth://totp/...` URIs, raw Base32 keys, and `steam://` URIs.
2. If `steam://` URI is present, route to `SteamTotpGenerator` (26-char alphanumeric alphabet: `23456789BCDFGHJKMNPQRTVWXY`).
3. **MANDATORY PURGE**: Immediately purge and garbage collect all passwords (`login.password`), credit card numbers, secure notes, and personal identity fields in RAM.
4. Map `folderId` ➔ Pod category (defaulting uncategorized items to `"General"`).

---

## ⚔️ Step 3: Conflict & Duplicate Resolution
1. Cross-reference parsed tokens against existing items in Room DB via `TotpItemDao.getAllTotpItemsSync()`.
2. Present user with the `BitwardenImportPreviewDialog.kt`:
   - Display total token count, category breakdown, and duplicate count.
   - User selects Conflict Policy: `[ Skip Duplicates ]` (default), `[ Overwrite Existing ]`, or `[ Keep Both ]`.

---

## 🔀 Step 4: Dual-Pathway Persistence Routing
1. User selects destination: `[ 📱 Save to Local Vault Only ]` vs `[ ☁️ Save & Sync with Remote Gateway ]`.
2. **Local Pathway**:
   - Batch insert sanitized `TotpItemEntity` records into Room SQLCipher with `is_local_only = true`.
3. **Remote Gateway Pathway**:
   - Convert tokens to ShellGuard Pearl DTOs.
   - Encrypt each payload via `ShellCryptionEngine` (`huKey` + `userUuid` + AAD `vault_pearls_totp:{id}`).
   - Push batch upstream via Ktor client `POST /api/vault`.

---

## 📜 Step 5: Post-Commit Hooks & Verification
1. **Audit Log Hook**: Insert an immutable `AuditLogEntity` event into `security_audit_logs`:
   - `eventType: "IMPORT_SUCCESS"`, `description: "Imported X accounts (Y updated, Z skipped) ➔ [Destination]"`
2. **Auto-Backup Hook**: If `Automatic Backups` is enabled in `SettingsBackupsScreen.kt`, trigger `BackupManager.triggerAutomaticBackupIfEnabled()`.
3. Verify zero leakage: assert no password strings or temporary files exist on device disk or in memory dumps.
