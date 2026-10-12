---
trigger: model_decision
description: When Updating the project/application versioning
---

# Workflow: Intelligent Project Versioning & Documentation

This rule defines an automated, multi-step process for releasing a new version of the project. I MUST follow this logic precisely to ensure a correct and consistent release version incrementation methodology.

### Step 1: Determine New Version (Work-Driven 4-Digit Logic)

My primary task is to calculate the new version number following the `MAJOR.MINOR.PATCH.REVISION` (`X.Y.Z.N`) convention paired with Conventional Commits semantics. **We do not force artificial target versions**; the nature and magnitude of the actual completed work dictates which tier increments:

1.  **Check for Explicit User Input:**
    - First, check if the user has provided an explicit version type or number (e.g., `major`, `minor`, `patch`, `revision`, `v0.0.0.4`, `v0.0.1.0`).
    - If yes, this is the highest priority. Proceed to calculate the new version based on this input.

2.  **Infer from Conventional Commits & `CHANGELOG.md`:**
    - If no explicit type is given, infer by analyzing the scope of work and conventional commit types under `[Unreleased]`:
        - **MAJOR (`X+1.0.0.0`)**: Triggered by breaking changes (`feat!`, `fix!`, or `BREAKING CHANGE:` footer), fundamental structural overhauls, database breaking migrations, or full public production milestones.
        - **MINOR (`X.Y+1.0.0`)**: Triggered by significant new subsystems or large cohesive feature groups (`feat(subsystem)` - e.g., entire Glance widget framework, multi-tier modular settings hub).
        - **PATCH (`X.Y.Z+1.0`)**: Triggered by discrete feature additions (`feat`), completed multi-task phase deliverables, or substantial bug fixes (`fix`).
        - **REVISION (`X.Y.Z.N+1`)**: Triggered by targeted bug fixes (`fix`), hotfixes, performance tweaks (`perf`), CI pipeline adjustments (`ci`), or minor UI polish (`style`).
        - **Android Play Console Invariant**: Every release upload MUST increment `versionCode` (`N + 1` strictly monotonic integer) in `app/build.gradle.kts`, even when maintaining `versionName` display parity.

3.  **Conventional Commit Type to SemVer & Changelog Mapping Matrix**:
    | Conventional Type | Changelog Section | 4-Digit SemVer Impact | Play Store Notes |
    | :--- | :--- | :--- | :--- |
    | `feat!` / `BREAKING CHANGE` | `Added` / `Changed` | **MAJOR** (`X+1.0.0.0`) | Include breaking notice |
    | `feat(scope)` (Major Subsystem) | `Added` | **MINOR** (`X.Y+1.0.0`) | Highlight feature |
    | `feat` (Phase Deliverable) | `Added` | **PATCH** (`X.Y.Z+1.0`) | Highlight feature |
    | `fix` (Hotfix / Bug Fix) | `Fixed` | **REVISION** (`X.Y.Z.N+1`) | Include bug fix summary |
    | `perf` / `refactor` | `Changed` | **REVISION** (`X.Y.Z.N+1`) | Include optimization note |
    | `fix(security)` / CWE fix | `Security` | **PATCH** / **REVISION** | Include security notice |
    | `revert` | `Removed` | **REVISION** (`X.Y.Z.N+1`) | Detail removed feature |
    | `chore(release)` | *(Excluded)* | Sets release tag | N/A |

4.  **Ask User on Ambiguity (Fallback):**
    - ⚠️ **If my inference is ambiguous**, I **MUST NOT GUESS**.
    - Instead, I MUST ask the user for clarification. Present my analysis and provide clear choices:
      > "I have analyzed the changes in `[Unreleased]` and the version increment tier is ambiguous. Based on the work completed:
      > - **PATCH (`vX.Y.Z+1.0`):** For completed feature phases or substantial bug fixes.
      > - **REVISION (`vX.Y.Z.N+1`):** For small adjustments, iterative tweaks, or hotfixes.
      > - **RE-RELEASE (`vX.Y.Z.N (Build N+1)`):** Keep display version, increment `versionCode` only.
      >
      > Which version increment should we apply?"

### Step 2: Pre-flight Check & Confirmation

Before modifying any files, perform an internal verification and present a plan to the user.

1.  **Internal Verification:** Use a thinking block to confirm I have all necessary information dynamically read from the project:
    ```xml
    <thinking>
    1.  Current version source file read (`app/build.gradle.kts`).
    2.  Current version read dynamically: [e.g., versionName = "X.Y.Z.N", versionCode = N]
    3.  Increment type determined: [e.g., minor (inferred)]
    4.  Calculated new version: [e.g., versionName = "X.Y+1.0.0", versionCode = N + 1]
    5.  Required files are present and accessible.
    Plan is ready for execution.
    </thinking>
    ```
2.  **State Your Plan:** Present the confirmed new version number to the user and ask for final approval before writing any changes. For example: "I will increment the version from `vX.Y.Z.N (Build N)` to `vA.B.C.D (Build N+1)`. Is this correct?"

### Step 3: Execute Core File Modifications

Once the user confirms, I will proceed with the following precise file modifications.

1.  **Update Central Version Source:**
    - Locate and update the version number in the central source file (`app/build.gradle.kts` for Android, setting `versionCode = N + 1` and `versionName = "X.Y.Z.N"`).

