# DNA resume verification fixture follow-up

## Summary

Retain every current runtime correction and every existing assertion. Refine only
the new key-resume fixture to use the lifecycle actually supplied to the screen.

## Why

Implementation4be6ad63 compiles and executes974 tests with one new fixture timeout
at the first synthetic resume wait. The other973 cases, including the original
three red cases, real Quick/Standard wire/Room Save, concurrent request progress,
auth/schema/timeout and cancellation guards, pass. This does not establish whether
the timeout was fixture setup or a product defect; no platform success is inferred.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/StyleDnaComposeTest.kt`

Only the new fixture changes. No runtime, key storage, gateway, source, journal,
schema, signing, dependency, selector or approval behavior changes.

## Behavior before / after

The fixture previously injected a separate LifecycleRegistry and changed pause
and resume in one UI action. It now obtains LocalLifecycleOwner from the actual
composed screen, asserts its observers and states, and dispatches pause/resume
in separate observed UI actions. The10000ms wait, all key add/replace/remove,
disabled/enabled paid gate, exact frozen-plan identity, preparation count and zero
submission/Save assertions remain unchanged. No idling/timeout bypass is added.

## Tests actually run

[Android38708500788](https://github.com/Zsombor0918/RenkinAI/actions/runs/38708500788)
at4be6ad63 compiles production/tests, then runs974/1. Downloaded223270-byte failure
report11959118399 has SHA256
e4ab841572b85ff19542c253ac741e3280a8089efc7bfe1ce181831f6a318c44.
The sole failure is settingsKeyChangesRefreshOnResumeWithoutRepreparingOrSubmitting
at its first resume condition. Revised fixture execution and final full gates are
required before claiming key refresh verified. Previous red and failure reports
remain evidence; the test is neither removed nor reclassified as a passing test.

## Not tested

Physical phone/Keystore/account/signature acceptance and live provider use remain
NOT RUN. No secure Work credential or verified spend controls are available.

## Risks

A passing initial key read does not prove resume refresh. The refined fixture
must exercise real observed lifecycle events and retain the exact saved plan,
with no hidden network submission. A further failure stays blocking.

## Rollback

Revert only this fixture change on the feature branch if needed. Keep all runtime
fixes, historical failures, request rights, data and signing keys intact.

## Documentation updated

This follow-up and QA baseline retain the actual failed checkpoint and the narrower
verification boundary. The main diagnosis remains in the plan-refresh record.

## Follow-up

Run the retained full matrix on the final source, inspect actual XML and fresh APK,
and update PR8 only with observed evidence. Keep the PR draft/open and its audit
BLOCKED until mandatory gates complete; no live call or merge is performed.
