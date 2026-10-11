# Regression and test matrix

Current checkpoint is Codex 01; Codex 00 results below are historical.
A test class existing in source is not a passed run.
Results and environment belong in [BASELINE](BASELINE.md) and each PR audit.

## Codex 01 — feature regression coverage

| Area | Automated coverage and boundary | Required device scenario | Current checkpoint |
| --- | --- | --- | --- |
| Home/accessibility | ProjectHomeComposeTest: states/IDs/duplicates/long titles, tags/descriptions, light/dark/hinge/48dp | TalkBack, fonts, phones/tablets/fold/cover/rotation | Passed in CI 38141510224; device NOT RUN |
| Navigation/Back | ProjectShellComposeTest: same editor handler, real dialog/search priority, route/draft restore, Home, dirty exit, refresh, Extend, intro dismissal during pending prefs | Full MainColumn nested editor Apply/Cancel, keyboard/dialogs | Two form-opening tests still FAILED at phone viewport in CI 38143988918; measured idler isolation pending; full Activity NOT RUN |
| Profiles/async | ProjectSessionTest: active no-op, Save/Cancel/origin, queued dirty check, delayed baseline, failure, one create/no replay | Existing/default/delete/import/restore, rapid taps, kill/relaunch | Fake backend and existing manager/gate/migration tests passed in CI 38141510224; device NOT RUN |
| External ingress | SharedImageIngressTest uses Activity's URI guard and SavedStateHandle counter; shell tests entry/pending-watch priority and actual ActivityResultRegistry during loading | Notification owning/missing profile, real SEND/Upload, URI grant/recreation | URI/counter and real result-registration tests passed in CI 38142645850; form/ingress case still failed idle; real delivery/device NOT RUN |
| Existing AI1/AI2 | Existing mock AiImagePipeline/AiPersistence/AiBatchRunner suites | Paid trial only under separate authorization | Passed in CI 38141510224; no new endpoint/client/batch behavior |
| Global/preview/build | Existing provider/build/signing/installer suites | GlobalOptions Apply/Back/result; BuildPackFab → WallpaperPreview → build/sign → install/update; installer onResume | Contracts/IDs unchanged; device NOT RUN |
| Data/source/OEM | Existing profile/backup/provenance/render suites | Source picker, backup/restore, default/delete, Theme Park/One UI/cover/fold, old pack update | No schema/SDK/permission/key changes; device NOT RUN |
| Governance/enforcement | 45 guard tests passed locally; final three checks must match final PR head | Owner enable reviews/conversations and explicitly accept audit | Active rule requires PR/checks; final feature CI pending |

Only successful final feature-head compile/unit/APK plus governance closes pending
CI. Fake-state tests do not prove actual MainActivity lifecycle, import, all old
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
