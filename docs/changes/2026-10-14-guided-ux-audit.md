# Guided UX evidence audit and Stage 1 scope

## Summary

Stage 0 audits the owner screenshots and verified Style DNA baseline before runtime
edits. Stage 1 implements the existing four-step journey; Stage 2 fidelity experiments
and Stage 3 cleanup remain plans. Baseline is
`2f422ca8dbd58174491c954826350bac29bc6dd2`, stacked above PR6 `454bd5ec`.

## Why

All eight private owner screenshots were inspected outside Git. They show exposed
component/status details, duplicate review/save instructions and repeated trial cards.
Their README correctly says the installed APK identity is unknown. Visual mismatch
in Munch/PADTHAI is observed evidence, not a proven provider transport defect.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

This note records investigation before production edits. Additive test-first paths:
`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectSelectionComposeTest.kt`,
`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleGuidedFlowComposeTest.kt`,
`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/GuidedTrialComposeTest.kt`,
`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectGuidedNavigationComposeTest.kt`.
Implementation paths and results belong in the subsequent implementation note.

## Behavior before / after

| Symptom | Evidence and location | Smallest justified fix / proposed UI | Regression risk |
| --- | --- | --- | --- |
| DNA and aggregate paths compete | `ProjectStyleAnalysisPanel` initializes LEGACY and renders both panels; screenshot545/547 | One selected analysis path; Quick recommended for fresh DNA-capable projects, historic aggregate retained explicitly | Existing saved style and pending records must not migrate or rebill |
| DNA save leaves another Save | `ProjectStylePanel.onDnaSaved` updates local draft but lacks navigation callback; screenshot551 shows aggregate Use/Save burden | Notify parent only after immutable revision persistence; one save-and-continue action | Async completion, dirty instruction, historical revision and owner checks |
| Reopened saved targets look unselected | `TargetSelection` starts empty; saved keys load only via review button | Hydrate only a current saved selection without a write; stale input still needs review | Catalog/source/lock invalidation must not silently refresh approval |
| Target save needs a return trip | TARGETS/STYLE/TRIAL routes carry independent visible instructions; screenshot553 | Guided source summary, style, persisted target choice then trials, completion | Preserve classic/editor and external-intent priority |
| Repeated raw trial history dominates | `ProjectTrialPanel` sequential records/variants/options; screenshots536/538/540/553 | Current results with names and readable review; expandable complete history/details | Older unreviewed successes still block approval |
| Historic result and current error coexist | `OwnedStyleAnalysis` holds last record separately from current operation error; screenshot547/549 | Label failed current preparation separately from retained successful candidate | Never hide saved history or treat a failed attempt as saved |
| Lux excludes176 mappings | Screenshot545 and sampler missing-drawable classification | Preserve counts under Details; explain excluded samples, without claiming176 pack defects | Real installed resources require device reproduction |
| Artwork fidelity insufficient | Munch/PADTHAI screenshot comparison | Stage2 owner-authorized blind A/B plan only | Synthetic tests cannot certify real generated style |

Before: project/source → targets → style (parallel DNA/aggregate) → separate save →
targets/save roundtrip → all trial history → separate approval.
After Stage1: project/source summary → one style analysis/review/save → persisted
target choice and separate trial consent/review → truthful completion.

Ownership map: Compose presentation → shell step and repository-derived readiness →
`OwnedStyleAnalysis`/`OwnedStyleTrials` → existing session mutex/asset lease → Room
immutable revisions and separate noBackup journals → local sampler or explicitly
consented one-shot gateway → historical original/RAW variants. No second database,
request coordinator or session owner is justified.

## Tests actually run

Windows PowerShell repository status, fetch, remote refs, PR6/PR7 metadata/comments and
all screenshot inspection completed. Main checkout is clean at `af7bdb10`; isolated
`feat/ux-flow-stabilization` worktree starts at verified PR7 head. Before setup,
`gradlew.bat --version` initially failed because no full JDK/SDK existed. With the
owner-authorized portable toolchain, the test-first command ran before runtime edits:
`gradlew.bat testDebugUnitTest --tests dev.renkinProject.renkin.ui.project.ProjectStyleGuidedFlowComposeTest --tests dev.renkinProject.renkin.ui.project.GuidedTrialComposeTest --tests dev.renkinProject.renkin.ui.project.ProjectSelectionComposeTest --tests dev.renkinProject.renkin.ui.project.ProjectGuidedNavigationComposeTest --console=plain`.
Actual XML: four suites, 17 tests, 11 failures, zero errors/skips; five retained
selection tests passed. Production/test compilation completed in this native run.

A second pre-edit command selected
`OwnedStyleAnalysisTest.staleDnaHistoryDoesNotInvalidateACurrentLegacyCandidate`,
`OwnedStyleTrialsTest.openingAnotherProjectDuringAConfirmedRequestEventuallyLoadsItLocally`
and `GuidedTrialComposeTest.currentVerificationErrorCannotExposePreviousReadyOrApproval`
with their full package prefixes using the same Gradle task. Actual XML: three
suites/tests, three failures, zero errors/skips. Logs, source SHA and original XML
are retained outside Git under the portable tools folder in `ux-red-baseline` and
`ux-red-owned-baseline`. The direct trial-readiness assertion reached its intended
boundary. Inspection of the later XML established that the two owned-controller
cases failed earlier at journal submission/setup, before the intended validity or
cross-profile assertion. Their failing status alone therefore does not prove those
two causes; local journal-platform investigation and subsequent checks are separate.
None of these failures establishes the cause of unsuitable real provider artwork.

## Not tested

No physical device, emulator or paid provider was used. The initial missing build
prerequisites were resolved with the owner's portable-tools authorization. Windows
Developer Mode was enabled by the owner; the actual Java symbolic-link probe then
passed, retaining the original persistence-security test. No system setting was
changed by the implementation.

## Risks

Retain exact launcher/component identity, genuine original IMAGE1, style-only refs,
legacy AI1/AI2/Stability bytes, modifiers/immutable bases, profiles, migrations/backup,
watch/locks, calendar/adaptive/clock, app/pack IDs and both signing keys. No paid request,
automatic replay, apply, build/install side effect or data deletion is authorized.

## Rollback

Investigation and additive tests change no stored data. Stage1 must preserve tagged
DNA readers, immutable history, consumed UUIDs and existing signing continuity; use
a reviewed forward repair for populated v21 rather than a destructive downgrade.

## Documentation updated

This audit separates source-proven seams, screenshot usability evidence and unverified
device/provider hypotheses. The [Stage1 implementation](2026-10-14-guided-ux-implementation.md)
records final behavior and local verification without replacing historical evidence.

## Follow-up

The observed red gates informed Stage1. Final-head checks, local APK and the stacked
draft audit accompany the implementation. Keep PR6/PR7/main unchanged and require
explicit owner review. Stage2/3 remain planned in the [owner check](../qa/GUIDED_UX_OWNER_CHECK.md).
