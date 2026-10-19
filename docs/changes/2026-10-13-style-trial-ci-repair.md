# Style trial regression and Create context repair

## Summary

Repair the remaining PR #6 routed UI failures and keep CREATE style saves source-free.
Continue the existing feature branch; final-head automated evidence belongs in the PR audit.

## Why

Head 3133b522 compiled production and tests but executed 871 tests with ten failures,
so the CI intentionally skipped APK assembly. The downloaded exact-head regression
XML identifies text-matcher, offscreen-control, unfinished-save and fixture failures,
plus a real CREATE source-stamp leak that clears a previously saved target selection.

## Scope / changed files

Behavior impact: changed
Architecture impact: none
Milestone impact: none
Durable decision: none

- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/RenkinAiRoot.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/CreateExtendFlowComposeTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleTrialJourneyComposeTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/StyleTrialComposeTest.kt`

The source-context guard implements the existing CREATE/EXTEND contract. No schema,
dependency, HTTP client, consent journal, signing, installer or billing change is made.

## Behavior before / after

CREATE always passes a null source stamp to STYLE, including when setup input exposes
coverage for another context. EXTEND uses only the current profile/source snapshot.
The repository still invalidates selections on genuine source or mode changes.
Routed tests now scroll before touch input, observe durable save completion, explicitly
match explanatory text fragments and provide the saved-selection prerequisite in the
legacy shell fixture. The CREATE journey additionally asserts null source stamp and
unchanged exact saved component keys. Every existing test and selector remains.

## Tests actually run

Baseline CI 38366509384 and regression run 38366509274 compiled both Kotlin sources
and independently executed 871 tests with ten failures. Downloaded artifact 11796848515
has verified ZIP SHA256 8dd04bb1f52795171cc2c81a144bbc95daa607b208f4cfeb59e278f3d08ecbb7;
full-suite XML failures were inspected before editing. Local Python 3.12/Linux
`python3 -m unittest discover -s scripts/tests -v` passed 52/52 in 7.464 seconds.
The local full Gradle command failed before configuration downloading Gradle 8.11.1
with Network is unreachable. Final hosted full/filtered results, genuine schemas and
APK are measured after publication and recorded against their actual SHA in PR #6.

## Not tested

No physical Samsung/One UI/Theme Park, emulator, live provider or paid image-generation
request runs in this correction. The owner's prior phone HTTP cause remains unproven;
the corrected APK still needs owner journey verification before merge.

## Risks

Hosted Compose/Room evidence does not prove OEM lifecycle or provider image quality.
Observed UI completion is required before Back; the existing 10000 ms bounded wait
remains, with no timeout increase, disabled idling, ignored test or narrowed runner.
The original manifest, keys, profile identity and legacy icon pipeline stay intact.

## Rollback

Revert this commit through a reviewed PR while preserving the prior owner-blocker,
one-shot and approval fixes. Room stays v21; an older populated-database downgrade
is unsafe. Retain app/pack keys and saved user data.

## Documentation updated

CURRENT_STATE and ARCHITECTURE explain source-free CREATE style navigation. HANDOFF,
BASELINE and TEST_MATRIX retain the failed exact-head evidence and corrective scope.
INDEX points to this correction; no new architecture or milestone is introduced.

## Follow-up

Check all 21 retained regression commands, the unfiltered Android compile/unit/APK
command and governance on the actual final head. Download and inspect that APK,
update the existing ten-section PR audit, then deliver the exact phone walkthrough.
PR #6 stays draft/open/unmerged until the owner accepts the corrective device journey.
