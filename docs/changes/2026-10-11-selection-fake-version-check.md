# Codex 03 selection fake version comparison

## Summary

Give the real-selection test backend an explicitly Long expected version and
retain diagnostics of the previous comparison. Generate offline HTTP fixture
credentials in memory instead of committing even a fake credential string.

## Why

Regression run 38176212110 on 02c23aa5 executed 717 tests with one failure: target
Save. Its XML shows the button enabled/displayed, one selected component, an error
node, and saves=0/metadata=null. Therefore the narrow failure is before the fake
backend counter, not scrolling, layout, idling or durable write completion. The
backend's only preceding operation is its version equality check with a bare
integer default. Correct the expected version to the actual Long API and measure
the old comparison/type to verify this boundary. No product semantics are changed.

Original M1/M2 groups, data/migration/backup/session 30/30 and gateway/legacy 31/31
passed on that regression runner. The separate original CI attempt failed 490
Robolectric fixtures before test entry due a Maven SDK checksum 404; that is a
separate environment failure and its failed job was rerun on the identical SHA.

## Scope / changed files

- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectSelectionComposeTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/IconGenerationGatewayTest.kt`

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

Only test backend typing and in-memory fixture construction are changed. The
existing runtime remains identical; no governance or production API change.

## Behavior before / after

Selection Save still uses the real TargetSelection button and all exact-key,
completion-count and restoration assertions. The fake expected value now has the
same Long type as metadata.version and the callback. Temporary diagnostics retain
the old comparison result and are removed after verification. MockWebServer receives
a generated UUID credential fixture that exists only in the test process; the body still
must exclude it and production endpoints are never used.

## Tests actually run

Downloaded regression artifact 11715579142 and examined isolated/full XML and
system output. The first Android attempt 38176212112 counted 717 tests, 490 fixture
fetch failures, APK skipped; the independent regression full suite was 717 tests,
one failure, zero errors/skips. Original cases 1/1, 1/1, 2/2, 3/3, Create/Extend
10/10, coverage 28/28, data 30/30 and gateway/legacy 31/31 passed. Local governance
on 02c23aa5 passed 45/45 in 21.989 s; range docs PASS 48 paths and conventions PASS
three subjects. This test correction still needs its own hosted verification.

## Not tested

No next-head Android success or APK is claimed. Physical devices, TalkBack, OEM,
URI providers, physical upgrade/backup/signing/installer remain NOT RUN. No live
paid provider request exists in this test run or implementation work.

## Risks

The version mismatch diagnosis remains bounded to the fake callback until its old
comparison output is verified. Correctly typed expected Long does not relax the
version requirement. Network fixture-fetch errors cannot count as product passes.

## Rollback

Revert only test fixture typing for investigation while retaining failure evidence
and real UI actions. Runtime storage and v19 remain untouched by this commit; the
existing forward-repair/pre-upgrade-backup downgrade restriction still applies.

## Documentation updated

HANDOFF and QA retain measured semantic/callback evidence, independent environment
failure and pending next-head verification. Production state/architecture/roadmap
are unchanged because this commit modifies only the two test fixtures.

## Follow-up

Verify the old comparison/type output and all assertions, remove temporary probes,
run complete Android/APK and original filtered groups on the final head, then
update the PR audit from actual evidence. Keep PR #4 draft/BLOCKED until then.
