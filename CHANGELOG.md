# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
- No unreleased changes yet.

## [0.0.0.2] - 2026-09-01
### Fixed
- **CI Pipeline**: Unconditionally register the release signing configuration in `app/build.gradle.kts` to bypass Gradle Configuration Cache misses during tag release workflows (Build 5).
- **Test Stability**: Resolved Robolectric `JobCancellationException` and `ComposeTimeoutException` issues during CI headless unit tests (Build 4).

### Added
- **Migration Engine**: Added `BitwardenSanitizer` to parse standalone Bitwarden Authenticator and Password Manager exports.

### Changed
- **UI Architecture**: Implemented `IntakeWelcomeScreen` and `IntakeViewModel` to support new user onboarding flow and JSON import processing.

## [0.0.0.1] - 2026-08-31
### Added
- **Core Base**: Initial project setup, 16 KB page size alignment for Android 15, SQLCipher 4.6.1 integration.
