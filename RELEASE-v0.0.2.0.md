# 🦞 ShellGuard TOTP — Release v0.0.2.0 (Build 10)

## *Milestone 2: The Categorized Settings Hub*

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

Welcome to **v0.0.2.0** of **ShellGuard-TOTP** — **Milestone 2** of the post-launch expansion. The monolithic settings page is now a fully **Categorized Settings Hub**: seven preference sections with dedicated sub-screens for **Appearance** and **Behavior**, every control backed by structured preference stores that apply changes to the dashboard **instantly** — no restart, no re-login. 101/101 tests green.

---

## 💎 Key Themes & Highlights

### 🎨 1. Categorized Settings Hub

* **`SettingsMetaScreen`**: Seven preference categories (🎨 Appearance, ⚡ Behavior, 📦 Icon packs, 🔐 Security, ☁️ Backups, 🛠️ Import & Export, 📈 Audit log) with icons and descriptive subtitles in Reef Modernist cards.
* **Honest Roadmap UX**: Categories whose sub-screens ship in Phases 12–14 route to clearly-labeled in-app placeholders with a bridge to server/backup settings — no dead ends.

### ⚡ 2. Preferences Store Architecture (Task 21)

* **Structured stores**: `AppearancePreferences` (view mode, issuer icons, next-code preview, expiration blink, digit grouping, issuer/account display rules, Group Manager hidden-groups) and `BehaviorPreferences` (search scope, focus search on start, minimize on copy, copy on tap, haptics, multiselect groups, highlight & freeze tokens).
* **Durable persistence**: SharedPreferences-backed `StateFlow`s with corrupt-value fallback and persistence across process recreation; 14 granular setters.
* **Regression armor**: `UserPreferencesStoreTest` (5 cases) covering defaults, synchronous flow updates, recreation persistence, group management, and corrupt-enum fallback.

### 🖌️ 3. Live Dashboard Application (Task 22)

* **Digit grouping & haptic feedback** drive `TotpCard` rendering and interaction directly.
* **Search scope (All/Local/Synced) and hidden groups** filter dashboard streams reactively via `combine` chains.
* **Focus search on start** focuses the dashboard search bar through a `FocusRequester`.
* **`SettingsAppearanceScreen` & `SettingsBehaviorScreen`**: full sub-screens with segmented selectors and switches, all live StateFlow-backed.

---

## ✅ Verification Gate

* 101/101 unit & Robolectric tests passing (100% green)
* `assembleDebug` successful
* Live on-device verification: Settings Hub, Appearance & Behavior sub-screens captured from a physical Pixel (README screenshots refreshed)

---

## 📦 Upgrade Notes

* `versionCode` 9 → 10 (monotonic Play Console invariant); `versionName` "0.0.2.0"
* No schema migration required; existing preferences carry over untouched
* Milestone 2 of the post-launch roadmap — next stop: Phase 11.5 Settings Continuity (v0.0.2.1), then Phase 12 Security Suite (v0.0.2.2)
