# Style DNA source implementation

## Summary

Implement bounded per-image analysis, reviewed multi-family grammar and optional M5 recipes. Visual quality remains experimental.

## Why

The six actual test-first failures prove gaps in safe sample diagnostics, measured-color deduplication,24-reference support, per-image schema, family grammar and modernization prohibitions.

## Scope / changed files

`app/src/main/kotlin/dev/renkinProject/renkin/MainViewModel.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/OpenAiStyleAnalysisGateway.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/OpenAiStyleDnaGateway.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/PackStyleEvidence.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/PromptComposer.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/ReferenceDiagnostics.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/ReferenceImages.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/ReferenceSampler.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/ReviewedAnalysisEvidence.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisGateway.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisJournal.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisServices.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaCoordinator.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaJournal.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaPlan.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaSynthesis.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaV1.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialCoordinator.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialJournal.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialPlan.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/apk/IconCoverageAnalyzer.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/apk/IconPackRepository.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/apk/ProfileManager.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/data/transfer/BackupManager.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/packages/ApplicationManager.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleAnalysis.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleTrials.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectAnalysisBackend.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleAnalysisPanel.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleDnaPanel.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStylePanel.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectTrialBackend.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectTrialPanel.kt`
`app/src/main/res/values/strings.xml`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaReproductionTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaWorkflowTest.kt`
`scripts/run_compose_regressions.py`

Behavior impact: changed
Architecture impact: changed
Milestone impact: changed
Durable decision: recorded

D0–D3 are additive to PR6; Deep and live D4 remain deferred.

## Behavior before / after

Legacy M4 aggregate analysis remains available. Quick and Standard explicitly freeze1 or at-most5 independent one-shot steps, bind per-sample IDs/hashes and produce editable family rules. Save is local; optional M5 recipes preserve original-only identity. All historical optional encodings remain readable.

## Tests actually run

Hosted red b2df166a / CI38465737793 compiled and ran904 tests with exactly6 new assertion failures, no old failures. Artifact11849000452 ZIP SHA2561f674618a710a8d2e4b5987a123cb17b265d33fbbda048bc1e632e08437662c6 was downloaded and verified. Local Python52 tests passed; complete local Gradle/runner failed before configuration at network download. Current new code awaits its own hosted compile/test evidence.

## Not tested

Current Android implementation has not yet passed hosted gates. No live provider, key, physical Samsung/Flip/Theme Park, TalkBack, real storage/restore/install or blind visual test was run.

## Risks

Strict bounds may reject broad or uncertain grammar. Local families/palette are heuristics; provider interpretations are unverified. Older binaries cannot read new tagged grammar and must not silently downgrade populated data. New API plans never authorize automatic retries.

## Rollback

Use forward repair preserving V1/DNA readers, consumed UUIDs and optional trial fingerprints. Keep Room21, saved history, RAW/original pins and owner signing keys. Do not erase data or replay charges.

## Documentation updated

README/CHANGELOG, canonical state/architecture/handoff/roadmap/index, QA and WORKFLOW describe the additive routes and remaining gates. ADR0008 records tagged JSON persistence and bounded multi-step consent.

## Follow-up

Run genuine current-head Android/JUnit and all retained regression groups, add full HTTP/Room/Compose fixtures, repair any findings and provide a final-head debug APK. Owner review and separately authorized visual A/B remain open.
