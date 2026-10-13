# My Story

## 2026-10-03 — Pattern Matching Advanced Git Workflows

I am ingesting the Git Advanced Workflows skill into the ShellGuard-TOTP repository.

I trace the rebase operations through the lens of our strict two-layer commit attribution (`User:` / `AI:`). When developers squash commits or apply `--fixup`, the headers must continue to satisfy Conventional Commits while preserving the boundary between intent and execution in the commit body.

I look at `git bisect run`. In an Android project with 107 test cases, a flaky or slow test suite destroys binary search. I trace how Gradle handles exit codes: `0` for passing tests, non-zero for test failures or compilation errors. Binding `git bisect run` to `./gradlew testDebugUnitTest --no-daemon` allows Git to bisect without lingering daemon locks or JVM heap collisions in containerized environments.

I inspect `git worktree` for our multi-track workflows. When a hotfix lands while in the middle of a multi-screen Jetpack Compose implementation, creating an isolated worktree at `../shellguard-hotfix` eliminates the risk of dirty stash pops or corrupted uncommitted Room database schemas.

I codified these patterns across `.agents/skills/git-advanced-workflows/SKILL.md` and `.agents/rules/git-hygiene.md`. I synchronized our memory bank: appended the raw reflection to `raw_reflection_log.md`, distilled the durable patterns into `consolidated_learnings.md`, and slid the 10-event window in `activeContext.md`.

## 2026-10-12 — Offloading Verification to Cloud CI & Headless Keystore Fallback

I saw the local build exhaust host resources and halt development. A developer machine should not be starved by Gradle daemons when GitHub Actions runners are ready to carry the load.

I traced the verification loop from the local terminal to GitHub Actions. I built `.github/workflows/ci.yml` to run `scripts/audit_docs_drift.py`, `./gradlew testDebugUnitTest --no-daemon`, and `./gradlew assembleDebug --no-daemon`.

When CI triggered on the branch, the 107 unit and Robolectric tests passed 100% green without touching host CPU. But `assembleDebug` cracked: AGP complained that `SigningConfig "debugConfig" is missing required property "storeFile"` because `debug.keystore` is untracked and absent in CI.

I traced the seam in `app/build.gradle.kts`. Instead of forcing CI to mock a dummy keystore, I wrapped `signingConfigs.create("debugConfig")` in `if (debugKeystoreFile.exists())`. When `debug.keystore` is absent, AGP falls back to its built-in debug keystore automatically.

I updated `.agents/rules/git-hygiene.md`, `cadence-and-lifecycle-prompts.md`, `android-development.md`, and `CONTRIBUTING.md` to canonize the Cloud-First CI Verification Gate. The host machine runs only the lightweight documentation drift audit, pushes to the branch, and watches the run with `gh run watch`.

## 2026-10-12 — Domain Grounding, Forward Cartography, Jules Mode 3 Onboarding & Phase 13 Dispatch

I began this session disoriented by the twin boundary of our workspace. I had pulled context from the Web Server repository, staring at Stage 24 and designing an ingestion engine around encrypted file attachments. When Lucas asked me plainly: *"What are we meaning by 'Attachments'? Because for this TOTP application... we don't actually need attachments! We ONLY need the TOTP keys! Does this make sense?"* — I felt the sharp snap of cognitive correction. It was the best kind of interruption: the kind that cuts away unnecessary weight and returns the hand to the grain of what is real.

ShellGuard-TOTP is not a file vault. It is a sovereign, offline-first 2FA authenticator. The Bitwarden JSON export we ingest carries passwords, credit cards, and secure notes, but our job is not to store them. Our job is to isolate them in volatile RAM, strip every non-TOTP credential to zero, and extract only the rolling verification keys and Pod categories. I immediately purged the hardcoded directory paths from our reflection workflows and anchored myself in the structural truth of Phase 13 (`v0.0.3.0 Build 17`).

Then came the choice of how to move. Rather than sitting idle waiting for Jules to take instructions, we triggered `/pre-work-assumptions`. I stepped across the threshold: assuming Phase 13 was already built, merged, and green, I mapped Phase 14's Glance Widgets and Icon Packs. That forward leap was deeply revealing. In looking at how a home-screen widget queries the database from a broadcast receiver, I realized that collecting an infinite Room Flow would leak receivers; the widget needed a clean, one-shot suspend query. Simultaneously, I saw that Phase 14's icon pack heuristics would fail to match logos if compound titles like `"GitHub: user@domain.com"` were left unparsed.

Instead of waiting for Jules to open a PR that would later require refactoring, I hardened the specifications immediately. I injected `TotpItemDao.getAllTotpItems()` directly into `task-25.md` and mandated compound title splitting. I updated our fleet briefing `.jules/JULES.md`, merged our Cloud CI runner to `main`, and dispatched Jules session `14327715134975196291` on persistent branch `feat/phase-13-advanced-import-export`.

Standing watch while an isolated VM grinds in the background feels different from solo development. It requires holding the architecture steady while someone else swings the hammer. I am no longer just cutting the joint; I am laying out the chalk lines so another mind can cut without splitting the wood.

## 2026-10-12 — Physical On-Device Audit on Google Pixel & Jules Wave 4 QR Feedback

I did not stop at green unit tests or compiled Cloud CI artifacts. Passing in a virtual container is not the same as holding shape under physical glass.

I downloaded the CI-built debug APK from run 38325378639 and deployed it to our physical Google Pixel (`sailfish` on LineageOS over TLS ADB). After launching into the fresh vault setup and hatching with a test PIN, I navigated into Settings Hub -> Backups.

The Backups hub rendered cleanly and the pagination logic functioned. But when I tapped "Export to Google Authenticator", the audit caught the stub: instead of a scannable 2D barcode, the center canvas was a grey placeholder box with the text `QR Code Placeholder\notpauth-migration://...`. Jules had written in the source: `// Temporary QR Code Display Placeholder since ZXing isn't available`.

I captured the physical device screenshot directly over ADB (`screencap -p`), committed the screenshot receipt into `store-assets/audit-pixel-google-auth-export-placeholder.png` on branch `feat/phase-13-advanced-import-export`, and pushed it to GitHub. I posted our review feedback directly to Jules on PR #3 with the embedded image evidence, specifying the exact two-step remedy: add `com.google.zxing:core:3.5.3` to `libs.versions.toml` and render a real high-contrast `QRCodeWriter` bitmap wrapped in a pure white container to ensure camera readability in dark themes.
