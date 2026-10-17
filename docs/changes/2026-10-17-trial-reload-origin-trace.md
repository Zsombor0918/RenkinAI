# Observe the shared Trial reload initiator

## Summary

Add optional fixed-code transition instrumentation to distinguish effect changes,
composition re-entry, lifecycle resume, package invalidation and explicit reload.

## Why

Retained ebc5ef95 hosted traces show the prepared plan survives but repeated loads
leave sub-millisecond idle windows. The originating event is not recorded, so a
controller or fixture correction would currently be speculative.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectSetup.kt`,
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectTrialPanel.kt` and
`app/src/androidTest/kotlin/dev/renkinProject/renkin/offline/StyleDnaEmulatorTest.kt`
add the optional sink and synthetic installation. CURRENT_STATE, HANDOFF and
OFFLINE_STYLE_DNA explain its limits. No controller, journal or schema changes.

## Behavior before / after

The existing invalidation/effect/release callbacks keep identical keys and order.
A null-by-default CompositionLocal emits nothing in ordinary app use. Only the
instrumented Trial subtree supplies a stable sink. Trace changed key names, not
values; package events emit fixed action/source-match codes without private names.
Existing trace provides monotonic sequence/thread/plan/busy/save/load boundaries.
Explicit reload and decline are labeled separately. No timer or observer retry.

## Tests actually run

Phase1 Python guards pass67/67 in52.659s. Android instrumentation compilation is
checked locally; one exact-head hosted diagnostic will provide runtime evidence
on PR8. No production fix is claimed from source inspection or compile success.

## Not tested

No live or paid requests, phone, real credentials, visual-quality comparison or
installed-signer compatibility. Hosted Standard is not declared repaired.

## Risks

Logging and the synthetic fixture can perturb scheduling. A passing diagnostic
alone cannot prove a cause or correction. Existing StateFlow conflation limits
negative inferences; retain failure logs and do not increase timeouts or retry.

## Rollback

Remove the instrumentation through an ordinary reviewed commit without altering
controller behavior, persistence, consumed markers or any retained assertion.

## Documentation updated

Canonical state/handoff and offline trace instructions distinguish the new origin
observations from proven defects. Final exact-head run results belong on PR8.

## Follow-up

One hosted run is authorized. If an initiating cause is proven, add deterministic
RED then a minimal correction and GREEN; otherwise stop this phase BLOCKED.
Reference-first UX and concise versioned prompts remain separate later work.
