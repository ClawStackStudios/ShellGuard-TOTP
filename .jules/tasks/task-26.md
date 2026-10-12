# 📋 Jules Task 26 — UI Migration Wizard, Google Auth Multi-QR & Settings Backups

> **Milestone**: Phase 13 / Stage 14 (Milestone 3) — `v0.0.3.0 (Build 17)`  
> **Lead Authority**: 🎓 ClawStack Studios Project Lead & Lead Build Agent  
> **Target Branch**: `feat/phase-13-advanced-import-export`  

---

## 🎯 Task Objective

Implement the complete user interface components and dialogs for Phase 13:
1. **GoogleAuthMigrationEncoder.kt**: Pure Kotlin Protobuf binary encoder serializing `MigrationPayload` (`otpauth-migration://offline?data=...`) in batches of 10 tokens.
2. **BitwardenImportPreviewDialog.kt**: Material 3 dialog showing summary counts, category breakdown, conflict resolution radio buttons (`Skip`, `Overwrite`, `Keep Both`), and destination selector (`[ 📱 Save to Local Vault Only ]` vs `[ ☁️ Save & Sync with Remote Gateway ]`).
3. **GoogleAuthExportViewerDialog.kt**: Paged QR code carousel for multi-account migrations with previous/next controls, page counter (`1 of 3`), brightness boost, and account count badge.
4. **SettingsBackupsScreen.kt**: Backups hub sub-screen with automatic backups toggle, backup reminder interval, and Android cloud backups toggle.
5. **Update `SettingsImportExportScreen.kt`**: Replace the Phase 13 placeholder card with functional action tiles triggering the new import wizards and export dialogs.
6. **Navigation Wiring (`TotpNavHost.kt` & `SettingsMetaScreen.kt`)**:
   - In `TotpNavHost.kt`, add `data object SettingsBackups : Screen("settings_backups")` and register its composable route.
   - In `SettingsMetaScreen.kt`, update `SettingsDestination` to include `BACKUPS`, and map the `"☁️ Backups"` category (line 139) to navigate directly to `SettingsBackupsScreen`.

---

## 🔒 Critical Invariants

1. **Granularity Ceiling**: Keep composable files modular (target ~250 lines, hard ceiling 500 lines). Decompose sub-components where needed.
2. **Offline-First Safety**: When in standalone offline mode (no gateway configured), disable the Remote Gateway destination option with an informative tooltip or chip.
3. **QR Code Readability**: Never bundle more than 10 accounts in a single QR code. Split larger sets into a paginated carousel (`GoogleAuthExportViewerDialog.kt`).
4. **Zero Non-TOTP Leakage**: Preview dialogs must display explicit user assurance copy: *"X 2FA accounts found. Passwords and personal notes are strictly excluded."*
