# Codex 03 — Project AI style, assets and gateway

## Summary

Implement Codex 03 per-profile style/history, immutable private reference PNGs, explicit local UI Save, durable revalidated preparation and a real future image gateway. Existing M1/M2/editor and legacy AI paths remain available.

## Why

Future recognition/trial work needs durable profile-owned references and revision identities. Global AI settings and a deletable Upload name cannot safely serve as project state; source selections must not imply billable permission.

## Scope / changed files

Behavior impact: changed
Architecture impact: changed
Milestone impact: changed
Durable decision: recorded

- `app/src/main/kotlin/dev/renkinProject/renkin/MainViewModel.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/IconGenerationGateway.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/OpenAiImageClient.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/StabilityImageClient.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/DbApplication.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/RenkinPackRepository.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/project/ProjectAiCodec.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/project/ProjectAiData.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/project/ProjectAiRepository.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/project/ProjectAiSchema.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/project/ProjectAssetStore.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/project/ProjectSelection.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/transfer/BackupCodec.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/transfer/BackupManager.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/transfer/ValidatedBackupArchive.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/di/AppModule.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectPreparation.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectSetup.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStylePanel.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/RenkinAiRoot.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/TargetSelection.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/IconGenerationGatewayTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/data/RenkinPackMigrationTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/data/project/ProjectAiMigrationTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/data/project/ProjectAiPersistenceTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/data/transfer/BackupManagerTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/data/transfer/ProjectAiBackupTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectPreparationTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectSelectionComposeTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleComposeTest.kt`
- `scripts/run_compose_regressions.py`

## Behavior before / after

Before: style/references were global legacy settings and M2 selections were transient UI state. After: projects start unset, users explicitly edit/copy/import/reorder/Save their own style and target snapshots; reopening requires source/catalog review. Full backup/share embed the new history/PNGs and remap share ownership. Legacy single/batch generation continues unchanged through its service, not this new gateway.

## Tests actually run

Fresh base aefeb1b69e7757c757c60ac2f00ce2125893e1e6: python3 -m unittest discover -s scripts/tests -v passed 45 tests in 17.344 s. git diff --check and Python runner compilation passed. Local full Gradle command was attempted but failed during Gradle 8.11.1 download with Network is unreachable, before configuration. Initial feature Android execution and generated schema export await hosted CI; no Android success claimed.

## Not tested

Physical Android upgrade, Samsung/One UI/Theme Park/Flip/fold/TalkBack, actual URI providers/SAF, on-device restore/sign/install/update and real API quality/billing are NOT RUN. No live/paid request is authorized or performed. Feature-head Android tests are pending hosted verification.

## Risks

Room v19 prevents blind downgrade to v18 APKs. PNG quota/disk/URI errors must keep old revisions; cross-store restore remains staged, not distributed ACID, even though project records replace transactionally after asset validation. Draft references are locally pinned until explicit discard; static coverage and Android-user ambiguity retain M2 limits. UI/Room/HTTP changes need fresh Android CI before acceptance.

## Rollback

Do not install a v18 APK over v19 data. Prefer a forward fix retaining the additive schema; retain a pre-upgrade full backup for an explicit older-version recovery if necessary. Reverting UI/gateway changes on v19 is possible only with storage-compatible code. Never alter pack signing keystore or package identities.

## Documentation updated

Canonical CURRENT_STATE, ARCHITECTURE, ROADMAP, HANDOFF, WORKFLOW, INDEX, QA baseline/matrix and unreleased CHANGELOG are updated. ADR 0004 records model/file/backup/global/gateway choices. Historical AI handoff and failed M1/M2 results are preserved as historical evidence.

## Follow-up

Complete targeted and full Android/Room export evidence on final PR SHA, retain BLOCKED until mandatory automation is proved, then request owner/reviewer acceptance. Physical-device gaps remain explicit. Only after accepted merge may Codex 04 add its own analysis port and approval; 05 trials, 06 staging, 07 edits and paid planning are not implemented.


Local pre-publication verification: the staged documentation guard passed 43 changed paths. The full governance suite passed again, 45/45 in 18.348 s. No Android execution succeeded locally.
