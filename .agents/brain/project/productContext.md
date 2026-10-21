# ShellGuard TOTP — Product Context

## Why ShellGuard TOTP Exists
Traditional 2FA authenticator apps lack end-to-end cryptographic integration with self-hosted password managers and enterprise vaults. ShellGuard TOTP provides hardware-backed, offline-first 2FA generation with automatic synchronization to self-hosted ShellGuard/ClawStack server instances.

## Key User Experience Goals
- **Instant 2FA Code Access**: Sub-millisecond code generation using RFC 6238 HMAC-SHA1/SHA256/SHA512.
- **Biometric Gating**: Smooth biometric unlock on app open and resume.
- **Zero-Latency Offline Mode**: Immediate display of cached vault items on launch.
- **Seamless Setup**: QR scanner supporting CameraX + ML Kit and 1:1 ClawStack Gateway identity login.
- **Anti-Snoop Protection**: Clipboard auto-clearing after 30 seconds and FLAG_SECURE screenshot protection.
- **Sensitive Key Input Masking & IME Protection**: Base32 secrets masked with bullet characters (`••••••••`), interactive visibility eye toggle, and hardened IME options suppressing keyboard predictive dictionary learning (CWE-359).

