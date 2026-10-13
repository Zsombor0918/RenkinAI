# Keep deleted trial journals valid and non-replayable

## Summary

Make populated trial-journal deletion preserve valid consumed records for OpenAI
and Stability while removing original inputs, style text and staged PNG files.

## Why

Final source review found that replacing style text left its old fingerprint,
which strict plan encoding rejects. Clearing Stability references also violated
its one-reference schema. The earlier profile-delete test covered Room/GC with an
empty deleted journal, so it could not establish populated journal cleanup.

## Scope / changed files

Behavior impact: changed
Architecture impact: none
Milestone impact: none
Durable decision: recorded

The existing epoch, provider, endpoint counters and consumed UUID ledger remain.
No new storage, retry, gateway or profile/session identity is introduced.

- `app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialJournal.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialJournalTest.kt`

## Behavior before / after

Deleted plans now hash their redacted text. A Stability tombstone keeps one all-zero
sentinel reference hash, preserving its real provider and two-endpoint history
without retaining a real input. Its epoch is invalid and prepared flag false; the
same plan/item IDs remain consumed. Strict schema checks are unchanged. The test
reopens both populated journals, checks redaction/files/counters/other-profile
isolation and rejects reused item IDs under a new owner without a gateway call.

## Tests actually run

Pre-correction d61c2f91 Android CI38291847276 passed production compilation,
unfiltered unit tests and debug APK. Regressions38291847281 artifact11766448252
was downloaded: matching HEAD/digest,143 XML/855 tests/zero failures/errors/skips,
all21 commands/selectors and genuine19/20/21 schema bytes. APK artifact11766302772
exists on that exact head. This is prior-head evidence, not a pass for the new
deletion test. Fresh correction Python52 guards passed in7.704s. Staged/range
checks are recorded in QA and the PR audit. No original assertion/selector changes.

## Not tested

The corrected populated-journal test and all original/new Android groups require
fresh hosted execution. Local Gradle download is unavailable. Device process kill,
storage failure, actual profile deletion/restore/upgrade, signing/install/OEM and
live provider account/quality/billing remain NOT RUN; live paid requests=0.

## Risks

Consumed records must remain parseable after privacy cleanup. The zero reference
is a tombstone only: its old epoch, consumed IDs and missing PNG prohibit requests.
A disk failure can still interrupt local cleanup after epoch invalidation.

## Rollback

Keep equivalent valid redacted fingerprints and Stability endpoint history when
reverting. Do not remove consumed markers or interpret deleted plans as consent.
Populated Room v21 still needs forward repair or compatible pre-upgrade backup.

## Documentation updated

ADR0006, architecture, current state and handoff describe the valid tombstone and
retained real endpoint counters. QA preserves the prior successful code checkpoint
and identifies the new test's separate verification boundary.

## Follow-up

Verify the new test in its isolated journal group and the full suite on the next
head, then perform complete closing-head checks and artifact handoff. Owner
review/merge and physical/provider acceptance remain separate decisions.
