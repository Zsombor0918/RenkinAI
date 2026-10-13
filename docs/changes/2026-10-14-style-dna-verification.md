# Style DNA D0–D3 verification and handoff

## Summary

Implement pack-specific per-icon reports, independently validated family grammar and target-specific drawing recipes on a separate stacked draft. D0–D3 code is verified; Deep and generated visual-quality acceptance remain deferred.

## Why

The working PR6 aggregate12→1 route could not expose sample-bound observations and family-specific rules. The owner requires24 individually labeled references, explicit missing-resource diagnosis, exclusive target-logo identity and genuine test-first proof before claiming improved style transfer.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

This verification note was introduced in the docs-only checkpoint63e5e5be. The subsequent targeted readiness correction changes tests/docs only and preserves all runtime behavior. The complete accumulated PR7 inventory relative to source baseline454bd5ec is below, including the follow-up paths. Production scope is D0–D3, optional Quick/Standard, legacy compatibility and additive deterministic tests; no M6/M7, live call, main change, merge or force push.

| Change | Literal repository path |
| --- | --- |
| Changed | `.github/workflows/ci.yml` |
| Changed | `.github/workflows/compose-regressions.yml` |
| Changed | `.github/workflows/governance.yml` |
| Changed | `CHANGELOG.md` |
| Changed | `README.md` |
| Changed | `app/src/main/kotlin/dev/renkinProject/renkin/MainViewModel.kt` |
| Changed | `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/OpenAiStyleAnalysisGateway.kt` |
| New | `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/OpenAiStyleDnaGateway.kt` |
| Changed | `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/PackStyleEvidence.kt` |
| Changed | `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/PromptComposer.kt` |
| New | `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/ReferenceDiagnostics.kt` |
| Changed | `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/ReferenceImages.kt` |
| Changed | `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/ReferenceSampler.kt` |
| Changed | `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/ReviewedAnalysisEvidence.kt` |
| Changed | `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisGateway.kt` |
| Changed | `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisJournal.kt` |
| Changed | `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisServices.kt` |
| New | `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaCoordinator.kt` |
| New | `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaJournal.kt` |
| New | `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaPlan.kt` |
| New | `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaSynthesis.kt` |
| New | `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaV1.kt` |
| Changed | `app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialCoordinator.kt` |
| Changed | `app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialJournal.kt` |
| Changed | `app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialPlan.kt` |
| Changed | `app/src/main/kotlin/dev/renkinProject/renkin/apk/IconCoverageAnalyzer.kt` |
| Changed | `app/src/main/kotlin/dev/renkinProject/renkin/apk/IconPackRepository.kt` |
| Changed | `app/src/main/kotlin/dev/renkinProject/renkin/apk/ProfileManager.kt` |
| Changed | `app/src/main/kotlin/dev/renkinProject/renkin/data/transfer/BackupManager.kt` |
| Changed | `app/src/main/kotlin/dev/renkinProject/renkin/packages/ApplicationManager.kt` |
| Changed | `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleAnalysis.kt` |
| Changed | `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleTrials.kt` |
| Changed | `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectAnalysisBackend.kt` |
| Changed | `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleAnalysisPanel.kt` |
| New | `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleDnaPanel.kt` |
| Changed | `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStylePanel.kt` |
| Changed | `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectTrialBackend.kt` |
| Changed | `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectTrialPanel.kt` |
| Changed | `app/src/main/res/values/strings.xml` |
| New | `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaGatewayTest.kt` |
| New | `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaReproductionTest.kt` |
| New | `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaWorkflowTest.kt` |
| Changed | `app/src/test/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialGatewayTest.kt` |
| New | `app/src/test/kotlin/dev/renkinProject/renkin/data/project/StyleDnaPersistenceTest.kt` |
| New | `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/StyleDnaComposeTest.kt` |
| Changed | `docs/ARCHITECTURE.md` |
| Changed | `docs/INDEX.md` |
| Changed | `docs/ROADMAP.md` |
| Changed | `docs/agent/CURRENT_STATE.md` |
| Changed | `docs/agent/HANDOFF.md` |
| Changed | `docs/agent/WORKFLOW.md` |
| New | `docs/changes/2026-10-13-style-dna-implementation.md` |
| New | `docs/changes/2026-10-13-style-dna-integration-fixtures.md` |
| New | `docs/changes/2026-10-13-style-dna-reproduction.md` |
| New | `docs/changes/2026-10-14-style-dna-principal-colors.md` |
| New | `docs/decisions/0008-style-dna-stacked-development.md` |
| Changed | `docs/qa/BASELINE.md` |
| Changed | `docs/qa/TEST_MATRIX.md` |
| Changed | `scripts/run_compose_regressions.py` |
| Changed | `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleTrialJourneyComposeTest.kt` |
| New | `docs/changes/2026-10-14-style-dna-rejection-readiness.md` |
| New | `docs/changes/2026-10-14-style-dna-verification.md` |

## Behavior before / after

Legacy defaults and manual style Save remain available. New D0 inspects up-to600 candidates with safe exclusion counts and measured-color duplicate veto, selecting up-to24 thumbnails. Quick obtains up-to12 labeled reports in one one-shot POST with local synthesis. Standard groups up-to24 into six-image reads and a separate text synthesis, maximum5 explicitly consented endpoint submissions. Typed11-trait reports and15-rule family grammar retain observed/inferred/unknown provenance, color-role prevalence, sampled counts, applicability, must/should/must_not and uncertainty. Human review/edit/Save creates an immutable revision.

