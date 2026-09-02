# 🦞 ShellGuard-TOTP — Release v0.0.1.0

## *Vault Security Orientation, Grouped Local/Remote Dashboard & Spotlight Tour*

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

Welcome to **v0.0.1.0 (Build 7)** of **ShellGuard-TOTP**! This release marks the completion of **Milestone 1** (**Phase 9: Vault Security Education, Enlarged Spotlight Tour & Empty Vault Landing** alongside the **One-Way Mirror Sync & Grouped Dashboard Refactor**).

This update transforms the initial onboarding experience with educational zero-knowledge security cards, an interactive protection mode selector with real-time password strength estimation, Android KeyStore hardware wrapper keys for all modes, clean visual segregation between local on-device and remote synced 2FA codes, and a breathable, physics-based Spotlight guided tour.

---

## 💎 Key Themes & Highlights

### 🛡️ 1. Zero-Knowledge Educational Orientation & Hardware Protection
* **Vault Security Screen (`VaultSecurityScreen.kt`)**: Replaced raw setup screens with a guided orientation explaining client-side encryption, local storage boundaries, and hardware KeyStore isolation.
* **Unified Protection Modes**: Interactive selector for numeric PIN (4–8 digits) and Master Passwords, featuring a real-time 4-tier entropy strength meter (`Weak`, `Fair`, `Good`, `Strong`).
* **Hardware KeyStore Wrappers**: Extended `AndroidKeyStoreHelper` and `AuthRepository` with dedicated hardware-backed AES-256-GCM keys (`KEY_ALIAS_PIN_WRAPPER`, `KEY_ALIAS_PASSWORD_WRAPPER`) that protect vault secrets at rest in device hardware.
* **Biometric Fast-Unlock**: Integrated toggle card for instant fingerprint / face authentication.

### 📱 2. Grouped Local vs Synced Dashboard Architecture
* **Distinct Visual Separation**: The main dashboard now clearly segregates TOTP items into vertical groups:
  - `📱 Local Vault`: High-security tokens created manually or scanned via QR directly on the phone.
  - `☁️ Synced from ShellGuard`: Read-only mirrors synced from self-hosted ShellGuard servers.
* **One-Way Mirror Sync**: Remote connections operate strictly as read-only mirrors of the server. Local codes are never pushed upstream, preventing inadvertent cross-device data leaks.
* **Export Integrity**: `BackupManager` exclusively packages Local Codes into encrypted `.sgtotp.bak` unified archives, preventing remote cache duplication across client backups.

### 🔦 3. Enlarged Spotlight Guided Tour & Empty Vault Landing
* **Spring Geometry Engine**: `SpotlightOverlay` now uses Jetpack Compose `Spring.DampingRatioMediumBouncy` physics for fluid transitions.
* **Breathable Cutouts**: Added density-aware radial padding (+18dp) around interactive targets (Settings and Gateway buttons) to eliminate visual crowding.
* **Pulsing Cyan Halos**: Double-ringed bioluminescent pulse highlighting key action areas.
* **Centered Controls**: Stacked button layout with a prominent action button and a centered `[ Skip Tutorial ]` button.
* **Empty State Transition**: Fresh onboarding lands smoothly on the empty local vault state (`TotpEmptyState`) with guided quick-action triggers.

---

## 🏗️ Architectural Topology Map

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                      🌐 [Onboarding & Intake Flow]                       │
│  ┌────────────────────────┐                 ┌────────────────────────┐  │
│  │   IntakeWelcomeScreen  │ ──────────────> │   VaultSecurityScreen  │  │
│  │  (Hero + SAF Import)   │                 │ (Education + Selector) │  │
│  └────────────────────────┘                 └───────────┬────────────┘  │
└─────────────────────────────────────────────────────────┼───────────────┘
                                                          │ (Vault Hatched)
                                                          ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     📱 [Grouped Vault Dashboard]                        │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │  📱 Local Vault (is_local_only = 1)                                │  │
│  │  • Manual additions • QR scans • SGTOTP backups                   │  │
│  ├───────────────────────────────────────────────────────────────────┤  │
│  │  ☁️ Synced from ShellGuard (is_local_only = 0, Read-Only)         │  │
│  │  • One-way server mirror • Delta synced via Ktor                  │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                 │ (Tour Step 1)                         │
│                                 ▼                                       │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │         SpotlightOverlay (+18dp Spring Radial Geometry)           │  │
│  │  • Highlight Settings ──> Step 2: Highlight Server Gateway        │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📦 Changes by Layer

| Component | Files | Description |
|:---|:---|:---|
| **Onboarding & Security UI** | `ui/screens/onboarding/VaultSecurityScreen.kt`, `ui/navigation/TotpNavHost.kt` | Zero-knowledge educational cards, PIN/Password selector, strength meter, biometric switch, and updated routing. |
| **Guided Tour** | `ui/components/SpotlightOverlay.kt` | Animated spring geometry engine, +18dp breathable radial offset, pulsing halos, stacked actions with centered Skip button. |
| **Dashboard UI** | `ui/screens/TotpListScreen.kt`, `ui/viewmodels/TotpViewModel.kt` | Grouped vertical streams (Local vs Synced), empty state landing, swipe-to-delete protection on remote items. |
| **Crypto & KeyStore** | `crypto/AndroidKeyStoreHelper.kt`, `data/repository/AuthRepository.kt` | Dedicated `AES-256-GCM` hardware wrapper key generation for PIN and Password modes. |
| **Backup Engine** | `data/backup/BackupManager.kt` | Filtered export logic packaging only Local Codes into `.sgtotp.bak`. |
| **Cleanups** | `ui/screens/HatchVaultScreen.kt` | Removed deprecated vault hatch screen. |

---

## 📦 Downloads & Artifacts

| Asset | Format | Recommended For |
|:---|:---|:---|
| **`shellguard-totp-v0.0.1.0.aab`** | Android App Bundle | Google Play Console internal test track |
| **`shellguard-totp-v0.0.1.0.apk`** | Standalone Android Package | Direct sideloading & offline verification |

---

*Engineered with precision by ClawStack Studios.*
