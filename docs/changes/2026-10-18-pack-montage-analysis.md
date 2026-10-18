# Analyze saved montage images with one explicit request

## Summary

Implement Phase2 analysis operation from local checkpointf8d28668. Reuse the
existing journal/owner/storage boundary; guided UI and TrialV3 wiring follow.

## Why

The pack needs one practical reusable drawing prompt from its actual saved
contact sheets, without per-icon reports/families/synthesis or mandatory extra
style references. One small result plus durable billing safety is sufficient.

## Scope / changed files

Behavior impact: changed
Architecture impact: changed
Milestone impact: changed
Durable decision: recorded

`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/PackStyleGateway.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisGateway.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisJournal.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/data/project/ProjectAiRepository.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/LocalPackMontages.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/PackStyleAnalysis.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/PackStyleAnalysisTest.kt`

No Room/schema, signing, image model/provider configuration, AI1/AI2 or batch change.

## Behavior before / after

New explicit analysis operation uses exact saved/staged montage PNGs, frozen task
instructions and optional quoted preferences. One high-detail Responses POST,
store=false/stream=false/reasoning-none,4096-token cap, two string result fields.
RequestBody streams one encoded image at a time. Existing journal contract1 bytes
and fingerprints are unchanged; optional contract2 binds high detail, images and
frozen instructions. No key read/POST before confirmation; uncertain/cancelled
requests stay consumed. Validate source/owner/revision/epoch before submission
and Save. Received result may retry only local Save, never the provider call.
Local montage source feature work now runs off main while retaining owner locking.

## Tests actually run

Windows/JDK17: production Kotlin compile PASS. Focused5 classes33/33 PASS in2m12s:
new PackStyleAnalysisTest4 and old journal9/schema8/persistence9+3. Actual loopback
HTTP verifies model/native inputs, exact base64 sheet order, output schema,
headers and marker before network. False consent and preparation write/send
nothing. Incomplete/503 fail safely; cancel/stale source do not replay. A received
result survives owner unavailability and recovers local Save with one POST total.
Old journal/grammar/full/shared backup persistence cases still pass together.

## Not tested

No live model, real credential, paid generation or physical-device action.
Offline useful text is a deterministic fixture, not visual-quality evidence.
Guided panel, new Trial request and final-head hosted gates remain next work.

## Risks

The new plain result is not a quality score. Incomplete/error outcomes may still
have consumed credits and require fresh explicit consent; no automatic retry.
Source changes leave received candidates in private history without treating
them as current. Preserve both journal versions/readers on rollback. Existing
historical governance-title failure is not changed by this feature.

## Rollback

Use normal reviewed reverts/forward repair while retaining contract2 readers and
consumed IDs for any stored records. Keep montage revisions, evidence and files;
never reset data, restage uncertainty or rewrite a consumed request identity.

## Documentation updated

ADR0016 records the small result and versioned journal decision with official
model/vision sources. Architecture/state/roadmap/handoff and QA describe the
one-shot operation, actual focused evidence and still-unexposed integration.

## Follow-up

Complete Phase3 guided montage UX, durable varied target recommendations,
versioned original+montage generation and safe Back navigation. Reuse the existing
Android harness, then one authoritative final-head Linux/build plus required
Python/governance/order/device gates; no paid visual trial is authorized.
