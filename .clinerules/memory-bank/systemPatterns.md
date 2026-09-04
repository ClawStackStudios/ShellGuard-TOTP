# ShellGuard TOTP — System Patterns

## Architecture: Clean Architecture + MVI
```
UI (Compose) → ViewModel (StateFlow/Intent) → UseCase/Repository → Data Source (Room SQLCipher / Ktor / KeyStore)
```

## Security Invariants
1. SQLCipher AES-256 at rest; DB key derived from Android KeyStore hardware key.
2. ShellCryption AAD binding `vault_pearls_totp:{record_id}` validated on every decrypt.
3. KeyStore wrapper aliases: `sg_totp_biometric_wrapper`, `sg_totp_pin_wrapper`, `sg_totp_password_wrapper`.
4. Zero-knowledge import sanitization — passwords/notes/cards stripped in RAM before Room write.
5. TOTP seeds stored Base32; cleared on session purge.

## One-Way Mirror Sync
- Local creations: `isLocalOnly = true`, never pushed upstream.
- Remote pull is read-only; offline cache preserved; remote pruning keeps local-only rows.
- `BackupManager` exports Local Codes only into `sgtotp.bak` (`protectionMode` stamped via `vaultMode.name`).

## Ingestion & Deduplication
- Pre-DAO fingerprint dedup: `secret.uppercase().replace(" ","").replace("-","") + "_" + title.trim().lowercase()` — filter against existing rows before `upsertItems()` (SQLite REPLACE only conflicts on UUID PK).

## ClawKey Identity (Phase 15, spec'd)
- Format: `hu-` + 64 chars = 67 total. Single validator `ClawKeyValidator.isValid()`.

## UI Patterns
- Spotlight overlay: `onGloballyPositioned` bounds + Canvas `BlendMode.Clear` on `graphicsLayer { alpha = 0.99f }`, +18dp breathing cutouts, spring physics.
- Splash: `Theme.App.Starting`, `installSplashScreen()` before `super.onCreate()`.
- Staggered slide-up entrance lists, `Modifier.animateItem()` placement animations.
- IME: `.imePadding()` + `.verticalScroll(rememberScrollState())` on input screens.

## Release Pattern
- `.github/workflows/release.yml`: tag `v*` or `--release X.Y.Z.N` flag → signed `.aab` + `.apk` → GitHub Release.
- Dual notes: `RELEASE-vX.Y.Z.N.md` (GitHub) + `RELEASE-PLAY.md` (<500 chars, Play).
- ProGuard keeps for SQLCipher, Ktor, Kotlinx Serialization, Room.

## Process: Rule of 2
Every phase = Task A (Functionality/Engine) + Task B (UI Component), cross-referenced 1:1 between `ROADMAP.md` and `project/meta-prompt-ai-studio.md` stages.