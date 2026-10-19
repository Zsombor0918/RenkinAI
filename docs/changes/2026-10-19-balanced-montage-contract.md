# Balance montage Trial cost and version genuine restyling

## Summary

Introduce future-only V4 drawing instructions and Sunburst/medium Balanced defaults,
retaining Sunburst/high as premium and all old payable request interpretation.
Supply an auditable offline cost study rather than an unverified cheaper-model claim.

## Why

Owner screenshots/feedback show improved Sunburst quality, residual container/literal
logo bias and reportedly6–7x higher cost. Actual usage receipts are absent. Official
rates and complete input/output scenarios support lowering quality provisionally;
they do not establish a measured visual winner or explain the actual bill.

## Scope / changed files

Behavior impact: changed
Architecture impact: changed
Milestone impact: none
Durable decision: recorded

`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/PromptComposer.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialPlan.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialJournal.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/OpenAiImageClient.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/IconGenerationGateway.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleTrials.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectTrialPanel.kt`
`app/src/main/res/values/strings.xml`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialPlanTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialJournalTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/PackStyleAnalysisTest.kt`
`app/src/androidTest/kotlin/dev/renkinProject/renkin/offline/StyleDnaEmulatorTest.kt`
`scripts/estimate_montage_costs.py`
`scripts/tests/test_montage_costs.py`
`scripts/run_offline_dna_emulator.py`
`scripts/tests/test_offline_dna_emulator.py`

Extend existing version/consent/HTTP/UI infrastructure; add no test group, reference
store, endpoint or generation engine. UI adds only labels to existing quality controls.

## Behavior before / after

V3 preserves distinctive geometry strongly and repeats identity/drawing constraints;
V4 makes geometry reconstruction and removal of incidental platform badges explicit.
One reviewed brief remains editable and appears once, with optional distinct user
guidance. Original-first actual montage bytes and transparent single-icon output
remain. New requests default to Sunburst/medium; existing explicit options work.
Old V1/V2/V3 plans retain their own text/model/quality and cannot be retagged/replayed.

Profile invalidation previously fails to encode a V3 zero-reference tombstone. Both
V3/V4 now retain one non-identifying sentinel, their actual version and consumed
markers while clearing images and invalidating permission. No network operation.

## Tests actually run

Native Windows focused RED reproduces unsupported future contract dispatch; the same
invocation passes the corrected density regression1/1. The initial tombstone fixture
has no reference and fails before deletion; it is not application-deletion evidence.
The corrected fixture explicitly checks the former zero-reference transformation
against immutable V3, then actual version-preserving tombstones. The Android validator
requires current V4 and retains rejection of V2 plus stale V3 acceptance receipts.
New offline pricing tests2/2 and all Python guards69/69 pass in52.518s. The nine-class
native run executes99 cases:98 pass and the no-reference fixture fails. Correcting
only that fixture gives the affected case1/1 PASS in46s; production code is unchanged.
The strict-runner V4 guard update gets a fresh Python run. Exact-head Linux/Android/
build evidence follows on PR10 after actual execution.
Tests cover unchanged V3 golden hash, plan round trips, exact native multipart,
premium/explicit options, unchanged stored1024 pixels, original-source detail/alpha,
montage recomposition, deletion, cancellation and consumed/restart protections.

## Not tested

No live/paid provider request or real model comparison. No actual owner RAW/source
resources or billing receipts; JPEG screenshots do not supply those. Native/OEM
highest packaged resolution and precise real per-request cost remain unknown.

## Risks

Medium may lose essential typography or style and need more paid attempts; Best
quality remains available before a new explicit approval. Scenario arithmetic is not
guaranteed billing. V4 needs a compatible reader on rollback. Historical91e5737d
governance and installed-signature compatibility remain independent and unwaived.

## Rollback

Use a compatible forward repair that preserves V4 readers, old artwork, journals,
original identities and consumed IDs. Never silently change already frozen content,
retry an uncertain request or delete data to make an older binary usable.

## Documentation updated

ADR0018 records the version/default and sentinel boundaries. Canonical architecture,
state/handoff/index and QA baseline/matrix link the quality/cost study, which includes
source limitations, before/after PNG evidence and assumptions for1/6/30/100 icons.

## Follow-up

Freeze the verified source, run one authoritative Linux/build plus existing hosted
Android/Python and required diagnostic checks. Independently inspect the matching APK
and publish its exact link/hash on draft PR10. The owner performs any later paid
visual comparison; no additional feature phase starts.
