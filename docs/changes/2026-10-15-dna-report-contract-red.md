# DNA report contract reproduction

## Summary

Preserve two failing regressions before correcting production on existing PR8.

## Why

The supplied screenshots show Standard stopped with twelve accepted reports and three submission markers. They do not contain the rejected response.

## Scope / changed files

- `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaGatewayTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaWorkflowTest.kt`

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

## Behavior before / after

Runtime remains unchanged. Tests demand a frozen-count/binding request schema and a specific third-batch rejection without losing prior reports or consumed rights.

## Tests actually run

Windows portable JDK17/SDK36/Gradle8.11.1: reliability-a-red runs the two new methods, compiles production/tests, executes two tests with two assertion failures and zero errors/skips in22.920s. Schema lacks minItems; journal returns generic REPORT_SCHEMA_INVALID instead of REPORT_COUNT. Evidence remains outside Git in task tools2026-10-15/reliability-a-red.

## Not tested

No final suite, device or live provider execution occurred at this test-only checkpoint.

## Risks

The phone's exact report defect is unknown. No raw response, key, prompt or image content is recorded in diagnostics.

## Rollback

Revert this test-only checkpoint through a feature-branch commit; preserve all existing assertions and history.

## Documentation updated

This scoped note records actual red evidence. Small two-file Android-test exemption applies; canonical runtime facts remain unchanged.

## Follow-up

Align the outbound contract without weakening client validation, add fixed report categories, then verify focused security/durability and final-head gates.
