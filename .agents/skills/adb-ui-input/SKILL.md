---
name: adb-ui-input
description: Cheatsheet for driving Android device UI over ADB — text input quirks, keyboard handling, screenshot verification loop. Hardened through ShellGuard-TOTP on-device testing (Pixel, TLS ADB).
license: MIT
metadata:
  author: ClawStack Studios
  keywords: [adb, input, screencap, ui-automation, android-testing]
---

# ADB UI Input & Verification Cheatsheet

## Connection
```bash
ADB="/config/Android/Sdk/platform-tools/adb -s adb-<DEVICE-ID>._adb-tls-connect._tcp"
$ADB exec-out screencap -p > /tmp/screen.png   # screenshot for verification
```

## Core Invariants
1. **Screenshot-verify EVERY input before acting on assumed state.** The field contents you assume are wrong until the screenshot says otherwise (dot-count = truth for password fields).
2. **`input text` APPENDS** to the focused field — it never replaces content.
3. **Clearing a field:** tap field → `input keyevent 123` (cursor to end) → `input keyevent 67` (DEL) ×N → **screenshot to confirm 0 chars** → then type.
4. **The IME keyboard covers the bottom third of the screen.** Dismiss it with `input keyevent 4` (BACK) before tapping bottom-anchored buttons (Unlock / Confirm / FAB).
5. `input keyevent 111` (ESC) closes dialogs/popups only; use `keyevent 4` for the keyboard.
6. **Prefer tapping the visible submit button** over `keyevent 66` (Enter) — Enter behavior varies per field type (some insert a newline instead of submitting).
7. **Every `adb install -r` re-locks a PIN-protected vault** (backgrounding invariant / FLAG_SECURE). Budget an unlock step after every reinstall.
8. Slow animations / first-launch: add `sleep 2–5` after navigation taps before screencap, or the screenshot races the transition.

## Recipe: Reliable Field Entry
```bash
$ADB shell input tap <field-x> <field-y>; sleep 1
$ADB shell input keyevent 123
for i in $(seq 1 15); do $ADB shell input keyevent 67; done
# screenshot -> verify empty
$ADB shell input text 1234
$ADB shell input keyevent 4      # dismiss keyboard
$ADB shell input tap <submit-btn>
# screenshot -> verify result
```
