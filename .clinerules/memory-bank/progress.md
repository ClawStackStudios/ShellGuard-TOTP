# ShellGuard TOTP — Progress

## Completed
- ✅ Release v0.0.1.3 (Build 9) HOTFIX published — CI green (3m46s), signed .aab + .apk on GitHub Release. Patched v0.0.1.2 remote sync regression (`TotpRepository.classifyDeltaPearls` null-stamp invariant, self-healing) + synced card badge wrap; `DeltaSyncClassificationTest` added; 96/96 tests passing. Verified live on Pixel 8.
- ✅ Release v0.0.1.2 (Build 8) published — Expandable Speed Dial FAB, ML Kit ImageQrDecoder, one-way sync hardening; 90/90 tests at release.
- ✅ Release v0.0.1.0 (Build 7) published; CI green; 79/79 unit tests passing.
- ✅ RFC 6238 TOTP engine, ShellCryption (HKDF-SHA256 + AES-GCM-256 AAD), live epoch ticker.
- ✅ SQLCipher Room persistence, One-Way Mirror Sync, WorkManager worker (6h periodic).
- ✅ Grouped dashboard (Local Vault / Synced), unified `sgtotp.bak` export (Local Codes only).
- ✅ CameraX QR scanner + inline gallery image decode (QrScannerScreen).
- ✅ First-run Brand Hero intake, MultiVaultBackupPreValidator (Bitwarden/Aegis/2FAS/ShellGuard), zero-knowledge sanitization.
- ✅ Vault Security Screen, dynamic PIN/Password/Biometric protection, hardware wrapper keys.
- ✅ Spotlight tour (spring physics, +18dp cutouts), splash screen, adaptive icon, 6 accent palettes.
- ✅ Headless CI hardening (KeyStore HMAC fallback, FrameworkSQLiteOpenHelperFactory, -XX:-UsePerfData).
- ✅ Codified workflows: `android-device-adb-verification.md` + version-update Steps 5–6 (release anchors, gh CI verification) via /learn (2026-09-04).
- ✅ Task 24 "Screen security toggle" (FLAG_SECURE) spec fully expanded in ROADMAP.md + meta-prompt (live-session context).

## In Progress
- None — clean state after v0.0.1.3 hotfix.

## Backlog (Roadmap)
- Phase 11: Categorized Settings Hub (v0.0.2.0)
- Phase 12: Security Suite, Panic Purge & Audit Logging (v0.0.2.1) — includes the spec'd Screen Security toggle (Task 24)
- Phase 13: Advanced Import/Export & Multi-QR (v0.0.3.0)
- Phase 14: Glance Widgets & Icon Packs (v0.1.0.0)
- Phase 15: ClawKey Vault Creation (v0.1.1.0) — spec'd, Tasks 29 & 30.
- Play Console internal-testing upload of v0.0.1.3 .aab (pending Lucas's go).

## Known Issues
- None; all 96 tests green as of v0.0.1.3.
- Watch item: server-side `updated_at` on vault pearls is null/missing — delta optimization never engages until ShellGuard Server populates it (app is correct either way).