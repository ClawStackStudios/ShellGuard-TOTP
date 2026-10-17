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
Date: 2026-09-22
TaskRef: "Phase 12: Security Suite, Panic Purge, Audit Logging & Web Server v0.0.2.3 Sync Parity [v0.0.2.3 (Build 16)]"

Learnings:
- In multi-factor authentication applications mirroring a dynamic backend (such as ShellGuard Web Server v0.0.2.3), TOTP secrets may arrive in RFC 6238 Key URI formats (`otpauth://totp/...`) or raw Base32 strings. Piping decrypted secrets through `TotpUriParser.parse()` normalizes both variants into clean Base32 while extracting custom algorithm (SHA256/512), digits (8), and period parameters, preserving interoperability across services.
- When remote servers omit `updated_at` or emit null timestamps on remote sync responses, comparing candidate entity fields (`isContentIdentical`) before executing Room transactions prevents unnecessary database write cycles and UI recomposition churn.
- Room database version bumps (from v1 to v2 with `AuditLogEntity`) require careful instantiation across test harnesses (`RoomDatabaseTest`, `MainActivityLaunchTest`) and application singletons (`ShellGuardTotpApp`). Providing fallback destruction/migration and explicit DAO test fixtures ensures 100% test suite resilience.
- When testing on physical devices with differing screen sizes (Google Pixel 1080x1920 vs Nexus 7 tablet 1200x1920), soft keyboard appearances alter touch targets and scroll offsets; dumping the live hierarchy via `uiautomator dump` yields exact `[x1, y1][x2, y2]` bounds for deterministic interaction.

Difficulties:
- During headless testing and local builds, Gradle configuration cache cached stale paths pointing to `/config/Documents/...`. Running with `--no-configuration-cache` resolved directory resolution discrepancies.
- A missing `</intent-filter>` closing tag in `AndroidManifest.xml` broke manifest merger; resolved immediately with balanced tags.

Successes:
- Successfully implemented Tasks 23, 24, and 24b: `AuditLogDao`, `AuditLogEntity`, `SecurityPreferenceController`, `PanicTriggerReceiver`, `SettingsSecurityScreen`, `SettingsAuditLogScreen`, and `TotpRepository` dynamic URI sync.
- 107/107 unit and Robolectric tests passing 100% green.
- Live verified on physical Google Pixel and Nexus 7 tablet.
---
Date: 2026-10-02
TaskRef: "Changelog Automation & Conventional Commits Skill Knowledge Ingestion (/learn)"

Learnings:
- Keep a Changelog 1.1.0 standardizes 6 exact functional sections: `Added`, `Changed`, `Deprecated`, `Removed`, `Fixed`, and `Security`. Adhering to these strict names guarantees interoperability with automated changelog parsers and release note generators.
- Conventional Commits (`<type>[optional scope][!]: <description>`) provides a deterministic, mathematical mapping to changelog categories: `feat` ➔ `Added`, `refactor`/`perf` ➔ `Changed`, `fix` ➔ `Fixed`, `revert` ➔ `Removed`, security fixes ➔ `Security`.
- In pair-programming agent workflows requiring two-layer attribution (`User:` / `AI:`), placing the Conventional Commit header on line 1 followed by blank lines and the attribution block satisfies both Git commitlint/git-cliff parsers and human/AI accountability standards.
- In multi-digit version schemes (e.g. 4-digit `MAJOR.MINOR.PATCH.REVISION`), Conventional Commit types map cleanly: breaking changes (`!`) trigger `MAJOR`, subsystem features trigger `MINOR`, phase deliverables trigger `PATCH`, and hotfixes/refactors trigger `REVISION`, while monotonically incrementing Android `versionCode = N + 1`.
- Comparison links at the footer of `CHANGELOG.md` (`[X.Y.Z.N]: https://.../compare/...`) provide instantaneous 1-click Git diff inspection for auditors and developers.
- `git-cliff` (`cliff.toml`) provides an ultra-fast, zero-dependency, standalone binary solution for generating changelogs directly from git history in containerized/headless environments.

Difficulties:
- Bridging standard 3-digit SemVer tools (Node/Rust) with Android's 4-digit SemVer and monotonic integer `versionCode` requirement. Resolved by codifying an explicit 4-tier mapping matrix in `semantic-versioning.md` and adding `cliff.toml` with regex group parsers.

Successes:
- Bolstered `.agents/rules/git-hygiene.md` with Conventional Commits specifications, scope taxonomy, breaking change syntax, and two-layer attribution alignment.
- Bolstered `.agents/rules/semantic-versioning.md` with Keep a Changelog 1.1.0 standards, comparison links, release note templates, and type-to-SemVer mapping matrix.
- Created dedicated `.agents/skills/changelog-automation/SKILL.md` and project root `cliff.toml`.
- Updated root `CHANGELOG.md` with full Keep a Changelog compare links.
---
Date: 2026-10-03
TaskRef: "Deterministic Documentation Automation & Root Docs SSoT Synthesis (doc-automation skill)"