M5 optionally freezes reviewed family/symbol-type recipes and DNA fingerprint; the native edit boundary keeps component originalC as IMAGE1/exclusive identity, ordered style-only refs and one transparent1024 PNG contract. No arbitrary modernization/dark tile/gradient is authorized by unknown evidence. Room21, backup6/payload3, old V1 records/plan JSON, old consumed rights, editor/watch/profile/sign/install, legacy AI1/AI2 null-override bytes and non-DNA Stability wire paths remain.

## Tests actually run

Closing63e5e5be actually passed Android/unit925 but failed1/23 in isolated trial-ui while the full149 XML/925 remained green. Independent artifact11856632498,644124bytes,SHA25682ec516e2af3be8bdba3fa42687adaf7b69135a0f8b7635f2f5e4e4f40abbb5b preserves the disabled-before-Room-ready assertion. The follow-up waits for actual enabled semantics in that retained rejection journey within the same10000ms bound; no old assertion, selector, timeout, idling rule or runtime changes. Exact follow-up final-head execution remains required in PR7's audit.

Both owner documents and mandatory repo docs/sources/tests/reviews were read before production changes. Initial b2df166a CI38465737793 and complete regressions38465737896 executed904 tests with six new regression failures and no old failures; independently downloaded artifact11848579461,610761bytes,SHA256e8a59d21a3a321913b48965bdfe6b7e85722f83f08815ba014635bbaeb95f9bd proves the actual red assertions. Implementation2bf8ca7e failed KSP syntax before tests;4531da8a compiled and executed925/1, then principal interior-paint measurement fixed the one duotone failure without weakening its expectation.

Code89eacb0d Android38477038060 passed compileDebugKotlin, testDebugUnitTest and assembleDebug. Full22 regressions38477037827 artifact11857125237 is638786bytes,SHA256076d772e6961d67ef9a50652fe5cdfed117b2cf90451c4964fcff8eea9585f12. Independently149 full-suite XML/925 actual testcase nodes/0 failures/0 errors/0 skipped, all22 command exits0, every selector present and suite counts consistent. Additive DNA26/26, exact-original M5 gateway15/15. Genuine Room19/20/21 exports match tracked bytes;21 identity8c7a3c8b5b26ad29673ebbd77605838e.

New fixtures exercise real MockWebServer labeled low/high/schema/privacy and one-shot error/refusal/partial/timeout boundaries; actual Room history/full-share rekey and owned Save;320dp/dark/restoration/source-change Compose; durable step consumption/cold/delete/no replay; renderer/alpha/state/animation/dark-visible diagnostics. Six synthetic golden cases pass family/palette/recipe checks;50/150/500/1500-mapping scans select24 under300/600 bounds. Actual timings/GC-sensitive JVM heap deltas are in QA BASELINE. These are not generated-image fidelity measurements.

Local Python3.12/JDK17:52/52 guard tests in14.988s; docs origin/main range123paths and commit23subjects pass at code89eacb0d. Local Gradle/full runner stop before configuration at8.11.1 distribution download, Network is unreachable; no local Android success is claimed. Code-head hosted conventions pass; documentation audit rejects the intentionally BLOCKED initial PR body until final gates are complete. The closing docs-only SHA must run all mandatory checks independently; its exact SHA, final fresh XML/APK and completed governance results are recorded in [PR7](https://github.com/Zsombor0918/RenkinAI/pull/7).

## Not tested

Zero live/paid API calls; no credential requested/reused/logged. Real model access, endpoint behavior, dollar pricing, generated pack fidelity, blind held-out comparisons and real Lux/Basit assets are unverified. Deep is disabled. Physical Flip8/One UI/Theme Park/TalkBack/fold, process kill/storage exhaustion, peak native memory and signed installed-data upgrade are NOT RUN. Native/mock/Room fixtures are not phone or provider proof.

## Risks

Quick conservative recurrence may leave UNKNOWN traits; Standard depends on unverified multimodal observations and may fail closed on unsupported grammar. Family paint measurements are bounded heuristics and can be uncertain for soft/thin/gradient artwork. Save and references preserve immutable history; new typed grammar requires a compatible forward reader for safe rollback. CI's ephemeral debug certificate cannot be assumed to match a prior installed build. Neither passing CI nor this proposed ADR constitutes owner review/merge approval.

## Rollback

Use forward repair/revert retaining tagged DNA readers, optional trial fields, old immutable styles/originals/RAW and global consumed step IDs. Do not automatically resume or rebill uncertain requests, erase data, overwrite old art, reuse an incompatible signing key or downgrade a backup into a reader that rejects DNA. Main and PR6 remain unchanged.

## Documentation updated

README, CHANGELOG, INDEX, ROADMAP, CURRENT_STATE, HANDOFF, ARCHITECTURE, WORKFLOW, QA BASELINE/TEST_MATRIX and ADR0008 distinguish verified implementation from experimental quality, preserve historical red checkpoints and point to PR7's exact final-head audit. This new ten-section note supplies the complete file inventory and closed scope.

## Follow-up

Complete this docs-only head's mandatory local attempts and hosted Android/all22/governance checks, independently inspect final XML/schema/APK and deliver the fresh debug APK while leaving PR7 draft/unmerged. Next safe task is free owner device inspection. Before genuinely useful live A/B, prepare exact models/request counts/current expected cost limits and secure app credential setup, then request explicit owner authorization. No paid call or key setup is performed here.
