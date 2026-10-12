# ADR 0006 — consent-bound project style trials and reusable approvals

Date: October 12, 2026. Status: proposed for owner review in Codex 05.
Accepted base: owner-merged PR #5, `af7bdb104869b0d0dded2acdf020fc030642dcf9`
at 16:10:49 UTC; source `aceb691c6429e0f7cd86be75d3da5d0df087f149`.

## Fact register and scope

At the accepted M4 baseline, source owns immutable StyleRevision, ordered references,
ReferenceAsset and IconAssetVariant in the existing Room v20. Its image gateway
does not persist consent/results or approve styles. M4 consent only authorizes
structured analysis. Legacy global AI1/AI2 and the real editor/build/install remain.
The original plan requests usually three trial icons before a later batch; this
milestone implements only 0–3 explicit trials, individual review/regeneration,
revision refinement and durable readiness. M6 gallery/bulk Apply and M7 image edit
remain separate tasks. No live/billable request is authorized during development.

## Decision

Use the existing Activity VM/session owner and profile identity. Add a TRIAL route
from STYLE and TARGETS. Require a saved style and freshly verified saved component
selection. Unknown/dynamic/work-user/locked/protected local targets stay disabled;
source-covered overrides require a separate visible user choice. Recommend stable
spread across sorted component keys, without claiming calibrated visual complexity.
Only the selected 0–3 actual originals are rendered to bounded 1024px PNG snapshots.

Freeze profile/mode/source/catalog/selection/version/revision/epoch, ordered owned
reference hashes, original hash+origin, model/quality and independent per-item UUIDs.
Use PromptComposer for saved description and separate instruction; global settings
provide only explicit provider/model/quality defaults. Display images, N requests,
Stability's up to 2N endpoints, upload consequences and unknown exact cost before
the separate confirmation. Opening/resume/recreation only reads state.

Keep a separate versioned, bounded noBackup style-trial journal. Persist consumed
plan/PENDING before REQUESTING, then each endpoint marker before its POST. Both
image clients use one-shot multipart bodies with disabled retries/redirects. Serial
execution permits at most one trial icon at a time; Stop skips unstarted items and
marks an interrupted sent request UNCERTAIN. No automatic replay, resume or retry.
Stability's two endpoint markers remain distinct; a failed second step cannot
repeat either endpoint without an entirely new, explicit consent.

Persist a validated result in atomic staging before the Room insert. Install owned
original+RAW PNG assets and idempotent request-ID variant in one DB transaction,
then mark REVIEWABLE. A file/DB/marker interruption performs only local recovery,
never another POST. Keep saved versions, original input and NOT_APPLIED status.
The current 4 MiB project-asset limit remains: an oversized normalized result stays
in bounded staging with a visible persistence error, never false completion.

Room v21 adds nullable variant trial evidence and a narrow StyleTrialApproval table
linked to profile+immutable revision, reviewed accepted variant IDs and frozen
context. A Boolean UI flag cannot represent durable, portable approval. Explicit
Accept/Reject only changes review; a separate common-style approval needs at least
one accepted current result and no unreviewed successful current trials. New style,
source/target/selection/reference changes or restore invalidate readiness. M6 gets
read-only AcceptedTrialVariant values only after fresh identical input validation,
without generation or Apply. Approval does not authorize future paid requests.

Full/share format 6/project payload v3 carries originals, RAW variants, evidence
and approval history. Share remaps profile/variant IDs and marks approval stale;
full restore rotates trial epochs and marks restored approvals for revalidation.
Legacy formats and existing gallery remain readable. The new trial payload excludes
journals/credentials and adds no signing material; existing full-backup pack-keystore
behavior is unchanged. Profile deletion tombstones consumed IDs. Asset leases, FK
ownership, history/variant pins and targeted GC remain authoritative.

## Alternatives and consequences

- Reusing the M4 or AI2 journal would mix different paid permissions and Apply
  semantics; use a separate request journal with existing project assets instead.
- A second gallery/database/session duplicates ownership and is rejected.
- Approval only in noBackup or rememberSaveable would lose valuable readiness on
  backup or process recreation; use the smallest additive existing-DB migration.
- Automatically resuming REQUESTING or a Stability second step could bill twice;
  preserve uncertainty and require new human consent rather than guessing success.
- A computed complexity score is unnecessary for correctness; stable distribution
  and manual selection avoid scanning every phone icon at provider resolution.

Genuine KSP v21 export, physical v20→v21/fresh migration, full/share/legacy restore,
split-write faults and real MockWebServer negative controls are required evidence.
v20 APKs cannot open populated v21; use forward repair or deliberately compatible
pre-upgrade full-backup recovery with unchanged app/pack signing identities.
Legacy multi-store restore is still not distributed ACID. Device/provider quality,
account access, exact costs and Samsung/TalkBack acceptance remain separate tests.

## References

Official [image generation](https://developers.openai.com/api/docs/guides/image-generation),
[image edits](https://developers.openai.com/api/reference/python/resources/images/methods/edit)
and [pricing](https://developers.openai.com/api/docs/pricing) are checked for the
existing original-first PNG/transparent edit contract, not account/model access.
See [ARCHITECTURE](../ARCHITECTURE.md), [HANDOFF](../agent/HANDOFF.md) and
[QA](../qa/BASELINE.md) for implementation and actual head-specific evidence.

Consumed per-item UUIDs also have a separate device-wide AtomicFile ledger (8,192
markers maximum). Another plan/profile cannot reuse an item UUID; deletion and
restore retain these markers. Reaching the bound fails closed before a POST.


Explicit local revalidation retains the immutable generation context/original origin
and records a separate reviewContext/reviewOriginalOrigin in evidence schema v2
(v1 remains readable). After source/selection/epoch or share changes, identical
profile/revision/original pixels/ordered refs/provider/model/quality can be rechecked
and accepted locally, then approved separately, with zero POST. Changed artwork,
style revision or refs cannot reuse the old PNG. Eligibility and fresh owner/source
checks still fail closed. Read-only M6 reuse requires this current review proof and
current approval, never merely an old ACCEPTED status. Historical regeneration may
prepare a new current-input plan, but always needs new paid confirmation.


The current input also hashes the actual saved specification plus separate user
instruction. Reused numerical profile/revision IDs after restore cannot imply
identical style text. Unknown historical style hashes remain viewable and cannot
authorize a new request/reuse. Result insertion checks the saved text and ordered
refs again under the asset lease. A 32,000-character composed-prompt ceiling keeps
quoted maximum-length saved inputs intact and is validated before cost consent.
Native multipart factories remain compatible; only actual network requests receive
the whole-body one-shot wrapper (pinned4.12 does not propagate child flags).
Provider model/account failures use safe typed categories with no raw server text,
no automatic fallback/retry. Historical cards display their actual producer options.

Profile deletion writes schema-valid redacted tombstones: the style fingerprint
matches the redacted text, and Stability retains one all-zero sentinel reference
hash so its real provider and two endpoint counters remain readable. Prepared=false,
rotated epoch, missing images and retained device-wide consumed item IDs prohibit
replay under the old or another owner. Other profiles' records/assets stay intact.