Learnings:
- The fundamental axiom of Docs-as-Code is that the codebase is the physical, ground-truth reality, and documentation is a compiled projection. If code and documentation disagree, the documentation is defective by definition.
- Documentation drift typically falls into four concrete failure topologies: Metric Drift (test counts/badges), Schema/Contract Drift (Room tables/DAOs), Strategic Blast-Radius Drift (roadmap phase build re-indexing), and Store Constraint Leakage (Play Store markdown or character overflows).
- The 8-Point Root Documentation Mesh connects source code changes across `app/build.gradle.kts`, `ROADMAP.md`, `CHANGELOG.md`, `RELEASE-vX.Y.Z.N.md`, `RELEASE-PLAY.md`, `README.md`, domain specs (`project/*.md`), and the agent memory bank.
- Automating drift detection via a standalone Python auditor script (`scripts/audit_docs_drift.py`) transforms documentation verification into a deterministic, non-zero-exit-code CI check.

Difficulties:
- Generic documentation templates often carry legacy assumptions (e.g. web frameworks like React/Zustand or JSDoc). Purging these in favor of Android Kotlin MVI primitives (`StateFlow`, `Room + SQLCipher`, `KDoc`) ensures total contextual fidelity.

Successes:
- Created sibling skill `.agents/skills/doc-automation/SKILL.md` matching the epistemic shape of `changelog-automation`.
- Created standalone CLI tool `scripts/audit_docs_drift.py` for automated pre-commit and CI verification.
- Fortified `.agents/rules/docs-hygiene.md` to reference the new skill and enforce zero-deferred documentation under the "Docs Bow to Code" axiom.
---
Date: 2026-10-03
TaskRef: "SECURITY.md Root Policy Creation & Skill/Rule Integration"

Learnings:
- High-assurance cryptographic applications require a public root-level `SECURITY.md` establishing supported versions, cryptographic threat model boundaries, and coordinated vulnerability disclosure channels.
- Integrating `SECURITY.md` into the Root Documentation Mesh elevates the protocol to a 9-Point/10-Point Mesh, requiring verification that the Supported Versions table matches `app/build.gradle.kts` and that cryptographic defense claims (SQLCipher, KeyStore StrongBox, `FLAG_SECURE`, IME dictionary protection, panic purge) reflect real code implementations.
- Automating `SECURITY.md` checks in `scripts/audit_docs_drift.py` ensures that release version bumps immediately flag if `SECURITY.md` supported version tables are not updated.

Difficulties:
- Ensuring the threat model clearly scopes out physical decapping or rooted device compromise to prevent ambiguous or frivolous vulnerability submissions while maintaining a strict 48h response SLA for legitimate issues.

Successes:
- Created root `SECURITY.md` with complete cryptographic invariants, supported versions table, and disclosure timeline.
- Integrated `SECURITY.md` into `.agents/skills/doc-automation/SKILL.md` (9-Point Root Mesh).
- Fortified `.agents/rules/docs-hygiene.md` and `.agents/workflows/walk-the-docs.md`.
- Added `audit_security()` check to `scripts/audit_docs_drift.py`.
- Linked `SECURITY.md` in root `README.md` Documentation Index.
---
Date: 2026-10-03
TaskRef: "Git Advanced Workflows & History Engineering Skill Knowledge Ingestion (/learn)"

Learnings:
- Interactive rebase (`git rebase -i`) is history engineering: verbs (`pick`, `reword`, `edit`, `squash`, `fixup`, `drop`) allow pre-merge transformation into clean, atomic Conventional Commits.
- The autosquash pattern (`git commit --fixup <hash>` combined with `git rebase -i --autosquash`) completely removes manual reordering overhead, pairing defect fixes with their parent feature commits deterministically.
- `git bisect run` combined with a non-daemon Gradle test runner (`./gradlew testDebugUnitTest --no-daemon`) automates regression discovery in O(log N) commit steps using standard process exit codes (0 = good, 1-127 = bad).
- Git worktrees (`git worktree add`) provide physical workspace isolation for hotfixes and code reviews without dirtying the working directory, stashing, or risking in-flight work.
- `git reflog` preserves orphaned and reset commits for 90 days, providing deterministic recovery paths from botched rebases or accidental `reset --hard` commands.
- Pre-rebase safety branches (`git branch backup/<name>`) and strictly enforced `--force-with-lease` ensure zero-loss history rewrites on local feature branches while prohibiting any rebase on public branches (`main`, `master`, `release/*`).

Difficulties:
- In headless containerized environments, interactive git rebases can hang indefinitely waiting for an interactive editor (vim/nano). Mitigated by mandating `GIT_EDITOR=:` and `GIT_SEQUENCE_EDITOR=:` (or `--no-edit`).
- Reconciling autosquashed commits with two-layer attribution: fixups must preserve or update the `AI:` implementation trace while retaining the original `User:` architectural intent.

