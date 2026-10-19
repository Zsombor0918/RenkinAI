# Preserve paid trial history through explicit local re-review

## Summary

Repair the measured suspend fixture compilation failure and allow identical saved
trial artwork to undergo explicit current-context re-review/approval without new
generation. Retain the genuine KSP21 export from the initial hosted compilation.

## Why

A suspend Room assertion needs a suspend fake observer. Imported/restored or
source-stale accepted PNGs also need a real free revalidation path; simply disabling
old review would force an unnecessary paid generation for unchanged artwork.

## Scope / changed files

Behavior impact: changed
Architecture impact: changed
Milestone impact: none
Durable decision: recorded

Retain immutable generation evidence; add separate typed local review proof in
existing JSON schema v2, with v1 decoding. No extra SQL field or database is added.
Exact changed non-document paths:

- `app/schemas/dev.renkinProject.renkin.data.RenkinPackDatabase/21.json`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialJournal.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialPlan.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/project/ProjectAiData.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/project/StyleTrialRepository.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleTrials.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectTrialPanel.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialFixtures.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/data/project/StyleTrialPersistenceTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/data/transfer/ProjectAiBackupTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleTrialsTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/StyleTrialComposeTest.kt`

## Behavior before / after

Old ACCEPTED alone stays insufficient after context/restore/share change. New local
re-review checks current eligibility, original pixels, immutable revision, ordered
refs/configuration and owner/source, then stores current review context separately.
Only new explicit common approval makes identical PNGs reusable. Changed artwork or
style remains historical; regeneration prepares a fresh plan and paid consent.
The observer now permits its original durable Room assertion without weakening it.

## Tests actually run

Initial hosted2920a5d7 production compiled, test compilation failed at the single
non-suspend observer boundary, zero Android tests and no APK. All21 regression
commands failed compilation with zero tests. Downloaded artifact11763476310 ZIP
integrity/digest and HEAD were checked, and genuine KSP21 bytes match the source
copy (identity8c7a3c8b5b26ad29673ebbd77605838e, SHA25611912e933464d86952e3c70ced6ceb2e7506fe3407ac44f3425ebed9a89e0dad).
Local52 guards and whitespace review passed before initial publication; corrected
full local guard rerun is recorded in QA. Hosted next-head verification is required.

## Not tested

The fixed callback and new local-review/backup/owned/UI negative and positive cases
await hosted execution. Local Android download is unavailable. Device/OEM/TalkBack/
physical upgrade/process/storage/signing/install/provider/account/cost remain NOT RUN.

## Risks

Revalidation never mutates original generation input or authorizes an endpoint.
It must still reject different original pixels/revision/refs/provider configuration,
unsafe targets or a changed owner. Global consumed markers remain after restore.

## Rollback

Do not replace free current-input re-review with automatic approval or automatic
regeneration. Revert only with equivalent consent/history protection. Populated v21
still needs forward repair or compatible pre-upgrade backup recovery, not v20 APK.

## Documentation updated

ADR0006/state/architecture/handoff distinguish immutable generated evidence from
current local review proof. QA preserves the actual first compilation failure and
genuine schema artifact; new evidence is not called an Android/migration pass.

## Follow-up

Verify every retained/new group and full suite/APK on this exact next head. Retain
real KSP identity equivalence and update the PR audit only after measured success.
Owner review/merge and physical/live acceptance remain separate decisions.
