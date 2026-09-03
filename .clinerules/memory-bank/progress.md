# ShellGuard TOTP — Progress

## Completed
- ✅ Release v0.0.1.0 (Build 7) published; CI green; 79/79 unit tests passing.
- ✅ RFC 6238 TOTP engine, ShellCryption (HKDF-SHA256 + AES-GCM-256 AAD), live epoch ticker.
- ✅ SQLCipher Room persistence, One-Way Mirror Sync, WorkManager worker.
- ✅ Grouped dashboard (Local Vault / Synced), unified `sgtotp.bak` export (Local Codes only).
- ✅ CameraX QR scanner + inline gallery image decode (QrScannerScreen).
- ✅ First-run Brand Hero intake, MultiVaultBackupPreValidator (Bitwarden/Aegis/2FAS/ShellGuard), zero-knowledge sanitization.
- ✅ Vault Security Screen, dynamic PIN/Password/Biometric protection, hardware wrapper keys.
- ✅ Spotlight tour (spring physics, +18dp cutouts), splash screen, adaptive icon, 6 accent palettes.
- ✅ Headless CI hardening (KeyStore HMAC fallback, FrameworkSQLiteOpenHelperFactory, -XX:-UsePerfData).

## In Progress
- ⏳ Phase 10: Expandable Speed Dial FAB (v0.0.1.2 Build 9) — **implementation complete, verified** (86/86 tests, build green). On-device live run + release tagging pending.

## Backlog (Roadmap)
- Phase 11: Categorized Settings Hub (v0.0.2.0)
- Phase 12: Security Suite, Panic Purge & Audit Logging (v0.0.2.1)
- Phase 13: Advanced Import/Export & Multi-QR (v0.0.3.0)
- Phase 14: Glance Widgets & Icon Packs (v0.1.0.0)
- Phase 15: ClawKey Vault Creation (v0.1.1.0) — spec'd, Tasks 29 & 30.

## Known Issues
- None currently; all tests green as of v0.0.1.0.