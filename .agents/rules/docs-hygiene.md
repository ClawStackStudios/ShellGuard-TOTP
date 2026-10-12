# Documentation Hygiene & Anti-Rot Rule

**Objective:** Prevent documentation rot by ensuring that architectural, state, and API changes are fundamentally tied to their documentation updates within the *same* branch and commit.

## 1. Zero-Deferred Documentation & Docs Bow to Code
- **The Code is Ground Truth**: Documentation is a compiled projection of the physical codebase. The documentation MUST unconditionally bow to the code.
- **Never defer documentation updates**: If you change the behavior of a module, component, database entity, or state flow, the corresponding documentation MUST be updated in the same branch before committing.
- "I will update the docs later" is treated as an incomplete task and a failure of the Definition of Done (DoD).
- Refer to the companion skill: [`.agents/skills/doc-automation/SKILL.md`](../skills/doc-automation/SKILL.md).

## 2. Trigger Conditions
You MUST proactively update the corresponding `.agents/memory-bank/` files or root/`project/` documentation when:
- **State & Storage Model Changes:** If you alter Room `@Entity` definitions, DAOs, or how state flows across ViewModels and `StateFlow`, update `project/room-storage-schema.md` and `systemPatterns.md`.
- **API/Endpoint & Wire Contracts:** If a REST route, DTO serialization model, or dynamic TOTP URI format changes, update `project/routes-and-contracts.md` and `compatibility_layer.md`.
- **UI Screen & Navigation Refactors:** If a Jetpack Compose screen or `TotpNavHost` route is added or renamed, update `README.md` and `activeContext.md`.
- **Cryptographic & KeyStore Updates:** If key derivation (HKDF, AES-GCM-256), KeyStore aliases, or zeroization receivers (`PanicTriggerReceiver`) change, update `project/crypto-and-keystore.md`.
- **Dependency & SDK Changes:** If a library is added or updated (e.g. SQLCipher, AndroidX, ML Kit), update `techContext.md` and `README.md`.

## 3. Inline KDoc & Comment Integrity
- Maintain KDoc integrity. If you change a Kotlin function signature, suspend function contract, or class interface, update its KDoc (`@param`, `@return`, `@throws`) immediately.
- Preserve existing comments that explain architectural rationale and *why* code exists, unless the underlying design has changed.

## 4. The "Same Commit" Mandate
Documentation updates must not be deferred to an isolated "chore: update docs" commit if they belong to a feature or fix. They must be bundled into the specific `AI:` layer of the Conventional Commit that introduced the code change, proving that code and documentation evolved together.

## 5. Release Documentation & Template Protocol
When preparing a version increment, phase completion, or release, you MUST execute the `.agents/workflows/walk-the-docs.md` workflow to systematically verify that the documentation bows to the code with structural precision.

- **Use Official Release Template:** Draft `RELEASE-vX.Y.Z.N.md` in the repository root adhering strictly to `.agents/doc-templates/release-template.md`.
- **Google Play Store Notes:** Prepend concise (<500-char) `<en-US>` release notes to `RELEASE-PLAY.md` for direct mobile store deployment.
- **Synchronize Central Anchors (9-Point Checklist):** In the same release preparation commit, synchronize all of the following:
  1. `app/build.gradle.kts`: Increment monotonic `versionCode` and update `versionName`.
  2. `ROADMAP.md`: Check off completed tasks (`[x]`), update `current_position`, recalculate `features_completed: XX%`, and **re-align downstream monotonic build projections** (`Phase X ➔ Build Y`).
  3. `project/meta-prompt-ai-studio.md`: Re-index build codes and stage headers for upcoming phases.
  4. `CHANGELOG.md` (Repository Root): Add `## [X.Y.Z.N] - YYYY-MM-DD (Build N)` entry following Keep a Changelog format.
  5. `RELEASE-vX.Y.Z.N.md`: Ensure root release note exists for `.github/workflows/release.yml`.
  6. `RELEASE-PLAY.md`: Ensure `<en-US>` block is strictly under 500 characters.
  7. `README.md`: Update version badge and verified test suite count.
  8. `SECURITY.md`: Verify active supported version matches `app/build.gradle.kts` and confirm cryptographic defense invariants.
  9. Domain & Storage Specs: Update `project/room-storage-schema.md`, `project/crypto-and-keystore.md`, and `ARCHITECTURE.md` if schema, crypto, or security flags were introduced.
  10. Cross-Repository Compatibility: Synchronize partner repository `compatibility_layer.md` if payload schemas or sync endpoints were modified.
- **Automated Publication Trigger:** Pushing with `--release vX.Y.Z.N` in the commit message or pushing tag `vX.Y.Z.N` automatically executes `.github/workflows/release.yml` to build, sign, and publish the release with the markdown notes and branded binaries.


