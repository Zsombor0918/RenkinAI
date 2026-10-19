# Codex 05 — project style trials with separate cost consent

## Summary

Implement 0–3 project style trials, explicit per-image review/regeneration and
persistent shared-style approval without Apply or batch. This initial head is
source-complete but awaits hosted verification; no live request occurred.

## Why

M4 analysis consent cannot authorize image generation. Paid results must survive
file/Room/marker interruptions without duplicate endpoint charges or loss of
original/RAW history. Existing project assets and variants already provide ownership.

## Scope / changed files

Behavior impact: changed
Architecture impact: changed
Milestone impact: changed
Durable decision: recorded

Use the existing VM/profile/session/asset lease and Room; preserve legacy editor,
AI1/AI2/watch/build/signing/install. Add the narrow v21 evidence/approval migration,
format 6/payload v3 history and five additive regression groups. Exact paths:

- `.github/workflows/compose-regressions.yml`
- `app/src/main/kotlin/dev/renkinProject/renkin/MainViewModel.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/IconGenerationGateway.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/OneShotBody.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/OpenAiImageClient.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/StabilityImageClient.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/PromptComposer.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialCoordinator.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialJournal.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialPlan.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialServices.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/TrialOriginals.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/apk/ProfileManager.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/DbApplication.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/project/ProjectAiCodec.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/project/ProjectAiData.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/project/ProjectAiRepository.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/project/StyleTrialData.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/project/StyleTrialRepository.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/transfer/BackupCodec.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/transfer/BackupManager.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/transfer/ValidatedBackupArchive.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleTrials.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectSetup.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStylePanel.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectTrialBackend.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectTrialPanel.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/RenkinAiRoot.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/TargetSelection.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialFixtures.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialGatewayTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialJournalTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialPlanTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialTargetSelectorTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/data/project/ProjectAiAnalysisPersistenceTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/data/project/ProjectAiMigrationTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/data/project/StyleTrialMigrationTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/data/project/StyleTrialPersistenceTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/data/transfer/ProjectAiBackupTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleTrialsTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleAnalysisComposeTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleComposeTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/StyleTrialComposeTest.kt`
- `scripts/run_compose_regressions.py`
- `scripts/tests/test_compose_regressions.py`

## Behavior before / after

Before, project style and analysis saved descriptions but no trial workflow or
portable approval existed. Now TARGETS/clean STYLE opens TRIAL, fresh eligible
selection freezes 0–3 real originals with saved style/ref/configuration and new
consent. One-shot multipart and durable endpoint counters prevent replay. Local
result recovery inserts idempotent original/RAW variants before REVIEWABLE. Separate
review and approval preserve old versions; M6 gets read-only identical accepted inputs.

## Tests actually run

Fresh local baseline Python guards 51/51 in 6.981 s, expanded 52/52 in 8.458 s.
The strengthened current-schema-negative runner guard was rerun: 52/52 in
7.473 s, exit 0, on this working source checkpoint. Synthetic XML/schema fixtures
are never Android evidence. Local full Gradle failed before configuration at
8.11.1 download with Network is unreachable; no local Android suite or APK.
Source/whitespace review passed; final-head hosted build/full/regressions are required.

## Not tested

New Kotlin/Compose/Room/migration/MockWebServer cases and genuine KSP v21 are
pending hosted execution. No physical device/emulator, provider access/quality/
exact cost, real process/storage/SAF/signing/install/OEM/TalkBack trial occurred.
All test requests use local fake gateways/MockWebServer; live billable requests=0.

## Risks

Image clients receive a shared one-shot correction, so legacy HTTP parity must
remain green. Project/noBackup quotas fail closed and a blocked local save retains
staging. Full restore spans old separate stores and invalidates request epochs;
imported approvals remain historical. Stable suggestions do not promise complexity.

## Rollback

Revert on the unmerged branch or deliver a forward repair for populated v21.
A v20 APK cannot open v21. Older-version recovery requires deliberate compatible
pre-upgrade full-backup recovery with unchanged app/pack signing identities. Do not
remove the no-replay guards, lose historical PNGs or weaken regression gates.

## Documentation updated

CURRENT_STATE/ROADMAP/INDEX/ADR0005 acknowledge actual owner merge of M4.
ARCHITECTURE, ADR0006, HANDOFF and QA describe the new input, endpoint/journal,
result/review/approval/restore and M6 read-only contracts. WORKFLOW preserves old
selectors/checks and explains the additive groups/path filters; README/CHANGELOG
present the explicit user flow and retained scope boundaries.

## Follow-up

Require real hosted compilation, full Android suite, every old/new isolated group,
actual APK, genuine schema 19/20/21 and a complete audit on the final SHA. Fix only
measured failures with meaningful retained assertions. Leave the PR unmerged;
owner/reviewer acceptance and device/provider trials remain separate. M6/M7 are later.
