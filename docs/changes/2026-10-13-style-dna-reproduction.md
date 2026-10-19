# Style DNA regression checkpoint

## Summary

Add test-first Style DNA contract reproductions on a separate stacked branch.

## Why

PR6 aggregates broken samples and lacks per-image family grammar and executable drawing rules.

## Scope / changed files

`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaReproductionTest.kt`
`.github/workflows/ci.yml`
`.github/workflows/compose-regressions.yml`
`.github/workflows/governance.yml`

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: recorded

Workflow filters only add the stacked base; unchanged jobs still enforce all existing checks.

## Behavior before / after

Production source remains identical to454bd5ec. New assertions expose unsupported development requirements without requiring missing production types to compile.

## Tests actually run

Read both owner documents, required repository sources and current PR reviews. Staged whitespace and shared Python gates execute before commit; hosted regression assertions must be observed next.

## Not tested

New Android assertions have not yet executed. No SDK or dependency download is available locally, and no live API call is authorized.

## Risks

The deliberately red draft is blocked. Reflection is limited to this compatibility reproduction; subsequent integration tests use typed APIs.

## Rollback

Revert this test and additive CI-filter commit without changing persisted app data. Main and PR6 are untouched.

## Documentation updated

HANDOFF and BASELINE record test-first scope; WORKFLOW and ADR0008 explain stacked gating.

## Follow-up

Inspect actual hosted failures before implementing the local diagnostics, reports, grammar and trial compiler.
