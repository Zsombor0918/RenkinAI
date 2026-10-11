# Regression and test matrix

Status is scoped to Codex 00. A test class existing in source is not a passed run.
Results and environment belong in [BASELINE](BASELINE.md) and each PR audit.

| Area | Automated coverage / command | Required manual evidence | Codex 00 status |
| --- | --- | --- | --- |
| Repo guards | `python3 -m unittest discover -s scripts/tests -v`; valid/invalid commits, missing/empty notes, good docs, bad links, exceptions, hooks, exits | Git Bash/IDE hook setup on Windows | Offline suite passed; native Windows not run |
| App compile/unit/build | `compileDebugKotlin testDebugUnitTest assembleDebug`; existing JVM/Robolectric suite | Install the resulting APK and cold start | Local attempt blocked at Gradle download; PR CI must supply results |
| App list/identity | InstalledAppCatalogTest, ApplicationManagerComponentTest | Multiple launcher activities, work profile, reload | Tests exist; not rerun locally |
| Manual/custom/modifier icons | IconGeneratorTest, IconDraftStateTest, SVG/adaptive/outline tests | Select/import/edit/save/reopen; custom icon survives refresh | Not tested on device |
| Profiles and data | ProfileManagerTest, ProfileOperationGateTest, RenkinPackMigrationTest, backup tests | Unsaved switch, delete/default guards, upgrade/backup/restore | No schema change here; device coverage outstanding |
| Single AI generation | AiImagePipelineTest, AiPersistenceTest; mock HTTP | Original/ref order, alpha/quality, cancel, profile switch, preview accept | No live call; test existence only locally |
| Batch | AiBatchRunnerTest, AiPersistenceTest | 2–5 app trial, pause/cancel/background/process death, retry one uncertain app | Offline state-machine tests exist; no actual paid trial |
| Pack build/export | IconPackBuildServiceTest, LauncherFormatTest, PackProvenanceTest | appfilter/appmap/theme_resources, calendar/clock/adaptive/themed launcher behavior | Pipeline preserved; no device pack built here |
| Signing/install | PackKeystoreTest, InstallerSelectionTest, InstallFallbackTest | Stable key, same-package update/conflict, external installer/Shizuku | App and pack IDs/keys unchanged; not exercised |
| Samsung use | No JVM substitute | Theme Park recognition/app mappings, One UI, Flip cover screen | Not tested |
| Main enforcement | Required `build`, `commit-conventions`, `documentation-audit` contexts | Active rule, empty bypasses, review/conversation block in PR merge UI | Inactive observed; owner configuration pending |

Never add a live paid API request to routine CI. Mocked HTTP proves client handling,
not provider/model availability or icon recognizability. Record any user-authorized
device/provider trial with its date, app build and precise scenario before updating
the matrix status.
