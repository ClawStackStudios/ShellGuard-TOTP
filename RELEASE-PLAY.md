# 📱 Google Play Console Release Notes (`RELEASE-PLAY.md`)

> **Single Source of Truth for Google Play Store What's New Notes**  
> *Google Play enforces a strict 500-character limit per localized language block.*

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
