# PR8 retained-controller panel recreation assertion

## Summary

Strengthen the existing real Room guided scan journey before any runtime edits.
An Activity-like composition restoration must preserve an unsubmitted frozen plan.

## Why

ProjectStyleAnalysisPanel calls releasePreview on composition disposal. The owned
implementation clears dnaPlan and samples, even though the retained controller
survives. The old journey scanned again after restoration without checking identity.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/GuidedProjectRoomJourneyComposeTest.kt`

Retain all old readiness, source stamp, selection, no-key, zero-call and re-scan
assertions. Add an exact plan equality assertion between restoration and re-scan.

## Behavior before / after

No production changes. The stronger existing selector now observes the actual
controller immediately after parent-panel disposal/restoration, before hiding
lost preparation behind another free scan. Physical Activity testing stays separate.

## Tests actually run

Local Python governance55/55 passed at the first reproduction checkpoint.
Local Gradle is blocked downloading its distribution before configuration.
Actual hosted Android RED for this additional assertion is required next.

## Not tested

Physical Samsung configuration changes, Settings navigation and actual process
death remain NOT RUN. Cold process recovery must never recreate paid consent.

## Risks

An unsubmitted in-memory plan is distinct from consumed journal history. Retaining
it may not bypass fresh context/source/hash checks or authorize any HTTP request.
No key, private provider prose or owner inventory enters this test record.

## Rollback

Revert only this added assertion on the topic branch if needed; preserve the
existing guided regression and all production data, rights and signing keys.

## Documentation updated

This new record identifies the specific missed recreation boundary and preserves
the initial diagnostic reproduction note. No milestone or architecture change yet.

## Follow-up

Observe hosted assertion failure before the smallest justified ownership correction.
Then run final-head Android/all23/governance and publish the exact verified APK on
draft PR8. No live requests, main/PR6/PR7 writes, merge or force push.
