# Android CI — avoid the unavailable legacy SDK tools package

## Summary

Preserve the Android compile/unit-test/debug-APK workflow and explicitly request
platform-tools in its SDK setup action. This is a Codex 00 infrastructure follow-up
to the first observed PR CI failure, not an Android application change.

## Why

PR run 38129900648 failed before compile: setup-android v3 asked sdkmanager for
`tools`, which returned `Failed to find package 'tools'` with exit code 1. The
action at commit `9fc6c4e9069bf8d3d10b2204b1fb8f6ef7065407` defaults its packages input
to tools + platform-tools. Explicit platform-tools avoids that obsolete request
without upgrading actions or weakening failure behavior.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

- `.github/workflows/ci.yml`: SDK setup packages input, comment documenting reason.

Canonical state/workflow/handoff/QA and contributor change-note guidance record
the failure and the narrow follow-up. No Gradle, dependencies or runtime source changes.

## Behavior before / after

Before, SDK setup attempted an unavailable legacy tools package and prevented all
Android verification. After, SDK setup requests platform-tools, while command-line
tools/accepted licenses remain managed by the existing action. Compile, unit tests,
assembleDebug, artifacts, triggers and the build job keep their existing behavior.
Subsequent task success must be observed in a new run.

## Tests actually run

The initial governance PR run succeeded and its 38 offline guard tests passed.
The actual Android job log was inspected and the v3 action definition was read.
Local YAML structure and staged hook validation are executed for this follow-up;
its completed Actions result is recorded in the PR audit after committing.

## Not tested

No Android build/test task completed in the failed first run or local attempt.
The corrected workflow is not called successful until its fresh run completes.
No physical-device, Windows-host or live provider request was made.

## Risks

The change only overrides SDK setup packages; downstream Gradle/dependency errors
may become visible after setup works. Existing current-state and historical-test
distinctions remain. A failed required Android check still prevents acceptance.

## Rollback

Revert this follow-up commit through a PR. The old SDK setup request will fail again
if tools remains unavailable; retain the build/unit-test/APK tasks and diagnose SDK
configuration rather than suppressing a setup error.

## Documentation updated

[WORKFLOW](../agent/WORKFLOW.md), [CURRENT_STATE](../agent/CURRENT_STATE.md),
[HANDOFF](../agent/HANDOFF.md), [BASELINE](../qa/BASELINE.md) and
[TEST_MATRIX](../qa/TEST_MATRIX.md) record the observed SDK failure and pending
fresh verification. [AGENTS](../../AGENTS.md) and [change-note rules](README.md)
clarify the staged follow-up commit requirement already implemented by the hook.

Primary evidence: [failed CI run](https://github.com/Zsombor0918/RenkinAI/actions/runs/38129900648),
[v3 action definition](https://github.com/android-actions/setup-android/blob/9fc6c4e9069bf8d3d10b2204b1fb8f6ef7065407/action.yml)
and [maintainer documentation](https://github.com/android-actions/setup-android#the-deprecated-tools-package).

## Follow-up

Observe fresh Android and governance CI results; resolve actual blockers before
requesting acceptance. Configure main protection and complete Codex 00 review,
then start the separately authorized ProjectHome task.
