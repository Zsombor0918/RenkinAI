# Await the actual instrumented trial preparation result

## Summary

The new Android fixture waits for a terminal trial result rather than accepting
idle alone. Both preparation sites retain the existing15-second bound and assertions.

## Why

Hosted API35 Standard reached parsed result and Style Save, then observed busy=false,
error=null and plan=null at the required-plan assertion. Quick and incomplete/cancel
passed in that same run. The precise scheduling or pending-refresh cause is not
uniquely established; idle alone is demonstrably insufficient evidence of a plan.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

Only `app/src/androidTest/kotlin/dev/renkinProject/renkin/offline/StyleDnaEmulatorTest.kt`
changes. Documentation preserves the hosted failure and exact verification boundary.

## Behavior before / after

The helper now awaits not busy and either an actual plan or an error. The original
error-null and plan-nonnull assertions still follow; a persistently absent/lost plan
times out and fails. No larger timeout, retry, forced state, ignored assertion,
production change or provider submission is introduced.

## Tests actually run

Downloaded actual a234 hosted emulator ZIP12028547052 is3649 bytes, CRC-clean with
SHA256bd9cc51a9704ff1c3c5a2dcda2848036a95a1d2bdb460bcb7ce829921fc09f00,
matching GitHub metadata. Phase1 runs3 tests/1 failure at Standard trial plan;
phase2 runs1/1 failure because Standard evidence was absent. Quick recorded12
reports/1 local POST and successful trial; failure flow recorded2 local POSTs,
fixed token/cancel reasons and zero styles/external requests. Local clean-tree
6dda emulator independently passes4/4, which does not erase the hosted failure.
The helper correction requires its own committed-source Android/CI execution on PR8.

## Not tested

No live/paid provider, physical phone, installed-data upgrade, generated-artwork
fidelity or real account cost is tested. A passing helper cannot establish the
unique cause of the earlier intermittent idle observation.

## Risks

Awaiting a real terminal state can reveal a persistent missing-plan defect as a
timeout rather than a premature assertion. It never creates a plan or conceals
an explicit error. Original/reference fingerprints and consumed markers stay.

## Rollback

An ordinary reviewed feature-branch revert restores the earlier test wait; no
runtime or data repair is needed. Never replace the missing-plan assertion with
a skip, retry, synthesized result or relaxed timeout.

## Documentation updated

Handoff and QA baseline record the hosted Standard observation and narrower
terminal-state wait, with no claim of an identified product/phone cause. Exact
closing-source results and artifact verification remain externally on draft PR8.

## Follow-up

Run final committed-source HTTP/full matrices, local and hosted real Android,
Python/governance and APK identity/signature/digest verification. Publish only the
successful final-source Actions artifact. Keep the existing PR draft/unmerged and
preserve the owner's phone, credentials, saved data and signing keys.
