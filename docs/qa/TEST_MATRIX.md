# Regression and test matrix

## Corrective acceptance coverage

The actual cfef34e1 hosted run866/10 is captured in BASELINE. Additive tests now
require wire maxItems6, safe HTTP400/402/422/auth404/429/5xx failures without retry,
allowlisted quota with no raw-error leakage, fresh UUID/new consent after rejection,
orphan plan/result durability and old-approval invalidation after a new success.

Routed native SDK33/320dp real-Room/controller tests retain both save orders and
analysis Use/Save, then extend to manual EXTEND with zero refs, dirty-style accessible
reason, changed-source remediation, rejected analysis/manual fallback,1/2/3 frozen
trial apps, decline/paid consent/review/common approval and restoration. Fixtures
remain offline. Explicit scrolling materializes the status assertion; a30-artwork
pool avoids assuming all twelve generated shapes are perceptually different.
No existing selector/idling/timeout is weakened. Final hosted checks are pending.

Measured live coverage is only the authorized one-icon Python Responses contract
probe (HTTP200/618 tokens), not Android/image-generation/phone evidence. Flip8/One UI,
TalkBack/fold, real saved-data upgrade/restore and source-pack failure retest remain
NOT RUN on the corrective APK until the owner verifies the journey.

## October 13 owner blocker additions

ProjectStyleTrialJourneyComposeTest enters the existing trial-ui group: fresh targets,
manual style with zero refs and no key, targets-first/style-first saves,1/3 frozen
apps, separate paid trial consent/review/common approval, EXTEND12-sample analysis,
explicit Use/Save and route restoration. Real Room/controllers/journals participate.
Additional regressions cover safe HTTP400 failure persistence/new UUID consent,
orphan plan/result .new recovery without replay and new unreviewed variant approval
invalidation. These are new assertions awaiting execution, not prior856-test proof.


Accepted baseline is Codex 04, owner-merged PR #5 as af7bdb10. M5 source/testing
is in progress; historical Codex 03 evidence follows. Head 06e71532 passed 718/718 and all filtered
groups/APK including legacy gallery compatibility. The final documentation/probe-
removal head is verified separately in PR #4's audit.
Codex 00/01/02 sections below remain historical, including M2's 669/669 baseline.
A test class existing in source is not a passed run.
Results and environment belong in [BASELINE](BASELINE.md) and each PR audit.

## Codex 01 — feature regression coverage

| Area | Automated coverage and boundary | Required device scenario | Current checkpoint |
| --- | --- | --- | --- |
| Home/accessibility | ProjectHomeComposeTest: states/IDs/duplicates/long titles, tags/descriptions, light/dark/hinge/48dp | TalkBack, fonts, phones/tablets/fold/cover/rotation | Passed in full 38152705800 (631/631); device NOT RUN |
| Navigation/Back | ProjectShellComposeTest: same editor handler, real dialog/search priority, route/draft restore, Home, dirty exit, refresh, Extend, intro dismissal during pending prefs | Full MainColumn nested editor Apply/Cancel, keyboard/dialogs | Originals passed separately/together and full 631/631 in 38152705800; final documentation-head checks in PR #2; full Activity NOT RUN |
| Shared profile form | ProfileDetailsDialogComposeTest: actual required/trimmed/capped fields, confirm once, restored draft/Cancel; SDK 33/native/320 dp, no idling bypass | Wider dialog/font scaling/keyboard/TalkBack | All 3 passed separately and full 631/631 in 38152705800; device NOT RUN |
| Profiles/async | ProjectSessionTest: active no-op, Save/Cancel/origin, queued dirty check, delayed baseline, failure, one create/no replay | Existing/default/delete/import/restore, rapid taps, kill/relaunch | Fake backend and existing manager/gate/migration suites passed in full 38152705800; device NOT RUN |
| External ingress | SharedImageIngressTest uses Activity's URI guard and SavedStateHandle counter; shell tests entry/pending-watch priority and actual ActivityResultRegistry during loading | Notification owning/missing profile, real SEND/Upload, URI grant/recreation | URI/counter, result-registration and formerly blocked ingress passed in full 38152705800; real delivery/device NOT RUN |
| Existing AI1/AI2 | Existing mock AiImagePipeline/AiPersistence/AiBatchRunner suites | Paid trial only under separate authorization | Passed in full 38152705800; no new endpoint/client/batch behavior |
| Global/preview/build | Existing provider/build/signing/installer suites | GlobalOptions Apply/Back/result; BuildPackFab → WallpaperPreview → build/sign → install/update; installer onResume | Existing unit suites passed in full 38152705800; contracts/IDs unchanged; device NOT RUN |
| Data/source/OEM | Existing profile/backup/provenance/render suites | Source picker, backup/restore, default/delete, Theme Park/One UI/cover/fold, old pack update | Existing unit suites passed in full 38152705800; no schema/SDK/permission/key changes; device NOT RUN |
| Governance/enforcement | 45 guard tests passed locally; final three checks must match final PR head | Owner enable reviews/conversations and explicitly accept audit | 45 guards passed fresh; build/regressions passed at b9ec57b5; exact final documentation-head contexts in PR #2; owner review/conversation settings pending |