Successes:
- Synthesized and created dedicated skill `.agents/skills/git-advanced-workflows/SKILL.md` with complete command tables, autosquash flows, Mermaid bisect diagram, worktree playbooks, and reflog recovery blueprints.
- Bolstered `.agents/rules/git-hygiene.md` with Section "Rebase & Advanced Workflow Hygiene" codifying editor bypass, autosquash, public branch rebase prohibition, `--force-with-lease` mandate, automated bisect runner, worktree isolation, and reflog recovery.
---
Date: 2026-10-12
TaskRef: "Cloud-First CI Verification Gate & Headless Debug Keystore Fallback"

Learnings:
- Heavy JVM Gradle build commands (`./gradlew testDebugUnitTest`, `./gradlew assembleDebug`) consume substantial CPU and RAM, starving local developer environments. Offloading these runs to a GitHub Actions CI runner preserves developer focus and hardware stability while still enforcing a strict verification gate.
- When running headlessly in CI without a physical local `debug.keystore` file, configuring `signingConfigs.create("debugConfig")` in `app/build.gradle.kts` without checking `debugKeystoreFile.exists()` causes Android Gradle Plugin to fail with missing required storeFile property errors. Wrapping the custom signing config block in `if (debugKeystoreFile.exists())` lets AGP cleanly fall back to its internal default debug keystore.
- Fast local pre-flight checks (such as `python3 scripts/audit_docs_drift.py`) provide sub-second verification of documentation Single Source of Truth without spinning up Gradle or JVM daemons.
- Terminal-based CI observability via `gh run list --branch <branch> -L 1` and `gh run watch <run-id>` provides real-time progress feedback without leaving the command line.

Difficulties:
- Initial CI run failed on `assembleDebug` because `debug.keystore` was gitignored and absent in the runner. Traced AGP signing config resolution and added the conditional existence guard.

Successes:
- Built `.github/workflows/ci.yml` running doc drift audit, 107 unit and Robolectric tests, and debug APK assembly with Gradle caching.
- Achieved 100% green verification in GitHub Actions CI run `38278380245` with zero local host machine load.
- Codified Cloud-First CI Verification Gate across all agent governance rules and `CONTRIBUTING.md`.
---
Date: 2026-10-17
TaskRef: "Phase 13 Milestone 3 Delivery, Jules Orchestration & Socratic Knowledge Integration (/learn)"

Learnings:
- External Knowledge Persistence (.jules/knowledge/KNOWLEDGE.md): Jules's native cloud memory system (purple cards 🐙) is currently opaque and unreliable. Transitioning persistent agent knowledge to an auditable, version-controlled markdown file (.jules/knowledge/KNOWLEDGE.md) alongside root AGENTS.md (auto-read by Jules) provides full git history auditability, allows lead architects to seed and review invariants in PR diffs, and establishes a clear 3-Tier Fleet Model: Tier 1 (Root AGENTS.md), Tier 2 (.jules/knowledge/KNOWLEDGE.md), and Tier 3 (.jules/tasks/task-N.md).
- Socratic Recursive Inward Reflection Protocol: Concluding deep pair-programming or co-pilot milestones with 5 recursive inward reflection questions (Physical Reality vs Virtual Mock, Offline-First vs Premature Network, Unwired Seam vs Illusion of Execution, MASVS-AUTH-1 Security Fallback Void, and Docs Bow to Code SSoT) causes the agent to develop an internal mental model of the codebase's grain rather than mechanically applying patches.
- Progressive Wave Multi-PR Supersession: When an autonomous agent creates sequential branches/PRs across development waves (e.g. PR #2 followed by PR #3), forensic diffing (git diff --stat) verifies whether the successor is a strict superset. Intermediate obsolete PRs must never be merged into main; closing them with explicit attribution comments keeps open PR debt at zero.
- OWASP MASVS-AUTH-1 Complete Step-Up Fallback Invariant: When biometrics are absent, un-enrolled, or disabled in app settings, sensitive export surfaces must NEVER default to unauthenticated access; a Master PIN/Password fallback challenge dialog must be strictly triggered before releasing plaintext cryptographic secrets.
- Headless vs Physical Glass Gap: Headless unit and Robolectric test runners only assert that composables exist in the virtual semantic tree; rendering text stubs (like placeholder QR text) passes headless tests but completely breaks real-world physical usage. Real 2D barcodes wrapped in high-contrast containers are mandatory.

Difficulties:
- Jules initially left an unwired dialog confirmation seam in SettingsBackupsScreen.kt (comment only) and an insecure biometric fallback bypass in SettingsExportScreen.kt. Detected during forensic PR review, rectified through proactive review guidance, and codified into android-development.md and .jules/knowledge/KNOWLEDGE.md.

Successes:
- Landed Milestone 3 (v0.0.3.0 Build 17) via PR #3 with 100% green Cloud CI runs, zero doc drift, and staged Gold Master debug APK.
- Deprecated JULES.md across jules-cli skill in favor of root AGENTS.md and .jules/knowledge/KNOWLEDGE.md.
- Bolstered jules-cli references with Progressive Wave Multi-PR Supersession and Socratic Recursive Inward Reflection protocols.
- Updated android-development.md with Section 11 OWASP MASVS-AUTH-1 Complete Step-Up Invariant.
- Initialized .jules/knowledge/KNOWLEDGE.md with verified Phase 13 atomic invariants.
---





