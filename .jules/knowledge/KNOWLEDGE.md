# 📚 Repository Knowledge & Learned Invariants (.jules/knowledge/KNOWLEDGE.md)

This file maintains the repository's persistent, auditable knowledge store for Google Jules and collaborating engineers. It captures empirical learnings, distilled architectural invariants, and rectified failure modes.

---

## 📱 Mobile UI & Ergonomics
- **Physical QR Code Invariant**: In mobile authenticator UI, 2D barcode migration features must render real, scannable QR bitmaps via ZXing wrapped in high-contrast white containers (`background(Color.White)` with padding), never textual placeholders or grey-box stubs.

## 💾 Storage & Data Architecture
- **Offline-First Migration Invariant**: Bulk vault migrations must be offline-first: insert imported records directly into local SQLCipher as `PENDING_SYNC` and delegate remote replication to `TotpSyncWorker` rather than executing sequential HTTP loops.

## 🧵 Component Seams & Lifecycle
- **Seam Wiring Invariant**: UI confirmation callbacks must never drop state or leave business logic as uncalled comments; always wire dialog confirmation directly to repository/persister suspend routines inside appropriately scoped coroutines.

## 🔒 Security & Authentication (OWASP MASVS-AUTH-1)
- **MASVS-AUTH-1 Step-Up Invariant**: Under OWASP MASVS-AUTH-1, sensitive export operations must enforce step-up authentication across all device configurations: challenge `BiometricPrompt` when available, and strictly fall back to a Master PIN/Password challenge dialog when biometrics are disabled.
- **Window Capture Protection**: All step-up dialogs and export viewer dialogs displaying sensitive secrets or 2D barcodes must enforce `FLAG_SECURE` on their window.

## 📖 Single Source of Truth (SSoT) Documentation
- **SSoT Documentation Invariant**: Every release version bump mandates a synchronized documentation walk: update `app/build.gradle.kts`, `CHANGELOG.md`, `SECURITY.md`, `README.md`, `ROADMAP.md`, and prepend a <500 character plain-text block in `RELEASE-PLAY.md` in the exact same commit.
