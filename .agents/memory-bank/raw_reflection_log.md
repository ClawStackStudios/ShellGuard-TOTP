# Raw Reflection Log

<!-- Processed entries from Phase 9, Phase 10, and Release v0.0.1.2 have been consolidated into .agents/memory-bank/consolidated_learnings.md -->
<!-- New raw reflections from subsequent tasks will be captured below -->

---
Date: 2026-09-04
TaskRef: "Google Play Store Asset Preparation, Pixel Device Screenshots & README Overhaul"

Learnings:
- Physical device screen capture via ADB screencap requires explicit display wake (`KEYCODE_WAKEUP`) to avoid black screen captures caused by aggressive display sleep timeouts.
- Google Play Console full description editor rejects markdown formatting (asterisks, hash headers, code backticks) and renders literal markdown tokens to store visitors; descriptions must be formatted as pure plain text with standard Unicode bullets (•) and uppercase section breaks under 4,000 characters.
- In Compose UI hierarchy navigation via ADB, keyboard presence can shift button bounds; closing soft keyboard (`KEYCODE_BACK`) prior to tapping submission targets ensures stable click coordinate mapping.
- Keeping internal community post drafts in `.agents/internal/` preserves confidentiality and avoids dirtying git commits via `.gitignore`.

Difficulties:
- Display sleep timeout on Pixel caused small (15KB) black screencap captures. Diagnosed and resolved by issuing `input keyevent KEYCODE_WAKEUP` and verifying pixel color histograms.
- Soft keyboard obscured lower form buttons during manual PIN entry on onboarding screen; solved by hiding keyboard before reading `uiautomator dump` bounds.

Successes:
- Captured 6 flawless 1080x1920 device screenshots directly from live hardware showing onboarding, hardware security, empty state, live animated TOTP token card, settings palettes, and authenticator gateway login.
- Overhauled README.md with snug hero layout (icon above title, badges row, full-width feature graphic banner, and 2x3 screenshot gallery).
- 100% test pass rate maintained (33 actionable tasks, 0 failures).
---