Final documentation-head compile/unit/APK, regressions and governance must all be
checked on that same SHA; their observed statuses are recorded in PR #2. Fake-state tests do not prove actual MainActivity lifecycle, import, all old
editor interactions or visual QA. Keep the manual scenarios marked NOT RUN.

## Codex 00 — historical matrix

| Area | Automated coverage / command | Required manual evidence | Codex 00 status |
| --- | --- | --- | --- |
| Repo guards | `python3 -m unittest discover -s scripts/tests -v`; valid/invalid commits, missing/empty notes, good docs, bad links, exceptions, hooks, exits | Git Bash/IDE hook setup on Windows | 45 tests passed locally after the P1 correction; native Windows not run |
| PR audit result | Actual CI CLI/event JSON; exactly one PASS or PASS WITH RISKS, LF/CRLF; reject BLOCKED/missing/duplicate/conflicting/malformed declarations in either order | Reviewer verifies declarations against evidence | Seven new regression methods passed locally; original P1 case reproduced before the fix; fresh-head Actions/review status is in PR #1 |
| App compile/unit/build | `compileDebugKotlin testDebugUnitTest assembleDebug`; existing JVM/Robolectric suite | Install the resulting APK and cold start | All three tasks passed in CI 38130190199 after SDK input fix; local download blocked; no fresh numeric test count in CI logs |
| App list/identity | InstalledAppCatalogTest, ApplicationManagerComponentTest | Multiple launcher activities, work profile, reload | Existing Android unit task passed in CI; device coverage outstanding |
| Manual/custom/modifier icons | IconGeneratorTest, IconDraftStateTest, SVG/adaptive/outline tests | Select/import/edit/save/reopen; custom icon survives refresh | Not tested on device |
| Profiles and data | ProfileManagerTest, ProfileOperationGateTest, RenkinPackMigrationTest, backup tests | Unsaved switch, delete/default guards, upgrade/backup/restore | No schema change; Android unit task passed in CI; device coverage outstanding |
| Single AI generation | AiImagePipelineTest, AiPersistenceTest; mock HTTP | Original/ref order, alpha/quality, cancel, profile switch, preview accept | Existing Android unit task passed in CI; no live provider/device call |
| Batch | AiBatchRunnerTest, AiPersistenceTest | 2–5 app trial, pause/cancel/background/process death, retry one uncertain app | Existing Android unit task passed in CI; no actual paid trial |
| Pack build/export | IconPackBuildServiceTest, LauncherFormatTest, PackProvenanceTest | appfilter/appmap/theme_resources, calendar/clock/adaptive/themed launcher behavior | Android unit task and app APK build passed in CI; no device-generated pack here |
| Signing/install | PackKeystoreTest, InstallerSelectionTest, InstallFallbackTest | Stable key, same-package update/conflict, external installer/Shizuku | App and pack IDs/keys unchanged; not exercised |
| Samsung use | No JVM substitute | Theme Park recognition/app mappings, One UI, Flip cover screen | Not tested |
| Main enforcement | Required `build`, `commit-conventions`, `documentation-audit` contexts | Active rule, empty bypasses, latest-push approval and PR merge state | Ruleset 24877351 active, main protected; three checks required; latest-push approval blocks #1 at closure read. Conversation resolution/strict checks disabled; exact settings in WORKFLOW |

