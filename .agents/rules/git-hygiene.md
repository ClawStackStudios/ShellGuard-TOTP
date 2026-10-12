---
trigger: always_on
---

# Git & Android Workspace Hygiene

## Isolation & Branching
- Never work directly on the default branch (`main`/`master`). Start every task on a fresh branch or worktree: `git checkout -b <type>/<short-desc>`.
- One task = one branch. Don't mix unrelated architectural changes into the same branch or working tree.
- Before starting, snapshot state: `git status` and `git diff --stat`. If the tree is dirty with work you didn't author, stop and ask.
- **Automated Bot & Security PR Forensic Verification**: Never trust commit messages, PR descriptions, or bot summaries claiming vulnerabilities are fixed. Always inspect the physical tree diff (`git show --stat <commit>` or `git diff <base>...<head>`) to confirm actual code modifications before accepting or branching from automated security fixes.


## Commits & Conventions
- Keep changes small and self-contained; one logical change per commit. No mega-commits, no unrelated refactors bundled in.
- **Strict Conventional Commits**: All commit messages must follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:
  `<type>[optional scope][!]: <short summary>`
- **Standard Types**:
  - `feat`: A new feature or user-facing capability (maps to `Added` in changelog).
  - `fix`: A bug fix or defect resolution (maps to `Fixed` in changelog).
  - `refactor`: Code restructure without changing behavior (maps to `Changed` in changelog).
  - `perf`: Performance improvement (maps to `Changed` in changelog).
  - `revert`: Reverts a previous commit (maps to `Removed` in changelog).
  - `docs`: Documentation only changes.
  - `style`: Whitespace, formatting, missing semi-colons (no code change).
  - `test`: Adding or correcting unit/UI tests.
  - `chore`: Build tasks, auxiliary tools, dependency updates, release version bumps (`chore(release)`).
  - `ci`: CI configuration files and scripts.
  - `build`: Changes affecting build system or external dependencies.
- **Canonical Scopes**: Use consistent domain scopes:
  - `(auth)`: Authentication, sessions, biometrics, PIN/Password handling.
  - `(sync)`: Remote vault synchronization, Web Server parity, delta classification.
  - `(crypto)`: KeyStore, SQLCipher, AES-GCM-256, HKDF key derivation.
  - `(ui)`: Jetpack Compose screens, design system components, themes.
  - `(settings)`: Preferences, controllers, backup/restore, audit logging.
  - `(backup)`: `.sgtotp.bak`, SAF import/export engines.
  - `(release)`: Version bumps, release notes, Play Store bundles.
  - `(ci)`: GitHub Actions workflows, headless signing pipelines.
- **Breaking Changes**:
  - Append `!` after type/scope (e.g. `feat(api)!: migrate to Room schema v2`).
  - Or include `BREAKING CHANGE: <explanation>` in the commit body/footer.
  - Signals a `MAJOR` version bump.
- **Issue References**: Reference issues/PRs in the footer where applicable (`Closes #123`, `Fixes #456`).
- Prefer new commits over amending. Never amend or rebase a commit without explicit written approval in the task.
- Never skip hooks (`--no-verify`) or bypass commit signing unless explicitly asked.

## Android Secrets & Keystore Safety (CRITICAL)
- **NEVER Commit Keystores or Private Keys**:
  - `*.jks`, `*.keystore`, `*.p12`, `*.pem`, `my-upload-key.jks`, `debug.keystore`.
- **NEVER Commit Local Machine Configs & API Credentials**:
  - `local.properties` (contains local machine Android SDK paths).
  - `.env`, `.env.*`, `google-services.json`, `secrets/**`.
- **NEVER Commit Build Outputs & Binaries**:
  - `build/`, `**/build/`, `*.apk`, `*.aab`, `*.apks`, `*.obb`, `*.dex`, `*.class`.
- **NEVER Commit IDE Caches**:
  - `.idea/`, `.gradle/`, `.kotlin/`, `captures/`, `.externalNativeBuild/`, `.cxx/`.
