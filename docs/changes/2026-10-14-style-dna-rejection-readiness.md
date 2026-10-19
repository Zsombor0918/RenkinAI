# Retained rejection journey observes initial Room readiness

## Summary

Correct the observed initial-load race in the retained legacy rejected-analysis journey without changing app behavior or any assertion.

## Why

Closing-head63e5e5be passed Android and925 unfiltered tests, but the isolated trial-ui command clicked the still-disabled prepare button before ProjectStylePanel's independent Room load completed. The existing controller-busy wait does not observe that load. The earlier successful EXTEND analysis journey already waits for its real enabled semantics.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleTrialJourneyComposeTest.kt`

Only the second retained journey receives the same bounded readiness observation; no runtime or global test-helper change.

## Behavior before / after

Materialize the prepare node and observe its actual enabled semantics within the existing10000ms readiness limit, then execute the unchanged displayed/enabled/click assertions. Preserve400 error explanation, consumed confirmation, fresh UUID, manual style Save, trial preparation and all request-count assertions. No timeout, idling rule, selector, expectation or failure condition is relaxed; no rerun is used to hide the failure.

## Tests actually run

63e5e5be Android38478557077 passed compile/unit/assemble. Complete regressions38478557064 independently show trial-ui23/1, every other21 command exit0, DNA26/26,149 unfiltered XML/925 tests/zero failures/errors/skips and genuine19/20/21 unchanged. Actual failure artifact11856632498 is644124bytes,SHA25682ec516e2af3be8bdba3fa42687adaf7b69135a0f8b7635f2f5e4e4f40abbb5b; failure is rejectedAnalysisExplainsConsumedConsentAndManualStyleStillReachesTrials: disabled prepare at line278. Local52/52 in15.775s, docs124paths/commit24subjects passed; local Android attempts fail before configuration at8.11.1 download.

## Not tested

The corrected test requires its own full final-head Android/all22/governance/XML/schema/fresh APK checks. No actual device, installed upgrade, provider model, cost, live paid call, blind visual-quality trial or credential setup was run.

## Risks

This targeted bounded observation fails if the real button never becomes enabled; it neither enables the button artificially nor skips assertions. Full-suite success does not erase isolated-group failure. Device and model quality remain unverified.

## Rollback

Revert the fixture observation only if a stronger real-readiness fixture replaces it. Preserve all runtime/history/consumed requests and signing keys. No data deletion, rebilling, main change, force push or merge is authorized.

## Documentation updated

CURRENT_STATE, HANDOFF, WORKFLOW and QA BASELINE lead with this actual failed closing checkpoint. The complete verification inventory includes both new follow-up paths and retains genuine red/green measurements rather than claiming old925 success as current proof.

## Follow-up

Run the corrected head through every mandatory original/new group and final Android/governance check. Independently inspect new evidence and deliver a newly built APK, with owner device/live acceptance separate and stacked PR7 draft/unmerged.