Never add a live paid API request to routine CI. Mocked HTTP proves client handling,
not provider/model availability or icon recognizability. Record any user-authorized
device/provider trial with its date, app build and precise scenario before updating
the matrix status.

## Codex 02 preparation and read-only coverage

| Area | Automated coverage (passed at a8b3e4b2) | Physical/device status |
| --- | --- | --- |
| Full Create inventory | IconCoverageAnalyzerTest: 0/1/N, edited rows, multiple activities/search; CreateExtendFlowComposeTest: no-source select/clear and saved round trip | NOT RUN: actual launcher inventory sanity check |
| Source facts / local actionability | Fake reader: two packages, mapped-drawn vs broken/unmapped/unknown/unavailable, independent manual/fallback/applied/lock counts and overrides | NOT RUN: installed-pack coverage and lock sanity check |
| Repository parser / actual draw | CoverageAppFilterTest: empty/invalid/missing XML, exact alias/last winner/dynamic declarations; CoveragePreviewTest: real bitmap/vector/adaptive/transparent/fault/large draw | NOT RUN: real external APK resource edge cases |
| Work-user / stale reads | Pure uncertainty fixtures, one disabled collision row, package/index invalidation and rescan, cancellation; UI source-version selection reset, real scoped package broadcast/disposal and setup Back | NOT RUN: work profile and uninstall/reinstall/update |
| Profile/navigation/lifecycle | CreateExtendFlowComposeTest and all retained M1 session/shell/form tests: explicit once, Save/Cancel, same editor draft, setup restore, ingress and nested Back | NOT RUN: rotation/process death/dirty profile checks on phone |
| Accessibility/layout | New tags, checkbox semantics, 48dp, narrow and wide dark/light; existing hinge/Home tests retained | NOT RUN: real TalkBack, fonts, fold/Flip cover screen |
| Legacy functionality | Full unchanged AI1/AI2/batch/profile/backup/render/build suite, full Android compile/unit/APK and retained targeted M1 workflow | NOT RUN: GlobalOptions/manual AI/pack build-sign-install/update, Samsung One UI/Theme Park |

Fresh M2 counts/SHA/artifacts were independently verified in BASELINE: 669 full,
10 new UI and 28 coverage fixtures, no skips; final documentation-head checks must
remain green in PR #3. Test source alone is never passing evidence. No physical scenario or live
provider test ran, and the main review/conversation/strict gaps remain owner work.


## Codex 03 project persistence/gateway regression matrix

| Area | Automated evidence required on feature head | Device status |
| --- | --- | --- |
| v18→v19, fresh/reopen/default/duplicate profiles | Real exported-v18 SQLite fixture with icon/base/source/custom/lock/build/preset rows, current identity equivalence and FK validation | NOT RUN: physical update/downgrade recovery |
| Style save and ownership | Immutable ordered revisions, equal/concurrent saves, stale/cancelled writes, queued session-owner change, invalid schema/owner rejection | NOT RUN: process kill during disk/DB save |
| PNG lifecycle | Signature/CRC/truncation/empty/size/path/symlink/hash/missing tests, Upload deletion, history/variant pins, explicit orphan/temp GC | NOT RUN: storage exhaustion/URI providers |
| Full backup / shares | Verified embedded PNG/history/variants/selection, old full clear, new-owner rekey, ZIP duplicates/paths/bounds/checksums, failure keeps previous style, credential exclusion | NOT RUN: SAF restore/device signing update |
| Project UI | Actual panel input/import/reorder/Save/completion/failure/Back/Cancel/restoration/ingress/isolation and explicit global-copy action; target revalidation | NOT RUN: light/dark TalkBack/Flip/fold/rotation |
| Image gateway | Real existing-client MockWebServer original/ref order 0–5, Stability two requests, invalid input/credentials, error/timeout/cancel no retry, fake counts | NO live/paid API testing authorized |
| Legacy M1/M2/AI1/AI2 | Preserve original isolated/combined/form/create-extend/domain suites and unfiltered Android suite/APK | Existing manual editor/global/watch/share/backup/build/install matrix remains NOT RUN |

