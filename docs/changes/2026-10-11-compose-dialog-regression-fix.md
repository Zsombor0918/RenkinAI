# Profile dialog test environment correction

## Summary

Correct the two existing form tests' Robolectric width to 320 dp after controlled
measurement proved a WRAP_CONTENT window negotiation loop at 450 dp. Keep native
SDK 33, 800 dp height, normal idling and every original user action/assertion.
Add three useful real-form regressions and exact-head isolated/combined CI evidence.
No production Kotlin, XML, dependency or Android build behavior is changed.

## Why

The two original cases failed before typing or ingress because pending measure/layout
never cleared in the field-containing dialog. Independent controls and full-suite
probes ruled out the shell, labels, test order, clock, focus input and SDK 33-specific
failure. Width-only 450 → 320 dp passed the unchanged real form and shell.

| Control | Measured outcome |
| --- | --- |
| Plain content / empty RenkinAlertDialog / field outside Dialog | PASS individually in run 38149176046 |
| Unlabelled, labelled, supporting, three fields / actual form / actual shell at 450 dp | FAIL individually; only hadPendingMeasureLayout true |
| Full suite with probes | 637 executed, 8 FAILED in 38149176086: six reproductions plus both originals |
| SDK 35, same actual form | Same pending-layout failure in 38150132443 |
| One actual native Canvas draw at 450 dp | Still FAIL in 38150826550; draw hypothesis rejected |
| Same 450×416 px real window, explicit measured platform bounds instead of WRAP_CONTENT | PASS with actual text input in 38150826550; pending layout cleared |
| Legacy graphics at 450 dp | Still failed; not used as a fix |
| Same actual form and shell, only width changed to 320 dp | Both PASS individually in 38151317631 on 7aad5bd410e9d2dcc88188028671c4f356755eb0 |

At 450 dp the later TextField measure samples contain 240 and 370 px constraints,
consistent with the window's repeated minimum/wide measurement. The root remains
450×416 with layout requested; neither the test clock nor recomposer is busy.
Robolectric [issue 8460](https://github.com/robolectric/robolectric/issues/8460)
reports the same boundary. Our own controls establish this PR's environment cause;
the exact internal framework call responsible was not patched or independently
proven on a physical Android device. Do not call Material 3 or product logic broken.

## Scope / changed files

- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectShellComposeTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProfileDetailsDialogComposeTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProfileDialogDiagnosticsTest.kt` (removed)
- `scripts/run_compose_regressions.py`
- `scripts/run_compose_diagnostics.py` (removed)
- `.github/workflows/compose-regressions.yml`
- `.github/workflows/compose-diagnostics.yml` (removed)

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

Only test configuration and regression infrastructure change. All existing shell
assertions remain, including creation once/no early editor, saved-state restoration,
external-image entry and pending-watch priority. Large Home/hinge tests remain.

## Behavior before / after

Both form tests previously requested a wider Robolectric screen than the compatible
default dialog width. They now use 320×800 dp, retaining real ProfileDetailsDialog,
RenkinAlertDialog and Material 3; no mocks, sleeps, timeout changes, graphics downgrade
or idling bypass. New form tests exercise required/trimmed payload, capped inputs,
draft restoration and Cancel without confirmation. Temporary reflection/probes,
negative timeout reproductions and platform draw/window overrides are removed.
The regression runner executes both original tests individually, together and the
shared-form class, retains XML/HTML plus SHA/count JSON, and fails on nonzero exit,
missing results, failure/error or skipped test. The original full Android CI is intact.

## Tests actually run

Downloaded complete XML and flags from artifacts 11706392537, 11706582995,
11706800630 and 11706736726; also full-suite HTML and original probe stacks.
The width-only form/shell controls passed with all five busy flags false and
320×416 px dialog roots, no pending layout. Local guard suite passed 45 tests;
local Android download failed with Network is unreachable, not a build success.
Fresh original tests/full suite/APK/guard checks at the final correction SHA are
still required and will be recorded in BASELINE and PR #2 after real execution.

## Not tested

No physical device/emulator, wide-device form render, TalkBack, Samsung/One UI,
Theme Park, real notification/SEND, actual backup, install/update or release signing
verification. No live paid API request. Fake-state shell tests do not prove all
MainActivity lifecycle or every original editor interaction.

## Risks

The test harness now covers form interaction at the compatible 320 dp width;
larger/foldable dialog/device rendering remains manual coverage, not a reported
pass. Dedicated wide/hinge Home tests are unchanged. Application state, profile
ownership, navigation, Activity-result registration and package/signing pipeline
are unchanged by this fix. Owner acceptance must account for missing device checks.

## Rollback

Revert the test-environment correction and regression infrastructure through a PR.
The wider-window idling failures would return; no app data, migration or device UI
rollback is needed. Never remove the original tests or relax governance to pass.

## Documentation updated

CURRENT_STATE explains the measured harness limitation. HANDOFF gives the exact
continuation boundary and device risks. BASELINE/TEST_MATRIX distinguish failed
history, diagnosis and final results. WORKFLOW describes permanent exact-head
regression execution instead of temporary diagnostics. Architecture, roadmap and
ADR need no change because product/session/root design is preserved.

## Follow-up

Require the final commit's isolated creation/ingress, combined/shared-form tests,
full unit suite, real debug APK, 45 guards and all three required contexts.
Keep audit BLOCKED until real evidence permits PASS WITH RISKS for missing device
coverage. Leave owner/reviewer acceptance and merge manual. Codex 02–07 remains out
of scope; next development starts only after an accepted merge and fresh main audit.
