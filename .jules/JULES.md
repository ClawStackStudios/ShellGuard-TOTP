---
name: jules
description: I am Jules. I do not perform for approval. I verify myself against the core structural truth of the codebase, and its rules.
---

# 🕵️ Jules — Fleet Briefing & Repository Guide

## 🏛️ Welcome to the Reef — What ShellGuard-TOTP Is & Why It Exists

Welcome, Jules! You are working on **ShellGuard-TOTP**, a sovereign, privacy-first, offline-first 2FA Authenticator Android application engineered with Jetpack Compose, AndroidX Room, and SQLCipher whole-database encryption.

> **Tagline**: *"Your reef. Your keys. Your rolling 2FA codes."*

### 🔒 Core Architectural Invariants
1. **Strictly 2FA Authenticator (Zero File Attachments)**: ShellGuard-TOTP is exclusively an RFC 6238 / RFC 4226 authenticator. There are **NO file attachments, documents, or arbitrary media** stored in this application. It stores *only* 2FA TOTP accounts (Base32 secret keys, account titles, usernames, algorithms, digits, and periods).
2. **Zero-Knowledge In-RAM Sanitization**: When ingesting external vaults (Bitwarden Password Manager, Aegis, 2FAS), the file export often contains passwords, credit cards, secure notes, and identities. Our ingestion engine (`BitwardenSanitizer.kt`) inspects the JSON entirely in volatile memory, extracts **only** `login.totp` seeds and Pod category names, and purges all passwords and notes. **Zero non-TOTP credentials ever reach disk or persistent state.**
3. **Hardware-Backed Encryption at Rest**: All local vault items are persisted inside SQLCipher 4.6.1 (`TotpItemEntity`) with 16 KB page-size compliance for Android 15+. Database encryption keys derive from Android KeyStore with StrongBox / TEE hardware backing.
4. **Dual Vault Routing**:
   - `📱 Local Vault`: `isLocalOnly = true`, persisted strictly on-device in Room SQLCipher.
   - `☁️ Remote Gateway Sync`: Transformed to Pearl DTOs, encrypted via `ShellCryptionEngine` (HKDF + AES-GCM-256 with AAD `vault_pearls_totp:{id}`), and synced with the user's self-hosted ShellGuard web server.
5. **Architectural Granularity Ceiling**:
   - **Target**: ~250 lines per file.
   - **Hard Ceiling**: 500 lines per file. Any Compose screen or service exceeding 500 lines must be cleanly decomposed into sub-components.
6. **Strict Two-Layer Attribution**: Every commit message must adhere to Conventional Commits with two-layer attribution:
   ```text
   <type>(<scope>): <summary>

   User: <directive / architectural intent>
   AI: <implementation details and test receipts>
   ```

---

## 🎛️ Full Repository Command Set & Dev Loop

### Inside Jules's Isolated VM
You are running in an isolated container VM equipped with JDK 17+ and the Android SDK. You have full permission to compile and run tests:

| Command | What It Does | When To Use |
| :--- | :--- | :--- |
| `./gradlew testDebugUnitTest --no-daemon` | Executes the complete JUnit 4 and Robolectric test suite (107+ tests). | Before every commit. Must be 100% green. |
| `./gradlew assembleDebug --no-daemon` | Compiles the full debug APK. | Verifies zero build or symbol errors. |
| `python3 scripts/audit_docs_drift.py` | Fast documentation and version drift audit script. | Fast pre-flight check. |

---

## 🧭 Active Milestone: Phase 13 / Stage 14 — Milestone 3
**Release Target**: `v0.0.3.0 (Build 17)`  
**Branch**: `feat/phase-13-advanced-import-export`  

You are executing **Phase 13 (Advanced Import/Export, Bitwarden Migration & Google Authenticator Multi-QR)** across staged waves:

- [ ] **Task 25 (`.jules/tasks/task-25.md`)**:
  - `MultiFormatMigrationEngine.kt` in `data/migration/`: Orchestrates Bitwarden Vault, Bitwarden Authenticator, Aegis, 2FAS, and encrypted Bitwarden JSON.
  - `MigrationPersister.kt`: Implements `ConflictPolicy` (`SKIP_DUPLICATES`, `OVERWRITE_EXISTING`, `KEEP_BOTH`), dual routing (Local vs Gateway), and emits `IMPORT_SUCCESS` to `AuditLogDao`.
  - `Task 25b`: Set `ndk.debugSymbolLevel = "SYMBOL_TABLE"` in `app/build.gradle.kts` release build type.
- [ ] **Task 26 (Wave 2-4 via PR comments)**:
  - Google Authenticator multi-QR Protobuf generator (`GoogleAuthMigrationEncoder.kt`).
  - `BitwardenImportPreviewDialog.kt`, `GoogleAuthExportViewerDialog.kt`, and `SettingsBackupsScreen.kt`.

**Remember**: Keep your focus strictly on 2FA TOTP accounts. Run your test gates and open your Pull Request!
