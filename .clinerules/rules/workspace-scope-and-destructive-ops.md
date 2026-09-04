# Rule: Workspace Scope & Destructive Operations

> Adopted 2026-09-03 from Lucas's corrections during the v0.0.1.2 release session.
> Two halves of one discipline: **stay in scope; prove before destroying.**

## 1. Workspace Scope Boundary

All Cline configuration and knowledge lives in the project's `.clinerules/`
directory. Exploration never needs to leave the workspace.

- ❌ NEVER run filesystem-wide searches (`find /`, `grep -r /etc`, etc.) to
  locate config, skills, or tooling.
- ✅ Check `.clinerules/` first; if a genuinely external path is required,
  state the exact path and get approval before touching it.
- *Origin:* During `/learn`, a `find / -name 'SKILL.md'` reached far outside the
  project ("the hermes folder?!"). The correct answer was always
  `.clinerules/workflows/` inside the repo.

## 2. Destructive Operations: Audit First, Tiered Approval

Any deletion, purge, or system-level mutation requires:

1. **Audit first** — measure per-target impact with real numbers (`du -sh`,
   `df -h`) and identify what each target actually is, BEFORE proposing removal.
2. **Tiered proposal** — present options with explicit risk labels
   (safe-regenerates / safe-but-slow-next-build / breaks-if-removed), including
   what is explicitly *kept*.
3. **Execute only the approved scope** — if the user narrows scope ("tier 1
   only, /tmp only"), that scope is a hard boundary; do not silently expand it.
4. **Pause on interrupt** — "hold on" / "wait" stops execution immediately;
   re-verify state (things may have changed underneath, e.g. external infra
   fixes like a Docker image → directory conversion) before resuming.
5. **Report before/after** — show the measured effect of the operation.

- *Origin:* A queued `rm -rf /tmp/*` + Gradle-caches wipe was paused by Lucas
  ("what exactly is being cleared?"). The audit revealed root was the Unraid
  `docker.img` loop at 100%; after Lucas converted it to directory mode, the
  purge became unnecessary entirely — validating pause-before-destroy.