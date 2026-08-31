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

