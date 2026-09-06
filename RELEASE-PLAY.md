# 📱 Google Play Console Release Notes (`RELEASE-PLAY.md`)

> **Single Source of Truth for Google Play Store What's New Notes**  
> *Google Play enforces a strict 500-character limit per localized language block.*

---

## `v0.0.2.1` — Settings Continuity: Server & Sync, Import/Export & Theme Parity (Build 11)

```xml
<en-US>
• New Server & Sync section: connection status, Sync Now, connect to your ShellGuard Gateway, and Disconnect Vault.
• New Import & Export section: export an encrypted .sgtotp.bak backup and restore your vault from a backup file.
• Theme parity restored: mode (Dark/Light/System) and 6 accent palettes live in the Appearance section.
• Fixed: Light mode (Ocean Mist) text readability — typography now follows the active theme.
• Guided Tour updated to spotlight the new Server & Sync controls.
• Navigation & UI polish across the Settings Hub and Gateway login.
• Full hardware-backed security unchanged: Android KeyStore, SQLCipher, FLAG_SECURE.
</en-US>
```

---

## `v0.0.2.0` — Milestone 2: Categorized Settings Hub (Build 10)

```xml
<en-US>
• All-New Settings Hub: 7 categorized preference sections with descriptive cards.
• Appearance Studio: View mode, issuer icons, next-code preview, digit grouping & display rules.
• Behavior Tuning: Search scope, haptic feedback, copy on tap, highlight & freeze tokens.
• Live Application: Preference changes reflect instantly on your dashboard cards.
• Honest Roadmap: Upcoming sections show clearly-labeled in-app placeholders.
• Full hardware-backed security unchanged: Android KeyStore, SQLCipher, FLAG_SECURE.
</en-US>
```

---

## `v0.0.1.3` — Hotfix: Remote Sync Restoration & Card Polish (Build 9)

```xml
<en-US>
• Fixed: Remote 2FA codes not syncing from ShellGuard Server on v0.0.1.2 — delta sync now self-heals on first pull.
• Fixed: Synced code cards rendering extra-tall due to badge text wrapping; cards are back to standard height.
• New regression suite covering the one-way sync engine (96/96 tests green).
• Full hardware-backed security unchanged: Android KeyStore, SQLCipher, FLAG_SECURE.
</en-US>
```

---

## `v0.0.1.2` — Phase 10: Expandable Floating Actions Speed Dial (Build 8)

```xml
<en-US>
• Expandable Speed Dial: Smooth 45° morphing FAB with outside-touch scrim.
• Image QR Scanner: Decode 2FA secret QR screenshots directly from your gallery.
• Elevated Action Pills: Instant access to live scanner, gallery & manual entry.
• Keyboard & Navigation Polish: Seamless back-handler and scrim dismissal.
• Android 16 (API 36) & 16 KB kernel ready.
</en-US>
```

---

## `v0.0.1.0` — Phase 9: Vault Security, Grouped Dashboard & Spotlight Tour (Build 7)

```xml
<en-US>
• Grouped Vault Separation: Clear visual segregation between Local Vault codes and Synced Server codes.
• One-Way Mirror Sync: Seamless read-only syncing with self-hosted ShellGuard servers.
• Vault Security Orientation: Zero-knowledge PIN/Password setup with real-time strength meter.
• Hardware Key Isolation: Android KeyStore AES-256-GCM hardware protection across all modes.
• Enlarged Spotlight Tour: Fluid spring guidance with breathable cutouts.
• Android 16 (API 36) & 16 KB kernel ready.
</en-US>
```

---

## `v0.0.0.2` — Phase 7: First-Run Intake & Habitat Import (Build 4)

```xml
<en-US>
• First-Run Welcome Wizard: Hero launcher shield branding and physical security guidance.
• One-Tap Habitat Import: Support for ShellGuard (.sgtotp.bak, .sgbak) and encrypted backups.
• Zero-Knowledge Multi-Vault Import: Bitwarden Vaults, Authenticator, Aegis & 2FAS with 0% leak.
• Key Rotation at Import: Choose between 1-tap secret reuse or rotating to a new PIN/Password.
• Steam Guard Support: Alphanumeric 5-character Steam TOTP generation & steam:// URIs.
</en-US>
```

---

## `v0.0.0.1` — Initial Release (Build 3)

```xml
<en-US>
• Initial release of ShellGuard-TOTP Authenticator!
• Zero-Knowledge Privacy: Hardware KeyStore AES-256 encryption.
• Offline Autonomy: Generate RFC 6238 2FA codes with live countdown arcs.
• Dynamic Design: Modernist Reef Pink default theme + 6 custom accent palettes.
• Fast Setup: CameraX QR scanner & screenshot import.
• Self-Hosted Sync: Optional two-way delta sync with ShellGuard servers.
• Android 15+ 16 KB page-size kernel ready.
</en-US>
```

---

## 📋 Release Notes Guidelines for Future Versions

When releasing version `vX.Y.Z.N`, prepend a new section above with `<en-US>` tags:
1. Keep the character count **strictly under 500 characters**.
2. Use bullet points (`•`) for readability on mobile screens.
3. Focus on user-facing benefits (features, performance, battery life, security improvements).

---

## 🔥 Appendix: Hotfix Under the Same Version Name (Retag Flow)

For hotfixes shipped under an unchanged `versionName` (e.g. the v0.0.2.1
light-mode fixes, Build 13→14). Proven flow — use exactly this order:

1. **Fix + bump `versionCode` only** (`N → N+1`); keep `versionName` unchanged.
2. Commit and push the release branch.
3. **Retag** (deleting the remote tag re-triggers the Release Pipeline):
   ```bash
   git tag -d vX.Y.Z
   git push origin :refs/tags/vX.Y.Z
   git tag -a vX.Y.Z -m "Release vX.Y.Z (Build N+1) — <hotfix summary>"
   git push origin vX.Y.Z
   ```
4. **Verify pipeline + artifacts** (wait ~5 min):
   ```bash
   gh run list --limit 2
   gh release view vX.Y.Z --json assets --jq '.assets[].name'
   ```
5. **Verify on-device**: `gh release download vX.Y.Z --pattern '*.apk'` →
   `adb install -r` → functional sweep (note: reinstall locks a PIN vault).
6. Keep `CHANGELOG.md` / `RELEASE-PLAY.md` updated under the same `vX.Y.Z`
   heading with the hotfix note.

> **Anti-pattern:** pushing a new tag while the old tag still exists locally and
> remotely — the pipeline may run against the stale commit. Always delete first.

