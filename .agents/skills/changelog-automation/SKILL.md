---
name: changelog-automation
description: Patterns and tools for automating changelog generation, release notes, and version management following Keep a Changelog 1.1.0, Conventional Commits, and multi-ecosystem tooling standards.
---

# 📜 Skill: Changelog Automation & Release Management

This skill provides patterns, workflows, and configuration templates for automating changelog generation, standardized release notes, and semantic version management across Git repositories.

---

## 🎯 When to Use This Skill
- Setting up automated changelog generation.
- Implementing Conventional Commits with scopes and breaking change semantics.
- Creating release note workflows (GitHub Releases, internal distribution, Google Play Console).
- Standardizing commit message formats across human and AI pair-programming flows.
- Configuring automation tooling (`git-cliff`, `standard-version`, `commitizen`, `semantic-release`).
- Managing semantic versioning (`MAJOR.MINOR.PATCH` and 4-digit Android `MAJOR.MINOR.PATCH.REVISION`).

---

## 🏛️ Core Concepts

### 1. Keep a Changelog Format (v1.1.0)
A structured, human-readable format adhering to [Keep a Changelog](https://keepachangelog.com/en/1.1.0/):

```markdown
# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- New feature X with biometric authentication.

## [1.2.0.0] - 2026-10-02
### Added
- User profile avatars.
- Dark mode support and customizable accent swatches.

### Changed
- Improved delta synchronization performance by 40%.

### Deprecated
- Legacy plaintext backup format (use encrypted `.sgtotp.bak`).

### Removed
- Deprecated v1 REST API endpoint.

### Fixed
- Fixed race condition during initial vault database unlock (#123).

### Security
- Applied `PasswordVisualTransformation` and disabled predictive IME learning for Base32 secrets (CWE-359).

[Unreleased]: https://github.com/owner/repo/compare/v1.2.0.0...HEAD
[1.2.0.0]: https://github.com/owner/repo/compare/v1.1.0.0...v1.2.0.0
```

#### Standard Changelog Categories:
| Category | Purpose | Typical Commit Types |
| :--- | :--- | :--- |
| **`Added`** | For new features, screens, or public APIs. | `feat`, `feat(...)` |
| **`Changed`** | For changes in existing functionality, performance upgrades, or refactors. | `refactor`, `perf` |
| **`Deprecated`** | For soon-to-be removed features or legacy workflows. | `feat` with deprecation notes |
| **`Removed`** | For now-removed features, dead code, or superseded screens. | `revert`, `refactor` |
| **`Fixed`** | For any bug fixes or defect resolutions. | `fix`, `fix(...)` |
| **`Security`** | In case of vulnerabilities, cryptographic hardening, or privacy patches. | `fix(security)`, `fix(crypto)` |

---

### 2. Conventional Commits Specification
Structure:
```
<type>[optional scope][!]: <description>

[optional body]

[optional footer(s)]
```

#### Type-to-Changelog Mapping:
| Type | Description | Changelog Category | Release Impact |
| :--- | :--- | :--- | :--- |
| `feat` | New feature or capability | **`Added`** | MINOR / PATCH |
| `fix` | Bug fix or defect resolution | **`Fixed`** | PATCH / REVISION |
| `perf` | Performance improvement | **`Changed`** | PATCH / REVISION |
| `refactor` | Code restructure with identical behavior | **`Changed`** | PATCH / REVISION |
| `revert` | Reverts a previous commit | **`Removed`** | PATCH / REVISION |
| `docs` | Documentation updates | *(Internal / Excluded)* | None / REVISION |
| `style` | Formatting, whitespace, linting | *(Internal / Excluded)* | None |
| `test` | Adding or updating tests | *(Internal / Excluded)* | None |
| `chore` | Maintenance, dependencies, release bumps | *(Internal / Excluded)* | None / REVISION |
| `ci` | CI/CD pipelines, workflows | *(Internal / Excluded)* | None |
| `build` | Build system, Gradle/npm scripts | *(Internal / Excluded)* | None |

#### Breaking Changes:
- Appending `!` immediately after type/scope (e.g. `feat(api)!: migrate to Room schema v2`).
- Or adding a footer starting with `BREAKING CHANGE: <explanation>`.
- Triggers a **MAJOR** version bump.

---

## 🛠️ Tooling & Implementation Blueprints

### Blueprint A: `git-cliff` (Rust-based, Ultra-Fast, Highly Configurable)
Ideal for cross-language, Android, and standalone git workflows.

```toml
# cliff.toml
[changelog]
header = """
# Changelog

All notable changes to this project will be documented in this file.
"""
body = """
{% if version %}\
    ## [{{ version | trim_start_matches(pat="v") }}] - {{ timestamp | date(format="%Y-%m-%d") }}
{% else %}\
    ## [Unreleased]
{% endif %}\
{% for group, commits in commits | group_by(attribute="group") %}
    ### {{ group | upper_first }}
    {% for commit in commits %}
        - {% if commit.scope %}**{{ commit.scope }}:** {% endif %}\
            {{ commit.message | upper_first }}\
            {% if commit.github.pr_number %} ([#{{ commit.github.pr_number }}](https://github.com/owner/repo/pull/{{ commit.github.pr_number }})){% endif %}
    {% endfor %}
{% endfor %}
"""
footer = """
{% for release in releases -%}
    {% if release.version -%}
        {% if release.previous.version -%}
            [{{ release.version | trim_start_matches(pat="v") }}]: https://github.com/owner/repo/compare/{{ release.previous.version }}...{{ release.version }}
        {% endif -%}
    {% else -%}
        [unreleased]: https://github.com/owner/repo/compare/{{ release.previous.version }}...HEAD
    {% endif -%}
{% endfor %}
"""
trim = true

[git]
conventional_commits = true
filter_unconventional = true
split_commits = false
commit_parsers = [
    { message = "^feat", group = "Added" },
    { message = "^fix.*security", group = "Security" },
    { message = "^fix", group = "Fixed" },
    { message = "^perf", group = "Changed" },
    { message = "^refactor", group = "Changed" },
    { message = "^revert", group = "Removed" },
    { message = "^doc", group = "Documentation", skip = true },
    { message = "^style", group = "Styling", skip = true },
    { message = "^test", group = "Testing", skip = true },
    { message = "^chore\\(release\\)", skip = true },
    { message = "^chore", group = "Miscellaneous", skip = true },
]
filter_commits = false
tag_pattern = "v[0-9]*"
sort_commits = "oldest"
```

CLI Usage:
```bash
# Generate complete changelog
git cliff -o CHANGELOG.md

# Preview unreleased changes
git cliff --unreleased

# Generate release notes for a specific tag range
git cliff v0.0.2.2..v0.0.2.3 -o RELEASE_NOTES.md
```

---

### Blueprint B: `standard-version` (Node.js Workspace Integration)
```javascript
// .versionrc.js
module.exports = {
  types: [
    { type: "feat", section: "Added" },
    { type: "fix", section: "Fixed" },
    { type: "perf", section: "Changed" },
    { type: "refactor", section: "Changed" },
    { type: "revert", section: "Removed" },
    { type: "docs", section: "Documentation", hidden: true },
    { type: "style", section: "Styles", hidden: true },
    { type: "chore", section: "Miscellaneous", hidden: true },
    { type: "test", section: "Tests", hidden: true },
    { type: "build", section: "Build System", hidden: true },
    { type: "ci", section: "CI/CD", hidden: true },
  ],
  commitUrlFormat: "{{host}}/{{owner}}/{{repository}}/commit/{{hash}}",
  compareUrlFormat: "{{host}}/{{owner}}/{{repository}}/compare/{{previousTag}}...{{currentTag}}",
  issueUrlFormat: "{{host}}/{{owner}}/{{repository}}/issues/{{id}}",
  releaseCommitMessageFormat: "chore(release): bump version to {{currentTag}}",
};
```

---

### Blueprint C: GitHub Actions Automated Release Notes Workflow
```yaml
# .github/workflows/release-notes.yml
name: Generate Release Notes

on:
  push:
    tags:
      - 'v*'

jobs:
  release-notes:
    runs-on: ubuntu-latest
    permissions:
      contents: write
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Generate Changelog with git-cliff
        uses: orhun/git-cliff-action@v3
        with:
          config: cliff.toml
          args: --latest --strip header
        env:
          OUTPUT: RELEASE_NOTES.md

      - name: Publish GitHub Release
        uses: softprops/action-gh-release@v2
        with:
          body_path: RELEASE_NOTES.md
          generate_release_notes: false
```

---

## 📋 Release Notes Templates

### 1. GitHub Public Release Template
```markdown
## What's Changed in v{{ .Version }}

### 🚀 Added
- **Dynamic Secret Masking**: Added password masking and IME dictionary protection for 2FA Base32 seeds by @author in #12.

### 🐛 Fixed
- **Delta Sync Skipping**: Resolved null timestamp delta classification skipping remote records by @author in #14.

### 🔐 Security
- Hardened `AddSecretScreen` against predictive keyboard caching (CWE-359).

### 👥 Contributors
- @author made their first contribution in #12!

**Full Changelog**: https://github.com/owner/repo/compare/v{{ .Previous }}...v{{ .Current }}
```

### 2. Internal / Milestone Release Template (`RELEASE-vX.Y.Z.N.md`)
```markdown
# Release vX.Y.Z.N - YYYY-MM-DD

## Summary
Brief description of the milestone, core architectural changes, and key user-facing features.

## Highlights
### 🌟 [Key Feature Name]
Description of the major feature, its architectural implementation, and user experience benefits.

## Breaking Changes
None in this release (or description of migration steps).

## Pre-Flight Verification Gate
- Unit & Robolectric Tests: X/X passing (100% green).
- Clean Build: `./gradlew assembleDebug` successful.
- Device Testing: Verified on physical hardware (e.g. Google Pixel).

## Dependencies & Schemas
| Component | From | To | Rationale |
| :--- | :--- | :--- | :--- |
| Room DB Schema | v1 | v2 | Added `audit_logs` table |
| SQLCipher | 4.6.1 | 4.6.1 | 16 KB page-size compliance |
```

### 3. Google Play Store Release Notes (`RELEASE-PLAY.md`)
> [!IMPORTANT]
> Google Play Console enforces a strict 500-character limit per localized `<en-US>` block. Plain text only.
```markdown
<en-US>
- Added Security Suite with screenshot privacy toggle & tap timeout.
- Added Emergency Panic Wipe for cryptographic vault zeroization.
- Added Security Audit Log timeline with Room persistence.
- Enhanced dynamic RFC 6238 TOTP URI sync with server delta filters.
- 100% passing tests & 16 KB memory page-size compliance.
</en-US>
```

---

## 🛡️ Best Practices: Do's and Don'ts

### Do's:
1. **Follow Conventional Commits strictly**: Makes automated changelog generation and version calculation deterministic.
2. **Keep commits atomic**: One logical change per commit; never bundle unrelated refactors with feature work.
3. **Use consistent scopes**: Define domain-specific scopes (e.g. `auth`, `sync`, `crypto`, `ui`, `settings`).
4. **Reference tickets and issues**: Add `Closes #123` or `Fixes #456` in commit bodies.
5. **Always maintain `[Unreleased]`**: Stage work in `CHANGELOG.md` as you build, so releases are seamless.

### Don'ts:
1. **Never edit generated changelog history arbitrarily**: Keep version tags, hashes, and dates aligned.
2. **Never hide breaking changes**: Always mark breaking modifications with `!` or `BREAKING CHANGE:` footer.
3. **Never skip pre-commit verification**: Passing tests and clean builds must precede any release commit.
4. **Never allow changelog desynchronization**: Updating code without updating `CHANGELOG.md` creates documentation drift.
