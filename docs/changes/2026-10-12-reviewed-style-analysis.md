# Codex 04 — bounded references and reviewed style analysis

## Summary

Implement the existing STYLE route from accepted main 6aed9e08 with local reference
sampling, separately confirmed structured OpenAI analysis, durable candidate recovery
and explicit project style review/Save. This initial implementation is not yet
Android-verified or merge-ready; no live request or image generation occurred.

## Why

Codex 03 saved manual project styles but had no source sampler, multimodal analysis
or portable reviewed provenance. A paid suggestion must survive interruption without
rebilling and cannot silently activate or overwrite user instructions/dirty drafts.

## Scope / changed files

Behavior impact: changed
Architecture impact: changed
Milestone impact: changed
Durable decision: recorded

The changes add the bounded read-only reference port, analysis domain/adapter/journal,
existing-session orchestration and STYLE UI. Only two nullable immutable revision
fields change schema (19→20); accepted backup evidence uses format 5/payload v2.
Runner changes retain old selectors and strengthen XML freshness/full-suite proof.
Exact non-document paths in this staged implementation:

- `app/src/main/kotlin/dev/renkinProject/renkin/MainViewModel.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/OpenAiStyleAnalysisGateway.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/PromptComposer.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/ReferenceImages.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/ReferenceSampler.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/ReviewedAnalysisEvidence.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StrictJson.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisCoordinator.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisGateway.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisJournal.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisServices.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleProfileV1.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/apk/ApplicationProvider.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/apk/IconCoverageAnalyzer.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/apk/IconPackRepository.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/apk/ProfileManager.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/DbApplication.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/project/ProjectAiCodec.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/project/ProjectAiData.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/project/ProjectAiRepository.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/transfer/BackupCodec.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/transfer/BackupManager.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/packages/ApplicationManager.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleAnalysis.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectAnalysisBackend.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleAnalysisPanel.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStylePanel.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/PromptComposerTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/ReferenceSamplerTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisFixtures.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisGatewayTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisJournalTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisSchemaTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/data/RenkinPackMigrationTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/data/project/ProjectAiAnalysisPersistenceTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/data/project/ProjectAiMigrationTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleAnalysisTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleAnalysisComposeTest.kt`
- `scripts/run_compose_regressions.py`
- `scripts/tests/test_compose_regressions.py`


## Behavior before / after

Previously STYLE offered manual text/0–5 refs only. It retains that local path and
adds EXTEND all-mapping static sampling or CREATE owned-reference preparation, an
explicit one-request upload/cost confirmation, structured candidate review, protected
Use into draft and separate Save. Invalid/no-key/stale/uncertain outcomes leave active
styles unchanged. Edited spec is USER; unedited accepted analysis is AUTO_ANALYSIS
with typed provenance. No icon generation/apply/build or global AI rewrite follows.

## Tests actually run

Linux local Python governance suite initially passed 45/45 in 13.018 s on accepted
base; with six runner fault-injection tests it passed 51/51 in 15.308 s, exit 0.
The actual runner is exercised with missing/empty/malformed/forged XML, missing
selectors, skips/failures/errors/nonzero Gradle and mandatory full-suite selection.
These synthetic fixtures are not Android/schema evidence. git diff --check passed.
Local ./gradlew compileDebugKotlin testDebugUnitTest assembleDebug FAILED before
configuration downloading Gradle 8.11.1: SocketException Network is unreachable.
Historical M3 718/718 is recorded separately, never reused as this feature's result.

## Not tested

New Kotlin/Compose/Room migration/MockWebServer tests have not yet executed. Hosted
Android build/full and filtered regressions, genuine KSP v20 export and exact-head
CI are required. Device/emulator, TalkBack, One UI/Theme Park/cover/fold/rotation,
installed pack updates, real process/storage interruption, account access, model
quality and real billable endpoint checks are NOT RUN. No actual API key is needed
or read in CI; offline credentials are random in-memory UUIDs.

## Risks

New sampler heuristics may not represent unusual source packs; low/mixed/unsupported
warnings are explicit, never calibrated confidence. Imported/stale evidence needs
fresh context validation. Unreviewed noBackup candidates are device-local and excluded
from full/share backups. Typed provenance needs v20; old APKs cannot open it. Legacy
restore is not cross-store ACID. Runtime account/model/cost/quality are unverified.
Durable consumed markers fail closed at corruption/quota instead of rebilling.

## Rollback

Revert this feature on its branch before acceptance, or deliver a forward repair
for installed v20 databases. Do not install a v19 APK over populated v20 data.
Older-version recovery requires an explicit pre-upgrade compatible full backup,
owning signing continuity and deliberate restore. Classic AI/editor/build pipelines
remain available, but unreviewed local candidates intentionally do not migrate.

## Documentation updated

Canonical state, architecture, roadmap, handoff, QA baseline/matrix, workflow, index
and unreleased changelog explain current code versus verified/pending checks.
ADR 0005 records separate vision/generation limits, durable consent/review, minimal
migration, portable provenance and official API contracts. ADR 0004 status and
pre-merge M3 state are corrected using the owner's actual #4 merge evidence.

## Follow-up

Run hosted isolated groups plus the entire retained suite and debug APK; fix measured
failures without bypass and retain genuine KSP v20 unchanged. Update the same PR
with final-head artifacts/counts and a single evidence-based audit declaration.
Owner/reviewer must accept residual device/provider risks and merge separately.
Codex 05 may only start after acceptance with fresh target/source/ref validation and
new explicit paid confirmation; no trial images, gallery, bulk Apply or edits now.
