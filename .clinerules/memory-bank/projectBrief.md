# ShellGuard TOTP — Project Brief (Cline)

## Overview
ShellGuard TOTP is a native Android authenticator application built for high-security environments, offline resilience, and cryptographic parity with the ClawStack / ShellGuard server ecosystem. Maintained by Lucas; developed via deterministic 2-task phases tracked in `ROADMAP.md` and executed via stage prompts in `project/meta-prompt-ai-studio.md`.

> Note: `.agents/memory-bank/` belongs to Google Antigravity — this bank (`.clinerules/memory-bank/`) is Cline's own.

## Core Objectives
1. **At-Rest Encryption**: SQLCipher AES-256 encrypted Room database with hardware-backed KeyStore keys.
2. **Cryptographic Parity**: ShellCryption engine (HKDF-SHA256 + AES-GCM-256 with AAD binding `vault_pearls_totp:{id}`).
3. **Biometric Vault Protection**: AndroidX BiometricPrompt guarding vault access (PIN / PASSWORD / BIOMETRICS modes; CLAWKEY planned Phase 15).
4. **One-Way Mirror Sync**: Remote is strictly read-only; locally created codes are `isLocalOnly = true`, never pushed upstream.
5. **CameraX QR Scanning**: CameraX + Google ML Kit Barcode Scanning.
6. **Encrypted Backup & Restore**: `sgtotp.bak` sealed JSON envelope, SHA-256 checksums, Local Codes only.
7. **Reef Modernist Design System**: Dark theme, dynamic Material 3 + `LocalShellGuardColors`, accent `ThemeAccent.REEF_DEFAULT` (#E4048A).

## Process Invariants
- Rule of 2: every phase = Task A (Functionality) + Task B (UI Component).
- Work-driven versioning `MAJOR.MINOR.PATCH.REVISION`; `versionCode` +1 per Play upload.
- Cross-Repository Execution Sequence: source codebase first, then compatibility docs/consumers.
- Test Oracle Audit: any layout/data-filter change requires auditing `app/src/test` fixtures.