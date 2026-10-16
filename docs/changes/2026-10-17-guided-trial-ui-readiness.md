# Await prepared Trial content before the first lazy-list action

## Summary

Correct only the guided Room journey fixture's readiness boundary before its
first Prepare click. Keep the complete persistence/approval/no-rebilling journey.

## Why

Exact-head896faa15 Linux run38977561978 executes1013 cases with one failure:
decliningLocalPreparationRestoresTheUnchangedDurableTrialApprovalWithoutRebilling
fails at click:132 from journey:246. The Trial screen exists, but its list has
only11 children and zero scroll extent; Prepare is absent. The test already passed
its backend readiness predicate, then attempted scrolling before its enabled wait.
That predicate does not establish delivery through collectAsState/composition.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/GuidedProjectRoomJourneyComposeTest.kt`
changes one readiness predicate. HANDOFF and this note record reproduction and
limits. No production source, schema, signing, test group or workflow changes.

## Behavior before / after

Before, a background Room/controller completion was treated as UI readiness.
After, the same10000ms wait samples controller state once and also requires the
real trial-style semantics marker, emitted only when the Trial panel has its
preparation. The subsequent lazy scroll, visible/enabled assertions and single
click are unchanged. Missing preparation/content still times out and fails;
no sleeps, retry clicks, longer timeout or fabricated success are introduced.
All variant/history/approval/one-provider-request/decline assertions remain.

## Tests actually run

Windows isolated original method genuinely re-executes1/1 PASS; its five-method
class executes5/5 PASS. The retained Linux XML identifies CreateExtendFlowComposeTest
immediately before the failing class. That two-class selection reproduces15/1 RED
on Windows at the same first Prepare scroll, in the shared create journey method.
The failure XML is preserved outside the checkout under the task evidence directory.
After the correction the same two-class selection passes15/15 with zero failures,
errors or skips in55s. Final committed-head hosted results are recorded on PR8;
no full suite was repeated during diagnosis.

## Not tested

No live/paid provider, phone, real credential, visual-fidelity or installed-signer
test. A shared fixture failure is reproduced; the exact earlier Linux thread
interleaving is not recovered from its uninstrumented XML. No claim of repairing
the unrelated hosted package-invalidation starvation mechanism.

## Risks

Backend and Compose readiness are distinct asynchronous boundaries. The fixture
continues to fail if the screen/control never becomes ready. Final hosted full
suite and Android Standard/restart must independently pass before delivery.
Windows historical six baseline failures are not relabeled as this regression.

## Rollback

Revert this test-only correction through an ordinary reviewed branch commit.
No persisted data, frozen payload, API marker or compatibility migration exists.

## Documentation updated

HANDOFF identifies the source checkpoint, reproduced neighboring selection, narrow
correction and final-head evidence location. Earlier failure records remain intact.
ADR0012 lean verification and all original application contracts remain unchanged.

## Follow-up

Run the authoritative final-head full suite once, existing Python/governance and
Android gates. Independently verify the resulting APK and publish evidence on
existing draft PR8. Stop if a final gate fails; no new product phase is authorized.
