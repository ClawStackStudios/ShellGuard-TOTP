# Raw Reflection Log

<!-- Processed entries from Phase 9, Phase 10, Release v0.0.1.2, Store Listing Assets, and Hotfix v0.0.2.2 have been consolidated into .agents/memory-bank/consolidated_learnings.md -->
<!-- New raw reflections from subsequent tasks will be captured below -->


---
Date: 2026-09-05
TaskRef: "GitHub Actions CI Release & Mirror Pipeline Optimization"

Learnings:
- Heavy Android Gradle build steps in GitHub Actions should be strictly gated behind version tags or intentional release commit flags (`--release`) to avoid consuming GitHub runner quotas on docs-only or incremental refactors.
- Chaining downstream jobs using `needs: [job_name]` combined with `if: always() && ...` allows the dependent job to evaluate standalone trigger conditions even when the upstream job was skipped, while still enforcing serial execution when both are scheduled.
- Re-arranging README hero branding with the logo centered above the title followed by badges and feature graphic maximizes visual balance on desktop and mobile GitHub preview renderers.

Difficulties:
- Initially `mirror` job would fail to run on documentation updates if `release` was skipped without `always()` in the conditional expression. Resolved by combining `if: always() && (...)` with explicit condition checks.

Successes:
- Optimized `.github/workflows/release.yml` with dual-mode gating: full release builds run on tags/`--release` commits, while release note mirroring executes on release builds and standalone `RELEASE-v*.md` changes.
- 96/96 unit and Robolectric tests verified 100% green before push.
---
Date: 2026-09-11
TaskRef: "Audit Bot PR #1 & Implement AddSecretScreen Secret Input Masking & Keyboard Hardening"

Learnings:
- Automated bot PRs (e.g. Jules / Sentinel) may submit empty commits with zero diff while reporting passing unit and lint checks. Always cross-verify tree hashes (`git rev-parse HEAD^{tree}`) and diffs directly against base before review.
- On Android, sensitive inputs like 2FA Base32 seeds require `KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)` to prevent system keyboards (Gboard/SwiftKey) from caching secret keys in predictive learning dictionaries.
- Unconditionally hiding 2FA Base32 seeds with `PasswordVisualTransformation()` creates a major usability obstacle for users manually typing 16-32 character codes; pairing the visual transformation with a toggleable eye icon (`toggle_secret_visibility`) alongside the Base32 validation indicator gives both privacy protection and character verification.

Difficulties:
- In Compose UI Robolectric tests, `assertExists` is not a top-level assertion function; use `assertIsDisplayed().assertHasClickAction()` on `onNodeWithTag`.

Successes:
- Hardened `AddSecretScreen.kt` with `PasswordVisualTransformation`, `KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)`, and a dynamic visibility toggle.
- 100% green test suite across all 33 tasks and clean `assembleDebug` build verification.
---