2.  **Update `CHANGELOG.md` (Keep a Changelog 1.1.0 Strict Standards):**
    - Create a new version heading below `[Unreleased]` using the format `## [X.Y.Z.N] - YYYY-MM-DD (Build N) — Phase Title`.
    - Group changes strictly into the standard Keep a Changelog categories:
      - `### Added`: New user features, capabilities, or screens.
      - `### Changed`: Modifications to existing functionality, architectural refactors, performance upgrades.
      - `### Deprecated`: Features planned for removal in upcoming versions.
      - `### Removed`: Features, screens, or legacy classes removed.
      - `### Fixed`: Bug fixes and error resolutions.
      - `### Security`: Cryptographic hardening, memory zeroization, or CVE/CWE remediations.
    - Update comparison links at the footer of `CHANGELOG.md`:
      ```markdown
      [Unreleased]: https://github.com/ClawStackStudios/ShellGuard-TOTP/compare/vX.Y.Z.N...HEAD
      [X.Y.Z.N]: https://github.com/ClawStackStudios/ShellGuard-TOTP/compare/vPREVIOUS...vX.Y.Z.N
      ```

3.  **Update `README.md` Version Badge & Metrics:**
    - Find the version badge in `README.md` and update the version number.
    - **Template:** `[![Version](https://img.shields.io/badge/version-X.Y.Z.N-blue.svg)](CHANGELOG.md)`
    - Update test count badge and verified documentation table.

### Step 4: 🧠 Synchronize Release Notes & Project Documentation

My goal is to ensure all technical and maintenance documentation reflects the new changes, not just list them.

1.  **Draft Milestone Release Notes (`RELEASE-vX.Y.Z.N.md`):**
    - Create or update the root release artifact following the Standard Milestone Template:
      ```markdown
      # Release vX.Y.Z.N - YYYY-MM-DD (Build N)

      ## Summary
      [High-level overview of the milestone and user value]

      ## Highlights
      ### 🌟 [Key Feature Name]
      [Architecture, UI/UX, and security details]

      ## Breaking Changes
      [None or migration steps]

      ## Pre-Flight Verification Gate
      - Unit & Robolectric Tests: 100% green ([Count]/[Count] passing).
      - Build Verification: Clean assembleDebug and bundleRelease.
      - Device Verification: Tested on target hardware.

      ## Dependencies & Schemas
      | Component | From | To | Rationale |
      ```

2.  **Draft Google Play Store Notes (`RELEASE-PLAY.md`):**
    - Formatted strictly under 500 plain text characters inside `<en-US>` tags.
    - Zero markdown syntax (no `**`, `*`, `#`, or backticks).

3.  **Analyze Affected Docs & Propose Updates:**
    - **`Added`**: If a new feature was added (e.g., caching), update `architecture.md`, `ROADMAP.md`, or setup guides.
    - **`Changed`**: Propose updates reflecting modified behavior or endpoints.
    - **`Removed`**: Mark removed features or add migration steps.
    - **`Fixed`**: Clarify previously misunderstood behaviors if applicable.
4.  **AWAIT USER APPROVAL:** 🚨 **CRITICAL:** I MUST NOT apply documentation changes directly without presenting a clear summary of proposed updates for user review and approval.

### Step 5: Prepare Git Commit Message

Once all file changes are approved and applied, I will prepare and output the commit message following the Conventional Commit + two-layer attribution format:

```
chore(release): bump version to vX.Y.Z.N (Build N+1) --release vX.Y.Z.N

User: <intent/direction provided by human>
AI: <files updated, tests verified, and documentation synchronized>
```

---

## 🚫 Versioning Anti-Patterns to Avoid

1. **Never Hardcode Static Version Assumptions**: Always read the live version dynamically from `app/build.gradle.kts` (`versionCode` and `versionName`). Never assume a hardcoded starting number.
2. **Never Duplicate `versionCode`**: Google Play Console will immediately reject any upload with an existing or decremented `versionCode`. It must strictly increment (`N + 1`).
3. **Never Desynchronize Version Anchors**: Updating `app/build.gradle.kts` without updating `CHANGELOG.md`, `README.md`, and drafting `RELEASE-vX.Y.Z.N.md` creates documentation drift. All anchors evolve in the same release commit.
4. **Never Guess Ambiguous Increments**: If changes in `[Unreleased]` do not cleanly map to a single SemVer increment tier, always ask the user with structured choices rather than guessing.
5. **Never Ignore the Downstream Blast Radius**: If you bump a version incorrectly in `ROADMAP.md` or `changelog.md`, every future phase and milestone mapped in those documents becomes mathematically corrupted. You must strictly adhere to the tier definitions (e.g., a phase deliverable is a PATCH, not a MINOR) to protect the integrity of the entire roadmap.

---

## 🛡️ Version Conflict Resolution Matrix

| Conflict / Edge Case Scenario | Resolution Strategy |
| :--- | :--- |
| **Play Console Bundle Rejection / Re-release** | Keep `versionName = "X.Y.Z.N"` identical for user visibility, increment `versionCode = N + 1`. |
| **Hotfix on Live Release** | Increment the 4th digit: `vX.Y.Z.N` ➔ `vX.Y.Z.N+1` and increment `versionCode = N + 1`. |
| **Simultaneous Breaking + Non-Breaking Changes** | The higher-precedence tier wins: `MAJOR` (`X+1.0.0.0`) supersedes `MINOR` and `PATCH`. |
| **Release Note Corrections Post-Release** | Edit `RELEASE-vX.Y.Z.N.md` on `main`; the CI `mirror` job syncs the description without rebuilding binaries. |