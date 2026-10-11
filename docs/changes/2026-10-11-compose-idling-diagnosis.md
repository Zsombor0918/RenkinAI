# Profile form idling isolation

## Summary

Add measured, independent reproductions of the two blocked profile-form Compose
tests before choosing a fix. Preserve all existing assertions and production UI.

## Why

CI 38143988918 on `8bd57953ec54c7056dac4acf850f5a8863b8bd64` still failed two
of 628 tests after phone viewport isolation. The downloaded report contains 135
HTML/support files and no XML. Its generic timeout does not identify a root cause.
Width bounds and explicit clock control also failed in earlier recorded runs.

## Scope / changed files

- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProfileDialogDiagnosticsTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectShellComposeTest.kt`
- `scripts/run_compose_diagnostics.py`
- `.github/workflows/compose-diagnostics.yml`

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

These are temporary test diagnostics, a read-only PR workflow and a report-retaining
runner. They do not alter app behavior, dependencies, CI gates or product scope.

## Behavior before / after

The existing full Android CI still runs unchanged. Separately execute plain content,
empty dialog, unlabelled/labelled/supporting fields, three fields, the actual shared
form and actual shell. Report snapshot writes and installed idler/root flags before
teardown, and retain each case's XML and HTML. Any failed case returns a nonzero exit.
No clocks, sleeps, timeouts, idling conditions or interactions are bypassed.

## Tests actually run

The latest failure HTML was downloaded and both full stacks inspected. Both fail
inside RobolectricIdlingStrategy waiting for idle before the first text input or
ingress state update; this proves a boundary, not a component cause. New diagnostic
Android execution requires GitHub Actions because the local SDK/Gradle is unavailable.

## Not tested

The new diagnosis cases have not yet executed. No physical Android, Samsung/One UI,
TalkBack, installation, real profile migration or live AI call was performed.

## Risks

Diagnostic reflection depends on the installed test internals and may report missing
flags. Snapshot observers collect bounded samples without modifying snapshot state.
The additional workflow costs CI time and will be removed when the evidence is captured.

## Rollback

Remove the temporary diagnostic workflow, runner, probe and isolation cases together.
The full product tests and original Android CI must remain and pass before acceptance.
No application data or schema was changed.

## Documentation updated

WORKFLOW explains the independent diagnostics and their nonzero failure behavior.
CURRENT_STATE, HANDOFF, BASELINE and TEST_MATRIX now record the actually failed phone
viewport attempt and the unresolved diagnosis rather than describing it as pending.

## Follow-up

Inspect resolved dependencies, full XML and measured flags, establish the narrowest
proven cause, then make the smallest compatible fix. Run both tests separately,
together and in the full suite, build the APK and audit the final SHA. PR #2 remains
BLOCKED and unmerged until real automated evidence warrants a new audit status.
