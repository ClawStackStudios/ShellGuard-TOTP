# ShellGuard TOTP — System Patterns

## Architecture: Clean Architecture + MVI

```
UI (Jetpack Compose) → ViewModel (StateFlow/Intent) → UseCase/Repository → Data Source (Room DB / Ktor Client)
```

## Security Invariants
1. **SQLCipher Database**: All tables encrypted at rest with AES-256.
2. **KeyStore StrongBox Wrapper**: Biometric key `sg_totp_biometric_wrapper` guarded by user authentication.
3. **ShellCryption AAD Binding**: AAD format `vault_pearls_totp:<record_id>` validated on every decrypt operation.
4. **Offline Resilience**: Local cache is strictly preserved on network errors and remote pruning preserves `is_local_only = 1`.
5. **Memory Safety**: TOTP seeds stored in Base32, cleared when sessions are purged.
6. **Zero-Knowledge Backup Intake & Sanitization**: Imported Bitwarden, Aegis, 2FAS, or ShellGuard exports have passwords, notes, cards, and metadata completely stripped in volatile RAM before writing to Room SQLCipher storage (0% non-TOTP secret leakage).

## First-Run Brand Hero & Intake Architecture
```
SAF File Picker (OpenDocument) → MultiVaultBackupPreValidator (Schema & RAM Sanitizer)
       ↓ (Encrypted)                                   ↓ (Plain/Sanitized)
PASSWORD_PROMPT BottomSheet                  SUMMARY_CONFIRM BottomSheet (Protection Mode + PIN)
       ↓ (Decrypted)                                   ↓ (Hatch & Upsert)
AuthRepository.hatchVault() + TotpItemDao.upsertItems() → Screen.CodeList
```


## Release Hardening & Splash Screen Patterns
1. **Android 12+ Splash Screen Integration**:
   - Declare `Theme.App.Starting` with `windowSplashScreenBackground`, `windowSplashScreenAnimatedIcon`, and `postSplashScreenTheme` in `themes.xml`.
   - Always call `installSplashScreen()` before `super.onCreate(savedInstanceState)` in `MainActivity`.
   - Set `android:theme="@style/Theme.App.Starting"` on `<application>` and `<activity>` in `AndroidManifest.xml`.
2. **Encrypted Storage Cloud Backup Guardrail**:
   - Always exclude SQLCipher DBs (`.db`, `.db-wal`, `.db-shm`) and `EncryptedSharedPreferences` in `backup_rules.xml` and `data_extraction_rules.xml` to prevent unencrypted cloud sync.
3. **Jetpack Compose Spotlight Cutout Pattern**:
   - Dynamically capture target component bounds with `Modifier.onGloballyPositioned` and `coordinates.positionInRoot()`.
   - Render overlay with `graphicsLayer { alpha = 0.99f }` and punch circular cutouts on `Canvas` using `BlendMode.Clear`.
4. **ProGuard & R8 Serialization Keep Rules**:
   - Preserve Kotlinx Serialization companions and serializers, SQLCipher native libraries, Room DAOs, and Ktor OkHttp engines.

## Strict Dynamic Material 3 Theming
1. **Dynamic Color Binding**:
   - No hardcoded color HEX values in Screen/Component logic.
   - Use `MaterialTheme.colorScheme` tokens (`primary`, `surface`, `onBackground`, etc.) to ensure immediate adaptation to user-selected palettes.
   - For custom brand colors not in the standard M3 spec, use a `CompositionLocal` provider (`LocalShellGuardColors`).
2. **Adaptive Contrast Invariants**:
   - Monochrome mode must dynamically switch between Silver Pearl (Dark Mode) and Slate (Light Mode) to maintain accessibility.
   - Surfaces must use `surfaceVariant` or `outline` for subtle carapace borders to preserve the marine aesthetic across all themes.

## Android 15+ 16 KB Page Alignment Pattern
1. **Zetetic SQLCipher 4.6.1+**: Drop-in release compiled with 16 KB ELF segment alignment for 64-bit kernels.
2. **Uncompressed JNI Packaging**: Configure `jniLibs.useLegacyPackaging = false` in `app/build.gradle.kts` so native libraries are stored uncompressed and page-aligned on 16 KB boundaries inside APKs and AABs.

## Settings Persistence & Cold-Restart Validation Pattern
1. **Persistent Backing**: All user preferences (theme mode, accents, biometrics, clipboard scrub, vault PIN/passwords) must be backed by `SharedPreferences` or `EncryptedDeviceVault`.
2. **Cold-Restart Simulation Tests**: Test classes (e.g. `AuthVaultModeRepositoryTest`) instantiate a fresh repository instance after writing preferences to assert 100% reload fidelity from disk.

## IME Soft-Keyboard Insets & Scroll Ergonomics Pattern
1. **IME Insets**: Form and onboarding screens must declare `.imePadding()` and `.verticalScroll(rememberScrollState())` on root columns.
2. **Floating Actions**: Prevents soft keyboards from obscuring password inputs, Base32 fields, and submit buttons on small devices.

## Dynamic Release Versioning Pattern
1. **Single Source of Truth**: `versionCode` (strictly monotonic integer) and `versionName` in `app/build.gradle.kts` match Google Play Console tracks.
2. **UI Dynamic Binding**: Settings footers read directly from `BuildConfig.VERSION_NAME` to automatically reflect version bumps.
3. **Release Updates Invariant**: When re-releasing or updating a release version (e.g. `0.0.0.1`), increment `versionCode` (+1 integer, e.g. `2`) while keeping `versionName = "0.0.0.1"` for human visibility.

