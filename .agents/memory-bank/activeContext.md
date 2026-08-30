# ShellGuard TOTP — Active Context

## Sliding Window of Events
1. [2026-08-30] Implemented SpotlightOverlay interactive guided tour with circular cutout punching, animated glowing rings, and tooltip pill navigation.
2. [2026-08-30] Updated SettingsScreen with Server Sync Card, Offline Codes Section, and verified complete test suite pass.
3. [2026-08-30] Persisted /learn patterns into systemPatterns.md: Android 12+ Splash Screen, Cloud Backup Rules, ProGuard Keep Rules, and Compose Spotlight Cutout overlay.
4. [2026-08-30] Adjusted back arrow button on LoginScreen and GatewayScreen with statusBarsPadding() and balanced spacing below system status bar.
5. [2026-08-30] Removed automatic mock authentication code seeding on vault creation/startup so new vaults start completely blank with empty state prompt.
6. [2026-08-30] Segregated Local Mode vs Server Connected Mode: hid 'Synced'/'Local Only' filter chips when disconnected in local mode, scoped item addition to local vault offline vs server sync when connected, and added reactive UI status indication.
7. [2026-08-30] Hardened Room encryption at rest with EncryptedDeviceVault (Android KeyStore AES-256-GCM hardware key protection for SQLCipher 256-bit passphrase, token/huKey persistence, and zero-knowledge end-to-end server sync encryption).
8. [2026-08-30] Implemented Dynamic Theme Engine with dual-mode tokens (Abyssal Dark / Ocean Mist Light) and 6 curated marine accent palettes (`REEF_DEFAULT`, `CYAN_VENT`, `PURPLE_SHELL`, `EMERALD_TRENCH`, `AMBER_FLARE`, `MONOCHROME`), injected via `LocalShellGuardColors` and selectable in Settings.
9. [2026-08-30] Resolved app launch crash failure modes: created static `ic_splash_icon` vector preventing AAPT gradient inflation crash on API <31, safeguarded database passphrase generation, and created `MainActivityLaunchTest` test suite.
10. [2026-08-30] Fixed `InvalidAlgorithmParameterException: Caller-provided IV not permitted` in `EncryptedDeviceVault.encrypt` by initializing AES-GCM cipher with provider-generated IV, verified 100% across all 43 Robolectric unit tests.

