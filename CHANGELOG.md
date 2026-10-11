# Changelog

User-visible release changes only. Detailed engineering and test evidence belong
in [change notes](docs/changes/README.md). No release is published by this PR.

## Unreleased

- Prepare a new pack from all launcher entries without an external source.
- Inspect an installed source pack with actual static drawable coverage, separate project/lock status and searchable target selection; open the full classic editor.
- Save project-specific AI descriptions and up to five ordered PNG references locally, separately from legacy global AI settings.
- Save exact Create/Extend targets for later review; source and app changes require revalidation. No generation plan or automatic AI work is added.
- Full backups and profile shares carry project style history and reference images; legacy archives remain readable.

- Start from Projects, continue any real Renkin profile or create a separate icon pack.
- Open the classic editor from either Create or Complete an existing icon pack;
  automatic style recognition remains planned.
- Return from the editor to Projects without switching or saving implicitly;
  save/cancel protects changes before entering a different project.

## 2026.10.04-ai2 — existing fork baseline

- Generate selected missing app icons sequentially with per-app instructions.
- Save successful results to the active profile and recover the batch journal
  after interruption, with explicit paid confirmation for resume/retry.
- Keep single-icon Generate / Preview / Use in editor and the Renkin pack workflow.

These entries describe source at `eb4daa5de82fab54e115363c073eb8981f2203d6`.
The [October 4 handoff](docs/AI_HANDOFF_HU.md) is historical build evidence,
not a new test run or proof that this fork has a published GitHub release.
