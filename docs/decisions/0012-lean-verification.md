# ADR0012 - one authoritative full suite with explicit targeted diagnostics

Date: October 17, 2026. Status: owner-authorized execution policy; implementation review pending.

## Context and authorization

The owner accepted the independent architecture/test-strategy audit and explicitly
authorized Phase1 of RenkinAI_Astra_Medium_Phased_Implementation_2026-10-17.md.
This amends ADR0009/0010 and older additive scheduling promises, not safety coverage
or the requirement for honest final-head evidence and explicit owner approval.

At ebc5ef95, retained hosted XML has 1013 unique cases in 156 classes. The 23
focused groups execute 420 cases (412 distinct); their full-suite group adds1013.
Separate CI adds1013 and separate HTTP adds14. These are 2460 executions, not
2460 distinct requirements. No Kotlin test method is removed by this decision.

## Decision

CI/build is the single authoritative full-suite job. The existing strict runner
executes compileDebugKotlin, testDebugUnitTest and assembleDebug once, validates
actual XML and all catalogued selectors, and retains reports/logs/Room exports on
success or failure. HEAD and working-tree dirtiness accompany the selected runs.
No skips, absent XML, forged counts, stale XML or missing selectors can pass.

The catalog retains all24 names for compatibility. Explicit repeated --group
options form one ordered selector union; creation/ingress/both overlap and the
ProjectSelection class shared by guided-ux/project-style-ui execute once in that
union. Mixing full-suite with filtered groups is rejected. The no-argument default
is full-suite alone. No every-group automatic invocation remains.

The Profile form workflow becomes dispatch-only --order-check: creation1,
ingress1, both2, shared-form3 at this baseline. Require this additional seven-case
diagnostic for shared UI harness/order changes and pre-merge/release review.
It intentionally tests isolation and combination; it does not rerun the full suite.
Select the actual feature branch when dispatching and retain SHA/result.

Remove the separate automatic HTTP job: both classes and all14 cases run in
CI/build. The offline-dna-http group remains available for focused development.
Retain the separate real Android fixture and strict Python/governance gates.
Do not conceal the hosted Standard/restart failure or infer phone/visual success.

| Schedule at unchanged 1013-case baseline | Before | After |
| --- | ---: | ---: |
| CI/build full unit cases | 1013 | 1013 |
| Automatic targeted regression executions | 420 | 0 |
| Second full suite in regression runner | 1013 | 0 |
| Separate HTTP executions | 14 | 0 |
| Total automatic JUnit executions | 2460 | 1013 |
| Unique JUnit cases covered | 1013 | 1013 |
| Explicit order diagnostic (additional when required) | included above | 7 |

The automatic reduction is1447 executions (58.8%) and 26 Gradle unit invocations
to1. Android/Python and deliberate development diagnostics are excluded. These
are schedule counts at the audited baseline, not measured time/money/token savings.

## Alternatives, limits and rollback

Deleting safety tests or accepting a mocked APK as full validation is rejected.
Full coverage is mandatory even for narrow changes before review. Historical
isolated evidence remains valid only for its own SHA. Dispatch-only diagnostics
need an explicit reviewer checklist; no branch-protection change is claimed.
Changes to signatures, migrations, manual/legacy workflows, original identity,
frozen payloads, consumed markers and data recovery remain protected by the full
suite. Revert scheduling through an ordinary reviewed commit if execution gaps
are demonstrated; do not rewrite history or remove failure records.
