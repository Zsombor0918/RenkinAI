# Inspect actual montage requests and scope their model default

## Summary

Prove the actual V3 multipart contract and apply the owner's conditional fallback:
new montage Trials default to Sunburst/high after request contamination is excluded.
Keep the existing branch/PR10 and all historical payable request interpretation.

## Why

The owner reports generic/glossy in-app results compared to a better manual ChatGPT
experiment. Its images and exact live request are not attached, so visual causality
cannot be proven. The reproduced configuration inherits the faster Flare/medium
default despite this precision-sensitive reference-editing task.

## Scope / changed files

Behavior impact: changed
Architecture impact: changed
Milestone impact: none
Durable decision: recorded

`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleTrials.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/PackStyleAnalysisTest.kt`
`app/src/androidTest/kotlin/dev/renkinProject/renkin/offline/StyleDnaEmulatorTest.kt`

Only montage preparation defaults change. Extend existing tests, not test groups
or provider workflows. The synthetic request diagnostic is test-only; it uses the
existing CI montage artifact without production logging or another store.

## Behavior before / after

Before, V3 correctly sends one fixed contract, one reviewed brief and original-first
saved montage images, but inherits global model/quality (default Flare/medium).
After, new montage preparation defaults to Sunburst/high. Explicit Trial options
remain available. Global/manual/AI1/AI2 defaults and V1/V2/V3 artistic text stay fixed.
There are no separate system/developer messages in this direct Image API request.
No old white-outline, standard icon or DNA recipe is appended to this V3 route.

## Tests actually run

Windows/JDK17/native Robolectric: extend the existing
PackStyleAnalysisTest.savedMontageAnalysisReachesFrozenTrialAndActualMultipartOnlyAfterSeparateConsent.
The targeted RED executes one case: expected Sunburst, actual Flare, after exact
prompt/hash, no-legacy/duplicate, image-role/order/count and output assertions pass.
Its captured request confirms medium quality. Logs/XML/JSON are retained outside Git.
Focused GREEN executes78/78 cases across seven classes, zero failures/errors/skips,
in one Gradle invocation (2m49s). Python guard tests67/67 pass in54.346s. Hosted
final-head results are recorded on PR10 after execution; no full local suite is
requested. The development command selects PackStyleAnalysisTest,
StyleTrialGatewayTest, StyleTrialPlanTest, IconGenerationGatewayTest, OwnedStyleTrialsTest,
StyleTrialJournalTest and PromptComposerTest in one Gradle invocation.

## Not tested

No live/paid API, owner key, physical phone or real visual comparison is executed.
The comparative owner images and actual historical device settings are unavailable.
Do not infer that every owner request used the application's default model.

## Risks

Sunburst/high can increase latency/cost; separate consent still shows the frozen
configuration. Account availability and improved aesthetics need separately
authorized live validation. Historical91e5737d main-range governance remains BLOCKED;
neither history nor policy is rewritten. Installed-signature compatibility is unknown.

## Rollback

Restore only future preparation defaults through an ordinary reviewed revert.
Retain saved plans, prompt versions, references, assets, journals and consumed IDs;
never override a model at submission or replay a previous request.

## Documentation updated

ADR0017 records the scoped model decision, evidence and uncertainty. Index,
architecture, current state, handoff, QA baseline/matrix and montage contract explain
the exact before/after request and test-only diagnostic. No new product milestone,
analysis schema, Room migration, batch engine or signing decision is introduced.

## Follow-up

Complete one authoritative final-head Linux/build plus hosted Android and Python
checks, retain the new request JSON and independently verify any debug APK. Publish
exact SHA/results on draft PR10. Stop after this correction; real visual testing
requires separate authorization and cannot be replaced by mock success.
