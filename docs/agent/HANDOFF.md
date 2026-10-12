# Agent handoff — Codex 05 style trials, owner acceptance pending

## Accepted baseline and scope

Owner merged PR #5 at 16:10:49 UTC on October 12, 2026 as
`af7bdb104869b0d0dded2acdf020fc030642dcf9`; source `aceb691c` passed its own
Android/regressions/APK, 792 tests, all 16 groups and four latest checks. All five
previous merges were reread. M4 owner acceptance is acknowledged; device/provider
quality and costs are still untested. Failed historical heads remain in QA.
M5 starts exclusively from accepted main on `feat/style-trials-m5`. Exact feature
HEAD, PR URL and final evidence belong in the external PR audit, after committing.

The source now connects TARGETS/STYLE to TRIAL in the same Activity VM/provider/
profile session. Saved immutable style plus separate userInstruction and ordered
0–5 owned refs feed 0–3 actual selected component trials. Stable sorted-key spread
is a recommendation, not calibrated visual complexity. CREATE needs no pack;
EXTEND rechecks actual coverage and explicit covered overrides. Locks, dynamic/
unknown/work-user/protected local targets remain disabled. Only selected originals
are freshly rendered at 1024px; thumbnails are never API inputs.

A separate frozen-input confirmation displays N generations, OpenAI N or Stability
up to 2N paid endpoints, exact model/quality, original/ref hashes and upload/cost
uncertainty. It authorizes only those UUIDs. Open/reopen/Back/resume/configuration,
M4 consent and saved target selection are never paid permission. Individual review
and a separate shared-style approval do not Apply/build/install or launch M6 batch.
Stability uses fixed style/remove-background endpoints; GPT model/quality settings
are not sent there and the UI says so. No provider/model defaults are silently changed.

## Request journal, crash and restore contract

`noBackupFilesDir/style_trials/<profileId>/plan-<UUID>.json` is a separate v1
AtomicFile journal, <=256 consumed plans/profile and <=64 KiB/record. Bounded original
and reference PNG snapshots plus result staging have a 64 MiB/profile quota.
PREPARED/PENDING consumes a plan even if its input writes fail. REQUESTING precedes
generation; each actual client persists its separate endpoint counter before enqueue.
The existing OpenAI and both Stability multipart bodies are one-shot and keep their
original byte/type/order contract, with disabled retries/redirects. There is no
429/503/backoff resend or automatic Stability second-step resumption.

Execution is serial. The existing VM job may continue a confirmed plan while the
Activity is recreated/another screen is open; no Worker/foreground guarantee exists.
Each next endpoint checks active owner, route, saved revision/version, source/index/
selection/catalog, refs, actual original hash+origin and epoch again. Stop cancels
sent work with uncertainty and skips unsent items. Cold PENDING is SKIPPED; cold
REQUESTING is UNCERTAIN if an endpoint marker exists (otherwise FAILED). Reopening
never replays either. A new attempt requires a newly displayed plan and confirmation.

Validated alpha/nonempty 1024 RAW first goes to atomic result staging, then original
and RAW install through the existing asset lease. One transaction inserts their
ReferenceAsset rows and request-ID IconAssetVariant; only then REVIEWABLE is written.
A result-file-before-marker interruption is found by the unique request filename.
PNG_SAVED recovery performs only idempotent local file/Room/marker repair, preserving
existing review status; no gateway call occurs. A failed local save stays PNG_SAVED,
with visible error and staged PNG. The existing 4 MiB project-image/64 MiB project
quota remains; an oversized normalized result (staging max 8 MiB) cannot be falsely
called complete. Reload may retry local persistence, never rebill.

Completed records retain consumed UUIDs while redundant staging inputs/results
can be released after Room durability. Historical styles, original/RAW asset rows
and variant pins survive. Profile delete rotates its epoch, tombstones metadata,
removes staging images but retains consumed markers. Full restore rotates the global
trial epoch before final DB replacement: old requests cannot attach to restored IDs.
M4 analysis and AI2 journals remain separate; no keys, keystores or inventories are
new journal/backup content. Legacy multi-store restore is not distributed ACID.

## Review, approval, schema and portability

