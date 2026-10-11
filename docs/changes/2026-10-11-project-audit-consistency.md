# Codex 03 persistence audit consistency

## Summary

Align ADR 0004's legacy-gallery verification sentence with the completed hosted
checkpoint. Preserve owner acceptance pending and exact-head PR evidence.

## Why

The compatibility regression passed at 06e71532, but the decision still said it
awaited verification. A new agent must find the correct current status directly
from the canonical decision and QA, without mistaking earlier pending text for a
remaining code blocker.

## Scope / changed files

Documentation-only: ADR 0004, QA BASELINE, agent HANDOFF and this change record.
No non-documentation path changes.

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

The accepted-data contract is unchanged; the proposed ADR still requires review.

## Behavior before / after

The decision links the actual compatibility checkpoint and the current PR audit.
It no longer describes a completed fixture as pending. Runtime, selectors and
assertions are identical to the preceding diagnostic-removal commit.

## Tests actually run

At 68c05698 local guards passed 45/45 in 18.300 s, range commit checks six subjects
and documentation audit 51 paths. Hosted governance passed 45 guard tests and
rejected only the still-BLOCKED PR body. Prior 06e71532 full/regression XML and
APK success is recorded in BASELINE. No next-head Android pass is assumed.

## Not tested

This documentation commit has not yet run at commit time; final SHA and all fresh
checks are recorded after publication in PR #4. Devices, physical upgrade/SAF/
signing/install, OEM/TalkBack and live paid API testing remain NOT RUN.

## Risks

No code change. Owner/device acceptance remains distinct from technical evidence;
green CI alone cannot accept the migration or release the application.

## Rollback

Restore prose only if contradictory evidence appears. Runtime v19 data still
requires forward repair or explicit pre-upgrade-backup recovery, not an old APK
opening the new DB directly.

## Documentation updated

ADR, BASELINE and HANDOFF now agree about the compatibility checkpoint, while
historical failed runs and their original observations remain recorded.

## Follow-up

Verify all original/new isolated groups, full Android/APK and three required
contexts at this final SHA. Update the single PR audit from those actual results;
leave merge and release to a separate explicit owner decision.
