# Quick DNA source readiness and local failure feedback

## Summary

Correct only the first local scan prerequisite and diagnostic boundary on existing
PR8. Keep the four-step presentation, Style DNA, Legacy and existing ownership,
freshness, request-size, persistence and no-replay validations.

## Why

At test-first876ad51e, Android38664556204 compiled production and tests, then ran
958 tests with precisely two new failures. The actual HTML reports show Continue
to style enabled while coverage is Loading, and expected source_required versus
actual operation in the owned controller. ProjectShell freezes null setupStamp
on that premature transition; prepareDna correctly rejects it before reading any
image. The persistent generic error gives no way to distinguish that prerequisite.
The downloaded223416-byte report artifact11940622007 has SHA256
64b79d6e54e0fac4ee5a359803510bcc656833014d22f27779cd4812855f6190.
This proves the matching offline failure path; screenshots do not recover the
exact exception from the owner's phone or prove every Lux drawable readable.

## Scope / changed files

Behavior impact: changed
Architecture impact: none
Milestone impact: none
Durable decision: none

- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectWorkflow.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleDnaPanel.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleAnalysis.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleAnalysisTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/StyleDnaComposeTest.kt`

## Behavior before / after

Guided EXTEND now waits for the matching verified source before Continue. A restored
or other entry with no setup stamp shows remediation and disables only the DNA scan;
manual style remains available. The controller preserves the same guards and emits
allowlisted reasons for missing/changed source, empty/unreadable samples, changed
inputs, invalid/oversized plans and storage. Raw exception prose stays private.
Successful retry, source route changes or leaving free preparation clear its local
error; saved analysis, dirty drafts and consumed request markers remain untouched.
Quick remains12 images/one submission, Standard24/up-to5, each body5MiB; scanning
without a key remains free and cannot call a gateway.

## Tests actually run

Hosted red evidence above precedes every production edit. Local Python governance
passed55/55 in25.928s; committed-range docs and four conventional subjects passed.
The local Gradle distribution download fails with Network is unreachable before
configuration. Corrective hosted Android,23-command regressions and final-head
governance/APK results must be inspected and recorded in PR8's external audit;
no green Android result is claimed at this correction checkpoint.

## Not tested

Physical Flip8/One UI/Theme Park, actual Lux mappings, TalkBack/fold, installed-data
and signing upgrades and live style fidelity remain NOT RUN. No paid API request,
uninstall, data clear, main/PR6/PR7 modification or merge is performed.

## Risks

A genuinely changed, unsupported or unreadable source still blocks analysis with
specific feedback. The two screenshots cannot certify which phone exception ran.
All validations and old assertions/selectors/timeouts/idling remain. No schema,
backup, signing, dependency, provider body, component identity or legacy client changes.

## Rollback

Use a compatible reviewed revert or forward repair on this topic branch. Preserve
Room21, tagged DNA readers, immutable history, consumed IDs and both signing keys;
no user-data deletion or paid-request replay is required.

## Documentation updated

CURRENT_STATE and HANDOFF record the evidenced source race and safe continuation;
BASELINE retains the failed checkpoint; TEST_MATRIX and owner checklist add free
first-scan/reopen acceptance. Architecture, milestones and durable decisions remain.

## Follow-up

Inspect corrective exact-head XML and all23 retained groups, verify genuine schema
bytes and a newly built APK, then update PR8's single complete audit declaration.
Keep the draft open for explicit owner phone testing and final approval.
