# M2 — invalidate changed sources and correct measured fixture errors

## Summary

Invalidate selected-package and displayed-theme changes while setup is visible,
dispose the read-only receiver on leaving, and preserve newest rescan loading state.
Correct three measured test fixture/API errors without altering form or editor UX.

## Why

Resume alone cannot catch an update/removal while the screen is foreground. Explicit
invalidation avoids stale coverage. Android 38159397296 executed 667 tests and exposed
three new fixture mistakes, established by downloaded HTML artifact 11708809083.

## Scope / changed files

Behavior impact: changed
Architecture impact: changed
Milestone impact: none
Durable decision: none

- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectSetup.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/apk/CoveragePreviewTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/CreateExtendFlowComposeTest.kt`

The runtime addition is a setup-scoped invalidation listener, not an app watcher,
profile operation, source preference write or new background job.

## Behavior before / after

Before, an already rendered source result revalidated on resume/read-key changes.
After, selected-package broadcasts and theme changes cancel/reset analysis as well;
older rescan cleanup cannot mark a newer request loaded. Tests use the real compiled
vector resource and dismiss label, and assert every source counter exactly.

## Tests actually run

At 4a447d88, Android 38159397296 compiled production and test Kotlin, ran 667 tests,
failed three new fixture cases and skipped APK. Full HTML traces proved compiled-XML
inflation ClassCastException, exact-fragment count assertion and missing literal
Cancel. The old M1 cases and all other tests passed in this full run. Local guards
previously passed 45; fresh hosted verification remains required after this commit.

## Not tested

The new broadcast/disposal/Back regressions and corrected fixtures have not executed
at this checkpoint. Local Gradle download is blocked; devices, OEM launcher, real
pack lifecycle and live/paid APIs remain NOT RUN.

## Risks

System package notifications need actual-device confirmation. The receiver is scoped
to setup and filters the chosen package; no icons or primary preferences are changed.
Static preview still cannot certify dynamic/OEM behavior or cross-user identity.

## Rollback

Revert this follow-up to restore resume-only invalidation and the three observed
fixture failures. Reverting all M2 commits restores accepted M1; no schema or signing
conversion is introduced and normal created profiles remain valid.

## Documentation updated

CURRENT_STATE and ARCHITECTURE explain scoped invalidation and rescan ordering.
BASELINE retains failed checkpoint evidence; HANDOFF and TEST_MATRIX record actual
fixture corrections and fresh tests still needed, without changing historical results.

## Follow-up

Require all retained originals/new groups and unfiltered Android suite/APK to pass
on fresh final head, then update the single PR audit result with honest device risks.
Owner acceptance remains necessary; no automatic merge or later phase implementation.
