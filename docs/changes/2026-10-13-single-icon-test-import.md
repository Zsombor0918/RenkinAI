# Single-icon red test compilation correction

## Summary

Add the missing trialVariants extension import in the existing regression test.
Continue PR #6 from b3d5a6bfccdcc19a2320409fd9959bdf9b459ba5 with production unchanged.

## Why

Android CI38378700845 compiled production, then failed compiling the new test at
StyleTrialGatewayTest.kt:102: unresolved trialVariants. No Android test executed;
that is not red assertion proof. The test needs the existing project extension import.

## Scope / changed files

- `app/src/test/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialGatewayTest.kt`

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

One test import only; retain every new and old assertion, selector, timeout and runner.

## Behavior before / after

Production remains byte-identical to a1c2310. Kotlin can now resolve the existing
repository extension used to assert three durable trial variants. Contract assertions
remain deliberately unmet until the production correction after real red proof.

## Tests actually run

Observed hosted production compile success and unit-test compilation failure in
Android CI38378700845 on b3d5a6bf. Local prior Python52/52 and documentation/commit
guards passed. git diff --check passed for this import. Next-head hosted assertions
must actually run; the previous failed compilation is not execution evidence.

## Not tested

No Android regression assertion ran at b3d5a6bf. APK was skipped. Local Gradle
cannot download8.11.1, Network is unreachable. Device/provider/live tests remain absent.

## Risks

Only the import changes; no production or persisted-state risk. Continue to require
legacy byte-parity cases passing and focused contract cases failing for their intended
reason. Do not infer success or numeric test counts from a failed compilation.

## Rollback

Revert this import-only correction if replacing the test with equivalent coverage.
No schema or file migration applies; existing user PNGs and paid markers remain intact.

## Documentation updated

QA BASELINE and HANDOFF record the measured compile failure and the next safe step.
The earlier reproduction note remains a truthful historical pending checkpoint.

## Follow-up

Run the corrected test-only head in existing Android/all21 regression workflows,
read actual failures, then implement production. Final-head passing gates and a
matching downloadable debug APK remain required; PR stays draft/open for owner acceptance.
