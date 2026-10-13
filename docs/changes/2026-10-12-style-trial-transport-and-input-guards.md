# Preserve multipart factories and bind exact saved style inputs

## Summary

Correct two measured retained regressions while keeping transport one-shot, and
bind actual immutable text/instruction hashes before result insertion and reuse.
Add explicit safe provider error handling and preserve original version stamps.

## Why

The legacy factory is inspected as MultipartBody; wrapping its actual transport
preserves that contract without replay. The current full backup is now format6.
Numerical revision IDs alone cannot prove identical restored style contents, and
escaped maximum saved inputs need the verified bounded prompt envelope.

## Scope / changed files

Behavior impact: changed
Architecture impact: changed
Milestone impact: none
Durable decision: recorded

Keep all legacy assertions and exact multipart bytes/type/length. No dependency,
SDK, signing, second database or model-default change. Exact non-document paths:

- `app/src/main/kotlin/dev/renkinProject/renkin/ai/AiSettings.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/IconGenerationGateway.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/OneShotBody.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/OpenAiImageClient.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/StabilityImageClient.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialCoordinator.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialJournal.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialPlan.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/TrialOriginals.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/project/StyleTrialRepository.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleTrials.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectTrialPanel.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialFixtures.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialGatewayTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialJournalTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialPlanTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/data/project/ProjectAiAnalysisPersistenceTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/data/project/StyleTrialPersistenceTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/data/transfer/ProjectAiBackupTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/StyleTrialComposeTest.kt`

## Behavior before / after

Native request factories retain multipart structure; actual clients wrap whole
transport bodies, since pinned4.12 does not propagate child flags. Actual source
text/instruction hash prevents reused revision IDs inheriting a different style;
result persistence rechecks saved text/refs. Prompt max32000 preserves escaped
valid inputs and fails before consent if exceeded. Long package-version stamps
remain exact. Safe HTTP/model categories guide UI without leaking server content,
automatic fallback or repeat. Missing images are visible; profile-delete/GC tests
preserve the other owner's originals, RAW and approval.

## Tests actually run

Actual dd855e74 CI38287235362 compiled and ran851 with two failures, APK skipped.
Downloaded regressions38287235504 artifact11763214314: digest/HEAD verified,
independent143 XML/851/2 failures/0 errors/skips, all selectors. M5 groups9/7/10/12/18
and retained project data34 passed; failures were exactly the native factory cast
and old format5 assertion. Genuine KSP21 matches committed export. Local52 guards
passed in7.378s before this correction; new local run and staged gates are recorded
in QA and the PR audit. Original tests/selector/timeout/idling remain intact.

## Not tested

Corrected next-head full/filtered Android/APK and the added typed-error/text-hash/
numerical-revision/profile-delete tests await hosted execution. Local Gradle download
is unavailable; device/process/storage/upgrade/SAF/signing/install/OEM/TalkBack and
provider account/quality/cost remain NOT RUN. Live/billable provider requests=0.

## Risks

One-shot must wrap the complete actual HTTP request, never merely a multipart
child. Reuse requires known exact style hashes/current local review proof; unknown
historical inputs fail closed. Paid or corrupt outputs never trigger automatic retry.

## Rollback

Keep equivalent whole-body protection and exact style/hash/owner validation.
Reverting alone restores the measured legacy cast or history-alias risk. Populated
v21 requires forward repair or explicit compatible pre-upgrade backup recovery.

## Documentation updated

State/architecture/ADR0006/handoff explain text fingerprints, transport wrapping,
provider errors and strict reuse. BASELINE preserves actual851/two-failure results,
all passed M5 groups and the next-head boundary rather than rewriting history.

## Follow-up

Verify every original/new group, unfiltered suite, real APK and schema bytes on the
next head. Only final-head complete checks can close the single PR audit. Leave
owner review/merge, device/provider acceptance and M6/M7 separate.
