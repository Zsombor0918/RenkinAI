# Component original resource regression

## Summary

Add the exact alias original test before changing the production loader. Continue PR #6 only, with baseline runtime 422ef578 unchanged.

## Why

Public activity icon APIs can return substituted artwork. A simulated themed activity icon A differs from the owning alias resource C and style artwork B, exposing this limitation without claiming Samsung device evidence.

## Scope / changed files

`app/src/test/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialPlanTest.kt`

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

Only an existing Android regression selector gains tests; every old assertion remains.

## Behavior before / after

Current production uses getActivityInfo and getActivityIcon, already independent of source B. The new test injects two distinct themed API icons and requires exact alias resource C each time. Removed aliases must still fail without fallback.

## Tests actually run

The initial style-fidelity red head is executing in hosted CI. This second checkpoint adds the resource regression before any runtime change. Local Gradle remains blocked before configuration at wrapper download; no Android pass is claimed.

## Not tested

Real Samsung overlays, work profile APIs and phone visual verification are not available. Fixtures are local Robolectric resources, never internet app artwork.

## Risks

A raw package resource is stronger source evidence but cannot certify absence of OEM resource overlays. Paid consent must expose this limit and require conscious original verification.

## Rollback

Revert this additive test if the simulated premise is invalid. Do not substitute a source-pack icon, change existing editor defaults or erase paid trial records.

## Documentation updated

This record supplements the earlier test-first note with precise original-resource behavior and limits. The final production decision requires canonical documentation and an ADR.

## Follow-up

Observe hosted failing assertions before the loader improvement. Retain current owner/selection guards, original bytes/hash/stamp freezing, legacy approvals and all existing selectors.
