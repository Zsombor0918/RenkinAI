# Windows AtomicFile regression harness correction

## Summary

The safe guided UX checkpoint is `3ee52fa19d91c0a921681be01b809aaa502e31b2`.
This bounded follow-up corrects Android AtomicFile replacement semantics on the
Windows Robolectric test filesystem. Production journals and security checks stay intact.

## Why

Integration98/26 clustered at journal updates before successful provider results.
A one-method native probe independently reproduced the platform mismatch: second
finishWrite retained first base bytes and second `.new` bytes, with a failed-rename
log. That explains the pre-existing Windows journal boundary rather than an Android
production regression. The temporary diagnostic source and original XML remain
outside Git in the task tools folder; no diagnostic logging enters this change.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: recorded

Only test harness/tooling change in this scope:

- `app/src/test/java/dev/renkinProject/renkin/testing/WindowsAtomicFileShadow.java`
- `app/src/test/kotlin/dev/renkinProject/renkin/testing/WindowsAtomicFileTest.kt`
- `app/src/test/resources/robolectric.properties`
- `scripts/run_compose_regressions.py`
- `scripts/tests/test_compose_regressions.py`

## Behavior before / after

Windows File.renameTo cannot replace an existing destination as Android's POSIX
rename does. The test-only API30+ shadow intercepts the verified private rename
method and uses atomic replace on Windows, without deleting the old base or falling
back to copying. Non-Windows calls the original Android method directly. Real
startWrite, finishWrite, failWrite and openRead behavior remains in use. Atomic move
failure stays a failure; no durability, consumed-marker or security predicate is relaxed.
All22 previous runner groups/selectors remain; the guided group additionally requires
the four AtomicFile guards, making23 groups with six guided/harness classes.

## Tests actually run

Native PowerShell/Temurin17/Gradle8.11.1/API36 compilation passed. The one-method
pre-fix probe failed as expected. One bounded corrected command passed8 actual tests,
zero failures/errors/skips in34s: four AtomicFile guards plus
`OwnedStyleAnalysisTest.useChangesOnlyDraftAndExplicitSavePreservesInstructionAndImmutableHistory`,
`OwnedStyleTrialsTest.regenerationKeepsAcceptedHistoryAndRequiresNewExplicitConsent`,
`StyleAnalysisJournalTest.atomicBackupRecoveryAndInterruptedNewMarkerStayConsumed`, and
`StyleTrialJournalTest.orphanFirstAtomicWriteDoesNotBlockRecoveryOrFreshConsentAndCannotReplayItsUuid`.
The command was `gradlew.bat testDebugUnitTest --tests` with each fully qualified
selector (AtomicFile class selects four methods), and `--console=plain`. Original
command/log/XML are outside Git in `windows-atomic-harness-1`. Nine runner guards
passed. Checkpoint full Python55 passed in105.263s; committed-range docs45 paths and
one commit subject passed. This narrow result does not certify the full98 or all23 matrix.

## Not tested

Full corrected unfiltered Android/all23 regression acceptance remains deferred under
the owner's usage-limit instruction. Hosted Linux execution of the original-method
branch, other Robolectric SDKs and real phone/installed-data/signature compatibility
are not proven by this Windows SDK33 subset. No physical or paid provider test occurred.

## Risks

A global test-resource shadow affects API30+ Robolectric AtomicFile calls. The four
guards prove successive committed replacement, failed/interrupted write preservation,
unreadable unfinished first write, and legacy backup recovery. Retained actual journal
and controller assertions also pass without replay. Remaining suites are mandatory
before regression acceptance; production storage behavior is unchanged.

## Rollback

Revert this test-only follow-up to reproduce the Windows mismatch again. Retain the
original probe/evidence and guided checkpoint. No runtime migration, stored data,
provider body, billing state, APK identity or signing key is changed by this harness.

## Documentation updated

HANDOFF and BASELINE record the observed narrow result and exact continuation commands.
WORKFLOW, TEST_MATRIX and ADR0009 explain the additive required selector and platform
fidelity decision. Complete acceptance still requires an external exact-head audit.

## Follow-up

Finish the bounded remaining journey/state subset, preserve any unresolved failures,
commit and produce a fresh exact-head debug APK. Then resume targeted tests before the
complete23-group runner in a follow-up session. Keep main and the two upstream drafts
unchanged; never promote successful assembly into full regression acceptance.
