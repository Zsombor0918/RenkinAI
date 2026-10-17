# Observe the resume-triggered Compose effect before reading the key state

## Summary

Synchronize the new lifecycle fixture through ordinary Compose idling, retaining
all original acceptance assertions, event checks and the10000ms result condition.

## Why

Diagnostic350fa843 in Android38710514093 executes974/1. Actual failure text records
resume events1→2, direct observer invalidation1→2, panel refresh2→2, key presence
false despite expected true. Thus lifecycle dispatch and the existing observer
work; the missing observed boundary is the panel's recomposition/refresh effect.
This still does not by itself distinguish a fixture scheduling issue from a
product defect. The earlier registry-replacement hypothesis did not resolve it.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/StyleDnaComposeTest.kt`

No runtime, provider, journal, storage, schema, signing or permission change.

## Behavior before / after

The fixture now waits for ordinary Compose idle before capturing callback counts
and after dispatching resume, matching the retained M2 observer test and its
documented lifecycle synchronization. It explicitly asserts one real resume and
one observer invalidation, then retains the original refresh/key condition and
all exact plan, enabled/disabled, preparation-count and zero-submission assertions.
No clock setting, timeout, idling rule, selector or assertion is weakened.

## Tests actually run

Android38710514093 at350fa843 compiles and executes974/1. Downloaded diagnostic
artifact11960816172 is223311bytes, SHA256
8e602d447c027322d85eae19ca27fe575996d0a133df1bce7d162fabb483eaa2.
Its actual counters above establish the next boundary. This follow-up needs its
own complete execution; no pass or product-only cause is claimed in advance.

## Not tested

Physical Settings/Keystore/Flip8/signature/account behavior and live provider use
remain NOT RUN. Key-resume acceptance stays blocked until the assertion passes.

## Risks

The test must observe the actual refresh, not merely an event or a previously
true key-presence value. Ordinary waitForIdle is synchronization, not permission
to skip assertions, extend timeouts or force the result state.

## Rollback

Revert only fixture synchronization if needed while retaining all runtime fixes,
historical failures, data, consumed request evidence and signing keys.

## Documentation updated

This record retains the measured event/invalidation/refresh counters and relates
the follow-up to [existing lifecycle evidence](2026-10-11-coverage-lifecycle-evidence.md).
Earlier failed attempts remain visible rather than being relabeled as passes.

## Follow-up

Inspect the next actual assertions, finish final-head mandatory tests/governance
and a fresh verified APK, then update PR8. Keep it draft/open and BLOCKED until
automated gates finish; no live request, main write, force-push or merge occurs.
