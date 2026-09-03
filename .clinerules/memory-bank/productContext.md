# ShellGuard TOTP — Product Context

## Why It Exists
Traditional 2FA authenticator apps lack end-to-end cryptographic integration with self-hosted password managers and enterprise vaults. ShellGuard TOTP provides hardware-backed, offline-first 2FA generation with read-only synchronization to self-hosted ShellGuard/ClawStack server instances.

## Key UX Goals
- Instant 2FA code access (RFC 6238 HMAC-SHA1/256/512, sub-millisecond generation).
- Smooth biometric/PIN/password unlock on open and resume.
- Zero-latency offline mode — cached vault items shown immediately on launch.
- Seamless setup: CameraX QR scanner, image (screenshot) QR decoding, manual Base32 entry, 1:1 ClawStack Gateway login.
- Anti-snoop: clipboard auto-clear after 30s, FLAG_SECURE screenshot protection.
- Grouped dashboard: "📱 Local Vault" vs "☁️ Synced from ShellGuard" — visually distinct vertical groups, no filter chips.