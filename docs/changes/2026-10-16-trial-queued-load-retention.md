# Retain a freshly prepared trial through queued read-only loads

## Summary

Fix the demonstrated hosted Standard plan loss in the existing trial controller. Keep a still-current frozen plan and staged bytes through a local reload, with paid confirmation disabled while revalidation is busy.

## Why

Actual aef0dca0 hosted trace records Prepare dispatch at93.424s, a load queued while busy at93.827s, plan assignment at98.004s, idle at98.012s, pending load busy at98.014s and plan clearing at98.022s. Standard then fails on absent trial-decline; restart lacks Standard completion evidence. Disk snapshot still has revision1, so saved DNA was not lost. Earlier75e polling could miss this short-lived plan; its original interleaving was not traced. This new hosted reproduction proves the queued-load defect rather than inferring it from a passing fixture.

## Scope / changed files

Behavior impact: changed
Architecture impact: changed
Milestone impact: none
Durable decision: recorded

`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleTrials.kt` and `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleTrialsTest.kt` change. Existing ADR0006 and canonical state/architecture/handoff/QA record the revalidation boundary. No UI refactor or journal/schema/transport change.

## Behavior before / after

Before, start.finally drains a pending local load after publishing busy=false; load unconditionally clears the newly prepared plan/images. That can erase a plan before the fixture observes it or remove its confirmation after observation. Retrying clicks or waiting longer cannot correct that application state loss.

Load now retains the visible plan while busy and rechecks fresh owner/route, style/revision/selection/catalog/source, epoch, provider/options, references/recipe and original hash/origin/evidence using the existing current predicate. Only identical context with the same still-present plan and staged bytes survives. Check cancellation identity both before and after suspending verification; decline cannot resurrect a plan. Changed inputs or read failure clear the plan, staging and verification proof. Read all suspending values before the final state merge. Existing submit independently revalidates and still needs explicit consent; local load submits nothing and creates no consumed marker.

## Tests actually run

Retained actual hosted failure artifact from run38901058748 contains both phase logs and trace-0.log. Focused deterministic RED uses CompletableDeferred barriers at original acquisition, queues load during preparation, observes the actual produced plan and completed queued read, then fails expected plan versus null:1 test/1 failure in41s before production edits. No sleeps, retries or widened timeouts. The same test also requires original bytes and zero request/journal activity before a later explicit mock Submit. Negative cases cover original/style/epoch/route changes and decline while original revalidation is suspended. Whole owned-controller plus focused style/gateway/native payload GREEN executes24/24, zero failures/errors/skips in2m17s. Final committed-head checks are recorded separately on PR8.

## Not tested

No live/paid API, phone, private credential, physical Samsung, billing or visual-quality test runs. The original untraced75e event order cannot be recovered; the later traced reproduction and controlled regression establish the demonstrated mechanism. Final hosted Android acceptance must independently execute on the corrected source.

## Risks

Current-input revalidation remains asynchronous and paid confirmation stays disabled while busy. An error or changed context invalidates the plan rather than retaining stale consent. Every actual submission still rechecks and uses existing one-shot durable consumed markers; this is not an automatic retry or recovery permission. Both signing keys, Room21/schema19-21, persistence/history/backups, AI1/AI2/editor/build/install remain unchanged.

## Rollback

Use an ordinary reviewed feature-branch revert or forward repair while retaining failure traces and all consumed IDs. Do not bypass revalidation, erase journals, replay uncertain work or restore a false passing test.

## Documentation updated

CURRENT_STATE and ARCHITECTURE state the proven local-read retention rule; HANDOFF records source/evidence and closes speculative diagnosis. TEST_MATRIX and BASELINE distinguish controlled RED, hosted failure and final committed-source gates. ADR0006 adds the cancellation/revalidation decision.

## Follow-up

This is the second justified emulator correction after the reproduced fixture-thread mismatch. Freeze the source, run final local/hosted Android and full required regressions, and update existing draft PR8 audit/artifact honestly. If Android still fails, stop further speculative changes and report its exact retained trace. No merge or new feature phase.
