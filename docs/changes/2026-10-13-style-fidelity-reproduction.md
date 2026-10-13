# Style fidelity and original provenance: test-first checkpoint

## Summary

Add failing regressions for the enhancement in owner comment 6149883090 on existing PR #6. Production remains byte-identical to 422ef578.

## Why

The current coarse color bins replace real hues, farthest-first selection amplifies minority families, and frozen confirmation does not identify original provenance separately from style source.

## Scope / changed files

`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/ReferenceSamplerTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisGatewayTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/StyleTrialComposeTest.kt`

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

Only three Android test paths change; existing assertions/selectors remain.

## Behavior before / after

No runtime change. Proven baseline: InstalledAppCatalog uses unbadged launcher component icons; TrialOriginals uses exact PackageManager activity icons independently of the EXTEND pack. OEM resource substitution is a hypothesis requiring device comparison, not a proven current code defect.

## Tests actually run

Local complete Gradle compile/unit/APK fails before configuration downloading Gradle 8.11.1 with Network is unreachable. Hosted red assertions must be observed before production changes. Python guards and staged checks are run before committing; exact results belong in the PR audit.

## Not tested

No physical Flip8, Samsung resource overlay, paid provider or generated-artwork verification is available. No live API call is authorized.

## Risks

Tests intentionally fail on current production. They assert measured color fidelity, dominant-family representation, explicit color philosophy and frozen original/style-source labels; they do not prove model quality.

## Rollback

Revert only additive test/docs changes if the reproduction is invalid. Retain every previous test, immutable image and paid-consent history.

## Documentation updated

This new record distinguishes proven source behavior from OEM hypotheses and planned incremental improvements. The existing QA historical results remain unchanged.

## Follow-up

Read executable hosted red failures, then implement bounded local analysis and visible original-source verification without changing Room v21, legacy request bytes or billable sample limits. Keep PR #6 draft/open/unmerged.
