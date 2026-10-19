# Retire obsolete active style-analysis workflows

## Summary

Implement independently testable Phase0 of the owner-authorized Phase0–3 effort
from7db94f1e on the existing feat/reference-first-prompt-v2 and draft PR10.
Remove exclusive Legacy/Quick/Standard machinery; keep active manual/reference
Save and Trial. Phases1–3 montage features are not implemented in this checkpoint.

## Why

The owner supersedes the old analysis product direction. Keeping unused provider
services and UI in parallel would retain complexity without an active product
requirement. Dependency inspection separates historical readers and shared
billing/data safeguards before removing generation-only implementations.

## Scope / changed files

Behavior impact: changed
Architecture impact: changed
Milestone impact: changed
Durable decision: recorded

`app/src/androidTest/kotlin/dev/renkinProject/renkin/offline/StyleDnaEmulatorTest.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/MainViewModel.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/OpenAiStyleAnalysisGateway.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/OpenAiStyleDnaGateway.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisCoordinator.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisGateway.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisServices.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaCoordinator.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaJournal.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaSynthesis.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaV1.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleProfileV1.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/LocalReferenceThumbnail.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/LocalStyleReferences.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleAnalysis.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleTrials.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectAnalysisBackend.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleAnalysisPanel.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleDnaPanel.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStylePanel.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectTrialBackend.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectTrialPanel.kt`
`app/src/main/res/values/reference_style_strings.xml`
`app/src/main/res/values/strings.xml`
`app/src/main/res/values/ux_style_strings.xml`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/HistoricalDnaFixtureSynthesis.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisGatewayTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisJournalTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisSchemaTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaGatewayTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaIncompleteReasonTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaOfflineHarness.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaOfflineHttpTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaWorkflowTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/data/project/ProjectAiAnalysisPersistenceTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/data/project/StyleDnaPersistenceTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/GuidedProjectRoomJourneyComposeTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleAnalysisTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleTrialsTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleAnalysisComposeTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleGuidedFlowComposeTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleTrialJourneyComposeTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/StyleDnaComposeTest.kt`
`scripts/run_compose_regressions.py`
`scripts/run_offline_dna_emulator.py`
`scripts/tests/test_compose_regressions.py`
`scripts/tests/test_offline_dna_emulator.py`

The exact retired files, case identities, dependency classes and remaining work
are recorded in docs/qa/PHASE0_RETIREMENT.md. No original Renkin infrastructure,
AI1/AI2/Global Batch behavior, schema/migration, signing or protected branch edit.

## Behavior before / after

Before: optional Legacy/Quick/Standard UI could prepare and submit analyses.
After: those entry points, provider transports, coordinators and exclusive owned
state machine are removed. Local manual/imported/source reference style Save and
Trial preparation remain. Shared thumbnail/owner port is extracted; old grammar
and journal readers/fingerprints, asset history and consumed IDs remain. Stored
selected-family references remain usable without a new analysis. Runtime local
synthesis and unused outbound schema constructors are removed, not reader codecs.
No replacement paid endpoint exists yet. Future montage work needs its own stable
saved provenance and generation version; V1/V2 are not edited in place.

## Tests actually run

Windows/JDK17: expected1/1 UI retirement RED before production removal. Production
compileDebugKotlin PASS. Changed unit and Android test Kotlin compile PASS after
correcting two test-only compilation references. Focused Gradle testDebugUnitTest
with14 class selectors executes124 cases:117 pass, six removed-toggle fixture
failures and one known Windows canonical-symlink baseline failure. The fixture
then awaits its real enabled form within the existing10-second bound; corrected
ProjectStyleTrialJourneyComposeTest7/7 PASS in47s. No sleep, timeout enlargement,
retry click or readiness gate bypass. Archived DNA image HTTP, journal/backup,
V1/V2 golden/hash/consent/replay and owned reload/source guards actually pass.
Strict Python67/67 PASS in56.295s after supplying existing Git sh on PATH; the
initial invocation lacked sh and its2 hook-execution errors are not called PASS.
Final committed-head Linux/build, Android and order diagnostics are recorded in
PR10 after actual execution; no repeated full local/hosted suite.

## Not tested

No live/paid analysis or generation, no stored key or physical phone access.
Montage acquisition, one-shot analysis and original+montage generation are not
implemented or tested. Visual fidelity, Samsung/OEM icon provenance and installed
update signing compatibility remain owner acceptance work. Baseline Windows
symlink failure is retained for authoritative Linux execution, never skipped.

## Risks

Analysis creation becomes unavailable intentionally. Historical styles and
request records depend on retained readers/compiler/journals; do not remove them
without saved-data evidence. Test-only archived-fixture synthesis is excluded
from production. Debug CI signing may differ across artifacts and must be checked;
no key is replaced and no uninstall/data reset is permitted. This checkpoint
cannot satisfy the complete new EXTEND product acceptance yet.

## Rollback

Use a normal reviewed forward repair or revert on this feature branch. No storage
migration is needed. Preserve all saved revisions, references, variants, frozen
V1/V2 contents and consumed/uncertain request markers. Never replay an old request
or clear data to recover obsolete UI.

## Documentation updated

ADR0015 records explicit owner authorization to retire obsolete feature tests,
amending earlier case-retention only. AGENTS, index, architecture, state, roadmap,
handoff, workflow, baseline and QA identify the current removal boundary and
remaining montage checkpoints. The retirement inventory lists every old/replaced
case, replacement coverage and preserved shared safety.

## Follow-up

Continue sequentially with Phase1 local20–30-icon montage acquisition/persistence,
then Phase2 one separately consented analysis, then Phase3 stable random eligible
Trials and immutable original+montage requests/navigation. Keep PR10 draft/open;
Phase0 final-head evidence is not completion of Phases1–3 or release approval.
