# Predictive Pattern Library

## Anticipated Learning Needs

### Pattern: Preferences Store & Structured Settings (Phase 11)
- **Prediction**: Phase 11 introduces `AppearancePreferences` and `BehaviorPreferences`. Migrating or expanding preferences from `SharedPreferences` to Jetpack `DataStore<Preferences>` will require reactive Flow binding and default fallback state management.
- **Confidence**: 0.92
- **Basis**: Observed in multi-screen Android applications requiring reactive configuration updates across Compose trees without Activity recreations.
- **Recommended Resources**: [AndroidX DataStore documentation, Compose StateFlow collectAsStateWithLifecycle patterns]

### Pattern: Emergency Security Purge & Hardware Key Deletion (Phase 12)
- **Prediction**: Panic Purge functionality will require atomic zeroization: clearing Room databases with SQLCipher `rekey`, deleting KeyStore aliases (`AndroidKeyStoreHelper.deleteKey()`), and resetting WorkManager jobs in a failsafe synchronous loop.
- **Confidence**: 0.89
- **Basis**: Hardware-backed KeyStore aliases must be completely purged to guarantee unrecoverability upon security breaches.

## Cross-Domain Connections

### Pattern: Cryptographic Wire & Rest Parity
- **Domains**: Android Kotlin Client, Node.js / Express Server, Rust / C Core
- **Common Principle**: Authenticated Encryption with Associated Data (AEAD) binding (`AES-GCM-256` with HKDF key derivation)
- **Transferable Insights**: Consistent AAD framing (`vault_pearls_totp:{id}`) ensures encrypted payloads cannot be swapped across entities or hijacked between client and server.

### Pattern: Strict One-Way Mirror Synchronization
- **Domains**: Android Authenticator, Web App Vault, Desktop CLI
- **Common Principle**: Client instances acting as mirrors must enforce immutability at the storage query level, avoiding bidirectional merge conflicts and preserving local user autonomy.
- **Transferable Insights**: Conditional SQL deletion (`deleteByIdIfLocal`) and client-side timestamp delta checks (`updated_at` comparison).