IconAssetVariant remains COMPLETED/UNREVIEWED/NOT_APPLIED until explicit review;
regeneration adds a new UUID/variant and never overwrites the previous PNG. Accept/
Reject changes review only. A separate StyleTrialApproval stores profile+immutable
revision, exact context, accepted variant IDs and approvedAt. Every successful current
trial must be reviewed and at least one accepted before common-style approval.
Uncertain/skipped requests remain visible and are not claimed successful. Rejecting
an approved variant makes readiness stale. New revision/source/selection/refs/original/
epoch cannot inherit approval; historical material remains readable.

Existing Room v21 adds only nullable variant trialContextJson and StyleTrialApproval
through explicit 20→21. Genuine KSP export and physical SQLite v20/fresh identity tests passed on 0df78ef8;
physical-device upgrade remains untested. Format 6/project payload v3 carries original+RAW/history/evidence/approvals;
full restore marks approvals for revalidation, share remaps owner+variant IDs and
marks stale. Old payload 1/2 and format 1–5 still load without fabricated approval.
Trial RAW backup validation requires actual transparent, nonempty 1024 PNG, as well
as signature/CRC/hash/dimensions. Journals and paid-request rights are not portable.
Downgrade to v20 cannot open populated v21; use forward repair or deliberate
compatible pre-upgrade full-backup recovery with unchanged app/pack signing keys.

## Verification and remaining acceptance

Verified implementation head 0df78ef8 passed Android 38293199741 and
regressions 38293200475. Independently downloaded artifact 11767029451: exact HEAD,
verified ZIP digest,143 XML/856 tests/zero failures/errors/skips. All21 commands and
original/new selectors passed; M5 domain10/gateway8/journal12/UI12/persistence-session19.
Original CI APK 11767123455 is genuine; KSP19/20/21 bytes match source, with v21 identity
8c7a3c8b5b26ad29673ebbd77605838e. Populated OpenAI/Stability deletion passes with
real endpoint counts, redaction, zero replay and other-owner isolation. Full/shared/
legacy backup, file→Room split faults and current local re-review also passed.

Local correction Python 52 guards passed in 7.704 s; hooks/range checks pass. Local
Gradle failed before configuration downloading 8.11.1 (Network is unreachable);
hosted Linux/JDK17/native Robolectric/Room/MockWebServer provide Android evidence.
Closing documentation changes no runtime/test/schema/build file. The exact final
SHA, fresh full/filtered XML, genuine schemas/APK and four latest contexts are
recorded in PR #6 after commit; do not substitute this ancestor's result for them.
Required ruleset contexts remain build/commit-conventions/documentation-audit,
plus task-required profile-form-regressions. Ruleset 24877351 active, empty bypasses,
force/delete prohibited, approvals 0 and conversation/last-push/strict false remain
unchanged. No main push, merge, rule write, release or live API call occurs.

NOT RUN: physical device/emulator/Flip8/One UI/Theme Park, URI/storage-full/actual
process kill, real upgrade/restore/install/update/signing/TalkBack/hinge/OEM,
provider account/model quality/recognizability/exact billing. Zero live requests.
Final-head build/regressions/governance and artifact verification are recorded in
PR #6 before readiness. No tests/selectors/idling/timeouts are weakened, and owner
review/merge remains an explicit decision; no APPROVED review is fabricated.

## M6 read-only reuse and next boundary

OwnedStyleTrials.accepted(profileId) freshly verifies the existing owner/route,
immutable revision, metadata/selection/catalog/source, ordered ref fingerprint,
provider configuration, original hash+origin and storage epoch. Repository
acceptedTrials returns immutable AcceptedTrialVariant(profileId, componentKey,
styleRevision, originalHash, referenceFingerprint, assetHash, approvedAt, variantId)
only for explicit accepted IDs in a current approval with valid private PNGs.
It performs zero network/apply operation. Imported/restored/stale approval returns
no reusable value; errors fail closed. A new M6 GenerationPlan must independently
repeat the same checks before using these existing bytes, avoiding duplicate paid
image generation. This does not authorize paid work for other targets, a new batch,
Apply Selected, build/install, editing or another profile. Those belong to M6/M7
and require their own scope, review and paid confirmation after owner M5 acceptance.

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
