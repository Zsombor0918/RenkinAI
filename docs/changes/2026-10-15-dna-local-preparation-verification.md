# PR8 local preparation verification closure

## Summary

Record actual red/green proof and retained verification for the narrow source
readiness correction. Runtime/test/build files remain identical to verified
f788b62b0cebe387c1c444b797831ac7bda66c6f on the existing PR8 branch.

## Why

Owner screenshots show free Quick preparation failing, despite prior green CI.
Two actual red regressions prove early EXTEND navigation freezes a missing setup
stamp and the owned controller conceals that reason as operation. The correction
waits for the verified source and retains all guards with safe feedback. A genuine
matching path is now green; the phone's original exception is not recovered or
claimed uniquely proven by screenshots. Other source failures remain fail closed.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

Documentation only: CURRENT_STATE, HANDOFF, BASELINE, TEST_MATRIX, owner checklist
and this new verification note. No Kotlin, resource, test, schema, dependency,
provider format, signing, installation or other branch change in this commit.
The [production correction inventory](2026-10-15-dna-local-preparation-fix.md)
and [test-first inventory](2026-10-15-dna-local-preparation-reproduction.md) remain.

## Behavior before / after

Application behavior is unchanged from the verified correction. Documentation
now records actual execution instead of treating pending checks as success. The
four steps, Legacy/DNA, free no-key scanning/manual Save, original/style separation,
immutable history and all old safety/consent/journal boundaries remain.

## Tests actually run

Red876ad51e: Android38664556204 and all23 run38664556460 compile958 tests with
precisely two new failures. Independently downloaded689893-byte artifact11940906530,
ZIP SHA2569624deacda309f8161ac89e18ea4cb1f52407aaa95610758573d176afd767047,
has154 full XML/958 tests/2 failures/zero errors/skips; old groups pass.

Greenf788b62b: Android38665453196 compileDebugKotlin/testDebugUnitTest/assembleDebug
and all23 run38665453253 pass. Independent675222-byte artifact11942105251,
ZIP SHA25651ecf1158245f175720ac9b0e3865871d1249f78de1473bec2a7701f33c36b46,
has154 full XML/960 tests/zero failures/errors/skips and every retained selector.
Analysis persistence/session24, DNA27, guided35 pass. Genuine Room19/20/21 bytes
match source. The same red assertions remain; no timeout/idling/selector weakening.
Local Linux/Python3.12.14/JDK17.0.20 Python55 passed23.020s, docs54paths/subjects5
and real hooks pass. Local Gradle fails before configuration at distribution download.

This closing documentation SHA requires its own original Android/full23/governance
checks and fresh APK. Its exact hash and actual results are recorded externally in
[PR8](https://github.com/Zsombor0918/RenkinAI/pull/8) after execution, avoiding a
file containing its own hash or a checkpoint result substituting for the final head.

## Not tested

Corrective physical Flip8/Lux, actual installed pack drawables, TalkBack/fold,
installed-data/certificate upgrade and live/paid style fidelity remain NOT RUN.
No device installation, uninstall, data clear or live provider call is performed.
Native SDK33/320dp Room/Compose fixtures are automated source-path evidence.

## Risks

The matching source-readiness failure is fixed; screenshots cannot exclude other
owner-device local causes. New reason codes support a free retest without billing.
CI debug signing is ephemeral and may not match the installed app; preserve data
and rebuild with the existing key when needed. Public certificate extraction is
not full APK signature verification or installed-certificate comparison.

## Rollback

Use a reviewed compatible topic-branch revert or forward repair, retaining Room21,
tagged DNA readers, immutable originals/RAW/history, consumed markers and signing
keys. Neither destructive downgrade nor paid-request replay is required.

## Documentation updated

Canonical state/handoff and QA record the measured red/green results, source
checkpoint and final-head execution boundary. Owner checklist explicitly keeps
phone acceptance separate. Historical failures and earlier Windows evidence remain.

## Follow-up

Finish this documentation head's own checks and freshly downloaded APK; only then
set the complete PR8 audit to PASS WITH RISKS if no automated blocker remains.
Keep PR8 draft/open/unmerged for the owner's free first scan, reopen/rotation/manual
Save, signature compatibility and explicit final device approval. Stages2/3 stay plans.