All Android execution is hosted; local Gradle download is blocked. 06e71532 passed
718/718, project UI 12/12, data/session 31/31, gateway/legacy 31/31 and all retained
M1/M2 selectors. Final documentation/probe-removal evidence must match the final PR
head; see BASELINE and its audit. New PNG references may be opaque; gateway output must still be square, visible and transparent. No fake coverage, automatic approval or price/cost consent is tested or claimed.

## Corrected regression boundaries

Strict persistence disables allowMainThreadQueries. Asset lease/queued GC, failed
grouped imports retaining prior/unrelated files, explicit removed/imported-draft
cleanup after restoration and fresh launcher/source Save validation passed in the
30-case group at 88933c95. Default source-only M2 broadcast assertions and explicit
launcher opt-in/disposal both passed. Real target Save retains version/completion/
exact-key/restoration assertions with correctly typed Long fake metadata.version.
No layout, clock, timeout, idle rule, test or original selector was weakened.

The added format-3 large and transparent gallery PNG byte-for-byte restore test
separately rejects those images as project references, with oversized gallery ZIP
negative coverage. It passed at 06e71532 in the nine-case ProjectAiBackupTest. Physical
process-kill, URI permissions, storage exhaustion, Samsung/Flip/One UI/TalkBack,
SAF/keystore and generated-pack install/update remain NOT RUN.


## Codex 04 analysis matrix — code checkpoint verified, device acceptance pending

Initial executable 53addf96 ran 789 tests with five fixture failures; actual group
counts and stack diagnoses are in BASELINE. Corrected code 47503e5c passed 792/792
and every isolated group, including same-profile mode/source guards, faithful v19
indexes/version and one-shot HTTP transmission. Exact closing-head checks belong
in PR #5; no code/test/schema/build change follows this verified checkpoint.

| Area | Deterministic evidence passed at 47503e5c | Physical/live boundary |
| --- | --- | --- |
| Source references | Three real PNG style families, sorted/diverse 8–12, mixed/duplicate/empty/low-alpha/broken/dynamic, 120 bound, stale/cancel; actual bitmap/vector/adaptive draw and owned-bitmap recycling; existing all-map parser | Installed monochrome/color packs, real themes, source update/uninstall NOT RUN |
| Schema/composer | Strict duplicate/depth/size/control/range/enum/null/version/palette validation, supported required-key schema, canonical/identity/user-priority escaping | Real model quality/self-assessment is NOT calibrated or tested |
| Responses adapter | MockWebServer ordered 1/5/8/12 data URLs, endpoint/model/text.format/store false, no local identity, usage/refusal/incomplete/UTF-8/size/auth/rate/server/cancel/redirect/timeout/zero retry | Account access/actual POST/billing NOT RUN; zero live requests |
| Durable request | Consent false/stale zero files/calls, REQUESTING before call, same UUID/concurrent/reopen no resend, uncertain crash/cancel, new retry consent, AtomicFile recovery/marker failure/quota/hash, cleanup/epochs/delete | Real process kill/storage fault requires device trial |
| STYLE UX | Actual root/dialogs/gestures with offline backend: CREATE/EXTEND, local samples, paid decline/confirm, draft edit/Use/Save, five ordered refs, stale/no-key/empty/error/uncertain, restoration/dark/light/wide/48dp | TalkBack, Samsung/One UI/Theme Park/cover/fold/rotation NOT RUN |
| Owner/save/restore | Real Room/assets/session guard, no active-on-result/Use, USER vs AUTO evidence, expected version/revision, dirty instruction, profile/catalog/source/epoch conflict; v19 file→v20 and fresh KSP identity; format 5/full/share/rekey and 3/4 legacy | Device install/update, all-store restore interruption and downgrade recovery NOT RUN |
| Runner/governance | 51/51 Python guards locally; independently counted genuine 135 XML/792 tests, all 16 groups, zero errors/skips/missing selectors; genuine schemas match source | Synthetic fault fixtures are never Android proof; exact final-head audit/Actions belongs in PR #5 |

