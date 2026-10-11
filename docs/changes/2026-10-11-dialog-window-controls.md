# Dialog window measurement controls

## Summary

Refine the measured platform controls without changing the application or the
original SDK 33/native interaction tests. A window must exist before it is examined.

## Why

Run 38150132443 on `bb2dde21c5439bfda45a056ddea640dc41c6670a` showed the
same pending-layout loop on SDK 35. Its draw case failed before drawing because
the dialog was not yet created; a single frame was not an observed completion.
Late samples identify repeated CoreTextField measure and positioned-coordinate writes.

## Scope / changed files

- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProfileDialogDiagnosticsTest.kt`
- `scripts/run_compose_diagnostics.py`

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

The controls render the actual form and continue to require normal idling and text
input. Existing product code, SDK 33/native tests and full Android CI remain intact.

## Behavior before / after

Use the standard Compose waitUntil on an attached, measured dialog window rather
than assuming one frame completes its asynchronous opening. Compare one real native
Canvas draw, window layout parameters fixed to its already measured bounds, and a
legacy graphics control. Legacy is diagnostic only; it does not replace native tests.
Log sampled TextLayoutResult constraints to discriminate repetitive measurement.

## Tests actually run

Downloaded full XML from controls artifact 11706582995. SDK 35 failed after 60
seconds with hadPendingMeasureLayout true, other pending flags false, 2347475 writes
and 195627 applies. The draw experiment did not execute and is not draw evidence.
The earlier full suite ran 637 tests with 8 failures, including the same two originals.

## Not tested

Refined controls require fresh CI. No device rendering, real profile action or
AI call was tested. There is still no justified production UX change or accepted fix.

## Risks

Fixed platform bounds might identify a Robolectric window negotiation issue but
could also alter measurement; interpret it against the same measured size and the
unchanged actual form. The graphics comparison must not be used to remove native
coverage. Bounded diagnostic samples can omit events, so flags alone are not causality.

## Rollback

Remove temporary controls and restore the runner when evidence has been captured.
Keep the original user-interaction tests and final useful regression checks.

## Documentation updated

WORKFLOW states the new event-based window controls. HANDOFF and BASELINE record
the SDK 35 failure and that the first draw experiment did not actually draw.
Architecture, roadmap and product behavior remain unchanged by instrumentation.

## Follow-up

Examine draw, fixed-bounds and graphics controls, choose a supported minimal fix,
then require isolated original tests, combined execution, full suite, APK and final
governance. PR #2 remains BLOCKED; do not merge or implement later feature phases.
