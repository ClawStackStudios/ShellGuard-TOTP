# 🦞 ShellGuard TOTP — Release v0.0.3.2 (Build 19)

> **Encrypted JSON Backup Hotfix & Stream Lifecycle Fix**: `versionCode` 19 (`versionName = "0.0.3.2"`). Resolves the `Export failed: Stream closed` error during encrypted vault export and restore, transitions canonical encrypted backups from `.sgtotp.bak` to standard `.json` (`shellguard-totp-backup.json`), preserves backward-compatible `.sgtotp.bak` restore support, and enforces fail-closed vault key validation. Install over previous builds in place; vault data is preserved.

---

## 🚀 Summary

This hotfix release resolves a critical stream lifecycle race in the Encrypted Backup & Restore flow (`SettingsImportExportScreen` + `AuthViewModel`) and standardizes encrypted vault exports on `.json` (`application/json`) for seamless portability across Android SAF, cloud drives, and the ShellGuard Web Server.

---

## 💎 Highlights

### 🔧 1. `Stream closed` Lifecycle Fix (Export & Restore)
* Moved `ContentResolver.openOutputStream` / `openInputStream` ownership into `AuthViewModel` inside `withContext(Dispatchers.IO)`, ensuring streams remain open for the full duration of `BackupManager` I/O.

### 📦 2. Standard `.json` Encrypted Backups
* `SettingsImportExportScreen` now exports `shellguard-totp-backup.json` (`application/json`) with `BackupEnvelope.format = "shellguard-totp-backup-v1.json"`.
* Existing `.sgtotp.bak` backups continue to restore cleanly on both Android and ShellGuard Web (`type = "shellguard-totp-backup-v1"`).

### 🛡️ 3. Fail-Closed Key Validation
* Removed the static `"shellguard_default_master_key"` fallback in `AuthViewModel.exportVaultBackup` and `importVaultBackup`; backup operations now fail closed if no vault or session key is active.

---

## 🧪 Verification Record

* **Cloud CI Verification Gate**: Passed 100% green on GitHub Actions (`run 44987326242` — `testDebugUnitTest` + `assembleDebug`).
* **Deterministic Doc Audit**: `python3 scripts/audit_docs_drift.py` passed 100%.

---

*ClawStack Mobile Studios©™ — Build features around security, not security around features.*
