# PR8 provider-result diagnosis and retained frozen plans

## Summary

Continue only draft PR8 on feat/ux-flow-stabilization from1bf21f0e. Correct
retained-controller recreation losing a ready plan, ambiguous no-plan previews,
and historical/processing errors being presented as one provider-result failure.

## Why

Full specification and all three screenshots were read before runtime edits:
667.jpg (16:57, Quick12),665.jpg (16:56, Standard24),658(1).jpg (10:14, earlier
preparing state). Newer text comes from plan==null plus retained FAILED/UNCERTAIN
history. It does not establish a new POST, response or charge. The exact old phone
response and reason its plan is absent remain unknown.

The earlier key-read merge race is already fixed at the starting head. A separate
actual defect remains: panel disposal invokes releasePreview, clearing a ready
plan in the retained VM. The real guided restoration journey previously scanned
again before checking identity. New assertions observe expected plan/actual null.
Separately, incomplete responses map to INVALID_RESPONSE; report parsing, local
synthesis and interruption all map to INVALID_OR_INTERRUPTED. Valid reports can
exceed the eight-family synthesis bound. That case remains rejected but cannot
truthfully be called malformed provider output. It is not proved on the phone.

## Scope / changed files

Behavior impact: changed
Architecture impact: none
Milestone impact: none
Durable decision: none

- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/OpenAiStyleDnaGateway.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaCoordinator.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleAnalysis.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleDnaPanel.kt`
- `app/src/main/res/values/ux_style_strings.xml`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaGatewayTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleAnalysisTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/StyleDnaComposeTest.kt`

Prior test-only notes retain the guided identity assertion and HTTP report/synthesis
matrix. All selectors/assertions/timeouts/idling/security/schema bounds remain.
Preserve legacy AI1/AI2/Stability bytes, original IMAGE1 identity, style-only pack
refs, editor/manual/imported icons, immutable bases/modifiers, profiles/preferences,
styles/trials/approvals/history, Room19/20/21, backup/restore, watch/provenance,
exports, APK/Theme Park and both signing paths. No model/body, schema/migration,
fingerprint, journal format, signing/build/dependency, sampler or roadmap change.

## Behavior before / after

Only idle unsubmitted DNA plans survive preview disposal with exact bounded bytes
in the existing VM. Busy local preparation still cancels; submitted work retains
its journal/no-replay rules. Preparation replaces the plan, submission consumes
it, and VM/process loss does not restore consent. Fresh owner/context/source/epoch/
hash checks remain before every durable marker/transport. No new persistence.

No-plan images say preview samples. Failure prose/reason stays under Details,
including while a new plan is ready. Historic INVALID_RESPONSE/INVALID_OR_INTERRUPTED
say the exact cause was not recorded; no old reason/consumption bytes are rewritten.
Key history allows unavailable/decryption as well as rejection: hasKey checks file
presence, not credential validity; actual key reading stays at explicit submission.

| Safe new reason | Proven boundary, with all original rejection rules intact |
| --- | --- |
| RESPONSE_INCOMPLETE | Envelope explicitly reports incomplete; partial style is not parsed |
| RESPONSE_FAILED | Envelope reports failed; raw error prose is discarded |
| ENVELOPE_INVALID | Envelope/status/output invalid; unexpected tools remain rejected |
| REPORT_SCHEMA_INVALID | Exact per-image schema/count/IDs/hashes/types/extra fields fail |
| SYNTHESIS_INVALID | Quick local synthesis or Standard grammar/evidence validation fails |
| INVALID_OR_INTERRUPTED | Remaining persistence/interruption uncertainty; old meaning is not narrowed retroactively |

Incomplete/failed envelopes conservatively keep billing uncertainty. Existing HTTP
400/402/422,401/403,429 and5xx/transport mappings remain. FAILED never means free.

| State/event for one project | Current plan/samples/key | Transport/history/Save |
| --- | --- | --- |
| Cold screen entry with old FAILED/UNCERTAIN | No new plan; local key-presence refresh | Old history under Details, no claim/marker/HTTP |
| Quick free freeze | Up to12 exact bindings/model/detail/draft/source/revision | No journal claim/HTTP; one future endpoint listed |
| Standard free freeze | Up to24, six-image groups plus text synthesis | No HTTP; maximum five listed consent-bound endpoints |
| New freeze with old history | Current matching plan has ready/no-key status | Old failure cannot cover current readiness/progress |
| Settings key add/replace/remove; resume | Refresh preserves exact plan; removal disables review | Presence is not decryption/auth; zero implicit HTTP |
| Open/decline paid review | Same ID/fingerprint/images, no consumed consent | Zero claim/marker/HTTP; review is not authorization |
| Explicit valid confirmation | Executable plan removed; exact images captured | Claim consumes IDs; durable beforeSubmit precedes enqueue; marker alone is not proof of HTTP/billing |
| Response/validation failure | No executable plan; images may remain previews | Safe reason/partial reports retained; no RESULT_SAVED/revision/retry |
| Validated Quick/Standard result | RESULT_SAVED review candidate, no plan | Loopback proves HTTP and completed steps; no implicit Save |
| Explicit review/Edit/Save | Immutable reviewed revision created | Real Room reload/history; zero generation/Apply/provider request |
| Scroll/recomposition/retained-VM recreation | Ready unsubmitted plan/images retained, no stale busy/network | Same exact plan, zero new consent/HTTP |
| Reopen/cold new VM | Retained VM may keep a plan; a new VM does not invent one | Candidate/history local reload; consumed steps never replay |
| Changed mode/model/detail/source/draft/base/project/selection | UI matching plus fresh owner/source/metadata/selection/catalog/epoch guards | Stale submit cannot reach marker/HTTP; no auto-reprepare/resend |

