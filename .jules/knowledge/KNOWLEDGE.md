# 📚 Repository Knowledge & Learned Invariants (.jules/knowledge/KNOWLEDGE.md)

This file maintains the repository's persistent, auditable knowledge store for Google Jules and collaborating engineers. It captures empirical learnings, distilled architectural invariants, and rectified failure modes.

---

## 📱 Mobile UI & Ergonomics
- **Physical QR Code Invariant**: In mobile authenticator UI, 2D barcode migration features must render real, scannable QR bitmaps via ZXing wrapped in high-contrast white containers (`background(Color.White)` with padding), never textual placeholders or grey-box stubs.
  - *Retrospective Insight (Jules)*: The boundary lies between logical structure verification (headless tests) and physical reality rendering (optical scannability). Text placeholders pass virtual tests but critically fail the human use-case; we must always render true optical targets wrapped in high-contrast layouts.

## 💾 Storage & Data Architecture
- **Offline-First Migration Invariant**: Bulk vault migrations must be offline-first: insert imported records directly into local SQLCipher as `PENDING_SYNC` and delegate remote replication to `TotpSyncWorker` rather than executing sequential HTTP loops.
  - *Retrospective Insight (Jules)*: A synchronous bulk network push blocks the UI, drains battery, and invites partial failures on spotty mobile data. The YAGNI offline-first cut cleanly separates concerns: the UI persists to local SQLCipher immediately with `PENDING_SYNC`, while `TotpSyncWorker` shoulders the burden of eventually-consistent remote network replication in the background.

## 🧵 Component Seams & Lifecycle
- **Seam Wiring Invariant**: UI confirmation callbacks must never drop state or leave business logic as uncalled comments; always wire dialog confirmation directly to repository/persister suspend routines inside appropriately scoped coroutines.
  - *Retrospective Insight (Jules)*: An unwired confirmation dialog provides the user a false sense of security while their data is silently dropped. The invariant requires an engineer to treat the UI flow as strictly incomplete until the precise action triggers a verified repository or persistence suspend function.

## 🔒 Security & Authentication (OWASP MASVS-AUTH-1)
- **MASVS-AUTH-1 Step-Up Invariant**: Under OWASP MASVS-AUTH-1, sensitive export operations must enforce step-up authentication across all device configurations: challenge `BiometricPrompt` when available, and strictly fall back to a Master PIN/Password challenge dialog when biometrics are disabled.
  - *Retrospective Insight (Jules)*: An unavailable biometric sensor does not mean a user's vault requires less protection. Failing open critically violates MASVS-AUTH-1. We must guarantee step-up authentication across all configurations by failing closed and enforcing a strict Master PIN/Password fallback.
- **Window Capture Protection**: All step-up dialogs and export viewer dialogs displaying sensitive secrets or 2D barcodes must enforce `FLAG_SECURE` on their window.

## 📖 Single Source of Truth (SSoT) Documentation
- **SSoT Documentation Invariant**: Every release version bump mandates a synchronized documentation walk: update `app/build.gradle.kts`, `CHANGELOG.md`, `SECURITY.md`, `README.md`, `ROADMAP.md`, and prepend a <500 character plain-text block in `RELEASE-PLAY.md` in the exact same commit.
  - *Retrospective Insight (Jules)*: Automated release pipelines expose mismatched documentation instantly. Skipping `RELEASE-PLAY.md` meant advertising stale `v0.0.2.3` features against `v0.0.3.0` binaries. All documentation must bow to the code synchronously within the exact same atomic commit.

---

## 🎓 Milestone Retrospectives Log

### Milestone 3 (`v0.0.3.0 Build 17`) — Socratic Retrospective Sign-Off
- **Session ID**: `14327715134975196291`
- **Lead Architect**: Lucas (ClawStack Studios) & Antigravity
- **Co-Pilot**: Google Jules (`@google-labs-jules[bot]`)
- **Deliverables**: Google Authenticator Multi-QR export carousel, Bitwarden/2FAS/Aegis import persister, MASVS-AUTH-1 step-up authentication with Master PIN fallback dialog, 100% SSoT documentation sync.
- **Sign-Off Status**: Completed & Verified ✅
