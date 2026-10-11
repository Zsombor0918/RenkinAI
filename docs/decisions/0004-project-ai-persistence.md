# ADR 0004 — Profile-owned AI style and immutable assets

Date: October 11, 2026. Status: proposed in Codex 03; owner/reviewer acceptance pending.
Base: owner-accepted main `aefeb1b69e7757c757c60ac2f00ce2125893e1e6`.

## Decision

Use existing Profile.id and the existing RenkinPackDatabase v19, with an additive
18→19 migration and actual KSP-exported schema. One ProjectAiMetadata row carries
mode/source/preparation, optimistic version and the only active revision pointer.
StyleRevision history is immutable, profile-local and monotonically numbered.
Saving is not approval. Ordered StyleReference links bind at most five immutable
ReferenceAssets. IconAssetVariant supports actual insert/read/owner relationships,
separate request/review/apply statuses and hashes; it adds no gallery/workflow and
does not reclassify old custom icons or AI2 SUCCEEDED journals.

PNG bytes live in private project_ai_assets/profile/hash.png, not Room blobs,
profile prefs or SavedState. Identical content deduplicates within its owner;
different profiles have separate paths so deletion cannot strand another owner.
History and variants pin files; draft imports have owner rows and remain pinned
until explicit discard. GC is explicit and only removes unreferenced files.
Validated PNG temp/fsync/rename precedes the database record; cross-store ACID is
not claimed. Normal writes are immutable; an explicit validated restore can repair
a corrupt file. Per-image 4 MiB, 1–4096 px and per-project 64 MiB limits are enforced;
restore can temporarily hold two quotas while previous images remain recoverable.

MainViewModel's existing profile-session mutex rechecks captured owner/readiness
after acquisition. Style saves check expected active revision; equal payloads are
idempotent. Selection saves check metadata.version and fresh source/catalog facts.
Persisted selection requires explicit revalidation and is never a billable plan.
Known work-user ambiguity remains UNKNOWN/disabled; no hidden identity migration.

Choose profile-share option **A**: full backups and shares carry relevant history,
references/variants and PNGs, without credentials or device-local journals/drafts.
Shares allocate the existing new profile ID and remap all new ownership/path links;
source/candidate context requires revalidation. Full replace clears missing new
metadata in legacy archives. Format 4 stages and validates allowlisted bounded ZIP
entries, owners, hashes, PNGs and duplicates before writes. Project files install
before the final profile/icon/project Room transaction. Legacy watch/preferences/
presets/gallery/keystore remain separate stores, so full restore can still be
partially interrupted there. Signing/password behavior and pack identity stay.

AiSettingsStore remains global for legacy AI1/AI2. A project starts unset; a labelled
manual copy action can copy style/references into a draft without changing globals.
The real IconGenerationGateway adapter reuses existing image clients/credentials;
internal HTTP seams serve offline tests. Legacy service/single/batch are deliberately
not rerouted yet. The port performs no persistence, scheduling, retry, approval or
price estimation. Cancellation/unknown post-submission outcomes require explicit
future user decisions. All Codex 03 UI callbacks are local, with no gateway consumer.

## Alternatives considered

- Global DataStore style as project style would leak between profiles and change
  legacy AI behavior; rejected.
- A second project DB/identity would complicate deletion, session ownership and
  backup associations; rejected.
- Referencing Upload names alone would lose project images on gallery deletion;
  protected private content copies are used instead.
- Overwriting one style row would lose history needed by later trial candidates;
  immutable revisions plus one active pointer are used.
- Profile shares containing only icons are permitted as a documented limitation
  by the task, but embedded assets/remapping are feasible here, so portability A
  is implemented and tested rather than promised for a later phase.
- Rerouting AI1/AI2 now would enlarge the acceptance/draft/batch risk; a real tested
  adapter is added without that integration. This is not full single/batch integration.

## Consequences and rollback

Old v18 APKs cannot read an upgraded v19 database; an ordinary code revert is not
a safe device downgrade. Keep a pre-upgrade full backup and restore it to the old
version's compatible storage only through an explicit recovery procedure, or ship
a forward repair preserving v19. New format 4 exports reject older importers rather
than silently lose references. No app/pack ID, signing, SDK, permissions or API key
changes. Physical device upgrade/restore/OEM tests remain required before acceptance.

Codex 04 may consume saved revision text/instruction and ordered validated refs,
after explicit source/catalog revalidation. It must add its own multimodal analysis
port and approval contract. Codex 05 trials, 06 staging, 07 image edits, GenerationPlan,
backend/login/credits/Billing and automatic sampling remain separate tasks.

See [architecture](../ARCHITECTURE.md), [current state](../agent/CURRENT_STATE.md),
[change record](../changes/2026-10-11-project-ai-persistence.md) and
[QA](../qa/BASELINE.md). Green automation is not owner acceptance.
