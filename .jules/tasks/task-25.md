# 📋 Jules Task 25 — Multi-Format Import Engine & Dual Vault Persister (Wave 1) [COMPLETED ✅]

> **Milestone**: Phase 13 / Stage 14 (Milestone 3) — `v0.0.3.0 (Build 17)`  
> **Lead Authority**: 🎓 ClawStack Studios Project Lead & Lead Build Agent  
> **Target Branch**: `feat/phase-13-advanced-import-export`  

---

## 🎯 Task Objective

Implement the complete zero-knowledge migration pipeline and dual vault persistence router in `com.clawstack.shellguard.totp.data.migration`:
1. **MultiFormatMigrationEngine.kt**: The unified orchestration entry point for ingesting Bitwarden Vault JSON, Bitwarden Authenticator JSON, Aegis Authenticator JSON, 2FAS JSON, and encrypted Bitwarden exports.
2. **MigrationPersister.kt**: Evaluates duplicate conflicts via `ConflictPolicy` (`SKIP_DUPLICATES`, `OVERWRITE_EXISTING`, `KEEP_BOTH`), routes items to either Local Vault (`is_local_only = 1`) or Remote Gateway (`POST /api/vault` via `ShellCryptionEngine`), and emits `IMPORT_SUCCESS` to `AuditLogDao`.
3. **Task 25b (Native Symbols)**: Add `ndk { debugSymbolLevel = "SYMBOL_TABLE" }` to `buildTypes.release` in `app/build.gradle.kts`.
4. **Comprehensive Unit Test Suite**: `MultiFormatMigrationEngineTest.kt` verifying 0% password/note leakage and correct conflict resolution.

---

## 🔒 Critical Invariants

1. **Zero Non-TOTP Credential Leakage**:
   - The engine must NEVER persist `login.password`, credit cards, identities, or secure notes to Room SQLCipher.
   - Only 2FA TOTP accounts (`login.totp` URIs or Base32 keys) and Pod categories (`folders[].name`) are retained in RAM and persisted.
2. **Steam Guard Support**:
   - `steam://` URIs must map to `algorithm = "STEAM"`, `digits = 5`, and generate valid 5-char alphanumeric codes using `TotpEngine.STEAM_CHARS`.
3. **Dual Routing**:
   - Destination `LOCAL_VAULT`: Inserts entities with `isLocalOnly = true`, `syncState = "LOCAL"`.
   - Destination `REMOTE_GATEWAY`: Converts to Pearl DTOs, encrypts via `ShellCryptionEngine` (`huKey` + `userUuid` + AAD `vault_pearls_totp:{id}`), and pushes upstream.

---

## 📋 File-by-File Specifications

### 1. `app/src/main/java/com/clawstack/shellguard/totp/data/local/dao/TotpItemDao.kt` (Prerequisite Query)
- Add a direct one-shot query to `TotpItemDao`:
  ```kotlin
  @Query("SELECT * FROM totp_items WHERE (:ownerUuid = 'local' AND owner_uuid = 'local') OR (:ownerUuid != 'local' AND (owner_uuid = :ownerUuid OR owner_uuid = 'local')) ORDER BY title COLLATE NOCASE ASC")
  suspend fun getAllTotpItems(ownerUuid: String = "local"): List<TotpItemEntity>
  ```
  *Rationale*: Provides `MigrationPersister` with a clean, suspendable snapshot of existing vault records without collecting an infinite Flow.

### 2. `app/src/main/java/com/clawstack/shellguard/totp/data/migration/MigrationPersister.kt`
- Implements `persistImportedTokens(tokens: List<ParsedTotpItem>, destination: ImportDestination, policy: ConflictPolicy)`:
  - Fetches existing items via `totpItemDao.getAllTotpItems(ownerUuid)`.
  - Maps duplicates by `secret` or `(title + username)`.
  - For `SKIP_DUPLICATES`: ignores matches.
  - For `OVERWRITE_EXISTING`: updates title, username, category, and `localUpdatedAt`.
  - For `KEEP_BOTH`: assigns new UUID and appends `" (Imported)"` to title.
  - Inserts/upserts into Room via `totpItemDao.upsertItems()`.
  - Emits audit log entry to `AuditLogDao`: `eventType = "IMPORT_SUCCESS"`.
  - Returns `ImportResult(added: Int, updated: Int, skipped: Int)`.

### 3. `app/src/main/java/com/clawstack/shellguard/totp/data/migration/MultiFormatMigrationEngine.kt`
- Orchestrates pre-validation and parsing:
  - Consumes `InputStream` or raw JSON string.
  - Uses `BitwardenSanitizer` for Bitwarden Vault & Authenticator formats.
  - Uses `AegisTwoFasParser` for Aegis and 2FAS formats.
  - **Compound Title Normalization**: If an item has a compound name (e.g. `"GitHub: lucas@work.com"` or `"AWS - admin"`) and `username` is null, split on `:` or `-` to extract clean `title` and `username`. Ensures clean UI display and high icon pack match rates.
  - Handles password-protected Bitwarden exports (`encrypted: true`) deriving AES-256-CBC key with PBKDF2 (600,000 iterations, SHA-256).
  - Returns `MigrationParseResult(items: List<ParsedTotpItem>, schemaType: BackupSchemaType, warnings: List<String>)`.

### 4. `app/build.gradle.kts` (Task 25b)
- Inside `android { buildTypes { release { ... } } }`, add:
  ```kotlin
  ndk {
      debugSymbolLevel = "SYMBOL_TABLE"
  }
  ```

### 5. `app/src/test/java/com/clawstack/shellguard/totp/MultiFormatMigrationEngineTest.kt`
- Add unit tests verifying:
  1. Plaintext Bitwarden Vault with mixed items (passwords, notes, and TOTP logins) -> extracts exactly TOTP tokens, zero passwords/notes retained.
  2. Standalone Bitwarden Authenticator JSON parsing.
  3. Steam Guard `steam://` URI ingestion and 5-character code generation.
  4. Conflict policy resolution (`SKIP_DUPLICATES`, `OVERWRITE_EXISTING`, `KEEP_BOTH`).

---

## 🧪 Verification & Acceptance Gate

Inside your isolated VM, run:
```bash
./gradlew testDebugUnitTest --no-daemon
./gradlew assembleDebug --no-daemon
```
Both commands must finish with `BUILD SUCCESSFUL` before opening the Pull Request.
