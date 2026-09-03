# Consolidated Learnings & Durable Patterns

## Test Oracle & Architectural Invariant Synchronization
**Pattern: Continuous Test Fixture Auditing**
- Whenever an architectural rule or data filtering logic changes (e.g. One-Way Mirror Sync excluding remote codes from backup exports, or Grouped Dashboard replacing filter chips), inspect all test fixtures in `app/src/test`.
- Audit both unit test expectations (`assertEquals`) and Compose UI test semantic finders (`onNodeWithTag`, `onNodeWithText`).
- *Rationale:* Prevents silent latent regressions from passing undetected until headless CI execution.

## Local Execution Environment & Java JBR Mapping
**Pattern: Android Studio JBR Java Runtime Provisioning**
- In environments where global `java` or `gradlew` is missing, configure:
  ```bash
  export JAVA_HOME="/config/Applications/android-studio/jbr"
  export PATH="$JAVA_HOME/bin:$PATH"
  ```
- Generate Gradle wrapper locally via cached binaries in `/config/.gradle/wrapper/dists/`.

## ClawKey Identity & Ingestion Deduplication
**Pattern: Pre-DAO Normalized Fingerprint Deduplication**
- SQLite `@Insert(onConflict = OnConflictStrategy.REPLACE)` conflicts solely on the primary key UUID.
- For backup imports and account ingestion, compute normalized fingerprint:
  `fingerprint = secret.uppercase().replace(" ", "").replace("-", "") + "_" + title.trim().lowercase()`
- Filter incoming entities against existing records before database insertion to prevent duplicate records.
