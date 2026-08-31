# ShellGuard TOTP — Active Context

## Sliding Window of Events
1. [2026-08-31] Unified Release Pipeline & CI Gradle Invariants: Persisted patterns for dual binary distribution (.aab + .apk), strict RELEASE-v*.md notes resolution, headless CI Gradle wrapper auto-provisioning, and Google Play versionCode increment strategy.
2. [2026-08-31] Verified Settings Persistence Hardening: 48 tests passed (100%). Confirmed "Auto-Scrub Clipboard" and other user settings survive cold app restarts via new AuthVaultModeRepositoryTest cases.
3. [2026-08-31] Unified Release Versioning & Parity: Set versionCode = 2 and versionName = "0.0.0.1" in build.gradle.kts to align 1:1 with Google Play Console update track; dynamically bound SettingsScreen release footer to BuildConfig.VERSION_NAME.
4. [2026-08-31] Audited & Hardened Settings Persistence across app restarts: verified ThemeMode, ThemeAccent, Biometrics, Vault Protection PIN/Password, Guided Tour, and Session persistence; wired Auto-Scrub Clipboard into AuthRepository SharedPreferences and added restart persistence tests.
5. [2026-08-31] Verified Theme Migration & Color Bindings: 100% test pass (46/46) and successful build. Configured Reef Pink (BrandLobsterRed) as global default and migrated screens to dynamic MaterialTheme tokens.
6. [2026-08-31] Configured Reef Pink (BrandLobsterRed) as default theme across entire app: migrated HatchVaultScreen, LoginScreen, GatewayScreen, QrScannerScreen, and SpotlightOverlay from hardcoded cyan to dynamic MaterialTheme tokens, and enhanced Monochrome contrast for Light Mode.
7. [2026-08-30] Resolved 16 KB page-size compatibility & Robolectric test looper deadlock: upgraded sqlcipher to 4.6.1, configured jniLibs.useLegacyPackaging = false, and refactored LocalModeUnlockAndVaultTest to scope runBlocking strictly around async IO.
8. [2026-08-30] Implemented SpotlightOverlay interactive guided tour with circular cutout punching, animated glowing rings, and tooltip pill navigation.
9. [2026-08-30] Updated SettingsScreen with Server Sync Card, Offline Codes Section, and verified complete test suite pass.
10. [2026-08-30] Persisted /learn patterns into systemPatterns.md: Android 12+ Splash Screen, Cloud Backup Rules, ProGuard Keep Rules, and Compose Spotlight Cutout overlay.