No old suite was removed/ignored or timeout/idling policy changed. Local Android
failed before configuration at download; hosted native/Room/MockWebServer execution
is verified. Final documentation head must independently pass all original/new
groups, complete suite/APK and governance before the PR is ready. Device/live gaps
above remain NOT RUN; green CI is not owner or Play release approval.


## Codex 05 trials — verified code checkpoint, device acceptance pending

| Area | Automated coverage passed on0df78ef8 | Physical/live status |
| --- | --- | --- |
| Selection/frozen inputs | StyleTrialTargetSelectorTest/StyleTrialPlanTest: Create/Extend 0–3/many, covered override, unknown/work/lock/local protections, duplicate/tie/manual, vector/adaptive/empty/broken originals, strict plan and composer bounds | Actual launcher/source/theme updates NOT RUN |
| Real client/no-replay | StyleTrialGatewayTest plus unchanged legacy gateway/AI suites: original/ref order, alpha/count/config/privacy, unsafe 503 control vs one-shot OpenAI/both Stability, invalid first step, 307/308/auth/404/429/5xx/malformed/timeout/cancel | Account/model quality/cost NOT RUN; zero live requests |
| Journal/recovery | StyleTrialJournalTest: no-consent/stale/duplicate UUID/concurrent/serial, cold request/Stop, separate Stability markers, result-file/asset/DB faults with no gateway recovery, corrupt/quota/restore | Physical process death/full storage NOT RUN |
| Owned UI/controller | StyleTrialComposeTest/OwnedStyleTrialsTest: real shell route/style refinement, frozen decline/double tap, previews/48dp/checker/theme/wide/restoration, no key/refs/targets, explicit review/approval/regenerate, profile/source/lock/original/style invalidation, external editor priority | Samsung/TalkBack/hinge/rotation/One UI/Theme Park NOT RUN |
| Room/history/backup | StyleTrialPersistenceTest/StyleTrialMigrationTest and added ProjectAiBackupTest cases: original+RAW durable/pinned, review/approval/reuse, stale/corrupt/competing, genuine physical v20→v21/fresh, format6/payload3/full/share/rekey/legacy5 | Upgrade/SAF/signing/install/update NOT RUN |
| Full acceptance | All original groups, five additive M5 groups, unfiltered compile/unit/debug APK, actual XML and KSP19/20/21 plus Python guards/conventions/docs on one final SHA | Owner acceptance and merge remain separate |

Code0df78ef8 passed143 XML/856 tests/zero failure/error/skip, all21 commands and
selectors: M5 domain10/gateway8/journal12/UI12/persistence-session19, plus backup3
within retained project-data34. Populated deletion preserves valid redacted OpenAI/
Stability records, real counters, consumed UUIDs and other-owner files. Native
multipart factory/whole-body one-shot, numerical revision/style text alias rejection,
safe model/account errors and32,000-character prompt bounds passed.

No old assertion/selector is removed/ignored; migration assertions use genuine21
with real18/19/20 starting fixtures. KSP19/20/21 bytes match source. Exact final
closing-head full/filtered/schema/APK/governance checks belong to PR #6 before
readiness; the ancestor's856 result cannot substitute for them. Physical/live
scenarios remain NOT RUN and CI is not owner/release acceptance.
