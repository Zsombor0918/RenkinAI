# Verify active ruleset and record owner acceptance

Date: October 11, 2026. Branch: `chore/agent-docs-governance`, existing PR #1.
Closure baseline: `50ccda65720e70bcb31e361b67f76bba59bbdacc`.
Final commit, fresh checks and actual merge outcome are recorded in the PR audit.

## Summary

Replace obsolete inactive-main statements with verified active ruleset settings.
Record the owner's explicit instruction to finish and merge #1, accept ADR 0001,
and prepare the handoff without asserting a merge before GitHub confirms it.

## Why

The owner created an active main ruleset after the previous P1 fix. Canonical
documents still described protection as inactive and acceptance as pending.
The actual ruleset also requires latest-push approval, which currently blocks #1.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: changed
Durable decision: recorded

- `docs/agent/WORKFLOW.md`: verified main rules and actual merge requirements.
- `docs/agent/HANDOFF.md`: closure scope, owner acceptance and next safe step.
- `docs/ROADMAP.md`: accepted implementation with externally gated merge state.
- `docs/decisions/0001-repository-governance.md`: owner acceptance and verified enforcement.
- `docs/qa/BASELINE.md`: observed current rule/PR state and rechecked prior CI.
- `docs/qa/TEST_MATRIX.md`: active main enforcement and remaining rule differences.
- `docs/changes/2026-10-11-governance-merge-readiness.md`: this closure record.

Only documentation changes. Android source/build/signing, shared validators,
tests, hooks and workflows remain identical to the closure baseline.

## Behavior before / after

Before: current documents incorrectly reported inactive protection and no owner
acceptance. After: they identify active ruleset 24877351, the three required
Actions contexts, no bypasses, and owner acceptance. Latest-push approval is
required even with zero configured approving reviews; no approving GitHub review
exists at this read. Conversation resolution and strict checks are disabled,
so those recommended requirements are not claimed as enabled.

## Tests actually run

Read the current branch, PR, ruleset, review threads and reviews through GitHub.
Main reports protected true; ruleset 24877351 is active and targets main.
Verified the P1 thread resolved and the prior-head jobs successful in governance
run 38134217603 and Android run 38133704315. These are rechecked earlier results,
not new test executions. Source and all CI/guard files are unchanged.
`python3 scripts/check_docs.py --staged` passed for seven changed paths, and
`git diff --check` passed; both returned exit 0 on the closure documentation.

## Not tested

No device, native Windows, live AI request or new local Android test/build run
is part of this documentation closure. Final-head checks must be observed after
pushing. Merge remains unconfirmed until the immediate GitHub merge operation
succeeds; no direct main push or administration change is used to bypass its rules.

## Risks

GitHub blocks #1 because latest-push approval is absent. The owner's chat
instruction authorizes merge but does not create an approving GitHub review.
The saved rule does not require conversation resolution or strict up-to-date
checks; this differs from the recommended setup and is explicitly recorded.
Existing device/provider coverage limitations remain from the earlier audits.

## Rollback

Revert only this documentation closure through a reviewed PR if its observations
become incorrect. Preserve the historical QA records and replace current rule
facts with freshly read values. This commit does not roll back or alter the
external GitHub ruleset, application data, signing identity or audit validation.

## Documentation updated

WORKFLOW and QA describe verified protection rather than an assumed setup.
ROADMAP and ADR separate owner acceptance from GitHub review and merge state.
HANDOFF points the next agent to the live PR result before fetching main and
starting the separately scoped ProjectHome work. No parallel architecture is added.

## Follow-up

Observe final-head required checks, satisfy latest-push approval with an eligible
reviewer, and attempt the owner-authorized merge with an expected head SHA.
Verify PR merged state and main afterward. If GitHub rejects merge, record its
concrete reason in the PR audit and leave the protected rule and branch intact.
