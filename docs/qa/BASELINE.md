# Verification baseline

## Codex 01 ProjectHome — current checkpoint

Date: October 11, 2026. Branch `feat/project-home-m1`; accepted main/base
`005cffa6d51b11e15c20e68349c0c4ce0f23cd8f`. Source/special routes reread before
implementation; #1 merged and its P1 fix/tests/thread verified.

| Check | Actual result | Boundary |
| --- | --- | --- |
| Git/main/PR gate | Clean branch from accepted main; initially no open PRs | No main write or merge in this work |
| `python3 -m unittest discover -s scripts/tests -v` | 45 passed, exit 0, freshly run twice | P1/hook/CI parity; Linux/Python 3.12.14/Git 2.51.1 |
| `./gradlew compileDebugKotlin testDebugUnitTest assembleDebug` at baseline | Failed, exit 1 at Gradle 8.11.1 download | `java.net.SocketException: Network is unreachable`; JDK 17.0.20, no SDK/device; no local Android test/count/APK |
| Main merge [CI 38138580216](https://github.com/Zsombor0918/RenkinAI/actions/runs/38138580216) | Existing push run rechecked successful on main SHA | Baseline only, not new ProjectHome validation |
| New Home/Shell/Session/ingress tests | Added; Android execution pending feature CI | Fake async state and real Compose/Back components; IDs/states, no replay, restoration, light/dark/hinge/48dp; not full Activity/device/pixel proof |
| Ruleset/branch at 12:16 UTC | Active/protected, PR and three checks; no bypass, deletion/force push blocked | Review count 0, last-push false, conversation false, strict false; owner must enable missing review/conversation requirements |
| Feature [CI 38140946784](https://github.com/Zsombor0918/RenkinAI/actions/runs/38140946784), head `b60b477864fd50a9776f1f1b2c85d5226e9ccfc1` | App compile SUCCESS; unit-test compile FAILED on DpRect width/height | Unit tests/APK did not execute; follow-up uses edge differences; no false suite success |
| Feature [Governance 38140946802](https://github.com/Zsombor0918/RenkinAI/actions/runs/38140946802), same head | Commit conventions PASS; 45 guard tests PASS; audit intentionally FAILED on BLOCKED draft | Log proves BLOCKED exit propagation; source/link/shape checks had no other errors |
| Feature [CI 38141510224](https://github.com/Zsombor0918/RenkinAI/actions/runs/38141510224), head `4c218c0e8e3862dfe9ddcd033bb27a7853ba6587` | App/test compile SUCCESS; 627 executed, **2 FAILED** | Both ProjectShellComposeTest form-opening cases hit AppNotIdleException; other tests passed; APK skipped. Failure-report artifact inspected |
| Feature [CI 38142645850](https://github.com/Zsombor0918/RenkinAI/actions/runs/38142645850), head `6435d8486463ea3709c0ca2aa63fe80da7d5f311` | App/test compile SUCCESS; 628 executed, **2 FAILED** | Same two form-idle failures remained after bounding widths. New ActivityResultRegistry loading/ID-change regression passed; APK skipped |
| Feature [CI 38143220425](https://github.com/Zsombor0918/RenkinAI/actions/runs/38143220425), head `277d463e2cbb95e256d75a068b2dcc28375dee96` | App/test compile SUCCESS; 628 executed, **2 FAILED** | Explicit clock also did not resolve the same form-idle failures; APK skipped |
| Feature [CI 38143988918](https://github.com/Zsombor0918/RenkinAI/actions/runs/38143988918), head `8bd57953ec54c7056dac4acf850f5a8863b8bd64` | App/test compile SUCCESS; 628 executed, **2 FAILED** | Phone viewport also ineffective; APK skipped; artifact has HTML only, no XML |
| Diagnostic [CI 38148847598](https://github.com/Zsombor0918/RenkinAI/actions/runs/38148847598), head `e8c2ee56d0481d50ba5cd4deb47566ecd2f78038` | App compile SUCCESS; test compile FAILED | Public rule exposed internal probe type; visibility corrected next; no executed tests/APK |
| [Isolation 38149176046](https://github.com/Zsombor0918/RenkinAI/actions/runs/38149176046), head `f67c3a6a020be922274fdb02ffc02b26130da2c6` | Nine independent cases: 3 passed, 6 failed | Plain/empty dialog/outside field pass. Every field-in-dialog variant failed only pending measure/layout; resolved UI/runtime/UI-test 1.9.3, Material3 1.5.0-alpha14, Robolectric 4.14.1; full XML retained |
| Full [CI 38149176086](https://github.com/Zsombor0918/RenkinAI/actions/runs/38149176086), same head | 637 executed, 8 FAILED; app/test compile success; APK skipped | Six diagnostic reproductions plus the two original cases; original dialog roots have the same sole pending-layout flag |
| [Controls 38150132443](https://github.com/Zsombor0918/RenkinAI/actions/runs/38150132443), head `bb2dde21c5439bfda45a056ddea640dc41c6670a` | SDK 35 also FAILED; first draw control failed before drawing | No draw conclusion; real window-availability condition added next |
| [Window controls 38150826550](https://github.com/Zsombor0918/RenkinAI/actions/runs/38150826550), head `8c1dddc4c3a75ffc0dd612fab543c5e3ed2de64d` | Native draw FAILED; same 450×416 px explicit bounds PASSED; legacy FAILED | Only exact platform bounds cleared pending layout; no production sizing change used |
| [Width-only 38151317631](https://github.com/Zsombor0918/RenkinAI/actions/runs/38151317631), head `7aad5bd410e9d2dcc88188028671c4f356755eb0` | Actual form and shell both PASSED separately at 320×800 dp | Same SDK 33/native/height/components/input; all busy flags false; artifact 11706736726 contains full XML |
| Corrected [Android 38152173163](https://github.com/Zsombor0918/RenkinAI/actions/runs/38152173163), head `ae80fbd818eb17c98a7463f689603cfb94dcbaa3` | Compile, full unit task, debug APK/upload all SUCCESS | Success log does not print numeric full count; artifact 11707221864 is real APK, not device installation proof |
| Corrected [regressions 38152173157](https://github.com/Zsombor0918/RenkinAI/actions/runs/38152173157), same head | Creation 1/1, ingress 1/1, combined 2/2, shared form 3/3, all exit zero | Zero failures/errors/skips; exact-head XML artifact 11707590246; full numeric collection added next |
| Complete [regressions 38152705800](https://github.com/Zsombor0918/RenkinAI/actions/runs/38152705800), head `b9ec57b59671ffdb36f4f091706c8f7ae62825f3` | Creation 1/1; ingress 1/1; both 2/2; form 3/3; unfiltered **631/631** and debug build SUCCESS | Zero failures/errors/skips/missing selectors; independently summed 116 XML reports in artifact 11707282850 and verified summary HEAD |
| Original [CI 38152705787](https://github.com/Zsombor0918/RenkinAI/actions/runs/38152705787), same head | Compile/unit/debug APK/upload SUCCESS | Real APK artifact 11707013144; no device install or release certificate proof |
| Local checks at complete evidence head | 45 guards PASS, 13.224 s; 7 runner fault-injection modes PASS | Positive plus nonzero/missing selector/missing report/empty/skipped/failed fixtures; fixture Gradle is not Android; staged/range/hooks PASS |
| Main/PR/rules read at 15:52 UTC | Main still 005cffa6; rule active/empty bypasses/three checks; PR #2 open/unmerged, no reviews/threads | Review count 0, last-push false, conversation false, strict false; owner actions still required |

Recreation follow-up adds a persisted editor-entry counter and an intro-visibility
helper that distinguishes pending prefs from explicit re-show, plus regressions.
Fresh full feature-head execution remains required after these changes.
The editor-session follow-up bounds profile form fields and tests a real registered
external-result callback across profile loading and ID changes. The retained
composition is concealed while loading; profile identity remains VM-owned.
That run confirms external-result delivery but not successful form interaction.
Explicit clock control also failed and has been removed. The next test-only
follow-up isolates both real-dialog tests at a 450 × 800 dp phone viewport, retaining
native graphics, automatic idling and every input/action/restoration assertion.
Fresh execution failed as recorded above. Wide/hinge Home tests retain their existing viewport;
large real-device forms still require the device matrix.

The latest HTML full stacks show 123016/124285 idle attempts over 60 seconds at
the first text-input lookup / first runOnIdle after Create. They do not expose
the busy condition or prove a Material 3, animation, focus or production defect.
Full XML and native controls above establish the WRAP_CONTENT/custom-width layout
boundary. The final test correction uses 320 dp for the two real-form cases and
adds three shared-form regressions. Temporary probes/negative controls are removed.
Corrected original isolated/combined/form and complete Android tasks passed above.
Complete XML/count collection now passed as recorded above. A final documentation
head must also pass its fresh exact-head Android/regression/governance runs; its
SHA and final statuses are recorded externally in PR #2 to avoid a self-hash cycle.
Upstream issue 8460 is context, not a replacement for these actual CI measurements.

Staged/range/PR audits and final feature-head Actions are recorded after execution
in this ledger or the PR audit. No pending tests or historical count is reported
as PASS. No physical device or live paid provider call.

## Codex 00 — historical verification

Audit date: October 11, 2026. Source baseline main:
`eb4daa5de82fab54e115363c073eb8981f2203d6`. Infrastructure branch:
`chore/agent-docs-governance`. Final head SHA is recorded in the PR audit.

## Owner-authorized closure verification

Closure baseline: `50ccda65720e70bcb31e361b67f76bba59bbdacc`, October 11, 2026.
The owner explicitly accepted the infrastructure work and requested merge of #1.

| Check | Observed result | Evidence boundary |
| --- | --- | --- |
| Main/ruleset API | Active main ruleset 24877351; branch protected true | Correct main target, PR rule, deletion/force-push restrictions, empty bypasses and three required GitHub Actions checks |
| Saved review/status settings | Latest-push approval true; approving-review count zero; conversation resolution false; strict status checks false | Report actual settings, not all recommended settings as implemented |
| [Governance run 38134217603](https://github.com/Zsombor0918/RenkinAI/actions/runs/38134217603) | Both checks successful on closure baseline | Existing results rechecked; 45-test source unchanged, no new local suite run claimed |
| [Android run 38133704315](https://github.com/Zsombor0918/RenkinAI/actions/runs/38133704315) | Build successful on closure baseline | Compile, unit tests, debug APK and upload passed; this is a rechecked prior run |
| P1 review thread / approvals / PR | P1 resolved; no approving review; PR open with merge state blocked | Owner acceptance is not GitHub latest-push approval |
| `python3 scripts/check_docs.py --staged` and `git diff --check` | Passed, exit 0 | Seven documentation paths; required impact documents, complete note and internal links validated |

The closure commit changes Markdown only. Its checks and actual merge result are
recorded in the PR audit after pushing; no merged state is asserted before GitHub
confirms it. Earlier inactive protection observations below are historical.

## Fresh P1 follow-up checks

Follow-up baseline: `ac6dc81f0abbee0ce9f0e43ee66d616761b6dcb1`; same branch and
[PR #1](https://github.com/Zsombor0918/RenkinAI/pull/1), still open and unmerged.
Environment: Python 3.12.14, Git 2.51.1, Linux; October 11, 2026.

| Check | Observed result | Evidence boundary |
| --- | --- | --- |
| `python3 -m unittest discover -s scripts/tests -p test_repo_guards.py -k pr_audit -v` before fix | Expected failure, exit 1 | New regressions reproduced acceptance of PASS plus BLOCKED and duplicate declarations in the original implementation |
| `python3 -m unittest discover -s scripts/tests -v` after fix | 45 tests passed, exit 0 | Original 38 plus seven PR-audit regression methods; actual committed-snapshot/event CLI, LF/CRLF, missing/duplicate/conflicting/malformed/BLOCKED cases |
| `python3 scripts/check_docs.py --staged --event-file ../p1-pr-event.json` | Passed, exit 0, eight changed paths | Staged follow-up, required WORKFLOW/HANDOFF/ADR/new note, all internal links and fetched PR audit body |
| `python3 scripts/check_commit_messages.py --subject 'fix(governance): require exactly one PR audit declaration'` and `git diff --cached --check` | Passed, exit 0 | New subject and whitespace validation; application/build/workflow/hook paths have no staged diff |
| Main configuration recheck, 10:49 UTC | Still inactive in observed APIs | Branch protected false, no required checks, rulesets empty; direct protection read 403. Reported manual setup remains unconfirmed |

Fresh commit SHA, Actions run/job results and the original P1 review-thread state
belong in the PR audit after pushing. Earlier successful CI runs below do not prove
that this changed guard passed in CI. No new local Android build/device run is claimed.

## Original Codex 00 checks — historical to this follow-up

| Check | Observed result | Evidence boundary |
| --- | --- | --- |
| Git/main/PR audit | Clean initial main; no open PR; source paths read | Main HEAD and existing docs/workflows/build/manifest/license inspected |
| `python3 -m unittest discover -s scripts/tests -v` | 38 tests passed | Offline real Git fixtures; hook/CLI parity, bad/good commits/docs/links, index/head and merge-base behavior |
| `./gradlew testDebugUnitTest assembleDebug` | Attempt failed, exit 1 before Gradle configuration | Gradle 8.11.1 download: `java.net.SocketException: Network is unreachable` |
| Android environment | JDK 17.0.20 present; SDK not found in standard inspected locations | No usable cached Gradle distribution; no Android test count or APK produced here |
| Main protection reads | Inactive in observed APIs; no rulesets | Branch protected false; rulesets empty; direct protection read 403 integration restriction |
| [Governance run 38129900629](https://github.com/Zsombor0918/RenkinAI/actions/runs/38129900629) | Both jobs successful on initial PR head `dfe8d45b4287aeddf5ba73f95c67080ceb98832e` | Real PR commits/title/audit/docs + offline guard suite |
| [Android run 38129900648](https://github.com/Zsombor0918/RenkinAI/actions/runs/38129900648) | Failed before compile/tests/APK at SDK setup | v3 default `sdkmanager tools`: Failed to find package 'tools'; corrected by the follow-up note |
| [Governance run 38130194904](https://github.com/Zsombor0918/RenkinAI/actions/runs/38130194904) | Both checks successful after the SDK fix | Head `8481c021818eef6cf14ac8ab3b33593dff2af81a`; commit conventions and docs audit/38 offline guard tests |
| [Android run 38130190199](https://github.com/Zsombor0918/RenkinAI/actions/runs/38130190199) | Compile, unit tests, debug APK and upload all successful | Same head; logs show BUILD SUCCESSFUL for all three Gradle commands; fresh unit count is not reported in logs |
| [Debug APK artifact](https://github.com/Zsombor0918/RenkinAI/actions/runs/38130190199/artifacts/11699098599) | RenkinAI-debug uploaded successfully | CI test artifact; no device installation or stable update signing asserted |

Staged/range checks passed for the original implementation and SDK follow-up. The
earlier QA checkpoint changed only Markdown. The P1 correction now changes the
guard/tests; Android source, tests, build files and CI remain identical to the
previously verified head. Fresh-head CI status and commit SHA are in PR #1.
Only a completed workflow's observed result can be called a fresh CI pass.
Governance checks being present in source does not make them required on main.

## Historical evidence, not rerun

[AI_HANDOFF_HU](../AI_HANDOFF_HU.md) reports a successful October 4 build, 592
tests and a matching debug certificate. This is preserved historical author
evidence; Codex 00 does not re-attest that count, certificate or device behavior.
The initial main HEAD had no GitHub Actions run returned by the workflow-run query.

## Not exercised

No physical device/emulator, live OpenAI/Stability call, actual key decryption,
backup restore/upgrade, pack installation/update, Theme Park, One UI or Flip cover
screen was tested. No Play submission or administration write was made.
Use [TEST_MATRIX](TEST_MATRIX.md) for the remaining scenarios.

## Codex 02 fresh baseline and implementation checkpoint

Started on fresh main `73a15e2210cb55cf003e60ebc2033130ee7a9e58`, owner-merged
PR #2 with identical tree to final feature head. The historical M1 631 count is not
fresh M2 evidence. Python 3.12.14, Git 2.51.1, Linux/JDK 17 workspace.

| Actual command / observation | Result | Boundary |
| --- | --- | --- |
| `python3 -m unittest discover -s scripts/tests -v` baseline | 45 PASS, 7.025 s | Offline governance only |
| Same guards after M2 implementation | 45 PASS, 6.912 s | Existing validators unchanged |
| `./gradlew compileDebugKotlin testDebugUnitTest assembleDebug` | FAILED, exit 1 | Gradle 8.11.1 download: `java.net.SocketException: Network is unreachable`; stopped before configuration; no Android tests/APK locally |
| Fresh M2 Actions | Not yet observed at this checkpoint | Final head must pass original Android, expanded regression evidence and governance |
| Paid/live API and device trials | NOT RUN | No API request authorized; no device/SDK available locally |

The expanded evidence runner preserves all old selectors and adds CreateExtendFlowComposeTest,
IconCoverageAnalyzerTest, CoverageAppFilterTest and CoveragePreviewTest. New fixtures
include actual vector/adaptive/bitmap draw and parser last-winner/error behavior;
existing profile, AI, batch, backup, package/build regressions remain in the full suite.

### M2 first hosted checkpoint — not successful

Feature head `28ed77265fbc392f88f07f905cccb046968e75de`,
[Android 38159074131](https://github.com/Zsombor0918/RenkinAI/actions/runs/38159074131):
production `compileDebugKotlin` PASSED; unit-test Kotlin compilation FAILED at new
CreateExtendFlowComposeTest line 208. This Compose version returns DpRect with
bottom/top, not a height property; the receiver mismatch is compile-time evidence.
No unit test executed and APK was skipped. Correct only that measurement expression,
preserving the 48dp assertion. Governance commit-conventions and offline guard job
passed; documentation-audit correctly failed the single BLOCKED declaration.

### M2 executable checkpoint — fixtures corrected next

At `4a447d88dea9506aecf3b6c42bb45429dd5d2b78`,
[Android 38159397296](https://github.com/Zsombor0918/RenkinAI/actions/runs/38159397296)
compiled app/tests, then ran **667 tests, 3 failures**, APK skipped.
Artifact 11708809083 HTML stack traces were downloaded and inspected:
CoveragePreviewTest passed raw XmlPullAttributes to the platform vector inflater,
which requires compiled XmlBlock.Parser; use the existing compiled ic_github vector.
The coverage node already contained all correct counts, but assertTextContains
requested exact matching to a fragment; assert the full observed count string.
ProfileDetailsDialog uses the dismiss resource, not literal Cancel; use that actual
resource label to test the unchanged dismissal interaction. These are fixture/API
errors, not evidence that source draw or dialog UX should be changed.

A scope-completeness inspection additionally adds screen-scoped package invalidation,
so a source removed/updated while foreground cannot retain its old result until
resume. Displayed theme is an explicit read key; rapid pack rescans retain the newest
loading flag. New actual broadcast/disposal and Back tests are awaiting execution.
