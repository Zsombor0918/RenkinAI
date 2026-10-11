# Complete regression evidence collection

## Summary

Retain full-suite XML and numeric results after isolated real-form regressions,
so the final audit can cite actual counts rather than infer them from source.

## Why

The correction at `ae80fbd818eb17c98a7463f689603cfb94dcbaa3` passed original
Android CI 38152173163, including compile, all unit tests and real debug APK.
Regression 38152173157 also passed original creation/ingress separately, together
and all three new form tests. The unchanged original CI logs no numeric passing
count and uploads only the APK on success; preserve full XML independently.

## Scope / changed files

- `scripts/run_compose_regressions.py`

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

Only the evidence runner changes. Product/test assertions, dependencies, original
Android CI, shared governance validators and hooks remain unchanged.

## Behavior before / after

After the four filtered runs, execute compileDebugKotlin testDebugUnitTest assembleDebug
without a test filter and retain the full XML/HTML/log/count evidence at that exact
HEAD. Validate that every requested class/method is actually present; missing selectors,
nonzero exit, failure/error/skipped or empty reports cannot pass. The original full
Android CI remains independently mandatory and is not replaced by this runner.

## Tests actually run

At ae80fbd8, Android CI 38152173163 returned BUILD SUCCESSFUL for compile/unit/APK
and uploaded debug artifact 11707221864. Regression 38152173157 reported creation
1/1, ingress 1/1, combined 2/2 and shared form 3/3, zero errors/failures/skipped,
all exit zero; artifact 11707590246 retains XML. Local 45 guards passed in 14.198
seconds; staged/range/hook checks passed. Fresh full-evidence runner execution is pending.

## Not tested

No full numeric passing suite count is asserted from the original success log.
The new runner step has not executed yet. All physical device scenarios and live
paid providers remain NOT RUN; no signing/install/update or Play approval is claimed.

## Risks

The extra complete run costs CI time but provides auditable exact-head counts and
detects order dependence after filtered execution. It does not expand product scope
or remove test coverage. Device UI and larger dialog widths still require the manual matrix.

## Rollback

Remove the extra evidence step if replacing it with equivalent complete XML/count
capture. Keep the original full CI and both formerly blocked real-form regressions;
no application or data rollback is needed.

## Documentation updated

WORKFLOW explains unfiltered full evidence and selector validation. CURRENT_STATE,
HANDOFF, BASELINE and TEST_MATRIX record the observed correction-head successes and
missing numeric/device proof, preserving all prior failed checkpoints as historical.

## Follow-up

Verify all final-head runs, exact full numeric count and debug APK before updating
the single PR audit result. Missing device tests require explicit owner/reviewer risk
acceptance, not an automatic merge. Later Codex phases remain separate work.
