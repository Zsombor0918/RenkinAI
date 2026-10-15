# Finish guided UX readiness and persistence journeys

## Summary

Finish the existing Stage1 implementation from3ee52fa1, retaining the committed
Windows test-only harness861ac270. Trial confirmation decline/dismiss now revalidates
unchanged durable approval locally. Stages2/3 remain planned and upstream PRs/main remain.

## Why

The first corrected local run reached real Room loads that were still disabling
new test fields, and a post-request local reload that had not yet displayed review
cards. Proper synchronization exposed the reported production defect: local prepare
clears ready, while cancel drops only the plan and never reloads existing approval.

## Scope / changed files

Behavior impact: changed
Architecture impact: none
Milestone impact: none
Durable decision: none

- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectTrialPanel.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/GuidedProjectRoomJourneyComposeTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleTrialJourneyComposeTest.kt`
- `.github/workflows/compose-regressions.yml`

The isolated regression workflow also triggers for shared Java shadows, test resources
and the harness-test package, preserving coverage for future harness-only corrections.
The correction uses existing local reload/controller/repository proof; no persistence,
gateway, database, provider, dependency, signing or application identity change is added.

## Behavior before / after

Before, preparing and declining a trial could leave an unchanged approved set visually
not ready until re-entry. The shared button/dismiss callback cancels the unsubmitted
plan and requests local proof revalidation. It never sets ready directly or submits a
provider request; unavailable/stale/error inputs still fail closed. Existing journal,
variant and approval objects remain byte-for-byte/equality unchanged in the real test.

The new and retained fixtures now wait for actual current review proof after request
completion before materializing the same Accept action. The first103-case corrected
integration had exactly one retained interaction failure at that transient boundary.
The test fixture also waits for the actual enabled description field after its independent
Room load, and processes completion effects before waiting for nonbusy current trial
preparation. Native320dp/SDK33, real Room/controller/journal, existing10000ms bounds,
scroll/actions/assertions, normal idling and every previous selector remain.

## Tests actually run

Actual resumed command selected AtomicFileTest, GuidedProjectRoomJourneyComposeTest,
the Legacy-history phrase case and cross-profile trial case:10 tests/4 failures;
all six non-Room cases passed. Corrected Room synchronization then ran
`gradlew.bat testDebugUnitTest --tests dev.renkinProject.renkin.ui.project.GuidedProjectRoomJourneyComposeTest --console=plain`:
4 tests/1 failure at the intended decline-ready assertion. After the narrow UI fix,
the identical command passed4/4, zero failures/errors/skips in115.467s.

Complete targeted guided/retained verification passed 103 tests /0 failures /0 errors /0 skips in
60.674s, selecting13 classes: six required guided/harness
classes plus retained style/analysis/DNA/trial/routed journey and both owned controllers.
Exact command/log/XML are in external task evidence `2026-10-15/targeted-guided-retained-green`.
Original failed logs remain; no result is replaced by a historical hosted count.
Final committed-head all23/unfiltered/Python/docs/PR checks and fresh APK measurements
are recorded externally after execution, since this file cannot contain its own hash.

## Not tested

Physical Flip8/One UI/Theme Park, emulator/instrumentation, TalkBack/fold/rotation,
installed-signature data upgrade and actual generated visual fidelity remain NOT RUN.
No device is attached and no owner phone installation or live provider call occurs.

## Risks

Local reload must still verify source/catalog/original/revision/owner context and may
correctly leave readiness unavailable. Existing atomic/symlink/persistence/consent and
legacy wire assertions remain mandatory. The test-only global Windows shadow requires
independent Linux verification of its original-method branch. New local debug signing
does not establish compatibility with an installed CI certificate.

## Rollback

Revert the topic correction after owner review; the conservative false-negative UI
would return. There is no migration, deleted history or changed billing marker.
Preserve v21/DNA readers, immutable assets, all consumed requests and both signing keys.

## Documentation updated

CURRENT_STATE/HANDOFF/BASELINE describe closed source scope, exact failure classification,
actual narrow/targeted outcomes and final verification boundary. ARCHITECTURE/ADR0009
clarify read-only revalidation. TEST_MATRIX and owner checks include approved-plan
cancellation; INDEX links this record. Historical notes and device/fidelity limitations
are preserved. New work remains confined to Stage1 stabilization.

## Follow-up

Execute all mandatory checks and build the APK on the final committed HEAD, then push
only this branch and open the complete-audit stacked draft targeting feat/style-dna-v1.
Record observed final counts/hash/signature/CI externally. Next safe work is owner free
phone acceptance; separately authorized Stage2 visual comparison and Stage3 cleanup
remain plans. No merge, main write, release, data deletion or paid request is authorized.