## Unified CI/CD Release Pipeline Pattern
1. **Dual Binary Artifact Distribution**:
   - Release workflows produce both signed `app-release.aab` (Google Play Console) and `app-release.apk` (Direct FOSS install).
2. **Strict Release Notes Resolution**:
   - Releases mandate `RELEASE-vX.Y.Z.N.md` in repository root as the single source of truth for release notes.
3. **Trigger Flexibility**:
   - Supports both `--release vX.Y.Z.N` commit message flags and `v*` git tag pushes, with auto-tagging on commit flags.
4. **Release Notes Mirroring**:
   - Edits to `RELEASE-v*.md` files on `main` automatically sync to GitHub Release descriptions via a lightweight mirror job.

## Headless CI Gradle Provisioning Pattern
1. **Setup Action**: Use `gradle/actions/setup-gradle@v4` to provide Gradle in `PATH` and configure dependency caching.
2. **Auto-Wrapper Fallback**: Ensure the wrapper exists and is executable (`if [ ! -f "gradlew" ]; then gradle wrapper --gradle-version X.Y.Z; fi; chmod +x gradlew`) before running Gradle tasks.

## Adaptive Signing Configuration Pattern
1. **Conditional Keystore Binding**: In `app/build.gradle.kts`, check `releaseKeystoreFile.exists()` before configuring `signingConfigs.create("release")` and attaching it to `buildTypes.release`.
2. **Headless Keystore Decoding**: In cloud CI, pass the base64 secret through environment variables (`SIGNING_KEY`) and decode via Python 3 (`base64.b64decode(os.environ['SIGNING_KEY'].strip())`) to ensure whitespace and newline resilience without GNU `base64: invalid input` errors.
3. **In-Engine AGP Signing**: Gradle natively compiles and cryptographically signs both `.aab` and `.apk` using Android Gradle Plugin's built-in `apksigner` and `bundletool` engines, eliminating deprecated third-party signing actions.
4. **Automated Ephemeral Key Cleanup**: A step with `if: always()` immediately removes `my-upload-key.jks` after compilation to ensure zero lingering private key exposure on runner disks.

## Dedicated Google Play Release Notes Pattern
1. **500-Character Constraint**: Maintain `RELEASE-PLAY.md` at repository root with `<en-US>` tags strictly under 500 characters for direct copy-paste into Google Play Console.
2. **Dual Notes Synchronization**: Every release bump updates both `RELEASE-vX.Y.Z.N.md` (detailed GitHub changelog) and `RELEASE-PLAY.md` (concise mobile store highlights).

## Branded Binary Packaging Pattern
1. **Branded Artifact Staging**: CI pipelines copy and stage release binaries under explicit project names (`shellguard-totp-${TAG}.aab` and `shellguard-totp-${TAG}.apk`).
2. **Dual Distribution**: Attach both Google Play Store App Bundles (`.aab`) and standalone direct-install APKs (`.apk`) to every GitHub Release.

## Synergistic 2-Task Phase Architecture (The Rule of 2)
1. **The Rule of 2**: Every development phase strictly consists of Task A (Core Functionality / Crypto / Backend Engine) paired with Task B (UI Component / State / Interactions).
2. **Meta-Prompt 1:1 Cross-Referencing**: All execution prompts in `project/meta-prompt-ai-studio.md` explicitly reference the corresponding `ROADMAP.md` Phase and Task IDs for deterministic execution.

## User-First Intake & Spotlight Invariants
1. **Brand Hero & Dual-Track Intake**: First launch presents app launcher shield branding, instant SAF "Import Habitat" pre-validation, and smooth bottom-right forward navigation for fresh security setup.
2. **Spacious Spotlight Geometry**: Target cutouts enforce +16dp to +20dp radial offset beyond view bounds to ensure clear visual focus during onboarding before landing on `TotpEmptyState`.

## Zero-Knowledge Bitwarden Migration & Dual-Routing Patterns
1. **Zero-Knowledge Sanitization**: Ingests Bitwarden Password Manager and Authenticator JSON, immediately extracting `login.totp` and mapping `folders[]` to Pod categories while purging passwords, secure notes, card numbers, and custom fields entirely in RAM.
2. **Steam Guard 2FA Generation**: Uses Steam's 26-char custom alphanumeric alphabet (`23456789BCDFGHJKMNPQRTVWXY`) for 5-char code generation.
3. **Conflict Resolution Policy**: Supports `SKIP_DUPLICATES`, `OVERWRITE_EXISTING`, and `KEEP_BOTH` policies during batch imports.
4. **Dual-Pathway Persistence Routing**:
   - *Local Pathway*: Directly writes to Room SQLCipher with `is_local_only = 1`.
   - *Remote Gateway Pathway*: Encrypts via `ShellCryptionEngine` (`huKey` + `userUuid` + AAD `vault_pearls_totp:{id}`) and pushes upstream via `POST /api/vault`.
5. **Post-Commit Hooks**: Automatically appends `IMPORT_SUCCESS` events to `AuditLogEntity` and triggers encrypted auto-backups via `BackupManager`.




