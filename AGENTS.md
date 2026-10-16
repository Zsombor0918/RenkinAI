# RenkinAI agent instructions

## Owner-approved lean verification (October 17, 2026)

This policy supersedes earlier requirements below to execute every historical group
on every PR. It changes scheduling only; all Kotlin cases/assertions remain.
[ADR0012](docs/decisions/0012-lean-verification.md) records the owner authorization and exact execution inventory.
CI/build alone runs the full unit suite, compile and debug assembly through
`python scripts/run_compose_regressions.py --group full-suite`, retaining exact-head
XML/HTML/logs, required selectors and real Room19/20/21 exports. HTTP contracts are
included there, not in a second automatic job. Android instrumentation and strict
Python/governance checks remain separate; hosted Standard/restart remain BLOCKED.
For development use `--group trial-persistence-session` (or another existing name);
repeat `--group` to run a deduplicated union in one Gradle invocation. No argument
also selects only full-suite. `--order-check` explicitly runs creation, ingress,
both and shared-form separately; run it for shared UI harness/order changes and
before owner pre-merge/release review, via the dispatch-only Profile form workflow.
Record its exact SHA and result; a missing diagnostic is NOT RUN, never PASS.
Older results below remain historical, not final-head evidence. No safety/data,
billing, signing, API, test failure condition or owner-review requirement is waived.


These rules apply to the whole repository. RenkinAI extends Renkin with manually
authorized AI icon creation while preserving the existing on-device icon-pack pipeline.
This repository is GPLv3; retain upstream attribution and library identities.

## Before work

1. Inspect `git status --short --branch`, `git rev-parse HEAD`, the current branch,
   upstream refs, and existing PRs. Fetch the current remote state; do not assume
   that a previous audit describes today's main. Preserve other people's changes.
2. Read [documentation index](docs/INDEX.md), [current state](docs/agent/CURRENT_STATE.md),
   [roadmap](docs/ROADMAP.md), and [handoff](docs/agent/HANDOFF.md), in that order.
3. Read the relevant [architecture](docs/ARCHITECTURE.md), decisions and actual
   Kotlin/build/manifest sources. Historical handoffs are evidence of earlier work,
   not current test results. Resolve contradictions by inspecting current code.
4. State the exact scope, acceptance criteria, required checks, and preserved paths.
5. Preserve installed-app/component identity, original/manual/imported icons,
   modifiers and immutable bases, profiles/preferences, Room migrations, backup,
   watch/provenance locks, calendar/adaptive/clock exports, APK/signing/install.
   Preserve the fork application IDs, generated pack IDs, smali class names and
   both kinds of signing key. Never silently retry a potentially billable request.
6. Work on a separate branch. Install the hooks from [CONTRIBUTING](CONTRIBUTING.md).
   Do not implement adjacent roadmap work without authorization.

## During and after work

1. Keep commits and PR titles in `type(scope): short imperative summary` form,
   in English, with a lowercase start and at most 72 ASCII characters. Existing
   history is exempt. See [workflow](docs/agent/WORKFLOW.md) for guard behavior.
2. Update relevant canonical documents; add a new [change note](docs/changes/README.md)
   for every meaningful PR and each meaningful staged follow-up commit (the local
   hook compares with HEAD). State/milestone/architecture/durable-decision impacts
   must be explicit. Make long-term decisions in numbered ADRs.
3. Run the required checks on the final change. Record commands, environment,
   observed result and missing coverage. Never call an attempted build a passed
   build or repeat a historical test count as a fresh run.
4. Update HANDOFF with the closed scope, source baseline, open risks and next safe
   task. Include the exact final commit in the PR audit after committing (a file
   cannot contain its own commit hash). Make the work independently reviewable.
5. Open a PR with the complete [audit template](.github/PULL_REQUEST_TEMPLATE.md).
   `BLOCKED` cannot merge. `PASS` and `PASS WITH RISKS` still require an authorized
   reviewer/repository owner's explicit decision. Green CI is not a code review.
6. Do not push directly to main, force-push main, auto-merge, publish, or make live
   AI requests as a side effect of testing. Never commit credentials or keystores.

If source access, SDK/network, device coverage or administration permissions are
missing, finish the independent work and report the concrete limitation. Do not
claim branch protection, device compatibility or release approval without evidence.
