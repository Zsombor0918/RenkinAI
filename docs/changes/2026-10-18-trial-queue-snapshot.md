# Tolerate concurrent removal while copying queued Trial reads

## Summary

Correct the demonstrated queue-copy exception blocking Phase3A Linux acceptance.
Keep the reference-first workflow and all existing request/readiness gates intact.

## Why

Exact b6906528 Linux CI39113733346 executes1019 cases,1018 PASS and1 failure.
ProfileDetailsDialogComposeTest reports earlier uncaught exceptions, both from
OwnedStyleTrials.start finally: ConcurrentHashMap iterator.next, Kotlin
toSingletonMap/toMap. The queue lost its last entry after the singleton size check.
All new reference-first tests and hosted Android5/5 pass at that same source.

## Scope / changed files

Behavior impact: changed
Architecture impact: none
Milestone impact: none
Durable decision: none

`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleTrials.kt`
copies queue entries without Map.toMap's singleton shortcut.
`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleTrialsTest.kt`
adds one deterministic last-entry-removal/copy-independence regression.

## Behavior before / after

The old size-then-single-entry copy can throw during concurrent cancellation or
release, escaping finally and reaching a later test. Entry iteration now tolerates
the disappearing request. Conditional remove(profile,request), route equality,
coalescing, busy and every source/original/cancel/paid guard remain unchanged.
No exception swallowing, dispatcher change, sleep, timeout increase or retry.

## Tests actually run

A standalone Java17 program invokes the actual cached Kotlin MapsKt.toMap with
a controlled ConcurrentHashMap that removes its entry before entrySet iteration.
It fails with the same NoSuchElementException/toSingletonMap stack before the fix.
Native Windows/JDK17 targeted `:app:testDebugUnitTest --tests "*OwnedStyleTrialsTest" --tests "*ProfileDetailsDialogComposeTest" --tests "*GuidedProjectRoomJourneyComposeTest"`
executes27/27 PASS, zero failures/errors/skips in2m30s including compile.
This includes the new shrinking/copy-independence guard, actual previously failing
form, reference-first persistence and existing original/cancel/replay/queue tests.
Final-head authoritative full CI and Android must run after this correction.

## Not tested

No physical phone, live provider or paid request. The CI trace proves this failure
under concurrent Default-dispatcher fixtures, not its frequency in the serialized
production Main owner or on Samsung. Earlier Windows baseline gaps are unchanged.

## Risks

Concurrent iteration is weakly consistent, as before. The existing conditional
remove and route checks still decide whether each observed read may run. A copied
queue entry cannot prepare, authorize or submit a provider request.

## Rollback

Revert this ordinary follow-up commit, retaining the captured CI failure evidence.
There is no persisted format, schema, signing or historical data change.

## Documentation updated

Current state, architecture and handoff record the actual Linux blocker and narrow
correction. Final source/evidence and audit status belong in separate draft PR9.

## Follow-up

Finish focused checks, one full Linux suite on the corrected head, hosted Android
and explicit form-order diagnostic; verify and publish its fresh debug artifact.
Remain BLOCKED until those results exist, then stop before Phase4.
