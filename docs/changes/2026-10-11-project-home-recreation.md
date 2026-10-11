# ProjectHome — retain editor ingress and intro dismissal

## Summary

Harden the same M1 entry point against process restoration before routing consumes
an intent, and Home returns before intro preferences finish writing. Correct the
new UI test's DpRect edge-based size calculation without altering dependencies.

## Why

An in-memory ingress counter could be lost while the original URI guard suppressed
another import. An initial true preference value could reset a saved intro dismissal.
The first CI app compile passed but test compile exposed unavailable size properties.

## Scope / changed files

Behavior impact: changed
Architecture impact: changed
Milestone impact: none
Durable decision: none

- `app/src/main/kotlin/dev/renkinProject/renkin/MainViewModel.kt`: delegate editor
  request versions to the existing VM SavedStateHandle.
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/SharedImageIngress.kt`:
  saved entry counter, preserving URI/import semantics.
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/MainScreen.kt`: nullable initial
  preference read and the shared saved intro visibility helper.
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/IntroShowcase.kt`: dismissal
  reset only on a true seen-to-unseen transition, without adding a tutorial.
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectHomeComposeTest.kt`:
  size assertions use right-left and bottom-top DpRect edges.
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectShellComposeTest.kt`:
  intro skipped → Home → pending prefs → Editor regression and Settings re-show.
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/SharedImageIngressTest.kt`:
  saved counter reconstruction before consumption, without an automatic new request.

The milestone and durable single-session/profile decision remain the same M1.
No app/pack identity, schema, source/AI/build pipeline, hook or CI change.

## Behavior before / after

Editor ingress now survives restoration before its consuming effect; a consumed
version does not override a user's later saved Home route. URI suppression is still
unchanged. Intro dismissal persists while preferences load/write, and Settings can
still request the old tour explicitly. Test size checks use the repository's actual API.

## Tests actually run

At prior source head `b60b477864fd50a9776f1f1b2c85d5226e9ccfc1`, Android
CI 38140946784 passed app compile then failed test compilation before executing
tests or building the APK. Governance 38140946802 passed commit conventions and
45 guard tests; documentation-audit correctly failed on the draft's BLOCKED status.
Local governance at the feature checkpoint passed 45 tests. Staged/range and new
Android execution for this follow-up are recorded after actual completion in QA/PR.

## Not tested

No local Android execution is possible while Gradle downloads are blocked. New
regressions await fresh feature CI. No actual process kill, full Hilt MainActivity,
device notification/SEND import, TalkBack, Samsung/fold or live provider was exercised.

## Risks

SavedStateHandle follows Android's saved-state lifecycle and does not make URI
decoding or profile storage transactional. Fake-state tests prove helper/routing
contracts, not every OS kill timing or preference write. Device matrix remains open.

## Rollback

Revert this follow-up through a reviewed PR with M1's overall rollback if needed.
No data/schema migration or key change; counter/UI marker removal leaves existing
profiles, uploads and saved icons intact. Do not clear user data for UI rollback.

## Documentation updated

CURRENT_STATE and ARCHITECTURE describe persisted ingress and nullable intro state.
HANDOFF/QA record the exact unsuccessful first run, new tests and pending checks.
ROADMAP clarifies the current M1 validation status, without advancing another phase.

## Follow-up

Run compile, the full old/new unit suite and debug APK at the latest feature head.
Close the draft audit only after actual success; verify the final required contexts,
then await owner/reviewer acceptance and outstanding device/protection settings.
