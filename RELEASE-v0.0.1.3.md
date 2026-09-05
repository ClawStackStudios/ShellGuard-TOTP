# 🦞 ShellGuard TOTP — Release v0.0.1.3 (Build 9)

## *The Self-Healing Mirror Hotfix*

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

Welcome to **v0.0.1.3** of **ShellGuard TOTP** — a targeted **hotfix release** patching the v0.0.1.2 remote sync regression where 2FA codes silently stopped syncing from connected ShellGuard Servers, plus a dashboard polish fix for synced code cards. No new features; existing v0.0.1.2 devices self-heal on their next sync pull. All 96 unit tests green.

---

## 💎 Key Themes & Highlights

### 🛡️ 1. One-Way Sync Engine Restoration (CRITICAL)

* **Root cause patched:** v0.0.1.2's delta-sync efficiency compared the nullable local `remoteUpdatedAt` mirror stamp against the nullable server `updated_at` field — a `null == null` match silently classified every affected pearl as "unchanged," skipping decryption and upsert while sync still reported success.
* **`TotpRepository.classifyDeltaPearls`:** Extracted the delta classification into a pure, tested function — a pearl is skipped only when a local mirror row exists AND the server stamp is non-null AND equal. Null/missing server stamps always sync (v0.0.1.0 behavior restored).
* **Self-healing migration:** Devices stuck on v0.0.1.2 require no migration or re-login — the next periodic (6h), pull-to-refresh, or manual sync populates all missing codes automatically.

### 🎨 2. Synced Card Visual Polish

* **Badge wrap fix:** The "Read-only" text pill on synced cards wrapped to three lines ("Rea/d-on/ly"), inflating card height. The pink cloud icon now alone denotes synced read-only codes — semantics preserved via `contentDescription` and the "☁️ Synced from ShellGuard" group header. Cards render at standard single-line height.

### 🧪 3. Regression Armor

* **`DeltaSyncClassificationTest` (6 cases):** First direct coverage of the sync delta path, including the exact production repro (fresh install + null server stamp). Suite total: **96/96 passing**.

---

## ✅ Verification Gate

* 96/96 unit & Robolectric tests passing (100% green)
* `assembleDebug` build successful
* Live on-device verification: remote sync restored + card layout confirmed on physical Pixel 8

---

## 📦 Upgrade Notes

* `versionCode` 8 → 9 (monotonic Play Console invariant); `versionName` "0.0.1.3"
* No schema migration required; no settings changes
* Recommended for all v0.0.1.2 users immediately
