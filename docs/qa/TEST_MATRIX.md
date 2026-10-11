# Regression and test matrix

Status is scoped to Codex 00. A test class existing in source is not a passed run.
Results and environment belong in [BASELINE](BASELINE.md) and each PR audit.

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
