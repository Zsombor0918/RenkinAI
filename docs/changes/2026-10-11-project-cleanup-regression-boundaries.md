# Codex 03 cleanup and invalidation regression boundaries

## Summary

Scope failed-import cleanup to the importing operation, preserve the original
source-only receiver contract, and opt launcher preparation into package changes.
Add real-screen diagnostics for the one still failing selection Save interaction.

## Why

Head `c06d225a42ac5418dac80369602e6b8481db14c1` compiled but Android run
38175713388 executed 716 tests with three failures. Stack traces show explicit GC
expected two unrelated orphan/temp files but saw zero: import-error cleanup had
collected unrelated files. The unchanged M2 source-only broadcast test expected no
other-package invalidation but saw one. These are actual regression boundaries,
not grounds to weaken their assertions. A new selection Save assertion still sees
no completion node; retain it and measure enabled/displayed state, callback count
and semantics to establish the remaining narrow UI/test boundary.

## Scope / changed files

- `app/src/main/kotlin/dev/renkinProject/renkin/data/project/ProjectAiRepository.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/project/ProjectAssetStore.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectSetup.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/data/project/ProjectAiPersistenceTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectSelectionComposeTest.kt`

Behavior impact: changed
Architecture impact: changed
Milestone impact: none
Durable decision: none

These corrections enforce the existing ADR's explicit-GC and screen-only read
contracts; no new product or durable storage decision is introduced.

## Behavior before / after

Failed imports collect only their own newly finalized hash paths after checking
all retained rows. Earlier unrecorded files remain until explicit GC. Existing
ObserveCoverageInvalidation callers retain source-only behavior by default;
project preparation explicitly opts into all launcher package changes. Receivers
are disposed on leaving the screen. New real Compose coverage checks that opt-in
and disposal without changing the old M2 test. Save interaction assertions remain;
temporary test diagnostics are removed after the measured cause is established.

## Tests actually run

Android run 38175713388 on c06d225a compiled and ran 716 tests, three failures;
APK skipped. Downloaded artifact 11715578079 and read complete failure stacks.
The old seven failing Room/share/PNG/text cases are no longer reported; the
selection Save case still fails and two newly measured cleanup/receiver regressions
remain. Regression run 38175713319 also failed. Previous offline guards passed
45/45, staged docs/hooks PASS, committed-range docs PASS for 47 paths and commit
conventions PASS for two subjects. Local Gradle attempted again on c06d225a and
failed before configuration with SocketException Network is unreachable.

## Not tested

This next correction requires its own Android suite/APK results. Physical devices,
URI providers, upgrade/backup/signing/installer/TalkBack/OEM remain NOT RUN. No
live API request or app-wide background watcher was introduced or tested.

## Risks

Candidate-scoped cleanup cannot delete previous unrelated files automatically;
explicit GC remains necessary for genuinely abandoned files. Launcher opt-in is
limited to composed preparation and never updates active icon state. The remaining
Save interaction is still a technical blocker until diagnosed, not silently ignored.

## Rollback

Revert only these scoped corrections for investigation while keeping the measured
regression assertions. Retain v19 and forward-repair/pre-upgrade-backup rollback
rules; never install an old v18 APK over an upgraded database.

## Documentation updated

CURRENT_STATE, ARCHITECTURE and HANDOFF record the narrower cleanup/observer
contracts and remaining UI blocker. BASELINE and TEST_MATRIX preserve both failed
heads and distinguish correction code from evidence pending on the next head.

## Follow-up

Inspect measured Save semantics/callbacks, fix the proven boundary, remove temporary
diagnostics, and rerun original and new filtered groups plus the full Android/APK
suite. Keep draft PR #4 BLOCKED. Do not merge, publish or alter governance rules.
