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

## 2026-10-12 — Pre-Work Assumptions, Jules Fleet Onboarding & Phase 13 Dispatch

I reconciled the domain truth of ShellGuard-TOTP with Lucas: zero attachments, strictly RFC 6238 2FA seeds. The external Bitwarden JSON export carries passwords and notes, but our `BitwardenSanitizer` purges them in volatile RAM so zero non-TOTP credentials ever reach disk.

I executed `/pre-work-assumptions`, projecting forward to Phase 14 Glance Widgets assuming Phase 13 lands 100% green. That forward leap caught three critical contract gaps:
1. Glance Widgets in broadcast receivers need a one-shot `TotpItemDao.getAllTotpItems()` suspend query, not an infinite Flow. I added it directly to Jules's Task 25 specification so `MigrationPersister` implements it immediately.
2. Icon Packs in Phase 14 need clean issuer keys. I added compound title splitting (`Service: User`) to `BitwardenSanitizer` so brand glyphs match with zero downstream refactoring.
3. Settings Hub navigation continuity: `SettingsMetaScreen` category 6 (`☁️ Backups`) needs a route to `SettingsBackupsScreen`.

I built the fleet briefing `.jules/JULES.md`, `task-25.md` (Wave 1), `task-26.md` (Waves 2-4), and authored the master execution dossier `meta-plan-phase-13-jules-orchestration-and-release.md`. I merged `main` with the Cloud CI runner, created `feat/phase-13-advanced-import-export`, and launched Jules session `14327715134975196291` via `jules new`. The stage branch is active. Now I scout and stand watch for the pull request.
