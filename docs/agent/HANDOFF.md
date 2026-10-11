# Agent handoff — Codex 01 ProjectHome

Work date: October 11, 2026. Branch: `feat/project-home-m1`, based exclusively on
accepted main `005cffa6d51b11e15c20e68349c0c4ce0f23cd8f`. PR #1 was verified
merged at 12:15:29 UTC; its P1 single-result fix/tests are in main and its original
review thread is resolved. No open PRs and a clean tree preceded this branch.
Exact final head, PR URL and Actions results belong in the feature PR audit after
committing. Owner acceptance/merge of Codex 01 is still pending.

## Implemented scope

- Real profiles keyed by ID, including default/duplicate/long names, description,
  saved unbuilt flag and active-session unsaved edits; explicit loading/error/busy.
- Equal Create/Complete cards defer automatic AI/style/source coverage. Create
  reuses the capped profile form/API. Complete explains the real source card/picker
  with working classic-editor and separate-profile actions.
- One Activity MainViewModel/provider owns the session. Explicit async completion
  waits for load and baseline; dirty different-project/Create requests allow Save
  or Cancel. Origin and dirty state are rechecked inside the existing mutex.
- Repeated acceptance is synchronously guarded. Recreation retains the VM job;
  process restoration never replays Create. Any committed profile remains a real,
  discoverable project after load failure/interruption. Cancel allocates nothing.
- MainColumn remains the full editor. Saved classic UI state and current VM
  icons survive Home returns. Child/search Back precedes Home navigation; standalone
  MainColumn retains double-Back exit. Intro progress/dismissal is retained. Classic
  refresh does not unmount the editor. Editor loading conceals content but retains
  its Activity-result registrations under a stable composition key. Home alone
  saves/switches/builds/requests nothing.
- Watch/image ingress opens Editor; pending watch and other VM-owned legacy prompts
  take priority. The original URI state key suppresses recreation imports; the toast
  explains the Upload tab. External Activity results and installer onResume remain
  in the same Activity/VM. Backup/restore/delete/import remain in the classic UI.

Files and rationale: [change note](../changes/2026-10-11-project-home.md).
Session diagram: [ARCHITECTURE](../ARCHITECTURE.md); proposed decision:
[ADR 0002](../decisions/0002-profile-backed-project-home.md).
Fresh evidence and missing coverage: [BASELINE](../qa/BASELINE.md),
[TEST_MATRIX](../qa/TEST_MATRIX.md). Codex 00 notes and the October 4 handoff are
historical; their 592-test claim is not a new result.

## Verification and open risks

### Targeted Compose correction — current checkpoint

