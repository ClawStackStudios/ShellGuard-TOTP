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
