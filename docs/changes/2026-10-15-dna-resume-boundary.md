# DNA resume event-boundary diagnosis

## Summary

Instrument only the still-failing new resume regression with fixed numeric/boolean
test evidence. Keep runtime and all acceptance assertions unchanged.

## Why

At8ee94a1f, actual composed lifecycle/observer/state assertions pass, but the first
resume refresh wait still times out. Android38709560170 compiles and executes974
tests with exactly that failure. Thus the earlier custom-owner hypothesis is not
established and is not a fix or passing platform result.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/StyleDnaComposeTest.kt`

The fixture separately counts actual lifecycle resume events, direct invalidation
callbacks through the existing observer, and panel refresh callbacks. Counts and
key-presence booleans contain no credential, path, prompt, image or user data.

## Behavior before / after

The same10000ms condition and all plan/consent/key assertions remain. A failure
now identifies whether event delivery, observer invalidation or panel refresh is
missing. No runtime change, retry, test removal, idling bypass or timeout increase.

## Tests actually run

Android38709560170 at8ee94a1f executes974/1; the sole timeout is the new first resume
condition, after real lifecycle state/observer assertions. Failure artifact11959694206
is223402bytes with SHA25662ff16eda115d0ded0b09f81de8af8317dbcca929c15d214cd001d36a81e7637.
Next execution must establish the exact boundary before further changes.

## Not tested

Key-resume behavior remains blocked pending diagnostic execution. Physical phone,
Keystore decryption, live account access and installed-signature acceptance are NOT RUN.

## Risks

Do not interpret initial key recognition or973 passing cases as proof that resume
works. Preserve the failing regression and separate platform observations from code
inference. No live provider call or automatic retry is authorized by this test.

## Rollback

Remove only diagnostic fixture observers after capturing actual evidence, keeping
all original acceptance assertions and controller/UI corrections intact.

## Documentation updated

This note preserves the failed hypothesis and exact next diagnostic boundary. The
earlier source and failure records remain unchanged and reviewable.

## Follow-up

Inspect actual event/invalidation/refresh evidence, make the smallest justified
correction, and finish all required final-head gates and APK. Keep PR8 draft and
BLOCKED until then; no main/PR6/PR7 change or live request occurs.
