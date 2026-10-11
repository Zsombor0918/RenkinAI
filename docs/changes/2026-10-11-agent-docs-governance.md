# Codex 00 — documentation and Git audit infrastructure

## Summary

Implement a canonical agent entry path, contributor hooks, shared offline guards,
read-only PR governance CI, substantive change records and a complete audit template.
Work stays on `chore/agent-docs-governance`; main baseline is
`eb4daa5de82fab54e115363c073eb8981f2203d6`. No merge or release is performed.
This note records the initial infrastructure commit. A subsequently observed SDK
setup failure is addressed by the [separate CI follow-up](2026-10-11-android-ci-sdk-setup.md).

## Why

Inherited docs did not consistently distinguish upstream identity, the current
AI fork, proposed work and historical test results. No automated documentation or
commit gate existed, and local AGENTS was ignored. A new agent needs a current
source-backed state and honest QA/handoff rather than reconstructing prior chats.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: changed
Durable decision: recorded

Application behavior/architecture stay unchanged; Codex 00 is now implemented on
a PR branch, pending acceptance and external main protection. Non-document files:

- `.gitignore`: track AGENTS and ignore Python caches.
- `.github/workflows/governance.yml`: shared validators, offline tests, PR title/body.
- `.githooks/pre-commit`: staged whitespace/docs audit with propagated failures.
- `.githooks/commit-msg`: shared Conventional Commit validator.
- `scripts/check_commit_messages.py`: subjects, message files, new-commit range and PR title.
- `scripts/check_docs.py`: required structure/links/new notes/path rules/PR audit.
- `scripts/tests/test_repo_guards.py`: positive/negative real-Git integration tests.

Document changes are listed below. No Kotlin, Android resource, manifest, Gradle,
schema, LICENSE, smali, key, submodule revision or `.github/workflows/ci.yml` is changed.

## Behavior before / after

Before: no common commit rule, no canonical state/roadmap/handoff, and undocumented
code could enter a PR without an automatic docs failure. Some inherited prose
named the upstream app ID and incorrectly implied a gate spanning installer UI.

After: a meaningful change needs a new populated record and path-relevant docs;
staged checks cannot use unstaged docs, CI reads committed head/merge-base, invalid
messages/links/audits return a failure exit. Docs-only and at most three Android
test paths have bounded exceptions. The docs map explains the real profile/custom
AI paths and planned ProjectHome. Existing Android functionality and CI are preserved.
Enforcement at merge still depends on the owner's GitHub protection settings.

## Tests actually run

- `python3 -m unittest discover -s scripts/tests -v`: 38 offline tests passed
  in this implementation environment, including actual shell hooks, CLI nonzero
  failures, stage/head snapshots, diverged base, renames/deletions, impact rules,
  empty/unfinished/duplicate notes, comment-only updates and PR audit outcomes.
- `./gradlew testDebugUnitTest assembleDebug`: attempted; exit 1 during wrapper
  download (`java.net.SocketException: Network is unreachable`), before any Android
  unit test/build ran. JDK 17.0.20 is present; a usable Android SDK was not found.
- Main API/PR audit: initial main HEAD verified, no open PR, protected false,
  required checks empty, rulesets empty; direct protection read returns 403.

Final staged/shared guard checks and PR CI results are reported in the PR audit
after the final commit. They must be observed, never inferred from configuration.

## Not tested

No actual Android unit tests or debug build completed locally. No physical device,
emulator, Windows/Git Bash host, live OpenAI/Stability call, upgrade/backup restore,
pack install/update, Theme Park/One UI/cover-screen or Google Play submission was used.
The October 4 592-test/certificate claims remain historical and were not rerun.

## Risks

Main is not yet externally protected; the connector cannot write administration
rules. Local hooks are bypassable and CI must be required explicitly. Natural-language
truth, impact declarations, visual compatibility and reviewer authority still need
human review. The local Markdown checker supports ordinary Markdown/reference/HTML
links but is not a complete CommonMark implementation. The strict one-review rule
requires an independent collaborator; the author cannot approve their own PR.

No data migration, package identity, APK signing or billable AI behavior changes.
CI checks on the PR and owner acceptance remain outstanding until actually observed.

## Rollback

Revert the infrastructure PR commit through an audited follow-up PR. Remove local
hook activation with `git config --local --unset core.hooksPath` only when rolling
back that infrastructure. If external required-check settings were activated,
the owner must coordinate their update so removed contexts do not leave PRs pending.
There is no application persisted-state migration or signing-key rollback.

## Documentation updated

- [AGENTS](../../AGENTS.md), [README](../../README.md), [CONTRIBUTING](../../CONTRIBUTING.md),
  [CHANGELOG](../../CHANGELOG.md), [PR template](../../.github/PULL_REQUEST_TEMPLATE.md).
- [INDEX](../INDEX.md), [CURRENT_STATE](../agent/CURRENT_STATE.md),
  [WORKFLOW](../agent/WORKFLOW.md), [HANDOFF](../agent/HANDOFF.md), [ROADMAP](../ROADMAP.md).
- [ARCHITECTURE](../ARCHITECTURE.md): fork IDs, DI collaborators, AI boundaries and
  original-profile persistence with a gate that does not span the installer UI.
- [AI_FORK_HU](../AI_FORK_HU.md): current clone/build/workflow pointers and historical
  artifact caveats. [AI_HANDOFF_HU](../AI_HANDOFF_HU.md) and
  [watch design](../icon-watch-feature.md) are explicitly historical; broken old
  source references now lead to the actual current components.
- [Change-note rules](README.md), [ADR 0001](../decisions/0001-repository-governance.md),
  [QA baseline](../qa/BASELINE.md), [test matrix](../qa/TEST_MATRIX.md),
  [Play release gate](../policy/PLAY_RELEASE_GATE.md).

## Follow-up

Open the audited PR, observe Android/governance CI, and obtain owner/reviewer
acceptance. Owner configures and verifies main protection following WORKFLOW;
no automatic merge is requested. After Codex 00 closure, the separately authorized
ProjectHome task starts from existing profiles and preserves the Renkin pipeline.
