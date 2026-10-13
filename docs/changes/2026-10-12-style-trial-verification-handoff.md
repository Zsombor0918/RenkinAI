# Record verified style trials and the read-only M6 handoff

## Summary

Close the M5 implementation documentation using genuine hosted tests, schema
exports and APK evidence while retaining owner, device and provider acceptance.

## Why

The corrected implementation passed every retained/new group and full suite.
Canonical state and handoff must identify this result without treating prior
failed heads or historical M4 tests as current success.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: changed
Durable decision: none

This closing commit changes documentation only, with no non-document paths.
ADR0006 stays proposed for owner review; its clarification describes existing
full-backup keystore behavior and valid redacted trial tombstones.

## Behavior before / after

Application behavior is unchanged. State/index/roadmap now identify implemented,
automatically tested M5 and the outstanding owner review. Handoff describes exact
input/review/approval reuse, deleted-plan no-replay, restore and rollback limits.
M6 gallery/bulk Apply, a new paid batch and M7 edits remain separate tasks.

## Tests actually run

Code 0df78ef8 passed Android 38293199741 and regressions 38293200475.
Downloaded artifact 11767029451 matched ZIP digest/HEAD:143 XML/856 tests/zero
failures/errors/skips, all 21 commands/selectors. M5 groups10/8/12/12/19; every
retained group and the last unfiltered compile/unit/APK passed. APK 11767123455
is genuine CI output. KSP19/20/21 exports match committed bytes; physical SQLite
v20→v21/fresh migration, full/share/legacy/GC and split-write no-POST repair passed.
Local 52 Python guards passed in 7.704 s; staged hooks/range/link/conventions passed.

## Not tested

This closing SHA still needs its own fresh complete Android/filtered/APK/governance
checks, recorded externally in PR #6 after commit to avoid self-hash recursion.
Local Gradle cannot download; physical device/emulator/storage/kill/SAF/upgrade/
signing/install/OEM/TalkBack and provider access/quality/billing NOT RUN. Live requests=0.

## Risks

Green automation is not owner review, device or release approval. The existing
multi-store restore is not distributed ACID. Oversized project saves keep staged
results; uncertain remote outcomes never retry. CI debug signing continuity and
populated v21 downgrade still need explicit compatible recovery consideration.

## Rollback

Revert documentation without changing application state. A runtime rollback from
populated v21 requires forward repair or compatible pre-upgrade full-backup recovery
with preserved app/pack keys. Never remove consumed paid-request markers for retry.

## Documentation updated

CURRENT_STATE, INDEX, ROADMAP, ARCHITECTURE, ADR0006, README, BASELINE, TEST_MATRIX
and HANDOFF align implementation/evidence/owner status. Historical failure notes
remain intact; exact final-head audit and artifact links belong to PR #6.

## Follow-up

Verify this exact closing head and leave PR #6 open for owner review/merge.
After owner acceptance, M6 starts from freshly verified merged main and revalidates
AcceptedTrialVariant inputs read-only before any separate new GenerationPlan.
