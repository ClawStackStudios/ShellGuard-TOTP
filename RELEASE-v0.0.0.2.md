# 🦞 ShellGuard-TOTP — Release v0.0.0.2

## *Welcoming First-Run Intake, Multi-Vault Migration & Sovereign Key Rotation*

```text
███████╗██╗   ██╗███████╗██╗     ██╗              ██████╗   ██╗   ██╗   █████╗    ██████╗     ██████╗ 
██╔════╝██║   ██║██╔════╝██║     ██║              ██╔═══╝   ██║   ██║  ██╔══██╗  ██╔══██╗    ██╔══██╗
███████╗███████║█████╗   ██║     ██║              ██║ ███╗  ██║   ██║  ███████║  ██████╔╝    ██║   ██║
╚════██║██╔══██║██╔══╝   ██║     ██║              ██║   ██║  ██║   ██║  ██╔══██║  ██╔══██╗    ██║   ██║
███████║██║   ██║███████╗███████╗███████╗  ╚██████╔╝╚██████╝  ██║   ██║  ██║   ██║   ██████╔╝
╚══════╝╚═╝  ╚═╝╚══════╝╚══════╝╚══════╝    ╚═════╝   ╚═════╝   ╚═╝  ╚═╝  ╚═╝   ╚═╝   ╚═════╝
                                                  ~ **ClawStack Mobile Studios©™** ~
```

---

## 🚀 The Core Summary

Welcome to **v0.0.0.2 (Build 4)** of **ShellGuard-TOTP**! This milestone deliverable (**Phase 7: Welcoming First-Run Wizard & "Import Habitat" Intake Flow**) redefines how users board the zero-knowledge privacy fortress. We have introduced an all-new **Brand Hero Welcome Screen**, one-tap **Storage Access Framework (SAF) multi-vault intake**, in-memory zero-knowledge sanitization for Bitwarden/Aegis/2FAS backups, native **Steam Guard 5-character alphanumeric token calculation**, dynamic **PIN/Password/Biometrics mode detection**, and seamless **Key Rotation at Import Time**.

---

## 💎 Key Themes & Highlights

### 🛡️ 1. Zero-Knowledge Cryptography & Sovereign Key Rotation
* **Key Rotation at Import Time:** Decoupled backup encryption keys from device vault keys. Users can choose `[ 🛡️ Reuse Secret ]` for 1-tap fast-track onboarding or `[ 🔄 Rotate Key (New) ]` to seal their local Android KeyStore under a fresh PIN or Master Password.
* **Zero-Knowledge Multi-Vault Ingestion:** Pre-validates Bitwarden Vault JSON exports (extracts `login.totp` and maps folders to Pod categories while 100% stripping passwords and notes in volatile RAM), Bitwarden Authenticator, Aegis, 2FAS, and ShellGuard envelopes.
* **Proprietary `sgtotp.bak` Format:** Native adoption of ShellGuard ecosystem standard (`.sgtotp.bak` / `.sgbak`) with embedded protection mode metadata, PIN length markers, and biometric flags.
* **Resilient Multi-Salt Derivation:** Replaced low-level OpenSSL cipher crashes (`BAD_DECRYPT`) with resilient derivation loops and friendly, actionable user feedback.

### 🎨 2. Welcoming Brand Hero UI & Physical Security Guidance
* **Vector Shield Branding:** High-resolution vector ShellGuard launcher shield with ambient bioluminescent backdrop aura and breathing forward navigation FAB.
* **Physical Security Notice Card:** Prominent zero-knowledge advisory instructing users to manually write down their secret in a physically safe spot, warning that unrecoverable lost secrets seal the vault forever.
* **Dynamic Input Form:** Decryption bottom sheet automatically presents numeric keypad (`KeyboardType.NumberPassword`) for PINs and full keyboard (`KeyboardType.Password`) for Master Passwords with a live segmented override toggle.

