# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
- No unreleased changes yet.

## [0.0.1.0] - 2026-09-02
### Added
- **Vault Security Orientation**: Implemented `VaultSecurityScreen` with zero-knowledge educational cards, interactive PIN/Password protection selector, real-time password strength meter, and biometric switch card.
- **Segregated Vault Grouping**: Vertically separated local on-device TOTP tokens ("📱 Local Vault") from cloud-synchronized tokens ("☁️ Synced from ShellGuard") on the main dashboard.
- **Hardware Key Isolation**: Extended `AndroidKeyStoreHelper` and `AuthRepository` to generate hardware-backed `AES-256-GCM` wrapper keys for PIN and Password protection modes.

### Changed
- **Spotlight Guided Tour**: Enhanced `SpotlightOverlay` with fluid `Spring` easing, density-aware +18dp radial padding, pulsing cyan halos, and stacked action controls with centered Skip button.
- **One-Way Mirror Sync**: Enforced strict read-only server sync and updated `BackupManager` to exclusively export local codes into `.sgtotp.bak`.
- **Navigation Architecture**: Integrated `VaultSecurityScreen` into `TotpNavHost` and transitioned fresh onboarding directly to `TotpEmptyState`.

### Removed
- **Legacy Components**: Removed deprecated `HatchVaultScreen.kt`.

## [0.0.0.2] - 2026-09-01
### Fixed
- **Play Console Compliance**: Upgraded `targetSdk` to API 36 (Android 16) and bumped `versionCode` to 6 for Google Play Console distribution requirements.
- **CI Pipeline**: Unconditionally register the release signing configuration in `app/build.gradle.kts` to bypass Gradle Configuration Cache misses during tag release workflows (Build 5).
- **Test Stability**: Resolved Robolectric `JobCancellationException`, `ComposeTimeoutException`, and cross-test database state pollution issues during CI headless unit tests (Build 4/5).

### Added
- **Migration Engine**: Added `BitwardenSanitizer` to parse standalone Bitwarden Authenticator and Password Manager exports.

### Changed
- **UI Architecture**: Implemented `IntakeWelcomeScreen` and `IntakeViewModel` to support new user onboarding flow and JSON import processing.

## [0.0.0.1] - 2026-08-31
### Added
- **Core Base**: Initial project setup, 16 KB page size alignment for Android 15, SQLCipher 4.6.1 integration.
