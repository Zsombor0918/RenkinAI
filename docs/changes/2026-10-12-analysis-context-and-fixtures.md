# Bind proposal route and repair measured fixture contracts

## Summary

Reject frozen consent and proposal Use/Save after the same profile changes mode or
chosen source. Repair five CI-proven fixture failures and retain genuine KSP v20.

## Why

Saved metadata cannot detect an unsaved STYLE route change. Initial executable
tests also exposed incomplete historical SQLite fixtures and incorrect wire/UI
expectations; those failures must be fixed while preserving meaningful assertions.

## Scope / changed files

Behavior impact: changed
Architecture impact: changed
Milestone impact: none
Durable decision: none

- `app/src/main/kotlin/dev/renkinProject/renkin/MainViewModel.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectAnalysisBackend.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleAnalysis.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleAnalysisPanel.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisSchemaTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/data/ExportedSchemaTestHelper.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/data/project/ProjectAiAnalysisPersistenceTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleAnalysisTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleAnalysisComposeTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleComposeTest.kt`
- `app/schemas/dev.renkinProject.renkin.data.RenkinPackDatabase/20.json`

The existing session/profile/journal/Room decisions remain; no second identity or
network operation is added. Canonical docs and README accompany these paths.

## Behavior before / after

STYLE binds mode/source before refresh; confirmation, Use and owner-guarded Save
reject another route's proposal. Same-source updates retain explicit stale review.
The SQLite helper creates exported indexes, and the v19 test checks physical version
after onCreate returns. JSON rejection retains literal 1.0; UI assertions check full
counter/source text. No old test, selector, interaction or timeout is removed.

## Tests actually run

53addf96 Android 38236937112 compiled and ran 789 with five failures, APK skipped.
Downloaded regression artifact 11741377662 independently totals 135 XML / 789 tests /
five failures / zero errors/skips with all selectors present. Sampler 11/11, gateway
6/6, journal 10/10 and retained M1/M2/data/legacy groups passed. QA names failed groups.
Genuine KSP v20 identity 7645511f2ebcb036f0f2da33eee84a17, SHA256
10410ffa2c99a0718ffb2552f2194280d00fe816e88af5e0ed9a667780abd8f8 is copied
byte-for-byte from that artifact. Local 51 guards passed at the prior head in 13.188 s.

## Not tested

Corrected migration/index and new owner/UI route tests await hosted execution.
Local Android is blocked at Gradle download. Device/provider/process/storage/OEM
checks remain NOT RUN; live billable requests are zero.

## Risks

Changing the selected source cannot use the same-source stale override. Unreviewed
journals remain device-local. Exported-schema fixtures must reproduce actual indexes;
v20 data still cannot be opened by a v19 APK.

## Rollback

Revert only with equivalent route/fixture protection; otherwise the measured gaps
return. Installed v20 requires forward repair or explicit compatible pre-upgrade
backup recovery with signing continuity, not an in-place older APK downgrade.

## Documentation updated

CURRENT_STATE/ARCHITECTURE explain route identity; HANDOFF/QA retain failed counts
and pending corrected checks. README removes obsolete future-only style claims.

## Follow-up

Require every retained/new group, unfiltered Android suite, debug APK and genuine
schema equivalence at the next head. Keep PR #5 BLOCKED until proved; owner merge
and separately confirmed Codex 05 trial generation remain later decisions.
