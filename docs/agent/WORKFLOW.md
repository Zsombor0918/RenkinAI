# Agent, Git and review workflow

Start with [AGENTS.md](../../AGENTS.md); setup is in [CONTRIBUTING](../../CONTRIBUTING.md).
The shared scripts own the automatic rules. This document explains their limits
and the external settings required to turn checks into enforced merge conditions.

## One work item

Inspect current branch/HEAD/tree/PRs, read the index/state/roadmap/handoff, inspect
related source, define scope and preserve the Renkin integration paths. Work on a
topic branch. Implement, update canonical docs, add a dated change note, run tests,
update handoff, commit with a validated subject, and open an audited PR. Report the
base and final head SHA. Leave reviewer acceptance and merge to the authorized owner.

## Profile form regressions

The read-only PR workflow `compose-regressions.yml` runs
`python3 scripts/run_compose_regressions.py` on the exact PR head: both formerly
blocked real-form shell tests individually, together, then the shared-form class.
It additionally runs CreateExtendFlowComposeTest and the coverage domain/parser/render fixtures separately, without narrowing any original selector. It retains per-case XML/HTML/logs and `build/compose-regressions/summary.json` with
HEAD, exit codes and counts. QA-document changes also trigger this workflow so a final evidence commit receives its own complete regression run; prior-head green checks are not substituted. Nonzero Gradle exit, missing results, failure/error or
skipped tests fail the runner/job. The full Android CI still runs the entire suite
and debug build; filtered regressions do not replace it or the three required checks.
After the filtered runs the runner executes the unfiltered
`./gradlew compileDebugKotlin testDebugUnitTest assembleDebug` command, retains
complete suite XML/counts, and verifies that requested selectors occurred. The
additional complete run also checks order dependence after the filtered runs.

Native SDK 33 form tests use 320 dp width to avoid the measured Robolectric
WRAP_CONTENT custom-width loop. Wide/hinge Home tests remain unchanged; wider-device
form/TalkBack checks remain manual. No production dialog sizing, clock, timeout or
idling behavior was changed. Temporary probes/reflection, draw/window overrides
and known-negative timeout cases were removed after capturing their evidence.
See the [correction record](../changes/2026-10-11-compose-dialog-regression-fix.md).

## Automatic documentation minimum

`check_docs.py --staged` reads Git blobs from the index and compares with HEAD.
`--base BASE --head HEAD` reads committed head blobs and compares with the Git
merge-base, so unrelated changes on a diverged base do not count as PR changes.
Each meaningful staged commit needs a new note relative to HEAD; a multi-commit
PR can have several related notes, all inspected together in the PR diff.
Deletes/renames count (renames appear as deletion + addition). Required documents
cannot be deleted. All tracked Markdown local file links, reference definitions,
HTML href/src and Markdown heading fragments are checked without network calls.
External URLs and fenced/inline code examples are not fetched or treated as links.
Use ordinary local paths without unescaped parentheses; keep complex examples in
code fences. This is a small stdlib validator, not a full CommonMark renderer.

| Changed paths | Minimum beyond local-link/required-file checks |
| --- | --- |
| Any non-document change | New dated substantive change note, literal changed paths in Scope, updated HANDOFF |
| `app/src/main/**`, `app/schemas/**` | CURRENT_STATE |
| `apk/`, `data/`, `di/`, `ai/`, `packages/`, `service/` under the Kotlin package; MainActivity/MainViewModel/RenkinApplication; manifests and assets | ARCHITECTURE |
| Gradle/build files, wrapper, catalog, `.gitmodules` | CURRENT_STATE and QA BASELINE |
| Workflows, hooks, scripts, `.gitignore`, `.gitattributes` | WORKFLOW |
| Shared validators, hooks or governance workflow | Numbered ADR added/updated |
| A path containing `ProjectHome` | ROADMAP |
| Explicit changed behavior / architecture / milestone / durable decision in note | CURRENT_STATE / ARCHITECTURE / ROADMAP / numbered ADR respectively |
| Docs-only (Markdown, LICENSE, docs text) | No mandatory change note/state/architecture rewrite; links and PR audit still required |
| At most three Android test/fixture paths under `app/src/test/` or `app/src/androidTest/`, plus optional docs | No mandatory full change note or architecture rewrite; real test evidence and PR audit still required |

