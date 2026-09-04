# 🦞 ShellGuard-TOTP — Release v0.0.1.2

## *Expandable Speed Dial FAB, Image QR Decoding Pipeline & Gallery Scanner*

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

Welcome to **v0.0.1.2 (Build 8)** of **ShellGuard-TOTP**! This release delivers **Phase 10: Expandable Floating Actions Speed Dial (QR, Image & Manual)**, replacing static dual floating action buttons with an animated, fluid Speed Dial controller.

This update introduces high-throughput image QR decoding using Google ML Kit on Android Storage Access Framework (SAF) image streams, allowing users to scan and import 2FA secret QR codes directly from screenshots and photos in their gallery without needing a secondary camera.

---

## 💎 Key Themes & Highlights

### ⚡ 1. Animated Speed Dial FAB (`ExpandableSpeedDialFab.kt`)
* **Fluid 45° Morphing Action**: The main action button smoothly morphs from `+` (collapsed) to `✕` (expanded) with spring-backed physics.
* **Semi-Transparent Scrim**: Dark alpha backdrop (`SpeedDialScrim`) provides immediate visual focus on the actions while dimming vault tokens underneath.
* **Outside Touch & Back-Handler Dismissal**: Tapping anywhere on the scrim or pressing the Android system back button automatically collapses the menu cleanly.

### 🖼️ 2. High-Throughput ML Kit Image QR Pipeline (`ImageQrDecoder.kt`)
* **Unified Image Vision Engine**: Extracts and decodes QR codes from Android URI bitmap streams using Google ML Kit `BarcodeScanning.getClient()`.
* **Shared Architecture**: Deduplicates vision logic across the codebase by powering both the new dashboard Speed Dial "Scan image" action and the existing `QrScannerScreen` gallery fallback.
* **Instant Ingestion & Toast Feedback**: Automatically verifies decoded `otpauth://` URIs and adds them directly to the local vault with clear user feedback.

### 📱 3. Elevated Staggered Action Pills
* **Staggered Entrance Animation**: Three action pills cascade outward with animated slide-up and fade transitions:
  1. `[ 📷 Scan QR code ]` — Launches the live CameraX preview scanner with targeting reticle.
  2. `[ 🖼️ Scan image ]` — Opens the native Android image gallery picker to decode screenshot QR codes.
  3. `[ ✏️ Enter manually ]` — Opens the manual secret entry form for custom Base32 keys, algorithms (SHA1/SHA256/SHA512), and periods.

### 🧪 4. Test Suite Oracle Alignment & Stability
* **86/86 Passing Tests**: Full test coverage across all unit and Robolectric suites, including 7 new tests in `SpeedDialStateTest` and `ImageQrDecoderTest`.
* **Test Oracle Synchronization**: Updated UI test selectors in `LocalModeUnlockAndVaultTest` (`scan_qr_fab` ➔ `speed_dial_fab`) ensuring continuous verification alignment.

---

## 🏗️ Architectural Topology Map

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                     📱 [Grouped Vault Dashboard]                        │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │  📱 Local Vault (is_local_only = 1)                                │  │
│  ├───────────────────────────────────────────────────────────────────┤  │
│  │  ☁️ Synced from ShellGuard (is_local_only = 0, Read-Only)         │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                      │                                  │
│                                      ▼                                  │
│         ┌─────────────────────────────────────────────────────┐         │
│         │        SpeedDialState (BackHandler + Scrim)         │         │
│         └──────────────────────────┬──────────────────────────┘         │
│                                    │                                    │
│         ┌──────────────────────────┴──────────────────────────┐         │
│         │       ExpandableSpeedDialFab (+ ➔ ✕ 45° Morph)      │         │
│         └──────────────────────────┬──────────────────────────┘         │
│                                    │                                    │
│         ┌──────────────────────────┼──────────────────────────┐         │
│         ▼                          ▼                          ▼         │
│  [ 📷 Scan QR ]            [ 🖼️ Scan image ]         [ ✏️ Enter manual ]  │
│  (CameraX Scanner)       (SAF Image Picker)          (AddSecretScreen)  │
│                                    │                                    │
│                                    ▼                                    │
│                         ImageQrDecoder (ML Kit)                         │
│                                    │                                    │
│                                    ▼                                    │
│                        TotpViewModel.importScannedUri                   │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📦 Changes by Layer

| Component | Files | Description |
|:---|:---|:---|
| **Speed Dial UI** | `ui/components/ExpandableSpeedDialFab.kt`, `ui/components/SpeedDialState.kt` | Animated FAB with 45° morph, dark dimming scrim, back-handler, and staggered action pills. |
| **Vision & Image Pipeline** | `scanner/ImageQrDecoder.kt`, `ui/screens/QrScannerScreen.kt` | Shared ML Kit barcode scanning pipeline on URI bitmap streams. |
| **Dashboard Integration** | `ui/screens/TotpListScreen.kt` | Replaced legacy dual FABs and `ScannerFab` with unified expandable speed dial and SAF gallery launcher. |
| **Cleanups** | `ui/components/ScannerFab.kt` | Removed dead component. |
| **Test Suites** | `SpeedDialStateTest.kt`, `ImageQrDecoderTest.kt`, `LocalModeUnlockAndVaultTest.kt` | Full test coverage verifying interaction transitions, image decoding, and updated UI test tags (86/86 passing). |

---

## 📦 Downloads & Artifacts

| Asset | Format | Recommended For |
|:---|:---|:---|
| **`shellguard-totp-v0.0.1.2.aab`** | Android App Bundle | Google Play Console internal test track |
| **`shellguard-totp-v0.0.1.2.apk`** | Standalone Android Package | Direct sideloading & offline verification |

---

*Engineered with precision by ClawStack Studios.*
