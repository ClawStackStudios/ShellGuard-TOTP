# Learning Proposal: Reusable Behaviors & Patterns from Phase 5 & 6

## 1. Context & Analysis

During the execution of Phase 5 (Bidirectional Delta Sync & Interactive Spotlight Tour) and Phase 6 (Adaptive Icon, Android 12+ Splash Screen & Hardening), several key architectural patterns and fixes were validated:

1. **Android 12+ Splash Screen Theme Wiring**:
   - `Theme.App.Starting` in `themes.xml` with `windowSplashScreenBackground`, `windowSplashScreenAnimatedIcon`, and `postSplashScreenTheme`.
   - Applying `android:theme="@style/Theme.App.Starting"` on both `<application>` and the launch `<activity>` in `AndroidManifest.xml`.
   - Calling `installSplashScreen()` in `MainActivity.onCreate()` *strictly prior* to `super.onCreate(savedInstanceState)`.
2. **Encrypted Storage Backup Exclusion Rules**:
   - Explicitly excluding SQLite WAL/SHM journal files and database roots in `res/xml/backup_rules.xml` and `res/xml/data_extraction_rules.xml` (`<cloud-backup>`) while permitting direct encrypted `<device-transfer>`.
3. **Jetpack Compose Spotlight Cutout Pattern**:
   - Fullscreen `Canvas` with `BlendMode.Clear` on a `graphicsLayer { alpha = 0.99f }` to punch precise transparent circular cutouts over target UI elements dynamically measured via `onGloballyPositioned` and `positionInRoot()`.
4. **ProGuard/R8 Rules for SQLCipher, Ktor & Serialization**:
   - Keeping Room DAOs/Entities, Ktor OkHttp engines, and `@Serializable` companion serializers to prevent runtime reflection stripping during release APK compilation.

---

## 2. Classification & Recommended Updates

### Proposed Destination: Update Memory Bank & Custom Project Instructions (`AGENTS.md` / `systemPatterns.md`)

- **Classification**: **Rule / Architecture Pattern**
- **Rationale**: These patterns represent security, UI/UX, and compilation invariants for this native Android + SQLCipher + Ktor stack.

### Proposed Additions to `.agents/memory-bank/systemPatterns.md`:

```markdown
### Release Hardening & Splash Screen Pattern
- **Android 12+ Splash Screen**:
  - Always declare `Theme.App.Starting` with `windowSplashScreenBackground`, `windowSplashScreenAnimatedIcon`, and `postSplashScreenTheme`.
  - Always call `installSplashScreen()` before `super.onCreate(savedInstanceState)` in `MainActivity`.
  - Set `android:theme="@style/Theme.App.Starting"` on `<application>` and `<activity>` in `AndroidManifest.xml`.
- **Encrypted Storage Cloud Backup Guardrail**:
  - Always exclude SQLCipher DBs (`.db`, `.db-wal`, `.db-shm`) and `EncryptedSharedPreferences` in `backup_rules.xml` and `data_extraction_rules.xml`.
- **Dynamic Spotlight Cutout in Compose**:
  - Use `graphicsLayer { alpha = 0.99f }` on a `Canvas` with `BlendMode.Clear` for dynamic spotlight overlays over targeted composables.
```

---

## 3. Feedback Request
Would you like me to persist these patterns to `.agents/memory-bank/systemPatterns.md` or into a dedicated custom instructions file like `AGENTS.md`?