Four test paths, guard-test changes, or tests mixed with build/runtime changes do
not receive the small-test exemption. UI strings/layout resources conservatively
count as current behavior. Unknown non-doc changes require a note and handoff.
Do not add irrelevant whitespace to satisfy a required document change.

Each new change note has ten populated level-2 sections and four exact impact
declarations specified in [changes/README](../changes/README.md). Empty/comment-only
sections, unfinished markers, an old edited note, and whitespace-only required-doc
updates fail. Milestones/durable decisions cannot reliably be inferred from every
diff: authors must declare them and reviewers verify the declarations.

## Hook and CI behavior

The shell hooks select Python 3 from PATH, fail if missing, and propagate script
errors. pre-commit additionally runs `git diff --cached --check`. Hooks are local,
optional to install and bypassable; no claim of server enforcement follows from them.

[Governance workflow](../../.github/workflows/governance.yml) runs on PR open,
synchronize, reopen, edits and ready-for-review, using contents:read and no secrets.
Checkout uses the PR head SHA, full history and no persisted credentials. Titles
and bodies are read from the event JSON, never expanded as shell commands.

- `commit-conventions`: every commit in base..head plus the PR title; old base
  history is exempt. Syntax is shared with commit-msg.
- `documentation-audit`: offline guard tests followed by merge-base/head docs
  audit, local links and the completed PR audit. Exactly one unindented
  `Audit result: PASS`, `Audit result: PASS WITH RISKS` or `Audit result: BLOCKED`
  line is required. LF/CRLF and trailing horizontal whitespace are supported.
  Missing, malformed, repeated or conflicting declarations fail; an indented
  or malformed extra `Audit result:` line also counts as a duplicate. A single
  `BLOCKED` declaration fails. This applies to the complete raw PR body, including
  any declaration lines left in examples; remove those extra lines before review.
- Existing [Android CI](../../.github/workflows/ci.yml), job `build`: compile,
  Robolectric/JVM unit tests, assembleDebug, APK artifact and failure reports.
  Its triggers, job and build commands are preserved. SDK setup explicitly requests
  `platform-tools`, replacing v3's default request for the unavailable legacy `tools`
  package; see the [CI follow-up audit](../changes/2026-10-11-android-ci-sdk-setup.md).

CI does not validate natural-language truth, visual quality, actual device behavior,
reviewer identity/authority, provider availability or Play acceptance. Scripts and
workflows modified by a PR are themselves reviewable code; required independent
review prevents accepting a weakened gate just because its own CI passes.

## Main protection — observed status

Verified October 11, 2026, 12:16 UTC at the Codex 01 gate and reread at 15:52 UTC
with the same values, after the owner merged
PR #1 as `005cffa6d51b11e15c20e68349c0c4ce0f23cd8f`:

- [Ruleset 24877351](https://github.com/Zsombor0918/RenkinAI/rules/24877351),
  `main-reviewed-and-audited`, is **active** and includes `refs/heads/main`, with no exclusions.
- Main reports `protected: true`. Its classic-protection summary remains disabled;
  the active ruleset, rather than a classic branch rule, provides the protection.
- Deletion and non-fast-forward updates are restricted. Bypass actors are empty,
  and the connector's current user can never bypass the rule.
- PRs are required. Required approving-review count is **zero** and
  `require_last_push_approval: false`; an approving review is not currently enforced.
  Stale approvals are dismissed if present. The previous closure audit observed
  latest-push approval true and a blocked PR; later owner settings and the actual
  merge supersede that observation. No administration write was made here.
- Required checks are exactly `build`, `commit-conventions`, `documentation-audit`,
  all sourced from GitHub Actions (integration id 15368).
- Conversation resolution is **not required** by the saved rule, and strict
  up-to-date status checks are **disabled**. These differ from the recommended
  setup below; do not describe those requirements as enabled. The P1 thread on
  PR #1 is nevertheless verified resolved.
- The owner accepted and merged #1. That acceptance does not approve later feature
  PRs. Explicit authorized acceptance remains required under AGENTS. Required review,
  conversation resolution and up-to-date checks remain manual owner tasks, using
  steps 7–9 below; do not claim them enabled.

The earlier inactive observations are historical, retained in QA. The connector
did not modify administration settings; the owner created the active ruleset.
The setup steps below remain the recommended configuration, while the observed
values above and the live PR merge state determine the actual enforcement.

## Owner setup, click by click

1. Open [RenkinAI settings](https://github.com/Zsombor0918/RenkinAI/settings).
2. Select **Rules → Rulesets → New ruleset → New branch ruleset**.
3. Name it `main-reviewed-and-audited`; set **Enforcement status: Active**.
4. Leave **Bypass list empty**, including administrators, bots and GitHub Apps.
5. Under target branches choose **Add target → Include by pattern** and enter `main`.
   Confirm there are no exclusions that remove main.
6. Enable **Restrict deletions** and **Block force pushes**. Do not enable Restrict
   updates without a compatible merge path; the PR rule below handles direct pushes.
7. Enable **Require a pull request before merging**, set required approvals to **1**,
   enable dismissal of stale approvals, require approval of the most recent
   reviewable push, and **Require conversation resolution before merging**.
8. Enable **Require status checks to pass**, add the exact observed Actions check
   contexts `build`, `commit-conventions`, `documentation-audit`; select GitHub
   Actions as source where offered. Enable up-to-date branch checks. If contexts
   are not yet listed, run this PR once, then select the emitted contexts; never
   substitute a workflow title or invent a context that has not run.
9. Save/Create the ruleset, reopen it and verify **Active**, target `main`, empty
   bypass list, required review, conversations, checks, deletion and force-push rules.
10. Check this PR's merge box: until required checks and approval complete, merge
    must be blocked. Record the rule id/settings and timestamp in HANDOFF/BASELINE.
    Read branch/ruleset APIs again if available; do not test enforcement by pushing
    unwanted code to main. Keep the PR unmerged until the owner accepts its audit.

A sole contributor cannot approve their own PR. Invite an independent reviewer
with suitable repository access before choosing the strict no-bypass setup above;
do not quietly turn reviews off or add an administrator exception to get past it.
If Rulesets is unavailable for the account, use **Settings → Branches → Add branch
protection rule**, pattern `main`, require PR/1 approval, dismiss stale reviews,
latest-push approval, conversations, the same status contexts/up-to-date branches,
**Do not allow bypassing the above settings**; keep Allow force pushes and Allow
deletions disabled. Save and verify the effective rule with the PR merge box.

Official references: [creating repository rulesets](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/creating-rulesets-for-a-repository),
[available rules](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/available-rules-for-rulesets),
[classic branch protection](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches).
Check current GitHub labels/options when applying the settings.


## Codex 03 verification additions

The existing regression runner retains all M1/M2 selectors unchanged, adds project
style/selection UI, data/migration/backup/session ownership and gateway/legacy AI
parity groups, then the same unfiltered compileDebugKotlin/testDebugUnitTest/
assembleDebug. It verifies selector presence, positive counts, zero failures/errors/
skips and nonzero command exits; no idle/timeout bypass or test exclusion. The actual
KSP-generated v19 schema is retained in the evidence artifact for source checkout;
a missing export fails the runner. Original Android/governance workflows and required
contexts are unchanged. New-schema rollback requires a pre-upgrade full backup or a
forward repair; never recommend installing a v18 APK over a populated v19 database.

At the Codex 03 gate, PR #3 is owner-authorized and merged as aefeb1b6. Ruleset
24877351 was reread: active, bypasses empty, PR and exact build/commit-conventions/
documentation-audit required, force/delete prohibited. Approvals remain 0,
conversation resolution/last-push approval/strict checks false. Do not claim these
recommended owner settings were enabled; no ruleset modification occurred.