- **NEVER Commit Internal Agent Scratchpads & Visual Design Assets**:
  - `.agents/internal/`, `.agents/scratch/`, `**/scratch/**`. Always ensure these directories are declared in `.gitignore` and excluded from git staging.

## Destructive Operations — NEVER Without Explicit Confirmation
- `git push --force` / `--force-with-lease`
- `git reset --hard`, `git checkout/restore` to an older commit
- Deleting branches, tags, or stashes
- `rm -rf` or any bulk file deletion
- If unsure whether a file belongs to another agent's in-flight work, stop and coordinate — don't delete to silence an error.

## Android Pre-Commit Verification Gate
- Run unit and UI tests before committing: `./gradlew testDebugUnitTest`.
- Ensure clean build verification: `./gradlew assembleDebug`.
- Never commit broken tests or unverified compilation states.

## Attribution & Commit Message Format
- Commit under the human's configured identity (`git config user.name` / `user.email`). No separate agent identity, no AI co-author line.
- Every commit message uses the Conventional Commit header paired with this two-layer attribution format:

  ```
  <type>(<scope>): <short summary>

  User: <the intention, system design, architecture decision, or glue that was provided>
  AI: <the concrete implementation, functions, refactors, or tests that were generated>

  [Optional Body / Migration Notes / Issue References (e.g. Closes #123)]
  ```

- The `User:` line is always the *why/what* — the intent, spec, or structural decision.
- The `AI:` line is always the *how* — the code, logic, or test coverage that fulfilled it.
- If the human did the implementation directly (rare), put it under `User:` and write `AI: (none)`.
- If the agent did purely exploratory work with no human direction in that commit, write `User: (autonomous)` — but this should be the exception, not the norm.
- No trailers, no co-author lines, no `AI-Model:` metadata. The two-layer message *is* the attribution.

## Rebase & Advanced Workflow Hygiene
- **Editor Bypass in Headless Environments**: When rebasing in headless or containerized environments, avoid opening terminal interactive editors: set `GIT_EDITOR=:` and `GIT_SEQUENCE_EDITOR=:` (or pass `--no-edit`).
- **Autosquash & Pre-PR History Cleanup**: On local feature branches, use `git commit --fixup <commit>` and `git rebase -i --autosquash $(git merge-base HEAD main)` to squash iterative WIP fixes into clean, atomic Conventional Commits before requesting review or merging.
- **Strict Prohibition on Public Branch Rebasing**: NEVER rebase or rewrite commits on shared branches (`main`, `master`, `release/*`). History rewriting is strictly reserved for private local feature branches before merge.
- **Safe Force Push Mandate (`--force-with-lease`)**: If local history was rewritten after clean interactive rebasing on a private feature branch, ALWAYS use `git push --force-with-lease`. NEVER use raw `git push --force`.
- **Pre-Rebase Safety Anchoring**: Before performing non-trivial interactive rebases, create a local safety backup branch: `git branch backup/<branch-name>`.
- **Automated Regression Hunting (Git Bisect)**: When tracking down regressions across commit history, automate binary search using the Gradle test oracle:
  ```bash
  git bisect start HEAD <known-good-tag>
  git bisect run ./gradlew testDebugUnitTest --no-daemon
  git bisect reset
  ```
- **Multi-Track Isolation via Worktrees**: For parallel tasks or urgent hotfixes without stashing or dirtying the working directory, use `git worktree`:
  ```bash
  git worktree add ../shellguard-hotfix -b hotfix/critical-fix main
  # Clean up upon completion:
  git worktree remove ../shellguard-hotfix && git worktree prune
  ```
- **Disaster Recovery via Reflog**: If commits are accidentally dropped or an erroneous reset occurs, use `git reflog` to locate the lost commit hash within its 90-day retention window and restore via `git reset --hard <hash>` or `git branch recovered-branch <hash>`.
- **Companion Skill**: Consult [`.agents/skills/git-advanced-workflows/SKILL.md`](../skills/git-advanced-workflows/SKILL.md) for full playbooks and advanced recipes.
