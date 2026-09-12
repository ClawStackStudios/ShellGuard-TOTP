# 🦞 ShellGuard TOTP — Release v0.0.2.2 (Build 15)

> **Hotfix: Compose Secret Key Masking & IME Hardening (CWE-359)**: versionCode 15 (`versionName = "0.0.2.2"`). Eliminates plaintext 2FA seed exposure in manual entry by masking the Base32 Secret Key with `PasswordVisualTransformation`, pairing it with an interactive eye visibility toggle, and disabling soft keyboard predictive dictionary learning. Install over previous builds in place; vault data is preserved.

## *Post-Hoc Interlude D: Compose Secret Key Masking & IME Hardening*

```text
███████╗██╗   ██╗███████╗██╗     ██╗              ██████╗   ██╗   ██╗   █████╗    ██████╗     ██████╗ 
██╔════╝██║   ██║██╔════╝██║     ██║              ██╔═══╝   ██║   ██║  ██╔══██╗  ██╔══██╗    ██╔══██╗
███████╗███████║█████╗   ██║     ██║              ██║ ███╗  ██║   ██║  ███████║  ██████╔╝    ██║   ██║
╚════██║██╔══██║██╔══╝   ██║     ██║              ██║   ██║  ██║   ██║  ██╔══██║  ██╔══██╗    ██║   ██║
███████║██║   ██║███████╗███████╗  ╚██████╔╝╚██████╝  ██║   ██║  ██║   ██║   ██████╔╝
╚══════╝╚═╝  ╚═╝╚══════╝╚══════╝╚══════╝    ╚═╝   ╚═╝   ╚═╝   ╚═╝   ╚═╝   ╚═╝
                                                  ~ **ClawStack Mobile Studios©™** ~
```

---

## 🚀 The Core Summary

Welcome to **v0.0.2.2** of **ShellGuard-TOTP** — **Hotfix: Compose Secret Key Masking & IME Hardening (Build 15)**. This patch delivers critical on-device cryptographic protection for manual 2FA entry (`AddSecretScreen.kt`): eliminating cleartext seed exposure, enforcing non-dictionary keyboard input policies to defeat soft-keyboard word-caching (CWE-359), and providing an accessible interactive eye toggle alongside the Base32 validity indicator. All existing Settings Hub, Server & Sync, and One-Way Mirror capabilities from v0.0.2.1 remain intact. 100% test gate passing, verified live on a physical Google Pixel device.

---

## 💎 Key Themes & Highlights

### 🛡️ 1. Secret Key Masking & Anti-Snoop Protection (CWE-359)

* **PasswordVisualTransformation**: Manual Base32 secret input is masked by default with password bullet glyphs (`••••••••`), preventing shoulder-surfing, screen recording leaks, and recents thumbnail snooping.
* **Predictive Dictionary Immunity**: Configured `KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)`. This explicitly signals system keyboards (GBoard, SwiftKey, Samsung Keyboard, etc.) to disable personalized word suggestions, learning dictionaries, and telemetry caching for secret seed values.

### 👁️ 2. Interactive Visibility Toggle

* **Compound Trailing Controls**: Paired the Base32 input field with an interactive eye icon button (`toggle_secret_visibility`) displaying `Icons.Default.Visibility` / `VisibilityOff` alongside the real-time Base32 validity indicator (`CheckCircle` / `ErrorOutline`).
* **Accessible Verification**: Users can toggle between masked and plain text to audit character accuracy when typing complex 16–32 character RFC 6238 Base32 keys.

### 🎨 3. UI Modernization & Asset Parity

* **Icon Modernization**: Upgraded deprecated `Icons.Default.ArrowBack` to `Icons.AutoMirrored.Filled.ArrowBack`.
* **README 3×3 Showcase Grid**: Expanded device screenshot showcase to 9 native 1080×1920 Google Pixel captures, including the new dark-mode manual entry screen (`screenshot-09-add-secret.png`).

---

## 🧪 Verification Record

* **101+ unit and Robolectric tests** passing 100% green (`Phase4ScreensTest`, `OneWaySyncAndReadOnlyProtectionTest`, `DeltaSyncClassificationTest`, `UserPreferencesStoreTest`, etc.).
* **Pre-flight invariants**: `targetSdk = 36`, `sqlcipher 4.6.1` (16 KB-aligned ELF segments), `jniLibs.useLegacyPackaging = false`, `FLAG_SECURE` on release builds, cleartext restricted to LAN/VPN origins.
* **Physical Google Pixel Walkthrough (ADB TLS)**: Verified masked password rendering, keyboard prediction suppression, eye toggle reveal/mask interaction, and live secret submission on Android 15.

---

## 🛡️ Security Posture — Unchanged

Android KeyStore hardware backing, SQLCipher AES-256 at rest, `FLAG_SECURE` screen-capture protection on release builds, zero-knowledge sync transport, Android 16 (API 36) + 16 KB page-size kernel ready.

---

*ClawStack Mobile Studios©™ — Build features around security, not security around features.*
