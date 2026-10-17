# Trace Standard and reproduce generation recipe gaps

## Summary

Continue existing PR8 from clean75e936a1 with test-only transition diagnostics and five generation-usefulness regressions. Production remains unchanged pending RED evidence.

## Why

Hosted Standard clicks Prepare but times out without enough state evidence. Earlier local success and a reverted dispatcher experiment do not establish a root cause. Existing overall guidance also repeats unknown placeholders and loses useful silhouette/synthesis details.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

Test paths: `app/src/androidTest/kotlin/dev/renkinProject/renkin/offline/OfflineDnaTrace.kt`, `app/src/androidTest/kotlin/dev/renkinProject/renkin/offline/StyleDnaEmulatorTest.kt`, `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaWorkflowTest.kt`, `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaGatewayTest.kt`, `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaOfflineHttpTest.kt`. Host capture: `scripts/run_offline_dna_emulator.py`; existing safety fixtures updated in `scripts/tests/test_offline_dna_emulator.py`.

## Behavior before / after

Delegate wrappers log actual bind/load/prepare/release invocations, synchronous return and observed busy/preparation/plan/error transitions. Child job completion runs after its finally block. Owner boundaries include job identity; Save/load/selection/evidence boundaries and journal filesystem writes/renames are logged. Each synthetic event includes monotonic nanoseconds, sequence, process/thread and main-thread identity. Click return and backend prepare dispatch are separate events, so input injection alone cannot count as preparation. The runner retains tag-filtered logcat after each instrumentation phase without clearing buffers. StateFlow observations can conflate concurrent states; filesystem watches can miss creation before a new-directory watch attaches. Trace missing transitions as missing evidence, never proof of absence. No production trace hook, dispatcher change, sleep, timeout increase, action retry or weaker assertion.

## Tests actually run

Cached Windows JDK17/SDK36 command testDebugUnitTest selects the three new StyleDnaWorkflowTest quality methods, StyleDnaGatewayTest.generationUsefulPromptIsVersionedAndKeepsV1PlansAndEvidenceReadable and existing StyleDnaOfflineHttpTest.savedDnaTrialUsesFrozenMemberReferencesAfterOriginalAtTheRealMultipartBoundary. Actual RED executes5 tests/5 failures/zero errors/skips before production edits. XML/logs are retained outside Git. Runner safety unittest9/9 passes. First diagnostic compilation fails on unavailable FileObserver.ISDIR; inspecting the actual directory on CREATE/MOVED_TO corrects that test-only compilation error. Fixed instrumentation compilation passes; current local/hosted journey results are recorded on PR8 after execution.

## Not tested

No live/paid API, private key, phone, visual fidelity or physical Samsung test runs. This diagnostic checkpoint intentionally leaves new recipe assertions RED and full Android acceptance unresolved.

## Risks

Logging can alter scheduling and observed StateFlow values are not every internal assignment. Actual dispatched-call/job/persistence evidence remains distinct from inference. Synthetic logs contain no prompts, artwork bytes, SQL parameters or credentials. All existing assertions, consumption safeguards, legacy/editor and Room/signing paths remain.

## Rollback

An ordinary reviewed feature-branch revert removes test diagnostics without a stored-data conversion. Keep failing evidence and do not reset journals, retry uncertain requests or rewrite history.

## Documentation updated

Handoff records source baseline, RED scope and next bounded task. Workflow and offline QA explain trace files and their observational limits. No milestone or architecture decision changes in this test-only checkpoint.

## Follow-up

Use the first hosted trace to establish the specific event ordering, then at most two justified corrective cycles. Improve the existing bounded recipe/prompt only after the recorded RED, retaining V1 evidence/readability, request count and final-head gates. Keep PR8 draft/open/unmerged and audit BLOCKED until justified.
