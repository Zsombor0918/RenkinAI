# ADR 0005 — bounded references and explicitly reviewed style analysis

Date: October 12, 2026. Status: proposed for owner review in Codex 04.
Accepted base: `6aed9e08ee5619c1061481972dc172546c7f5abe`, owner merge of PR #4.

## Decision and scope

Keep Profile.id, the existing Activity MainViewModel/session mutex, pack parser,
private project assets and immutable StyleRevision as the owners. Extend the same
parser with an explicit all-mapping read for source references; its default installed-
component coverage and legacy duplicate winner remain unchanged. Render bounded
static 256px PNGs off Main, exclude dynamic/declared Material You sources and unstable
drawables, remove exact/near duplicates, and choose deterministically diverse samples.
Sampling is local, not permission to upload or generate. Analysis may use up to twelve
samples; saved generation references still have the separate limit of five.

Use a dedicated OpenAI Responses structured-text StyleAnalysisGateway. The selected
vision model, schema, bytes and user text are frozen before an explicit confirmation.
The body carries only thumbnail PNGs and supplied text, never package inventory,
profile identifiers, paths or credentials. Keys come from existing AiCredentialStore.
Do not reroute legacy AI1/AI2 or the image-generation gateway. No tools or image
generation are requested. Account access and real visual quality need separate trials.

Persist one consent-bound request journal before submitting at most one POST. Keep
REQUESTING and completed candidate JSON/input hashes durably in app-private noBackup
storage. Reopening a killed request is uncertain, not replay. A saved candidate is
reviewable without rebilling. Retry is a new explicit confirmation and request ID.
Analysis samples are bounded separate staging; manual imports and historical project
pins are untouched. Explicit discard or completed review can release staging images,
while consumed request markers prevent replay. Unreviewed journals/staging are not
portable backups; this limitation must remain visible. Accepted style/references are.

Add only two nullable TEXT columns to StyleRevision through explicit v19→v20:
validated structured suggestion JSON and validated reviewed evidence JSON. Plain
specification/userInstruction remain separate. Legacy rows get NULL, never inferred
approval/provenance. Existing v19 alone cannot preserve typed structured evidence in
full/share backups without hiding JSON in user prose or a nonportable journal.
Format 5/project payload v2 preserves those fields and rekeys evidence ownership on
profile sharing; full restore marks evidence for revalidation and rotates the local
candidate epoch before identity replacement. Old formats remain readable. Older importers reject the new format
instead of silently dropping provenance. Genuine KSP export and a physical v19
fixture migration are required; never hand-write schema identity.

Only user review followed by explicit Save activates a revision. Unmodified canonical
suggestions are AUTO_ANALYSIS; edited descriptions remain USER with recorded analysis
provenance. Separate user instruction is retained. Validate current owner, optimistic
revision/metadata/selection and source fingerprints before writes. Stale candidates
stay visible with warning and require explicit review override. No icon autosave,
refresh, primary-source mutation, batch, build or implicit cost authorization.

## Alternatives and consequences

- A second parser/project DB/session would drift from source identity and ownership;
  rejected in favor of narrow existing interfaces.
- Store analysis JSON in global AI settings or specification prose would lose typed
  provenance/backup semantics and confuse manual text; rejected.
- Keep accepted structured evidence only in noBackup journal would break portability;
  use the minimal nullable forward migration, with a documented downgrade boundary.
- Automatically resend interrupted requests could bill twice; preserve consumed IDs
  and uncertainty even if no response was returned.
- A large style editor or new image workflow belongs to later milestones; keep an
  editable text/ref draft and a small structured summary in the existing STYLE route.

Old v19 APKs cannot open v20. Prefer forward repair; any older-version recovery needs
an explicit compatible pre-upgrade backup and signing continuity. Legacy full restore
still spans separate stores and is not distributed ACID. No SDK/permission/package/
signing/build changes. Device/provider trials remain NOT RUN until measured.

Current implementation and actual checks are recorded in [state](../agent/CURRENT_STATE.md),
[architecture](../ARCHITECTURE.md), [QA](../qa/BASELINE.md) and [handoff](../agent/HANDOFF.md).
Codex 05 requires a fresh, independent paid confirmation for trial images.


## Verified external API contract (October 12, 2026)

Official [model capabilities](https://developers.openai.com/api/docs/models/gpt-5.4-mini),
[vision input](https://developers.openai.com/api/docs/guides/images-vision),
[strict structured text](https://developers.openai.com/api/docs/guides/structured-outputs?api-mode=responses),
[Responses parameters](https://developers.openai.com/api/reference/cli/resources/responses/methods/create)
and [response output migration](https://developers.openai.com/api/docs/guides/migrate-to-responses)
were read. Model gpt-5.4-mini supports image input/structured output; the port uses
text.format, low-detail PNG data URLs, max_output_tokens and explicit store false.
The official [pricing](https://developers.openai.com/api/docs/pricing) and
[image cost guidance](https://developers.openai.com/api/docs/guides/image-cost-calculator)
do not prove this account's access, icon recognition quality or exact request cost.
No live request was made to establish those facts. Runtime unsupported/auth errors
remain explicit outcomes and manual style remains usable.
