# ADR 0001 — repository governance and agent context

Date: October 11, 2026. Status: accepted by the repository owner on October 11,
2026 through the explicit instruction to finish and merge PR #1.
Applies to development infrastructure; no Android architecture is replaced.

## Context

The AI fork had a detailed inherited architecture, a Hungarian usage guide and a
dated handoff, but lacked a canonical current state, work queue, fresh QA ledger,
commit convention enforcement and PR documentation gates. Some inherited identity
and build-lock claims conflicted with actual source. Local AGENTS was ignored.

## Decision

Track shared AGENTS and use INDEX → CURRENT_STATE → ROADMAP → HANDOFF for agent
entry. Retain ARCHITECTURE as the sole architectural description, correct current
facts, and label historical handoffs/specs. Require new structured PR change notes
and explicit behavior/architecture/milestone/durable-decision impact declarations.

Use Python standard-library guards as the common rules for Git hooks and read-only
PR CI. Local docs checks read the staged index; CI uses merge-base/head snapshots.
Validate all new commit subjects and the PR title, without rewriting old history.
Require owner/reviewer acceptance separately from a declared PASS and green CI.
Main enforcement belongs to GitHub Rulesets/Branch Protection, never a local hook.

The October 11 P1 follow-up enforces the existing single audit-result contract:
count every raw line beginning with `Audit result:` (also detecting indentation),
then validate the one remaining line against the exact uppercase status syntax.
Reject duplicates even when their statuses agree; reject malformed extra lines
and BLOCKED. LF/CRLF and trailing horizontal whitespace remain supported.
This is a governance correction, with no application architecture change.

## Alternatives considered

- Documentation-only convention: low effort, but drift and undocumented code cannot
  be automatically blocked.
- Local hooks alone: fast feedback, but clones may omit them and users can bypass.
- Node/third-party linters: useful richer Markdown support, but unnecessary network
  dependencies for the requested quick offline checks.
- Require every document on every edit: creates irrelevant churn; use conservative
  path mapping plus explicit impacts and bounded docs/small-test exceptions.
- Infer meaning from paths alone: insufficient for milestone/business decisions;
  declarations plus human review are retained.
- Accept the first audit result: rejected because a later BLOCKED declaration
  would be ignored. Counting only well-formed matches is also insufficient because
  an invalid second declaration could hide an ambiguous audit. Count candidates
  before validating syntax instead.

## Consequences and acceptance

The scripts verify existence, shape, literal paths and local links, not factual
accuracy or English grammar. New guard/workflow changes require this decision or
another numbered ADR and independent review. No credential, billable provider call,
Room migration, APK pipeline or existing Android CI behavior changes here.

Main ruleset 24877351 is now verified active; the owner configured it outside this
integration. [WORKFLOW](../agent/WORKFLOW.md) records the exact settings, including
latest-push approval, disabled conversation resolution and non-strict status checks.
Owner acceptance of this decision is distinct from a GitHub approving review.
The connector must satisfy the saved merge rules; no bypass or review is fabricated.
