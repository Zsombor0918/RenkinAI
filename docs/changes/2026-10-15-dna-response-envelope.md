# PR8 Responses envelope compatibility and diagnostic boundary

## Summary

Continue existing draft PR8 from verified clean Windows worktree
86c95e9d9241d3552022a8ed709553601a046605. Scope is the DNA outer REST envelope,
its realistic offline fixtures and bounded failure diagnostics, not another UX change.

## Why

The owner's new specification records Galaxy Z Flip8 Standard DNA failure at
18:55–18:56 Budapest time: FAILED: ENVELOPE_INVALID,0/24 analyzed,6 images submitted,
1/5 endpoint submissions. This establishes a retained first-step failure after
successful local sampling, consistent with a decoded2xx reaching the outer parser.
SUBMITTED is a pre-network marker, not proof of completion or billing. The reported
10 platform requests/23143 input tokens do not identify this plan's exact charges.
The three new screenshots are not mounted in Downloads or the provided attachment
list in this Windows session; their quoted observations come from the specification.
The raw phone response was not retained with store:false and is not available here.

Both Legacy and DNA parse the whole response at depth12. The real DNA report schema
under returned text.format.schema reaches depth14, whereas Legacy's actual schema
fits12. New full REST fixtures include the exact outbound schema, independently
bound six-image reports, reasoning and ordinary metadata. They do not pretend to
be a captured phone response. Official sources fetched October15:
[Responses reference](https://developers.openai.com/api/reference/typescript/resources/responses),
[structured outputs](https://developers.openai.com/api/docs/guides/structured-outputs),
[unchanged model/snapshot](https://developers.openai.com/api/docs/models/gpt-5.4-mini).

## Scope / changed files

Behavior impact: changed
Architecture impact: none
Milestone impact: none
Durable decision: recorded

- `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaGatewayTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/data/project/StyleDnaPersistenceTest.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/OpenAiStyleDnaGateway.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StrictJson.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleDnaPanel.kt`

Preserve all source/style/trial/editor/Legacy/AI1/AI2/Stability behavior, original
identity, immutable history, consumed markers, schemas19/20/21, backup formats,
watch/exports, app/pack IDs and both signing-key paths. No live call or retry.

## Behavior before / after

The test-first full envelope is genuinely rejected before valid reports are decoded.
Only root text.format.schema now has depth14; all other paths/default callers keep12.
The reader tracks exact object keys and cannot activate the exception through an
array or another parent. It still validates every ignored schema value, duplicate
key, number, surrogate, array bound and whole response byte bound. Output text is
independently parsed with the unchanged report/grammar/evidence validators.

Future decoded2xx failures use only fixed ENVELOPE_JSON_INVALID/JSON_SIZE/JSON_DEPTH/
JSON_DUPLICATE, ROOT_STATUS/ROOT_ERROR, OUTPUT/OUTPUT_ITEM, MESSAGE_STATUS, CONTENT,
TEXT_EMPTY or RESPONSE_ID codes (all with ENVELOPE_ prefix). Incomplete/failed/refused,
HTTP and transport behavior remain. Blank output is rejected at the envelope boundary
instead of later report parsing. Historic ENVELOPE_INVALID is unchanged. UI uses the
same envelope remediation and displays the retained fixed reason. No raw content,
new persisted field or automatic retry exists. The unique phone trigger is unproven.

## Tests actually run

The existing Windows portable JDK17/SDK36/Gradle8.11.1 and caches are reused.
Actual test-first production/test compilation passes, then5 tests/2 failures/zero
errors/skips in239.106s: first six-image fixture fails with ENVELOPE_INVALID after
its depth14/Legacy checks pass; real Room fails expected RESULT_SAVED/actual FAILED.
Three other persistence tests pass. Red assertions are unchanged. Corrected focused
gateway/Room/strict-Legacy/Compose verification (envelope-green-1) executes44/44,
zero failures/errors/skips in1624.412s on this Windows host. Exact-class selectors
are StyleDnaGatewayTest, StyleDnaPersistenceTest, StyleAnalysisSchemaTest,
StyleAnalysisGatewayTest and StyleDnaComposeTest; original command/XML/log are
retained outside Git. Full-schema Quick and all Standard steps now Save/reload;
17 independent bad-envelope fixtures, refused/oversize cases and five invalid-report
variants still fail safely with exactly the expected submission/consumed counts.
final committed-head all23/unfiltered/governance/schema/APK evidence belongs in
PR8 after execution, never inferred from historical success or a subset.

## Not tested

No phone is connected. New physical Galaxy Z Flip8, installed signer/data upgrade
and real provider envelope/quality/billing verification remain NOT RUN. The pasted
credential is not used, written into fixtures, logged or committed; no live paid
request, key lookup or automatic retry is authorized or performed.

## Risks

A realistic supported envelope can prove a parser incompatibility without uniquely
recovering the old device exception. Preserve byte/depth/array/duplicate/surrogate
and exact report/evidence/security bounds. Future fixed categorical diagnostics
must never retain provider prose, images, prompts, credentials or private paths.
The 18:55 failed record and all submission markers remain unchanged and consumed.

## Rollback

Use a reviewed compatible feature-branch revert or forward correction. Keep Room21,
tagged readers, originals/history, consumed IDs and both keys; no data reset,
destructive downgrade or request replay is required.

## Documentation updated

This dated diagnosis records the owner's new evidence, source comparison and
test-first boundary. Canonical state/architecture/handoff, QA/checklist and ADR0008
will describe any measured scoped exception and exact remaining device uncertainty.

## Follow-up

Finish focused red/green, mandatory final-head local/hosted checks and a fresh debug
APK. Publish a verified final-run artifact ZIP link and APK SHA256 directly on PR8.
Keep the PR draft/open/unmerged; owner-device confirmation is a separate later gate.
