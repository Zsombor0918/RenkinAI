# PR change notes

Every meaningful non-doc PR adds a **new** file named
`YYYY-MM-DD-short-topic.md`. Do not edit an old record to stand in for a new PR.
The local hook compares with HEAD, so meaningful follow-up commits also add a new
related note; CI considers their combined scope as one PR audit.
The small Android-test/docs-only exceptions are defined in [WORKFLOW](../agent/WORKFLOW.md).
Each document explains one reviewable scope; it is an engineering audit record,
not a replacement for user-facing CHANGELOG or canonical current state.

Use these exact level-2 headings, each with concrete prose (at least four words
and twenty characters). Comment-only/unfinished sections are rejected:

1. `Summary`: implemented result and scope boundary.
2. `Why`: concrete reason for the change.
3. `Scope / changed files`: every changed non-doc path literally in backticks,
   including removed/renamed paths; use the four declarations below.
4. `Behavior before / after`: observable differences; separate app from tooling.
5. `Tests actually run`: exact commands/results and environment.
6. `Not tested`: missing build/device/API coverage with reason.
7. `Risks`: regression, persistence, credentials and billing as applicable.
8. `Rollback`: how to revert and any state constraints.
9. `Documentation updated`: canonical docs/ADR updated and why.
10. `Follow-up`: next task and outstanding approval/settings/testing.

In Scope, put each declaration on its own line, with these exact values:

```text
Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none
```

Change values to `changed` for behavior/architecture/milestone, and `recorded` for
a durable decision. These refer to application behavior and architecture; tooling
governance has its own path-mandated WORKFLOW/ADR requirements. Even a `none`
declaration cannot exempt paths mandated by the script. Add explanations below
the declarations; a refactor can affect architecture without user-visible behavior.

See the completed [Codex 00 record](2026-10-11-agent-docs-governance.md). Formal
checks establish structure and consistency, while code review establishes truth.