### 🎮 3. Steam Guard 2FA Engine
* **5-Character Alphanumeric Generator:** Implemented RFC-compliant Steam Guard token calculation using Steam's custom 26-character Base32 alphabet (`23456789BCDFGHJKMNPQRTVWXY`).
* **Steam URI Parsing:** Instant parsing of `steam://...` URIs mapping directly to algorithm `"STEAM"` and 5 digits.

### 🧪 4. Test Oracle & Verification Parity
* **100% Passing Test Suite:** 18 comprehensive unit and Robolectric Compose test classes covering encryption at rest, KeyStore envelopes, SQLCipher Room queries, multi-vault sanitization, and UI flows.
* **Android 15+ 16 KB Page-Size Ready:** SQLCipher 4.6.1 with uncompressed JNI packaging (`jniLibs.useLegacyPackaging = false`).

---

## 🏗️ Architectural Topology Map

```text
┌─────────────────────────────────────────────────────────────────────────┐
│              🌐 [First-Run Intake Layer — Jetpack Compose UI]           │
│  ┌────────────────────────┐                 ┌────────────────────────┐  │
│  │   IntakeWelcomeScreen  │ ──────────────> │    HatchVaultScreen    │  │
│  │ (Hero + SAF File Picker)│                 │ (KeyStore Sealed Vault)│  │
│  └───────────┬────────────┘                 └────────────────────────┘  │
│              │ (Selected Backup File URI)                                │
│              ▼                                                          │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │               MultiVaultBackupPreValidator Engine                 │  │
│  │  • ShellGuard (.sgtotp.bak / .sgbak)  • Bitwarden (0% PW Leak)   │  │
│  │  • Aegis Authenticator JSON           • 2FAS Authenticator JSON   │  │
│  └───────────────────────────────────┬───────────────────────────────┘  │
└──────────────────────────────────────┼──────────────────────────────────┘
                                       │ (Decrypted BackupItemDtos in RAM)
                                       ▼
┌─────────────────────────────────────────────────────────────────────────┐
│             🛡️ [Local Storage & Cryptographic Boundary]                 │
│  ┌─────────────────────────────────┐   ┌─────────────────────────────┐  │
│  │    Android KeyStore StrongBox   │   │  SQLCipher 4.6.1 Room DB    │  │
│  │  (AES-256-GCM Master Wrapper)   │   │  (Offline-First Cache)      │  │
│  └─────────────────────────────────┘   └─────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📦 Changes by Layer

| Component | Files | Description |
|:---|:---|:---|
| **Intake & Onboarding UI** | `ui/screens/onboarding/IntakeWelcomeScreen.kt`, `ui/onboarding/IntakeViewModel.kt`, `ui/onboarding/IntakeState.kt` | First-run hero welcome screen, SAF document launcher, PIN/Password decryption sheet, and Key Rotation selector. |
| **Migration & Backup Engine** | `data/backup/MultiVaultBackupPreValidator.kt`, `data/backup/BackupManager.kt`, `data/migration/` | Schema detection, in-RAM zero-knowledge sanitization, multi-salt derivation, and `.sgtotp.bak` serialization. |
| **Crypto & Auth Repository** | `data/repository/AuthRepository.kt`, `crypto/EncryptedDeviceVault.kt` | Active vault secret persistence in KeyStore and retrieval during export/backup operations. |
| **TOTP Engine** | `engine/TotpEngine.kt`, `engine/TotpUriParser.kt` | Steam Guard 5-character alphanumeric calculation and `steam://` URI parser. |
| **Unit & UI Tests** | `app/src/test/java/com/clawstack/shellguard/totp/*` | 18 test suites covering engine, encryption, database, and Compose UI flows. |

---

## 📦 Downloads & Artifacts

| Asset | Format | Recommended For |
|:---|:---|:---|
| **`shellguard-totp-v0.0.0.2.aab`** | Android App Bundle | Google Play Console internal test track |
| **`shellguard-totp-v0.0.0.2.apk`** | Standalone Android Package | Direct sideloading & offline verification |

---

*Engineered with precision by ClawStack Studios.*
