# Decision Log

## 2026-09-22 — dynamic totp uri sync parity & delta fast-filter
Piping decrypted secrets through `TotpUriParser.parse()` instead of raw Base32 injection resolved backend sync failures when the web server emitted full `otpauth://` URIs with SHA256/8-digit/60s parameters. Added `isContentIdentical()` fast-filter in `TotpRepository` because web server remote sync omitted `updated_at`, preventing unnecessary Room SQLite transaction churn and UI recomposition flashes.

## 2026-09-22 — panic wipe broadcast & room v2 migration
Configured `PanicTriggerReceiver` to listen for emergency panic broadcasts with a fallback `clearAll()` covering KeyStore keys, SQLCipher Room DB, and encrypted preferences. Bumped `ShellGuardTotpDatabase` to version 2 with `fallbackToDestructiveMigration()` and verified full test suite synchronization with Robolectric SQLite open helper.
