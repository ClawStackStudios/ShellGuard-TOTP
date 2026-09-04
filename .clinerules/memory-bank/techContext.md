# ShellGuard TOTP — Tech Context

## Tech Stack
- **Language**: Kotlin 2.0+
- **UI**: Jetpack Compose (BOM 2024.11.00), Material Design 3
- **Persistence**: AndroidX Room 2.7.0 + SQLCipher Android 4.6.1 (16 KB page aligned, `useLegacyPackaging = false`)
- **Networking**: Ktor 3.0.1 (Android engine + Kotlinx Serialization)
- **Camera & Vision**: CameraX 1.3.4 + Google ML Kit Barcode Scanning 17.3.0
- **Security**: Android KeyStore + AndroidX Biometric 1.2.0-alpha05 + Security Crypto 1.1.0-alpha06
- **Background**: WorkManager 2.10.0
- **Testing**: JUnit 4, Robolectric 4.14.1, Roborazzi 1.39.0

## Build & Test Environment
```bash
export JAVA_HOME="/config/Applications/android-studio/jbr"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew testDebugUnitTest --no-daemon
```
- Gradle 9.3.1 wrapper committed; cached dists in `/config/.gradle/wrapper/dists/`.
- `gradle.properties` carries `-XX:-UsePerfData`; test `java.io.tmpdir` → `app/build/tmp`.
- Robolectric Room config must use `FrameworkSQLiteOpenHelperFactory` (detect via `Class.forName("org.robolectric.Robolectric")`).
- KeyStore wrappers (AndroidKeyStoreHelper, EncryptedDeviceVault) have HMAC `SecretKeySpec` fallback for headless JVM tests.

## Constraints
- Client-side only app; cleartext HTTP is intentional (local networks / VPN).
- No NDK/native code beyond SQLCipher binaries.
- Never hardcode brand colors; bind to `MaterialTheme.colorScheme` + `LocalShellGuardColors`.