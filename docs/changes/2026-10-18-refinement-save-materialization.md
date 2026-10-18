# Synchronize retained style Save fixtures

## Summary

Repair only the retained Trial refinement test after the Phase0 manual form
became longer. Production source remains exactly the8f33ce56 checkpoint.

## Why

Hosted Linux39133836212 executes921 tests with one failure: the Save semantics
node does not exist until its LazyColumn item is composed. Direct performScrollTo
cannot find an item that is not materialized. The real production control exists. Hosted Android39133836529 also exposes a test read before asynchronous Save completion: references0 at the immediate read, then durable revision1/references2 in trace after style.parent.saved. Await the real target-screen navigation emitted after Save, within the existing15 seconds.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/StyleTrialComposeTest.kt`
`app/src/androidTest/kotlin/dev/renkinProject/renkin/offline/StyleDnaEmulatorTest.kt`

Scroll the existing parent container to its retained Save tag, then require the
actual node to be displayed and enabled before one click. Keep all paid-plan,
history, refinement and external-editor assertions. No new case or selector.

## Behavior before / after

Only fixture interactions change. Android now observes the actual post-Save route before querying Room, preserving exact reference/count/hash assertions. It materializes a real lazy item instead
of assuming all items already have semantics nodes. No retry, sleep, enlarged
timeout, production readiness bypass or hidden failure.

## Tests actually run

The first hosted Linux RED is921 cases,920 PASS/1 FAIL with its exact XML and
source8f33ce56 retained. Focused StyleTrialComposeTest15/15 PASS in44s. Initial Android2/2 write journeys fail at references0 before Save completion; dependent restart lacks evidence. Its trace later confirms revision1/references2, not data loss. Corrected dirty-source local API35 emulator-5580 executes3/3 PASS (2 write/preparation plus1 separate-process restart); no provider submission. Corrected committed-head
full Linux/build/Android results are recorded in the external PR audit after
execution. Earlier counts are not reused as final evidence.

## Not tested

No live provider, key or physical phone test. Montage Phases1–3 remain absent.
Visual quality and installed signing compatibility remain unproven. A local
Windows symlink baseline remains separate from the Linux refinement fixture.

## Risks

The fixture must continue to find the real visible/enabled Save action. The
historical merge-title governance failure is still BLOCKED independently; no
rule or commit history is changed to conceal it.

## Rollback

A normal reviewed revert affects this fixture only. Preserve all old/new styles,
frozen requests, consumed markers and user data; no migration is involved.

## Documentation updated

Inventory and QA matrix now use actual Linux XML:112 removed/replaced old case
identities plus7 replacements, net105 fewer than1026, giving921. Current state,
handoff and baseline distinguish the retained RED and pending corrected results.
The initial record's regex estimate was one case too high, not an executed count.

## Follow-up

Run the focused class, commit this test-only correction, verify the final-head
mandatory gates and publish honest checkpoint evidence on existing draft PR10.
Keep the historical governance blocker visible and do not start montage features
until this independent cleanup checkpoint is safely handed off.
