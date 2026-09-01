# ShellGuard TOTP — Active Context

## Sliding Window of Events
1. [2026-08-31] Release Version Bump to Build 3: Bumped versionCode = 3 in build.gradle.kts and RELEASE-PLAY.md, maintaining versionName = "0.0.0.1" for Google Play Console parity with package com.clawstack.shellguard.totp.
2. [2026-08-31] Dedicated Play Store Notes & Workflows: Created RELEASE-PLAY.md with <en-US> tags and synchronized version-update.md and play-console-release-workflow.md protocols.
3. [2026-08-31] Python 3 Keystore Decoding & Native AGP Signing: Replaced deprecated r0adkll third-party signing with native Android Gradle Plugin signing via Python 3 base64 decoding from env vars, eliminating Node deprecations and GNU base64 wrapping errors.
4. [2026-08-31] Adaptive Signing & Headless CI Invariant: Implemented conditional signingConfigs in build.gradle.kts to decouple physical keystore existence on disk from headless CI builds, enabling clean artifact compilation and downstream GitHub Actions signing.
5. [2026-08-31] Unified Release Pipeline & CI Gradle Invariants: Persisted patterns for dual binary distribution (.aab + .apk), strict RELEASE-v*.md notes resolution, headless CI Gradle wrapper auto-provisioning, and Google Play versionCode increment strategy.
6. [2026-08-31] Verified Settings Persistence Hardening: 48 tests passed (100%). Confirmed "Auto-Scrub Clipboard" and other user settings survive cold app restarts via new AuthVaultModeRepositoryTest cases.
7. [2026-08-31] Unified Release Versioning & Parity: Set versionCode = 2 and versionName = "0.0.0.1" in build.gradle.kts to align 1:1 with Google Play Console update track; dynamically bound SettingsScreen release footer to BuildConfig.VERSION_NAME.
8. [2026-08-31] Audited & Hardened Settings Persistence across app restarts: verified ThemeMode, ThemeAccent, Biometrics, Vault Protection PIN/Password, Guided Tour, and Session persistence; wired Auto-Scrub Clipboard into AuthRepository SharedPreferences and added restart persistence tests.
9. [2026-08-31] Verified Theme Migration & Color Bindings: 100% test pass (46/46) and successful build. Configured Reef Pink (BrandLobsterRed) as global default and migrated screens to dynamic MaterialTheme tokens.
10. [2026-08-31] Configured Reef Pink (BrandLobsterRed) as default theme across entire app: migrated HatchVaultScreen, LoginScreen, GatewayScreen, QrScannerScreen, and SpotlightOverlay from hardcoded cyan to dynamic MaterialTheme tokens, and enhanced Monochrome contrast for Light Mode.
