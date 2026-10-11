# Codex 02 — retain measured complete verification evidence

## Summary

Record successful M2 full and isolated automation with verified SHA/count/artifact
links; clarify current versus historical handoff and keep actual device/owner risks.
This commit changes only canonical documentation, not runtime or test code.

## Why

The corrected source head now passed all Android and isolated regressions. New
agents need actual 669-test evidence and source/local/actionability boundaries,
not old M1 counts or unresolved earlier fixture failures as current state.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: changed
Durable decision: none

Only CURRENT_STATE, ROADMAP, HANDOFF, BASELINE, TEST_MATRIX and this engineering
record change. No non-documentation files are changed in this commit. Owner/device
acceptance remains pending; implementation verification is not release approval.

## Behavior before / after

Application, runner and workflows remain byte-identical to tested source head
`a8b3e4b22e6611042aa8de1d1ce03f3175989976`. Documentation now identifies the
successful checkpoint, preserves earlier failed runs and sets M2 automated status
while still requiring final-head checks and separate owner acceptance.

## Tests actually run

[Android 38160285188](https://github.com/Zsombor0918/RenkinAI/actions/runs/38160285188)
passed compile/unit/debug APK and uploaded artifact 11710600148.
[Regressions 38160285156](https://github.com/Zsombor0918/RenkinAI/actions/runs/38160285156)
passed creation 1/1, ingress 1/1, combined 2/2, actual form 3/3, new UI 10/10,
domain/parser/render 28/28 and full compile/unit/debug APK **669/669**, no skips.
Downloaded artifact 11709179629 summary SHA matched and 120 XML reports independently
summed to zero failures/errors/skips. Governance 38160285196 passed 45 guards and
commit conventions, failing docs only for then-BLOCKED audit. Local 45 guards and
range checks passed; local Android failed before configuration due blocked download.

## Not tested

Physical external-pack sanity, work-user, uninstall/reinstall/update, dirty profile
navigation, GlobalOptions/manual AI, pack build/sign/install/update, One UI/Theme Park/
Flip/fold/rotation/process death and real TalkBack remain NOT RUN. No live paid API.
Final documentation-head checks still require observation after commit.

## Risks

Read-only static previews cannot certify complete dynamic/monochrome/OEM behavior.
Room component identity still lacks user handle; affected rows are disabled. UI
selection is transient preparation, not a durable plan. No owner review is invented.

## Rollback

Revert documentation evidence if factual correction is needed; revert all M2 commits
to return to M1 UI. No schema or signing conversion exists, and explicitly created
normal profiles remain valid. Unsaved icons still need existing save/build.

## Documentation updated

CURRENT_STATE is concise current source truth with historical tests linked to QA.
ROADMAP records implemented/automatically verified but owner/device-pending M2.
HANDOFF, BASELINE and TEST_MATRIX carry verified artifact evidence and next safe scope.

## Follow-up

Observe fresh final-head Android/regression checks, update PR #3 to one honest
PASS WITH RISKS audit, observe green governance and obtain authorized owner review.
Do not merge automatically. Codex 03 may then design style/asset persistence and a
small API gateway; user-key migration and Play release remain separate decisions.
