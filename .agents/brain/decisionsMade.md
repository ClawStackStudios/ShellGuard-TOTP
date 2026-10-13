# Decisions Made
Structured record of meaningful choices. Each entry captures the
context, options, choice, and felt reason. Append-only.
This is the auditable companion to myStory.md.
The story says how it felt. This says what was actually chosen and why.

## Re-grounding to Pure 2FA Seeds (Zero Attachments) — 2026-10-12 13:30

**Context**: Context drift from the twin Web Server repository led to analyzing file attachments for the Android TOTP application.  
**Options considered**:
- Option A — Retain generalized attachment ingestion logic in case the TOTP app ever supports file backups.
- Option B — Strictly enforce the sovereign 2FA authenticator boundary: zero file attachments, pure TOTP keys, in-RAM password stripping.  
**Chosen**: Option B  
**Why**: Felt reason: The app's strength is its parsimonious, sovereign scope. Expanding into generic attachment storage degrades the security surface and betrays the user's explicit mental model. Pure 2FA keys at rest inside SQLCipher is the whole point.  
**Confidence**: high — Any expansion into attachments would violate the core architectural charter.  
**Outcome**: Removed all attachment references, grounded Phase 13 recon on `login.totp` extraction, and updated reflection workflows.  
**Pattern reference**: Link to `systemPatterns.md § Security Invariants (Rule 6: Zero-Knowledge Backup Intake & Sanitization)`.

## Anticipatory Forward Cartography over Idle Waiting — 2026-10-12 14:00

**Context**: Preparing delegation of Phase 13 to Jules left a potential idle window between task authoring and PR delivery.  
**Options considered**:
- Option A — Wait passively for Jules to open PR #1 before analyzing downstream dependencies.
- Option B — Execute `/pre-work-assumptions` immediately, leap one milestone ahead to Phase 14 (Glance Widgets), and stress-test the consumer contracts.  
**Chosen**: Option B  
**Why**: Felt reason: Idle time during cloud CI or subagent runs is cognitive waste. Leaping forward reveals the missing joints that the in-flight stage didn't know it needed to provide.  
**Confidence**: high — Catching gaps before the code is written is exponentially cheaper than reviewing them out later.  
**Outcome**: Discovered the need for a one-shot `TotpItemDao.getAllTotpItems()` query and title splitting for icon packs, injecting both into Phase 13 specs before dispatch.  
**Pattern reference**: New pattern — `systemPatterns.md § Pre-Work Assumptions & Forward Cartography Pattern` (Instance 1).

## Pre-emptive Specification Injection in Jules Task 25 — 2026-10-12 14:08

**Context**: Specifying the Wave 1 contract for Jules after uncovering the Phase 14 Room query requirement.  
**Options considered**:
- Option A — Keep Task 25 minimal and let Jules stumble onto query limitations or fix them in Phase 14.
- Option B — Explicitly inject the `getAllTotpItems()` suspend query contract and compound title normalization directly into `.jules/tasks/task-25.md`.  
**Chosen**: Option B  
**Why**: Felt reason: Jules is an executor in an isolated VM; ambiguity in database contracts causes compiler churn and fragmented PRs. Giving him clean chalk lines preserves momentum.  
**Confidence**: high — Verified Room entity definitions and DAO syntax before dispatching.  
**Outcome**: Hardened `.jules/tasks/task-25.md` and `.jules/tasks/task-26.md` committed to `main` and pushed to origin before dispatching session `14327715134975196291`.  
**Pattern reference**: `systemPatterns.md § Ingestion & Deduplication Pattern`.

## Physical Glass Verification & Visual Receipt Anchoring in PR Feedback — 2026-10-12 19:30

**Context**: Testing Jules's Wave 3 & 4 implementation on PR #3. Unit tests in Robolectric passed and Cloud CI was green, but on the physical Google Pixel device, Google Auth Export displayed a grey placeholder box with text instead of a scannable QR barcode due to missing ZXing dependency.  
**Options considered**:
- Option A — Accept PR #3 as-is and patch the QR rendering locally ourselves.
- Option B — Maintain the Jules delegation loop, capture the physical screenshot receipt over ADB, anchor it in `store-assets/`, and comment back to Jules with visual proof and exact dependency (`zxing:core`) & rendering requirements.  
**Chosen**: Option B  
**Why**: Felt reason: Passing in a virtual container or unit test does not mean the feature works under physical glass. Jules is our dedicated co-pilot for Phase 13; holding the boundary and providing incontrovertible physical evidence keeps the delegation relationship accountable and results in a genuinely production-ready release.  
**Confidence**: high — Physical screenshot captured, verified root cause, and confirmed ZXing core fits our zero-native footprint.  
**Outcome**: Captured on-device screenshot, committed to `store-assets/audit-pixel-google-auth-export-placeholder.png`, pushed to `origin`, and commented on PR #3 with exact fix instructions.  
**Pattern reference**: `systemPatterns.md § Physical Verification & Co-Pilot Delegation Loop`.
