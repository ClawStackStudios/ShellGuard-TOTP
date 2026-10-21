# Cross-Domain Knowledge Synthesis

## Synthesized Insights

### Insight: Multi-Tier Defense-in-Depth for Mirror State
- **Storage Layer**: Database queries enforce ownership invariants natively in SQL (`WHERE is_local_only = 1`).
- **Domain Layer**: Repository / ViewModel rejects mutating operations on remote entities.
- **UI Layer**: Visual signals and disabled interactions eliminate erroneous user expectations.
- **Meta-Principle**: Security and immutability must never depend solely on UI layer state or view-model flags; they must be anchored at the innermost persistence boundary.

### Insight: Containerized Execution Resilience
- **Gradle Daemons**: Hotspot performance counters fail in restricted container PID/IPC namespaces; explicitly disabling them (`-XX:-UsePerfData`) prevents unrecoverable VM crashes.
- **Temp Storage**: Forcing `java.io.tmpdir` to `$PWD/app/build/tmp` provides total control over file access and permissions without relying on host OS `/tmp`.
- **Meta-Principle**: Decouple build tooling from host filesystem conventions to ensure true portability between local IDEs and cloud CI runners.

### Insight: Git History as Cryptographic Source of Truth for Release Documentation
- **Commit Boundary**: When commits strictly follow Conventional Commits with domain scopes and breaking change tags, release documentation becomes a deterministic function of the commit graph: `Changelog = f(GitHistory)`.
- **Accountability Preservation**: Pairing Conventional Commits on line 1 with two-layer attribution (`User:` intent / `AI:` implementation) bridges automated release tooling with forensic human-AI accountability.
- **Meta-Principle**: Never write documentation that drifts from source code; derive release documents directly from atomic, verifiable git events.

## Evolution Tracking

### Concept: Synchronization Architecture
- **Phase 5 (Initial)**: Bidirectional delta sync with upstream pushing of locally created tokens.
- **Phase 7 (Refactor)**: Shifted to strict One-Way Mirror sync — local tokens are strictly offline and isolated, server tokens are read-only.
- **Phase 10 (Current State)**: Client-side delta snapshot comparison eliminates redundant AES-GCM decryption cycles for unchanged records while conditional SQL deletes prevent accidental local deletion.
- **Next Evolution**: Background pull-to-refresh with delta hash verification and offline change queueing for metadata only.

### Concept: Changelog and Release Documentation Architecture
- **Phase 0–8 (Initial)**: Manual changelog updates, ad-hoc category naming, unlinked release versions.
- **Phase 9–12 (Intermediate)**: Keep a Changelog headers with multi-point documentation walk (`ROADMAP.md`, `RELEASE-v*.md`, `RELEASE-PLAY.md`).
- **Current State (Pattern-Bolstered)**: Keep a Changelog 1.1.0 standard with 6 strict categories (`Added`, `Changed`, `Deprecated`, `Removed`, `Fixed`, `Security`), Conventional Commit type-to-SemVer mapping matrix, `git-cliff` configuration (`cliff.toml`), compare links footer, and two-layer attribution alignment.
- **Next Evolution**: Automated headless GitHub Actions release notes synthesis with `git-cliff` action directly populating GitHub Releases on tag pushes.

### Concept: Image & QR Decoding Architecture
- **Phase 1 (Initial)**: CameraX live preview with ML Kit analyzer.
- **Phase 10 (Current State)**: Shared `ImageQrDecoder` extracting QR codes from both camera buffers and SAF gallery image streams via unified ML Kit vision pipeline.
- **Next Evolution**: High-density animated QR decoding (multi-frame chunk ingestion for bulk vault import).

### Concept: CI/CD Build Gating & Runner Conservation
- **Initial**: Automatic build and release attempts on every branch push.
- **Phase 9 / 10**: Tag-based releases with separate mirror jobs triggering on all pushes to main.
- **Phase 10+ (Current State)**: Granular multi-trigger gating (`v*` tags, `--release` commit flags, `workflow_dispatch`) with chained `always()` evaluation for release note mirroring, eliminating wasteful runner hours on routine commits.
- **Next Evolution**: Selective Gradle task caching with remote build cache and automated Play Console draft deployment.

