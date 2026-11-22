# 🦞 ShellGuard TOTP — Release v0.0.3.3 (Build 20)

> **Opt-In Self-Signed HTTPS & TOFU Pinning**: `versionCode` 20 (`versionName = "0.0.3.3"`). Adds an explicit opt-in checkbox on the Auth Gateway (`GatewayScreen`) to connect to `TLS_ENABLED=true` self-hosted ShellGuard servers using self-signed certificates, backed by Trust-On-First-Use (TOFU) SHA-256 certificate fingerprint pinning and host-scoped `HostnameVerifier`. Strict platform TLS remains the default. Install over previous builds in place; vault data is preserved.

---

## 🚀 Summary

When `TLS_ENABLED=true` is enabled on the ShellGuard web server, the server auto-generates a persistent self-signed X.509 certificate. This release adds a safe, explicit opt-in pathway on the Android Auth Gateway without re-introducing unconditional CWE-295 bypasses.

---

## 💎 Highlights

### 🔐 1. Auth Gateway Self-Signed HTTPS Toggle
* Added an *"Allow self-signed HTTPS certificates"* checkbox on `GatewayScreen` (visible when `https://` is selected, unchecked by default).
* Persisted across cold restarts in `AuthRepository` (`pref_allow_self_signed_tls`) so background sync (`TotpSyncWorker`) continues working seamlessly.

### 📌 2. Trust-On-First-Use (TOFU) SHA-256 Certificate Pinning
* `KtorClientProvider` computes and pins the leaf X.509 certificate's SHA-256 fingerprint on first connect (`pref_pinned_cert_sha256`), scopes `HostnameVerifier` to the configured gateway host, and fails closed with `CertificateException` on any subsequent fingerprint mismatch.
* `ApiClient` invalidates its cached `ShellGuardTotpClient` whenever `baseUrl` or `allowSelfSigned` changes.

---

## 🧪 Verification Record

* **Unit & Robolectric Tests**: Added `SelfSignedTlsGatewayTest` covering TOFU pinning, mismatch rejection, empty chain rejection, `ApiClient` cache invalidation, and `AuthRepository` cold-restart persistence.
* **Deterministic Doc Audit**: `python3 scripts/audit_docs_drift.py` passed 100%.

---

*ClawStack Mobile Studios©™ — Build features around security, not security around features.*
