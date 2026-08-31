# 🐚 ShellGuard-TOTP v0.0.0.1 — Initial Release

> **Tagline**: *"Store and generate 2FA verification codes on your device."*  
> **Initial Launch Version**: `v0.0.0.1 (Build 1)`  
> **Google Play Console Track**: Internal Testing / Closed Beta  

---

## 🎯 Highlights & Features

### 🛡️ Privacy-First Zero-Knowledge Architecture
- **Hardware-Backed Encryption**: Master secret hashes and biometric gates are sealed inside the Android KeyStore using hardware AES-256-GCM (`sg_totp_biometric_wrapper`).
- **Encrypted Local Cache**: All 2FA secrets stored at rest in an encrypted Room SQLite database powered by SQLCipher 4.6.1.
- **Offline Autonomy**: Generates valid 6-digit and 8-digit RFC 6238 TOTP codes 100% offline without requiring continuous network connectivity.
- **Screen Shield**: Enforces `FLAG_SECURE` to block screenshots, malware screen recording, and task-switcher recents previews.

### 🎨 Reef Modernist Mobile Design System
- **Dynamic Theming**: Signature **Reef Pink** (`BrandLobsterRed` `#E4048A`) default accent with 6 curated palettes (*Reef Bioluminescent, Electric Cyan, Imperial Shell, Emerald Bio-Flora, Solar Vent, Minimalist Pearl*).
- **Adaptive Contrast**: Full support for Abyssal Dark, Ocean Mist Light, and System Default appearance modes with adaptive light-mode contrast.
- **Live Countdown Arcs**: High-precision Canvas countdown ring depleting smoothly with dynamic color alerts (<10s Warning, <5s Danger).
- **Tactile Ergonomics**: Spring press physics, haptic feedback on code copy, swipe-to-delete gestures, and `.imePadding()` keyboard handling.

### 📷 CameraX QR Scanner & Multi-Format Backup
- **Live QR Code Scanner**: Integrated CameraX with Google ML Kit Barcode Scanning for instant 2FA account setup.
- **Gallery Photo Import**: Scan QR codes directly from screenshots or saved images.
- **Encrypted JSON Backups**: Export and restore ShellCrypted AES-256 backup envelopes with SHA-256 integrity checksums.

### 🌐 Self-Hosted Server Synchronization
- **Bidirectional Delta Sync**: Two-way push/pull synchronization with self-hosted ShellGuard web servers.
- **LAN & VPN Routing**: Built-in support for unencrypted HTTP on local private subnets (`192.168.x.x`, `10.x.x.x`) and Tailscale/WireGuard CGNAT VPN tunnels (`100.64.0.0/10`).
- **Interactive Spotlight Tour**: Guided visual onboarding walking users through server pairing and settings configuration.

### 🚀 Android 15+ 16 KB Page-Size Ready
- Built with SQLCipher 4.6.1 and uncompressed JNI packaging (`jniLibs.useLegacyPackaging = false`) for 16 KB ELF segment alignment on Android 15/16 kernel devices.

---

## 📦 Downloads & Installation

| Asset | Format | Recommended For |
| :--- | :--- | :--- |
| **`app-release.aab`** | Android App Bundle | Google Play Console upload / Store distribution |
| **`app-release.apk`** | Standalone Android Package | Direct sideloading / FOSS distribution |

---

*Engineered with precision by ClawStack Studios.*
