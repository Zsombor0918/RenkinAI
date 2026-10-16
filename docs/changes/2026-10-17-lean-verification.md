# Lean verification scheduling with retained coverage

## Summary

Implement owner-authorized Phase1: one authoritative full unit/build invocation,
explicit deduplicated subsystem selection, separate order diagnostic and Android.

## Why

At ebc5ef95, 1013 unique JUnit cases execute2460 times automatically. Extra runs
are scheduling duplication, not additional safety requirements. Preserve all cases.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: recorded

`scripts/run_compose_regressions.py`, `scripts/tests/test_compose_regressions.py`,
`.github/workflows/ci.yml`, `.github/workflows/compose-regressions.yml` and
`.github/workflows/offline-dna.yml` change scheduling/report retention only.
AGENTS, CONTRIBUTING, WORKFLOW, TEST_MATRIX, OFFLINE_STYLE_DNA, CURRENT_STATE,
HANDOFF and INDEX document the policy. ADR0012 amends ADR0009/0010 scheduling.
No Kotlin, application data, signing, credential or provider code changes.

## Behavior before / after

Normal PR execution drops from26 unit invocations/2460 cases to1/1013 at the
unchanged baseline. All24 selector names remain. Multiple --group options execute
one distinct union; full-suite cannot be combined with targeted groups. Explicit
--order-check retains isolated creation/ingress/together/shared-form (7 executions)
for UI-order changes and pre-merge/release review. HTTP runs in the full suite.
The report validator, schemas19-21 and Android gate remain strict and mandatory.

## Tests actually run

Two focused RED tests first errored because the selection API did not exist.
After implementation, all11 runner tests passed in0.198s. An additional workflow
schedule guard joins the full Python guard run; final observed result is recorded
on PR8. Existing fault injection still rejects missing/stale/forged/skipped/failed
XML and missing schema/selector evidence. No Kotlin assertion was removed.

## Not tested

Hosted final-head unit/build/Android execution remains pending at this checkpoint.
No phone, paid/live provider, visual fidelity or installed-signer acceptance runs.
The previous six Windows failures remain baseline evidence, not new execution.

## Risks

Manual order diagnostics require explicit scheduling before owner merge/release
review. The shared hosted Trial reload starvation remains unresolved and BLOCKED;
removing duplicate HTTP invocations does not make it pass. No protection settings
are changed. Current GitHub branch-protection REST lookup returns404; repository
rulesets were not inferred from that response.

## Rollback

Revert orchestration through a reviewed feature-branch commit if a real coverage
gap is found. Keep all historical traces, tests, data and consumed request markers.

## Documentation updated

ADR0012 gives the exact before/after inventory and owner approval. Canonical
workflow/test documentation supersedes old scheduling promises while preserving
historical outcomes. The PR audit will identify exact committed-head evidence.

## Follow-up

Phase2 permits one causally targeted reload-origin diagnostic and only a proven
correction. If unresolved, retain BLOCKED. Phases3/4 are separate reference-first
UX and versioned concise-prompt changes; no paid quality experiment is authorized.
