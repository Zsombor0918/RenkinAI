# Style DNA principal-color measurement correction

## Summary

Correct the new family-level palette reading for genuine duotone artwork while retaining every golden expectation.

## Why

Executed Android925/1 shows white/cyan paint classified as FIXED_PALETTE: downsampling interpolates opaque boundary mixtures into recurring extra swatches. DNA must distinguish principal paint from edge mixtures before allowing intentional recoloring.

## Scope / changed files

Behavior impact: changed
Architecture impact: changed
Milestone impact: none
Durable decision: none

`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaSynthesis.kt`

The bounded additional measurement applies to new per-family DNA only. Existing reference geometry, legacy aggregate policy and null-override AI1/AI2 wire bytes remain unchanged.

## Behavior before / after

Measure stable opaque interiors on the256px thumbnail, requiring locally similar opaque neighbors. Aggregate principal colors with the existing Oklab classifier, retaining the old geometry and falling back for insufficient interior evidence. Recycle the one temporary bitmap. Keep all925 tests and every prior selector, assertion and timeout unchanged.

## Tests actually run

Head4531da8a CI38476057333 compiled production/test Kotlin and ran925 tests with one new duotone golden failure,924 passes. Downloaded failure ZIP11853479616,213996 bytes, SHA256 f3bf8d5964db87a4e273c39558a55b49e55cdbdb4689b536a5e4d0e88b6fa206. Local Python52/52 in15.178s and origin/main-range docs122paths/commit22subjects passed at that head. Original test-first b2df166a904/6 remains valid independent red evidence.

## Not tested

This corrected Kotlin must execute its own mandatory final-head Android/all22 regressions, XML, governance and APK checks. No live model, pricing, actual Lux resources, physical Samsung, accessibility or installed-data update test was run.

## Risks

Stable-interior selection is conservative and loses thin/soft pixels; insufficient samples fall back. It does not identify pigment perfectly in intentional gradients. Synthetic fixture accuracy is not blind visual fidelity or calibrated confidence. Consent and durable no-replay paths are unchanged.

## Rollback

Use a forward correction retaining compatible tagged payload readers and consumed plan/step IDs. Keep immutable styles and historical originals/RAW. No data deletion, merge, force push or paid replay is part of rollback.

## Documentation updated

ARCHITECTURE, CURRENT_STATE, HANDOFF and QA BASELINE record the genuinely observed failure and the new bounded family-only measurement; historical successful counts are not substituted for corrected execution.

## Follow-up

Complete corrected Android/all22/governance checks, independently inspect JUnit/schema evidence and deliver a fresh debug APK. Keep stacked PR7 draft and leave owner device/live visual acceptance separate.
