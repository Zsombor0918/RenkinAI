# Reject ambiguous PR audit-result declarations

Date: October 11, 2026. Existing branch: `chore/agent-docs-governance`, PR #1.
Follow-up baseline: `ac6dc81f0abbee0ce9f0e43ee66d616761b6dcb1`.
Final commit and fresh Actions results are recorded in the PR audit after pushing.

## Summary

Correct `pr_audit_errors()` to require exactly one well-formed audit declaration
in the complete PR body. Reject BLOCKED, missing, malformed, repeated and
conflicting statuses. Preserve all Android behavior and existing CI/hook wiring.

## Why

[P1 review 4241129872](https://github.com/Zsombor0918/RenkinAI/pull/1#discussion_r4241129872)
showed that the original `re.search` accepted the first PASS and ignored a later
BLOCKED. The explicit blocker could therefore receive a green documentation check.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: recorded

- `scripts/check_docs.py`: count declaration candidates before full-line syntax validation.
- `scripts/tests/test_repo_guards.py`: seven new regression methods using the CI CLI.
- `docs/agent/WORKFLOW.md`: exact single-declaration contract and main-state recheck.
- `docs/agent/HANDOFF.md`: targeted correction, verification boundary and next review step.
- `docs/decisions/0001-repository-governance.md`: rationale and rejected first-match alternative.
- `docs/qa/BASELINE.md`: fresh local results separated from prior CI evidence.
- `docs/qa/TEST_MATRIX.md`: audit-result coverage and unperformed checks.
- `docs/changes/2026-10-11-single-pr-audit-result.md`: this follow-up audit record.

Impacts describe application behavior and architecture, which are unchanged.
The existing governance decision is clarified; its acceptance still requires review.
CURRENT_STATE, ARCHITECTURE and ROADMAP need no unrelated application edits.

## Behavior before / after

Before: the first matching status controlled the result, so PASS followed by
BLOCKED or another PASS could pass. After: all raw declaration candidates count,
including malformed or indented extra lines; exactly one must match the exact
uppercase syntax and must not be BLOCKED. LF/CRLF and trailing horizontal
whitespace are supported. Audit section checks and nonzero failure exits remain.

## Tests actually run

- Before implementation, `python3 -m unittest discover -s scripts/tests -p test_repo_guards.py -k pr_audit -v`
  failed with exit 1 as expected; it reproduced the reported bypass.
- After implementation, `python3 -m unittest discover -s scripts/tests -v`
  passed all 45 tests with exit 0 on Linux, Python 3.12.14 and Git 2.51.1.
  The seven added methods test actual committed-diff/event CLI invocations,
  valid PASS/PASS WITH RISKS with LF/CRLF, BLOCKED, missing declarations,
  same-status duplicates, all conflicting-status orders and malformed extras.
  Existing hook/CI parity and failure-exit tests also passed.
- `python3 scripts/check_docs.py --staged --event-file ../p1-pr-event.json`,
  `python3 scripts/check_commit_messages.py --subject 'fix(governance): require exactly one PR audit declaration'`
  and `git diff --cached --check` passed with exit 0; the staged audit covers eight
  paths and the fetched PR body. Protected application/build/workflow/hook paths
  have no staged diff.

## Not tested

No new local Android build, physical device/emulator, native Windows host or
billable AI request was used for this tooling-only correction. Fresh-head GitHub
Actions results must be observed after pushing; prior successes are historical.
The connector cannot read the direct administration-protected branch-protection
endpoint or write protection settings. No administration setting is changed.

## Risks

PR authors must remove stale declaration lines, including lines inside examples;
an ambiguous body now fails regardless of order or repeated identical values.
Text validation still does not establish factual truth or reviewer approval.
The main API still reports protected false and no required checks; rulesets is
empty after the reported manual setup, so enforcement cannot be claimed active.

## Rollback

Revert this one follow-up commit through review on the existing PR branch. This
would reintroduce the P1 bypass; keep the PR blocked until a replacement correction
passes. No persisted application state, schema, signing key or APK identity changes.

## Documentation updated

WORKFLOW describes the corrected rule; ADR 0001 explains counting candidates
before validating syntax. HANDOFF records the scope and review continuation.
BASELINE and TEST_MATRIX distinguish fresh guard evidence from historical Android
CI results. The PR audit is updated with the exact commit and observed CI results.

## Follow-up

Verify new-head governance and Android Actions results, confirm the original P1
review thread is resolved, and obtain owner/reviewer acceptance without merging
automatically. Verify the owner's saved main rule targets main and is active.
ProjectHome and new AI work remain separate authorized tasks outside this correction.
