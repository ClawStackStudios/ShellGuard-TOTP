# Decision Log

## 2026-09-22 — dynamic totp uri sync parity & delta fast-filter
Piping decrypted secrets through `TotpUriParser.parse()` instead of raw Base32 injection resolved backend sync failures when the web server emitted full `otpauth://` URIs with SHA256/8-digit/60s parameters. Added `isContentIdentical()` fast-filter in `TotpRepository` because web server remote sync omitted `updated_at`, preventing unnecessary Room SQLite transaction churn and UI recomposition flashes.

## 2026-09-22 — panic wipe broadcast & room v2 migration
Configured `PanicTriggerReceiver` to listen for emergency panic broadcasts with a fallback `clearAll()` covering KeyStore keys, SQLCipher Room DB, and encrypted preferences. Bumped `ShellGuardTotpDatabase` to version 2 with `fallbackToDestructiveMigration()` and verified full test suite synchronization with Robolectric SQLite open helper.

## 2026-10-03 — git advanced history engineering & test oracle bisect
Pattern matched the Git Advanced Workflows skill into our hygiene rules and consolidated learnings. Codified `--fixup` autosquash workflows with two-layer attribution pairing and automated binary search bug hunting via `git bisect run ./gradlew testDebugUnitTest`.

## 2026-10-12 — cloud ci offload & headless keystore fallback
Local JVM Gradle test suites and debug builds exhausted host memory and froze terminals. Offloaded verification to GitHub Actions CI (`.github/workflows/ci.yml`) and codified a push-and-tail verification gate. Wrapping `signingConfigs.create("debugConfig")` in `if (debugKeystoreFile.exists())` within `app/build.gradle.kts` resolved missing keystore failures on headless runners, enabling automatic AGP default keystore generation.

