# Reference-first guided style and existing Trial handoff

## Summary

Phase3A.2 exposes the local acquisition increment through the existing style
editor, preserves optional analysis and verifies the free Save-to-Trial path.

## Why

Pack references can drive generation without a mandatory paid Style DNA stage.
The owner explicitly authorizes this bounded default-workflow change.

## Scope / changed files

Behavior impact: changed
Architecture impact: changed
Milestone impact: changed
Durable decision: recorded

`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStylePanel.kt`
integrates local candidates and explicit method selection.
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleAnalysisPanel.kt`
shares its existing small preview component.
`app/src/main/res/values/reference_style_strings.xml` supplies ordinary-language
guidance; `app/src/main/res/values/ux_style_strings.xml` labels Quick as optional.
`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleGuidedFlowComposeTest.kt`
and `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/GuidedProjectRoomJourneyComposeTest.kt`
extend existing guided and Room coverage.
`app/src/androidTest/kotlin/dev/renkinProject/renkin/offline/StyleDnaEmulatorTest.kt`
adds one free Save/Trial journey and extends restart evidence.
`scripts/run_offline_dna_emulator.py` and `scripts/tests/test_offline_dna_emulator.py`
retain fail-closed evidence validation for all5 journeys.
`.github/workflows/ci.yml`, `.github/workflows/offline-dna.yml` and
`.github/workflows/governance.yml` add only the new stacked PR base.

## Behavior before / after

Fresh guided entry previously prioritized Quick analysis. Reference-first now
uses the existing editable brief, instruction, import, preview, ordering and Save.
Only a fresh unsaved style gets neutral initial text. No visual measurements or
palette/tile assumptions are invented. Existing revisions are not rewritten.
Pack candidates are local; selecting them imports actual PNGs with source proof.
The user selects up to5 and may choose fewer. Text-only manual styles stay possible.
Optional paid analysis requires an explicit method choice and separate existing
consent. Unsaved edits block switching rather than disappearing. The existing
target/original resolver and frozen Trial pipeline need no analysis record.

## Tests actually run

Native Windows/JDK17: the new default regression first executes1/1 RED against
old UI (missing description). The seven-class targeted command executes66 cases,
64 PASS and2 new fixture failures. Corrected guided tests pass8/8; the new Room
journey ultimately executes1/1 PASS in48s including incremental compilation,
retaining restoration, exact byte/order/revision, zero-call, decline and reopen
assertions. Earlier fixture failures are retained: async list readiness, exact
versus substring matching, existing key-presence gate and collapsed Trial options.
Production readiness gates/timeouts/assertions are unchanged. Android test Kotlin
compiles successfully; Python emulator-runner fault guards execute9/9 PASS.
Commands: `:app:testDebugUnitTest --tests "*ProjectStyleGuidedFlowComposeTest"`
and `--tests "*GuidedProjectRoomJourneyComposeTest.referenceFirstPackSavesWithoutAKeyAndPreparesOriginalFirstOrderedTrial"`,
plus existing ProjectStyleCompose/Preparation/OwnedStyleTrials/StyleAnalysis/
StyleTrialJourney classes in the initial deduplicated targeted invocation.
Free Save is checked without a key; only synthetic presence is supplied afterward
for the unchanged Trial preparation UI gate. No provider submission occurs.
Final exact-head full Linux/Python/Android evidence belongs in the new PR audit.
ADR0012 keeps one authoritative full suite, without duplicate cloud/HTTP jobs.

## Not tested

No paid/live provider, physical phone, Samsung/OneUI identity, visual style fidelity,
installed-signature compatibility or release acceptance is claimed.

## Risks

Local diversity is a heuristic, not family recognition. User review determines
which references belong together. Prior intermittent hosted Compose concurrency
risk remains; retained failures must be diagnosed, not hidden by a wider timeout.

## Rollback

Revert this ordinary integration commit to restore presentation. Saved manual
revisions and private reference assets remain compatible; no migration or key
replacement exists. Preserve the independent acquisition increment if desired.

## Documentation updated

Index, architecture/current state, roadmap, handoff, workflow and QA describe the
new path and its limits. ADR0013 records owner authorization and compatibility.

## Follow-up

Complete final-head gates, publish the independently verified debug artifact on
the new draft stacked PR, and wait for owner/device acceptance. Stop before Phase4.
