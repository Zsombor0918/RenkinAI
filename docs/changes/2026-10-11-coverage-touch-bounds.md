# M2 — preserve the minimum touch target assertion

## Summary

Correct the new Compose test DpRect measurement to bottom minus top, retaining the
48dp touch target assertion and every interaction. Production behavior is unchanged.

## Why

Android run 38159074131 compiled production but failed test compilation at line 208:
DpRect has no height property in the resolved Compose API. The existing Home test
already uses bottom minus top; no timeout or test removal is involved.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/CreateExtendFlowComposeTest.kt`

Only one test measurement expression changes; canonical evidence and architecture
baseline text also record the actual owner-merged main and failed checkpoint.

## Behavior before / after

Before test sources did not compile; after the expression uses the supported DpRect
coordinates. The assertion still requires at least 48dp. No application code changes.

## Tests actually run

Initial hosted Android 38159074131 passed compileDebugKotlin then failed unit-test
compilation, with no executed tests and skipped APK. Existing local 45 guards and
staged/range checks passed. The correction requires a fresh hosted test execution.

## Not tested

No Android test or APK has yet run successfully for M2. Local Gradle download is
network-blocked; physical device and live AI trials remain NOT RUN.

## Risks

This compile correction does not certify layout on a device. The full unit suite,
original isolated regressions and new UI/domain groups must all execute afterward.

## Rollback

Revert this commit to restore the prior expression, which would restore the observed
compile failure. No runtime state, schema, signing or profile data changes exist.

## Documentation updated

BASELINE and HANDOFF retain the actual failed checkpoint without rewriting it as
success. ARCHITECTURE now identifies the fresh M2 main baseline rather than old M1.

## Follow-up

Await fresh compile/unit/APK and exact-head regression evidence; keep PR #3 BLOCKED
until mandatory automation passes, then seek separate owner acceptance without merge.
