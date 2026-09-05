# Raw Reflection Log

<!-- Processed entries from Phase 9, Phase 10, Release v0.0.1.2, and Store Listing Assets have been consolidated into .agents/memory-bank/consolidated_learnings.md -->
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