Starting PR head `8bd57953ec54c7056dac4acf850f5a8863b8bd64` is preserved in all
production files. The measured blocker is Robolectric 4.14.1 WRAP_CONTENT dialog
measure/layout with a custom 450 dp width, not profile/session logic or clock work.
Width-only native SDK 33 controls in
[38151317631](https://github.com/Zsombor0918/RenkinAI/actions/runs/38151317631)
passed the same actual form and shell at 320×800 dp. Explicit unchanged measured
450×416 px window bounds also passed; actual draw, SDK 35 and legacy graphics did not.
The exact internal platform call is not independently proven on a device.

Both original cases now use 320×800 dp, preserving every action/assertion and native
rendering. New shared-form tests cover required/trimmed/capped input, restoration
and Cancel; no profile allocation or API calls. Temporary probes and negative
experiments are removed. The permanent regression runner checks both originals
separately/together and retains complete XML/SHA/count evidence; full Android CI
and governance are unchanged. [Correction note](../changes/2026-10-11-compose-dialog-regression-fix.md)
contains measurements, boundaries, preserved scope and rollback. Final correction
`ae80fbd818eb17c98a7463f689603cfb94dcbaa3` passed
[Android 38152173163](https://github.com/Zsombor0918/RenkinAI/actions/runs/38152173163)
compile/unit/debug APK, and [regressions 38152173157](https://github.com/Zsombor0918/RenkinAI/actions/runs/38152173157)
passed original creation 1/1, ingress 1/1, combined 2/2 and form 3/3, with no skips.
The original success log has no numeric suite count; the evidence runner now retains
an additional unfiltered complete compile/test/APK run and verifies actual selectors.
At evidence head `b9ec57b59671ffdb36f4f091706c8f7ae62825f3`,
[38152705800](https://github.com/Zsombor0918/RenkinAI/actions/runs/38152705800)
passed creation 1/1, ingress 1/1, combined 2/2, form 3/3 and the unfiltered
compile/test/debug build: **631/631**, zero failures/errors/skips. All requested
selectors occurred; 116 XML files and SHA/count summary were independently inspected
in artifact 11707282850. Original [Android CI 38152705787](https://github.com/Zsombor0918/RenkinAI/actions/runs/38152705787)
independently passed and uploaded real debug APK artifact 11707013144.
Local 45 governance tests passed again in 13.224 seconds; seven offline runner
fault-injection cases passed (fixture Gradle, not Android), rejecting missing
selectors/reports, empty/skipped/failed results and nonzero Gradle exits.
The final documentation commit must have its own green checks; exact final SHA
belongs in PR #2. Missing devices need PASS WITH RISKS and an authorized owner/reviewer
decision; no automatic merge. Main remains `005cffa6d51b11e15c20e68349c0c4ce0f23cd8f`.

### Earlier failed checkpoints — historical

45 offline governance tests freshly passed twice. Baseline local
`./gradlew compileDebugKotlin testDebugUnitTest assembleDebug` stopped before
configuration at Gradle download: `Network is unreachable`. Environment:
Linux/Python 3.12.14/Git 2.51.1/JDK 17.0.20, no SDK/device. Existing main-merge
[CI 38138580216](https://github.com/Zsombor0918/RenkinAI/actions/runs/38138580216)
was rechecked successful, as baseline evidence only. New Kotlin/Compose tests
require feature-head CI; results are recorded after completion in QA/the PR audit.
First feature head `b60b477864fd50a9776f1f1b2c85d5226e9ccfc1` compiled in
[CI 38140946784](https://github.com/Zsombor0918/RenkinAI/actions/runs/38140946784),
then unit-test compilation failed on DpRect width/height property use, before any
unit execution/APK. Follow-up uses edge differences and tests retained editor-entry
version plus intro dismissal across pending preference reads. It also persists
ingress in SavedStateHandle. Details: [recreation note](../changes/2026-10-11-project-home-recreation.md).
Draft PR [#2](https://github.com/Zsombor0918/RenkinAI/pull/2) stays BLOCKED while
its own fresh full build results are pending; the guard correctly rejected that
declaration even though its 45 offline tests and commit conventions passed in CI.

At follow-up head `4c218c0e8e3862dfe9ddcd033bb27a7853ba6587`,
[CI 38141510224](https://github.com/Zsombor0918/RenkinAI/actions/runs/38141510224)
passed app/test compilation and executed 627 tests: **2 FAILED**, both shell tests
waiting for Compose idle after opening the profile form. No debug APK was built.
The next follow-up bounds the shared form fields, retains Editor Activity-result
registration during profile loading/early ID changes, clears stale profile data on
list retry, and adds an actual ActivityResultRegistry regression. These changes
require a new complete CI run; they are not a claimed successful build.
Details: [editor-session note](../changes/2026-10-11-editor-session-lifetime.md).

Editor-session head `6435d8486463ea3709c0ca2aa63fe80da7d5f311`,
[CI 38142645850](https://github.com/Zsombor0918/RenkinAI/actions/runs/38142645850),
compiled app/tests and ran 628 tests: **2 FAILED**, the same form-idle cases.
The new actual result-registry regression passed. Bounded widths alone did not
fix automatic-clock idle. A narrow test-only follow-up drives Compose frames
explicitly in those two form tests, keeping their real inputs, actions, restoration
and route assertions. It must pass fresh CI; no timeout increase or test removal.

That explicit-clock checkpoint `277d463e2cbb95e256d75a068b2dcc28375dee96`,
[CI 38143220425](https://github.com/Zsombor0918/RenkinAI/actions/runs/38143220425),
also executed 628 tests with the same **2 FAILED**. Clock changes were ineffective
and are removed. The next narrow test-only change uses a standard 450 × 800 dp
phone viewport for those real-dialog tests while keeping native graphics, normal
idling, real input and all assertions. This is isolation pending verification,
not a claimed production/dialog fix. Wide/foldable Home coverage is unchanged.

Phone checkpoint `8bd57953ec54c7056dac4acf850f5a8863b8bd64`,
[CI 38143988918](https://github.com/Zsombor0918/RenkinAI/actions/runs/38143988918),
also compiled and ran 628 tests with the same **2 FAILED**; APK skipped. Its
downloaded artifact has 135 HTML/support entries and zero XML. Full stack inspection
shows idle waiting before first form input or ingress update, without identifying
the busy flag. No new production fix is justified yet. The diagnostic follow-up
isolates plain content, empty dialog, individual labelled/unlabelled/supporting
fields, three fields, the real form and real shell. It captures installed idler
flags, snapshot write stacks and complete XML independently, with nonzero failure.
See [diagnosis note](../changes/2026-10-11-compose-idling-diagnosis.md).
Initial diagnostic head `e8c2ee56d0481d50ba5cd4deb47566ecd2f78038` failed
test compilation because a public JUnit property exposed an internal probe class.
A test-only visibility correction precedes remeasurement; it does not establish
the original idle cause. All 45 local guards freshly passed again.
At `f67c3a6a020be922274fdb02ffc02b26130da2c6`,
[isolation 38149176046](https://github.com/Zsombor0918/RenkinAI/actions/runs/38149176046)
ran nine cases individually: plain/empty dialog/field outside passed; six
field-in-dialog cases failed with only hadPendingMeasureLayout true. The dialog
root was attached, unfocused, pending measurement and requesting layout; no clock,
setContent, snapshot or recomposer pending flag. This establishes the narrow boundary,
not yet a missing-draw/component root cause. Draw and SDK controls are next; see
[experiment note](../changes/2026-10-11-dialog-layout-experiments.md).
Controls on `bb2dde21c5439bfda45a056ddea640dc41c6670a` failed on SDK 35 too;
the first draw control did not draw because it ran before a dialog existed.
Late write samples identify CoreTextField measurement and positioned coordinates.
Refined event-based native draw/window-bounds/legacy comparisons remain diagnostic,
not a production fix. See [window controls](../changes/2026-10-11-dialog-window-controls.md).
Upstream [Robolectric issue 8460](https://github.com/robolectric/robolectric/issues/8460)
describes the same custom-width dialog/field freeze. The next independent controls
change only 450 → 320 dp width, keeping 800 dp height, SDK 33/native and actual form
and shell. This must be verified locally to this PR's CI, not inferred from the
external report. See [width note](../changes/2026-10-11-dialog-width-isolation.md).

No schema, SDK, permission, app/pack identity, signing key, APK builder, installer,
credential store, AI1/AI2 or license change. No live paid API request. Fake sessions
are not a full Activity/device test. Manual Apply/Cancel, source picker, GlobalOptions
results, wallpaper/build/install, notifications, SEND/Upload, backup/restore, process
death, TalkBack, One UI/Theme Park and fold/cover/rotation are NOT RUN without a device.
Unsaved icons remain session-only until save/build. Room creation plus DataStore/icon
loading is not a cross-store transaction; inspect the list after interrupted Create.

## Main protection — actual observation and owner action

Ruleset 24877351 is active on main; protected true, PR and `build`,
`commit-conventions`, `documentation-audit` required, force push/deletion restricted,
no bypasses. October 11 12:16 UTC read: review count **0**, latest-push approval
**false**, conversation resolution **false**, strict checks **false**. This
supersedes the earlier latest-push block. The connector has no administration
write; this work did not weaken settings.
Rechecked October 11 at **15:52 UTC**: the same values remain, bypass list empty,
exact three required contexts intact. PR #2 has no submitted reviews or review
threads and is open/unmerged; passing automation is not owner acceptance.

Owner action: Settings → Rules → Rulesets → main-reviewed-and-audited → Edit
([existing rule](https://github.com/Zsombor0918/RenkinAI/rules/24877351)); require
**1** approving review, most-recent-push approval and **conversation resolution**,
retain the three checks/empty bypasses, Save and reread settings. Up-to-date checks
are recommended. Invite an eligible independent reviewer if needed. Full steps:
[WORKFLOW](WORKFLOW.md). Acceptance of this feature still requires an authorized
owner/reviewer decision; do not merge automatically.

## Next safe task

Verify the targeted correction's original isolated/combined tests, full suite and
APK plus governance at the final head; then review this feature, decide ADR 0002 and complete the device
matrix before acceptance. Do not call unresolved editor/profile regressions done.
After explicit acceptance/merge, fetch fresh main for separate Codex 02:
CreateProjectFlow/ExtendProjectFlow plus IconCoverageAnalyzer from component mapping
**and actual drawable decoding**. New packs target the whole selected launcher
set. `createdIcon == null` in old batch logic is not source coverage. Preserve
profile IDs, locks/provenance and the same pack pipeline.

Codex 03 style/asset persistence precedes 04 analysis, 05 trial icons, 06 approved
staging and 07 AI editing. These are not implemented. Backend/billing/auto-monitor,
renderer or installer replacement requires separate authorization.
