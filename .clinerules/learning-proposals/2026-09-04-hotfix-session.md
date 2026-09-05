# Learning Proposal — 2026-09-04 (Session: v0.0.1.3 Hotfix)

> request_feedback = true — awaiting Lucas's approval before any file modifications.

## Proposed Changes

### 1. CREATE — `.clinerules/workflows/android-device-adb-verification.md` (Skill/Workflow)

**Classification**: Skill (multi-step tool chain with diagnostic cheatsheet).
**Rationale**: Used 3× this session for live UI verification on Lucas's Pixel 8; two traps hit and resolved (display-suspend black frames misread as FLAG_SECURE blocking; app relaunch needed after streamed install). Pair with existing `android-container-gradle-testing.md`.

**Proposed content (full file)**:

```markdown
# 📱 Android On-Device UI Verification Workflow

> **Use When:** Verifying UI changes, card layouts, or sync behavior on a physical
> device (Pixel 8) from the container. Pairs with `android-container-gradle-testing.md`.

## Device Access
- adb: `/config/Android/Sdk/platform-tools/adb` (not on PATH)
- Device connects via wireless ADB (`adb-..._adb-tls-connect._tcp`); confirm with `adb devices -l`

## Standard Verification Loop
1. Build: `./gradlew assembleDebug --no-daemon` (container GRADLE_OPTS exports required)
2. Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
3. Relaunch: `adb shell am force-stop com.clawstack.shellguard.totp && adb shell monkey -p com.clawstack.shellguard.totp -c android.intent.category.LAUNCHER 1`
4. Wait ~6s for settle, then screencap

## Screenshot Capture
adb shell screencap -p /sdcard/shot.png && adb pull /sdcard/shot.png app/build/shot.png && adb shell rm /sdcard/shot.png
(Pull into app/build/ — persistent; /tmp is ephemeral per shell)

## 🚨 Black-Frame Triage (learned 2026-09-04)
A uniform-black, small (~15KB) screencap PNG means **the display was suspended at
capture time — NOT FLAG_SECURE blocking**. Debug builds are exempt from
FLAG_SECURE (`!BuildConfig.DEBUG` gate in MainActivity.kt). Triage order:
1. Wake immediately before capture: `adb shell input keyevent KEYCODE_WAKEUP`
2. Confirm: `adb shell dumpsys power | grep mWakefulness` → Awake
3. Confirm foreground: `adb shell dumpsys activity activities | grep topResumedActivity`
4. Re-capture. A healthy frame is ~100-200KB on Pixel 8.

## Flagged-for-future (not yet implemented)
- Task 24 will add a user-facing "Allow screenshots" toggle (FLAG_SECURE opt-in);
  full spec lives in ROADMAP.md Task 24. Release builds WILL block screencap of
  the locked/unlocked UI once that ships unless toggled.
```

### 2. UPDATE — `.clinerules/workflows/version-update.md` (Step 5 refinement)

**Classification**: Update existing workflow (missed edge cases hit in practice).
**Rationale**: The workflow stops at "Memory Bank Sync"; today's release added two
practical steps it lacks.

**Proposed diff — append after Step 4:**

```markdown
---

## 📦 Step 5: Release Anchor Checklist (all-or-nothing sync)

Update ALL anchors in one commit; partial syncs drift:
1. `app/build.gradle.kts` — versionCode (+1) & versionName
2. `README.md` — version badge
3. `CHANGELOG.md` — `## [X.Y.Z.N] - YYYY-MM-DD (Build N)` section
4. `RELEASE-vX.Y.Z.N.md` — from `.agents/templates/release-template.md`
5. `RELEASE-PLAY.md` — prepend `<en-US>` block (<500 chars)
> ROADMAP.md Phase headings are HISTORICAL — do not touch them for hotfix
> releases; future-phase version anchors (e.g. v0.0.2.1) must stay aligned.

## ☁️ Step 6: Push & CI Verification (gh CLI)

1. Commit + tag locally: `git tag vX.Y.Z.N`
2. Push (triggers Release Pipeline): `git push origin main && git push origin vX.Y.Z.N`
3. Verify: `gh run list --limit 2` → `gh run watch <id> --exit-status`
4. Confirm artifacts: `gh release view vX.Y.Z.N --json assets --jq '{assets: [.assets[] | {name, size}]}'`
   (expect signed .aab + .apk; gh at `/config/.local/bin/gh`, authenticated)
5. Log release in memory-bank raw_reflection_log + activeContext.
```

### 3. NO ACTION — Nullable-timestamp delta sync invariant
Already captured in `consolidated_learnings.md` (Nullable-Timestamp Delta Sync,
CRITICAL pattern) and codified in Task 24 spec + `classifyDeltaPearls`. A separate
rule would be redundant.
