# M2 — synchronize lifecycle assertions and retain final-head evidence

## Summary

Wait for the source-change composition/disposal before capturing a lifecycle counter;
retain every exact broadcast assertion. Run the unchanged regression workflow for QA
changes too, so the final evidence commit can prove its own SHA.

## Why

Run 38159888019 executed 669 tests with one observer-fixture failure: its baseline
was sampled before a newly registered observer delivered current ON_RESUME. The
Downloaded HTML showed expected 3 versus actual 4, not a leaked old-package callback.
QA-only commits otherwise fall outside the old path filter and lose fresh-head proof.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/CreateExtendFlowComposeTest.kt`
- `.github/workflows/compose-regressions.yml`

Production sources are unchanged. The workflow adds QA paths; it removes no selector,
step, check or failure condition and keeps read-only permissions.

## Behavior before / after

Before the test read a plain counter before a new effect committed; after it uses
normal Compose waitForIdle synchronization before measuring. All exact expected
counts still apply, including irrelevant package and post-disposal events. QA path
changes now execute original isolated/combined/form, M2 groups and full suite/APK.

## Tests actually run

Android 38159888019 compiled app/tests, ran 669 with one failure and skipped APK;
artifact 11709674763 was downloaded and inspected. Local 45 guard tests passed in
7.556 seconds and committed-range docs/commit guards passed. The correction still
requires fresh hosted execution; earlier failed checkpoints remain recorded.

## Not tested

No M2 full suite or APK is yet green at this checkpoint. Local Gradle download stays
network-blocked. Physical devices, OEM/pack install and live provider tests are NOT RUN.

## Risks

Robolectric lifecycle events are deterministic fixture evidence, not device proof.
QA changes now cause additional Actions runtime; full Android CI and all required
checks are untouched. No timeout, sleep, ignore or idling-disable workaround is used.

## Rollback

Revert this commit to restore the observed fixture race and older QA path filter;
there is no application state, schema, package, signing or API change to roll back.

## Documentation updated

WORKFLOW explains why final QA commits trigger exact-head regressions. BASELINE and
HANDOFF preserve 669/one-failure evidence and the measured synchronization boundary.

## Follow-up

Inspect fresh isolated groups, unfiltered counts and APK, then record final evidence
and retain a single honest PR audit with device risks. Owner acceptance remains
required; do not merge or implement the later AI/persistence phases.
