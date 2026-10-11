# Measured dialog layout boundary and controls

## Summary

After independent isolation established a pending dialog layout, add a bounded
actual platform draw and SDK 35 comparison to discriminate environment hypotheses.
This is a diagnostic experiment, not an application fix or accepted root cause.

## Why

On `f67c3a6a020be922274fdb02ffc02b26130da2c6`, diagnosis run 38149176046
passed plain content, empty RenkinAlertDialog and a labelled field outside Dialog.
Every field-in-dialog variant failed, including no label; real form and shell failed
independently. Only hadPendingMeasureLayout was true, with no clock/snapshot/recomposer
pending work. The attached, unfocused dialog root repeatedly requested layout.

## Scope / changed files

- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProfileDialogDiagnosticsTest.kt`
- `scripts/run_compose_diagnostics.py`

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

Keep the original real interactions and SDK 33/native regression tests unchanged.
The temporary runner now executes only the two new discriminating experiments.

## Behavior before / after

Previously the generic timeout did not identify its busy flag. Measurements now
establish persistent measure/layout in a field-containing dialog, independent of
the shell, labels or test order. The new draw case renders one already laid-out
real window to a native Canvas, then requires normal idle and actual text input.
It neither marks the idler idle nor increases timeouts. SDK 35 is a comparison,
not a replacement for SDK 33. Sample later snapshot writers to expose steady loops.

## Tests actually run

Diagnosis 38149176046 individually ran nine cases: three passed, six failed.
Each complete XML stack and measured flags was downloaded from artifact 11706392537.
The actual form recorded 1255977 snapshot writes and 104669 applies in 60 seconds,
while the outer content composed once; only pending layout blocked the final idle.
The new experiments still require fresh Actions execution.

## Not tested

There is no physical device result, and no evidence yet that missing draw is the
cause. The SDK/platform comparison remains pending. No real profile, API or install
operation is performed by these diagnostic cases.

## Risks

A manual platform draw could discriminate missing frame completion but does not
alone prove device compatibility. Reflection is temporary and measured flags must
be interpreted with full stacks and control cases. Do not change product UI from
these hypotheses without the next experiment's evidence.

## Rollback

Remove the two temporary control cases and restore the runner's case list. Preserve
the captured XML evidence and the original interaction tests; no app data changes.

## Documentation updated

WORKFLOW describes the discriminating controls; HANDOFF records the measured boundary.
BASELINE retains the unsuccessful attempts and the actual isolation outcomes.
No architectural or milestone change is justified by diagnostic instrumentation.

## Follow-up

Choose the minimal compatible fix only after the draw/SDK results and steady-state
writers are inspected. Verify the two original cases individually and together,
then full compile/unit/APK and final governance. Keep PR #2 BLOCKED and unmerged.
