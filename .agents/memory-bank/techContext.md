# ShellGuard TOTP — Tech Context

## Tech Stack
- **Language**: Kotlin 2.0+
- **UI Toolkit**: Jetpack Compose (BOM 2024.11.00), Material Design 3
- **Local Persistence**: AndroidX Room 2.7.0 + SQLCipher Android 4.6.1 (16 KB Page Aligned)
- **Networking**: Ktor 3.0.1 (Android engine + Kotlinx Serialization JSON)
- **Camera & Vision**: CameraX 1.3.4 + Google ML Kit Barcode Scanning 17.3.0
- **Hardware Security**: Android KeyStore + AndroidX Biometric 1.2.0-alpha05 + AndroidX Security Crypto 1.1.0-alpha06
- **Background Scheduling**: AndroidX WorkManager 2.10.0
- **Testing**: JUnit 4, Robolectric 4.14.1, Roborazzi 1.39.0
