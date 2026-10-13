# Style DNA integration fixtures and syntax repair

## Summary

Repair the new optional plan encoder syntax and add real transport, persistence and UI integration fixtures.

## Why

The first implementation CI stopped before assertions at an extra closing parenthesis. Integration must verify every paid and durable boundary, beyond schema existence.

## Scope / changed files


Behavior impact: changed
Architecture impact: changed
Milestone impact: none
Durable decision: none

`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/ReferenceSampler.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaJournal.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialPlan.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleDnaPanel.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaGatewayTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaWorkflowTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialGatewayTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/data/project/StyleDnaPersistenceTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/StyleDnaComposeTest.kt`
`scripts/run_compose_regressions.py`

## Behavior before / after

Optional recipe JSON now parses syntactically. Frozen-route confirmation disables stale source consent; all other additions strengthen deterministic tests, minority sampling and safe companion-file checks. Legacy-only interfaces and every existing assertion remain.

## Tests actually run

Observed CI38469333350 at2bf8ca7e stopped in KSP with StyleTrialPlan.kt:117 unexpected tokens; no Android test count or APK. Original b2df166a red904/6 remains independent evidence. Local Python guards execute for this runner change before commit; new hosted fixtures require their own execution.

## Not tested

Corrected Android assertions and APK have not yet executed. No live model/account/billing, key, actual Lux resources, physical phone, blind style fidelity or install was tested.

## Risks

Synthetic measurements do not establish peak native memory or device latency. UI/mock/Room assertions are not visual-quality proof. Changes retain bounded one-shot consent and consumed IDs.

## Rollback

Use a forward repair retaining all tagged grammar readers, old V1 history, optional trial fingerprints and consumed rights. Do not replay uncertain endpoints or erase data.

## Documentation updated

Current state, HANDOFF, ARCHITECTURE, WORKFLOW and QA retain the failed checkpoint and describe the additive test coverage accurately.

## Follow-up

Execute genuine final-head Android, all22 groups and governance; inspect JUnit XML, benchmarks, schema exports and debug APK before handoff.