Reviewed sources: both panels, owned controller/backend, lifecycle invalidation,
AiCredentialStore, plan/fingerprint/image validation, production gateway, shared
bounded UTF-8 transport, report codec/V1 schema, local/evidence synthesis,
coordinator, journal and real Room tests. Valid metadata/output order/split text
already works; tests verify it instead of speculative envelope relaxation.

Official [model](https://developers.openai.com/api/docs/models/gpt-5.4-mini) and
[structured-output](https://developers.openai.com/api/docs/guides/structured-outputs)
docs were checked October15: alias/snapshot supports Responses/image input/strict
output; incomplete/max_output_tokens and refusal handling are documented. Existing
reasoning none, strict text.format/store:false,low/high,12288 output tokens,
5MiB request/128KiB response, one-shot/no-redirect/no-retry remain. Caps do not
prove account availability, pricing or the phone's actual request.

## Tests actually run

Test-only61e7b166 precedes runtime changes. [Android38761014578](https://github.com/Zsombor0918/RenkinAI/actions/runs/38761014578)
compiles/runs978 tests with exactly four new assertion failures. Downloaded actual
HTML confirms incomplete versus INVALID_RESPONSE, malformed-envelope code,
report-schema versus INVALID_OR_INTERRUPTED and preview-only UI. Artifact11985886299
is225955bytes, SHA2567b66da1d42c222416406be006a7b6bd269eadd7aaf167d6b38fe24e2e20ea67f.

Test-only2207f82d precedes preview-lifetime edits. [Android38761504726](https://github.com/Zsombor0918/RenkinAI/actions/runs/38761504726)
compiles/runs978/5: previous four plus actual expected frozen plan/actual null after
real parent restoration, before re-scan. Downloaded artifact11985728802 is229111bytes,
SHA25680078f95930cdd9c33e6517e248b2a7b3c4f3d195dbf144bf0a5a72a8a5791c8.
Original RED assertions remain. The retained controller schema assertion changes
only to a more specific exact reason, preserving all status/count/privacy guards.

Local Linux/OpenJDK17 Gradle fails downloading8.11.1 before configuration: Network
is unreachable; zero local Android assertions. Python55/55 passed8.695s at first
test checkpoint. Exact final-head Android/all23/unfiltered XML/schema, Python/docs/
subjects/full PR audit and APK/signature evidence are recorded externally on PR8
after execution; checkpoint/RED counts do not substitute for final passes.

Deterministic production gateway/coordinator/journal/controller/Room/Compose tests
use fake ports or loopback MockWebServer. Existing Quick12/Standard24/five-step,
exact labels/hashes/images, marker-before-headers, explicit immutable Save/reload,
key lifecycle and no replay remain. New faults independently cover incomplete/
failed envelope, metadata/output order/split text, HTTP400/401/402/403/422/429/
500/503, refusal, malformed/missing/wrong/extra/duplicate/type reports, schema-valid
excessive family diversity, Standard grammar/evidence, UTF-8/oversize/truncated
body, unavailable key and pre-marker cancellation. Retained after-marker cancel,
interrupted/cold recovery/source/epoch safeguards remain. No secrets/raw response
or private exception prose is persisted in diagnostic records.

## Not tested

LIVE TEST NOT RUN: zero live/paid calls authorized or made. Phone key is inaccessible
in Work and was not requested/copied/exposed. No spend claim for historical phone
work. Physical Flip8/One UI resources, Keystore, actual Activity/process kill,
TalkBack/fold, visual quality and installed-signer compatibility remain NOT RUN.

## Risks

Exact original phone event sequence cannot be reconstructed from images. The
matched recreation defect and diagnostic ambiguity are proved offline; phone
retest remains required. Unsubmitted data retains the existing24-image/8MiB bound
per plan in the current VM, without durable consent/background work. CI debug
signing may differ from the installed app; retain keys/data and never uninstall
or clear app storage to work around a mismatched certificate.

## Rollback

Use compatible topic-branch revert/forward repair preserving Room21/tagged readers,
immutable style/target/trial/original/RAW histories, consumed rights and both signing
identities. No destructive downgrade, data reset or paid replay.

## Documentation updated

INDEX/state/architecture/handoff/QA/checklist describe current versus historical
evidence, retained-VM plans and safe stages. Earlier failures/diagnoses remain.
No milestone/ADR decision, new database/coordinator or credential channel.

## Follow-up

Complete final-head checks, inspect actual XML/schema/APK, then put its durable
GitHub artifact URL and SHA256 directly on PR8. PASS WITH RISKS only after mandatory
automated gates pass. Keep draft/open/unmerged for compatible-signing/free-scan/
Settings/decline/review/Save owner acceptance. Later live Quick is an independent
owner decision. No main/PR6/PR7 write, merge, force-push or release.
