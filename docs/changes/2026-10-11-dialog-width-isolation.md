# Dialog width-only comparison

## Summary

Compare the actual form and shell at 320 dp against their independently failing
450 dp cases, changing only width. Original required product tests remain intact.

## Why

The measured busy condition is pending layout in a TextField-containing dialog,
independent of labels, shell, test order or SDK 33/35. Upstream Robolectric issue
8460 reports the same custom-width qualifier failure, including with 4.16.1.
Use our own width-only native comparison to prove or reject this boundary.

## Scope / changed files

- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProfileDialogDiagnosticsTest.kt`
- `scripts/run_compose_diagnostics.py`

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

No application, dependency or original test changes. Both new controls preserve
SDK 33, native graphics, 800 dp height, normal idling and real input interactions.

## Behavior before / after

Previously width remained 450 dp while height/clock/field bounds were varied,
so those attempts did not isolate width. The new controls use 320 dp, retaining
everything else. This is a controlled environment comparison, not an app UX change.
No graphics mode, idling condition, timeout or user action is removed.

## Tests actually run

The nine independent and full-suite XML/HTML diagnostics show the same sole layout
blocker. SDK 35 also fails. Upstream issue 8460 and its comments were inspected as
supporting hypotheses; our width-only cases have not run yet and are not called passes.

## Not tested

No device result or final original-test success exists yet. The platform draw and
bounds controls remain diagnostic and cannot be called successful fixes.

## Risks

A compatible test viewport does not prove large-device form rendering. Retain
separate wide/foldable Home tests and the device matrix. Do not alter production
dialog properties or replace native rendering based only on an upstream report.

## Rollback

Remove the comparison cases and temporary runner when evidence is captured. Keep
the original tests, actual user interactions and the final useful form regressions.

## Documentation updated

WORKFLOW and HANDOFF describe the width-only discriminator. BASELINE preserves
failed history and explicitly distinguishes the upstream report from our evidence.
The product architecture and roadmap are unchanged by temporary isolation.

## Follow-up

If the unchanged actual form and shell pass only at matched width, use the smallest
test-environment correction, verify both originals separately and together, clean
temporary diagnostics and require full suite/APK/final governance. Do not merge.
