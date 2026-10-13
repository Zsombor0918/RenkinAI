# Codex 05 owner blocker reproductions

## Summary

Add deliberately failing regression coverage for the owner report on existing PR #6.
Production remains the exact 3c5c1de7 implementation during this checkpoint.

## Why

The owner observed twelve local samples followed by FAILED analysis, a disabled start,
and an undiscoverable trial journey on a Samsung Z Flip8 One UI device. Green prior CI
is insufficient. Two open bot reviews additionally identify orphan AtomicFile .new
records and stale common approval after a newly completed unreviewed trial.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

- `app/src/test/kotlin/dev/renkinProject/renkin/data/project/StyleTrialPersistenceTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialJournalTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleAnalysisTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisSchemaTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleTrialJourneyComposeTest.kt`
- `scripts/run_compose_regressions.py`

These tests use actual routed Compose screens, the existing controller interfaces,
real Room and journals, and offline provider ports; no second production VM is added.

## Behavior before / after

Application behavior is unchanged in this test-first checkpoint. New assertions demand
visible trial entry and prerequisites, safe persisted HTTP failure categories, bounded
wire arrays, recoverable orphan writes and invalidation of stale common approval.
The existing 21 regression groups gain one routed UI selector; none is removed.

## Tests actually run

Linux/JDK17/Python3.12.14: local `./gradlew compileDebugKotlin testDebugUnitTest
assembleDebug --console=plain` failed before configuration: Gradle8.11.1 download,
SocketException Network is unreachable. `git diff --check` is required before commit.
The hosted red reproduction run follows publication; no Android pass is claimed here.

## Not tested

The new Android assertions have not yet run at note creation. No physical phone,
provider account, live image generation, upgrade, install or TalkBack run occurred.
The submitted key is never part of source, fixtures, workflows, reports or artifacts.

## Risks

This checkpoint intentionally blocks merge and retains the owner P0 report as OPEN.
An automated reproduction does not establish the original phone's HTTP status.
The offline gateways cannot prove provider quality, account access or billing.

## Rollback

Revert this test checkpoint to recover the previous source and selector set. It adds
no Room migration, persisted application state, API calls or signing changes.

## Documentation updated

HANDOFF now names the owner blocker and red checkpoint. WORKFLOW and TEST_MATRIX
record the additive routed test and its actual boundaries. BASELINE records local
Gradle failure and leaves the prior M5 evidence explicitly historical.

## Follow-up

Capture hosted failing assertion/XML evidence, then implement focused fixes on the
same branch. Re-run all checks on the final head, deliver a new APK and exact English
phone journey, and leave owner acceptance and merge explicitly pending.
