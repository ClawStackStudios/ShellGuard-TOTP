# 🦞 ShellGuard TOTP — Release v0.0.2.1 (Build 11)

## *Phase 11.5: Settings Continuity — Server & Sync, Import/Export & Theme Parity*

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

Welcome to **v0.0.2.1** of **ShellGuard-TOTP** — **Phase 11.5: Settings Continuity**. This is a parity-restoration release: the vault capabilities that v0.0.1.3's monolithic settings page hosted — **server connection & sync**, **encrypted backup import/export**, and **theme mode + accent selection** — now have first-class, semantic homes inside the v0.0.2.0 Categorized Settings Hub. The hub's ☁️ Server & Sync and 🛠️ Import & Export cards finally lead somewhere real. 101/101 tests green, verified live on device.

---

## 💎 Key Themes & Highlights

### ☁️ 1. Server & Sync Sub-screen (Task 22c)

* **New ☁️ hub category** hosting the full server story: live connection status, manual **Sync Now**, **Connect → Gateway** navigation, and **Disconnect Vault** with a confirmation dialog.
* **Spotlight Tour continuity**: the `tourStep == 2` cutout spotlighting `settings_connect_button` migrated from the deleted legacy screen — onboarding points at the new home.
* Legacy `SettingsScreen.kt` deleted along with its route; the hub is now the single source of settings navigation.

### 🛠️ 2. Import & Export Sub-screen (Task 22d)

* **Export**: SAF `CreateDocument` flow producing an encrypted `.sgtotp.bak` backup, tagged `export_backup_button`.
* **Restore**: SAF `OpenDocument` flow to bring a vault back from a backup file, tagged `import_backup_button`.
* Secret/key resolution stays **ViewModel-first** — `AuthViewModel.exportVaultBackup`/`importVaultBackup` wrap the vault layer; the UI never touches key material.

### 🎨 3. Appearance Theme Parity (Tasks 22b/22c)

* Theme mode tiles (Dark/Light/System) and the 6 `ThemeAccent` palettes live in the Appearance section via the shared `SettingsControls.kt` tiles.
* Backed by the already-shipped `themeMode`/`themeAccent` `StateFlow`s with persistence across cold restarts (verified by the existing `AuthVaultModeRepositoryTest` suite).

### 🔧 4. Polish & Fixes

* **Gateway back-button unclip**: circular back button inset 10dp with the border stroke drawn outside the `CircleShape` clip.
* **Nav-graph integrity**: the `settings_import_export` composable is now verified present in the graph (grep-asserted, on-device walkthrough confirmed).

---

## 🧪 Verification Record

* **101/101 unit tests** passing.
* **Pre-flight invariants**: `targetSdk = 36`, `sqlcipher 4.6.1` (16 KB-aligned ELF segments), `jniLibs.useLegacyPackaging = false`, `FLAG_SECURE` on release builds, cleartext restricted to LAN/VPN origins.
* **Live device walkthrough** (Pixel sailfish, LineageOS): hub navigation → Import & Export → export/restore roundtrip → Appearance theme tiles + cold-restart persistence → Server & Sync status/Gateway → tour cutout → back-button rendering.

---

## 🛡️ Security Posture — Unchanged

Android KeyStore hardware backing, SQLCipher AES-256 at rest, `FLAG_SECURE` screen-capture protection on release builds, zero-knowledge sync transport, Android 16 (API 36) + 16 KB page-size kernel ready.

---

*ClawStack Mobile Studios©™ — Build features around security, not security around features.*
