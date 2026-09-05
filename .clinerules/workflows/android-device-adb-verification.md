# 📱 Android On-Device UI Verification Workflow

> **Use When:** Verifying UI changes, card layouts, or sync behavior on a physical
> device (Pixel 8) from the container. Pairs with `android-container-gradle-testing.md`.
> **Source:** Codified via /learn from the 2026-09-04 v0.0.1.3 hotfix session.

## Device Access
- adb: `/config/Android/Sdk/platform-tools/adb` (not on PATH)
- Device connects via wireless ADB (`adb-..._adb-tls-connect._tcp`); confirm with `adb devices -l`

## Standard Verification Loop
1. Build: `./gradlew assembleDebug --no-daemon` (container GRADLE_OPTS exports required)
2. Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
3. Relaunch: `adb shell am force-stop com.clawstack.shellguard.totp && adb shell monkey -p com.clawstack.shellguard.totp -c android.intent.category.LAUNCHER 1`
4. Wait ~6s for settle, then screencap

## Screenshot Capture
```bash
adb shell screencap -p /sdcard/shot.png && adb pull /sdcard/shot.png app/build/shot.png && adb shell rm /sdcard/shot.png
```
(Pull into `app/build/` — persistent; `/tmp` is ephemeral per shell)

## 🚨 Black-Frame Triage (learned 2026-09-04)
A uniform-black, small (~15KB) screencap PNG means **the display was suspended at
capture time — NOT FLAG_SECURE blocking**. Debug builds are exempt from
FLAG_SECURE (`!BuildConfig.DEBUG` gate in `MainActivity.kt`). Triage order:
1. Wake immediately before capture: `adb shell input keyevent KEYCODE_WAKEUP`
2. Confirm: `adb shell dumpsys power | grep mWakefulness` → Awake
3. Confirm foreground: `adb shell dumpsys activity activities | grep topResumedActivity`
4. Re-capture. A healthy frame is ~100-200KB on Pixel 8.

## Flagged-for-future (not yet implemented)
- Task 24 will add a user-facing "Allow screenshots" toggle (FLAG_SECURE opt-in);
  full spec lives in `ROADMAP.md` Task 24. Release builds WILL block screencap of
  the locked/unlocked UI once that ships unless toggled.
