# Current implementation

Codex 02 baseline: fresh merged main `73a15e2210cb55cf003e60ebc2033130ee7a9e58`,
inspected October 11, 2026. Owner Zsombor0918 merged PR #2 at 16:32:02 UTC;
its head `05dc320fd6dbdb1a16d4a5390d32c5617413e6a0` is an ancestor and the
merge tree is identical. Reviews/threads were empty; owner merge is acceptance,
not evidence of an independent APPROVED review. Codex 02 works separately on
`feat/create-extend-coverage-m2`; fresh feature CI is pending until recorded in QA.
Manifest, dependencies, Room v18, provider/storage contracts, APK/signing/install
and Android CI are unchanged. Existence in source and runtime validation are
different; see [QA baseline](../qa/BASELINE.md).

At head `8bd57953ec54c7056dac4acf850f5a8863b8bd64`, CI 38143988918 compiled
app/tests but failed the same two form-idle cases out of 628. Phone viewport,
bounded widths and earlier explicit clock did not fix them. Full XML and independent
controls subsequently established a Robolectric 4.14.1 WRAP_CONTENT field-dialog
measure/layout loop with custom 450 dp width. At the same SDK 33/native/800 dp height,
the actual form and shell passed at 320 dp; same measured 450×416 px explicit platform
bounds also passed. Clock, draw, SDK 35 and legacy graphics did not resolve it.
The two original tests now use compatible 320 dp width, all their assertions remain,
and three actual shared-form regressions plus isolated/combined CI were added.
Production code is unchanged from the audited starting head. At correction
`ae80fbd818eb17c98a7463f689603cfb94dcbaa3`, CI 38152173163 passed compile,
all unit tests and debug APK; 38152173157 passed original creation/ingress separately,
together and all three form regressions. At evidence head
`b9ec57b59671ffdb36f4f091706c8f7ae62825f3`, 38152705800 retained 116 XML
reports proving 631/631 full-suite tests passed with zero failures/errors/skips,
after the same separate/together/form tests; unfiltered compile/test/debug build
also passed. Original Android CI 38152705787 independently built/uploaded the APK.
PR #2 final head passed Android 38153469680, regressions 38153469669 and Governance 38154063977 before owner merge. These are historical M1 results, not M2 validation. Devices remain NOT RUN.
See [measured correction](../changes/2026-10-11-compose-dialog-regression-fix.md).

## Product and implemented paths

RenkinAI is a personal Renkin fork that turns app/pack/uploaded icons into a real
installable icon-pack APK on the phone, with optional paid external AI generation.
Ordinary icon creation/build can work offline; AI requires the chosen provider.

| Implemented area | Source evidence and boundary |
| --- | --- |
| Existing app list/editor | [MainActivity](../../app/src/main/kotlin/dev/renkinProject/renkin/MainActivity.kt), MainViewModel and UI; launcher components from InstalledAppCatalog, including user profiles; keys are package/activity, not labels |
| Projects entry point | [ProjectHome](../../app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectHome.kt) reads the real profile flow; ID-keyed cards show name, description, saved unbuilt flag and active-session unsaved edits; default/empty/loading/unavailable/busy states are explicit |
| Create / extend / classic entry | Create confirms the real profile form and waits for completion, then opens all known launcher targets without a source requirement. Extend chooses an external source by package, then explicitly chooses/creates a target profile through the existing Save/Cancel controller, then inspects actual source coverage. Project setup actions return to session selections; the complete classic editor and AI1/AI2 remain available |
| Home/setup/editor navigation | [RenkinAiRoot](../../app/src/main/kotlin/dev/renkinProject/renkin/ui/project/RenkinAiRoot.kt) uses the Activity's existing VM; editor Back returns Home after search/dialog handlers, Home clean Back exits, dirty Back offers return to editor or explicit exit without saving; no implicit save/switch/build/request on navigation |
| Pack sources, modifications, import | ApplicationProvider, IconGenerationService, IconGenerator, UploadedImageStore; custom/base/rendered separation and existing SVG/vector/adaptive paths |
| Named profiles | ProfileManager, DbApplication, DataPreferences; Room `renkinPack` v18, default id 1; settings snapshots switch with the profile, source/provenance locks remain |
| Export and installation | IconPackBuildService, IconPackBuilder, PackKeystore, ApkInstaller; calendar/clock/adaptive/themed outputs, appfilter/appmap/theme_resources; build snapshot persists to its owning profile |
| Backup/share/watch | data/transfer and data/watch plus WatchWorker/AutoBackupWorker; existing full-backup and profile-export semantics are retained |
| Single AI icon | [AiIconGenerationService](../../app/src/main/kotlin/dev/renkinProject/renkin/ai/AiIconGenerationService.kt), AiIconViewModel, IndividualOptions; original icon plus up to five reference files; PNG/alpha validation, 512 px gallery/draft persistence before preview; user accepts upload draft, then editor applies and Save/Build persists |
| Missing-icon batch | [AiBatchRunner](../../app/src/main/kotlin/dev/renkinProject/renkin/ai/AiBatchRunner.kt), AiBatchStore/ViewModel and GlobalOptions; searchable selection, app prompt, paid confirmation, serial calls, pause/cancel/manual resume and one-app retry |
| App instructions | AiPromptStore; DataStore keyed by hash of profile/component; empty inherits common style; 2000-character maximum |
| Provider/key settings | AiSettingsStore global settings; OpenAI and experimental Stability clients; AES-GCM credential ciphertext in noBackupFilesDir, Android Keystore aliases unchanged |

## Exact identity and build configuration

- Kotlin namespace/package: `dev.renkinProject.renkin`; display name: Renkin AI.
- Release app: `hu.zsombor.renkinai`; debug app: `hu.zsombor.renkinai.dev`.
- Generated default-profile pack: `${BuildConfig.APPLICATION_ID}.pack`; other
  profiles append `.p<profileId>`. Smali class package remains upstream.
- versionCode 56, versionName `2026.10.04-ai2`; JDK 17, Gradle 8.11.1,
  AGP 8.10.1; compileSdk 36, targetSdk 35, minSdk 23.
- App signing and generated-pack signing are different keys. Full Renkin backup
  carries the pack keystore/password; profile-only export does not. API keys and
  batch journals/draft markers are outside backups. See [architecture](../ARCHITECTURE.md).

## Known limitations and cautions

- Different-project/create requests offer Save or Cancel for dirty work. Success
  requires the profile load and baseline reset; repeated taps cannot enqueue a
  second insert. Process restoration reports interruption without replaying the
  request. Any committed profile stays visible by ID; a failed load can be reloaded.
  Profile creation and preference/ icon loading are not a cross-store transaction.
- Classic editor UI state is saved for Home round trips; icons remain
  in MainViewModel/ApplicationProvider. Session-only unsaved icons still do not
  survive process death without explicit save/build, as in the existing editor.
- On Editor, loading conceals classic content and blocks its taps while retaining
  Activity-result registrations across early profile-ID changes. Home does not
  keep a hidden editor. Profile form measurement is bounded to the dialog width;
  retry explicitly resets list loading rather than briefly reusing stale rows.
- Editor-entry versions use the VM SavedStateHandle, so a restored image/watch
  request is not lost before the shell consumes it. Intro dismissal distinguishes
  pending preference reads from an explicit Settings request to show the tour again.
- Watch requests/pending suggestions and VM-owned legacy prompts take priority over
  Home. Shared images open the editor and toast directs users to an app's Upload
  tab. The original URI instance-state key suppresses recreation imports; separately
  delivered shares can import the same URI again. Upload handling remains unchanged.
- Wide/dark/light/hinge and interaction tests use Robolectric/fake state; real
  TalkBack, Samsung/fold lifecycle, notification delivery and installer round trips
  still need the recorded device checks in QA.
- The existing **AI2 quick batch** definition of missing remains `createdIcon == null` and not locked. Fallbacks/custom icons make
  an app non-missing; it is not an audit of a source pack's appfilter coverage.
- Batch is a ViewModel coroutine plus AtomicFile journal, not an AI WorkManager
  or foreground service. Android can kill it. REQUESTING recovery without a saved
  matching result becomes FAILED/uncertain; no automatic rebilling retry occurs.
- Each successful batch step applies a custom base/rendered icon and saves the
  **whole active profile**, including earlier unsaved edits. Preview each result
  before building; batch acceptance is structural, not visual recognition.
- HTTP/network errors stop the batch. A cancelled/transient request may have been
  billed remotely. Stability uses one style reference and two provider calls;
  no live Stability/device verification is recorded for Codex 00.
- The hardcoded model list is configuration in source, not proof of current API
  availability. No provider account, pricing or live request is verified here.
- Theme Park/One UI/Flip cover-screen compatibility requires a real device.
  The historical manifest INTERNET comment predates AI and is incomplete; actual
  networking also includes OpenAiImageClient and StabilityImageClient.
- Source supports backup/migrations; this UI PR does not test an
  actual device upgrade or guarantee a reproducible CI debug signing identity.

## Codex 02 read-only preparation and limitations

[ProjectSetup](../../app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectSetup.kt)
and [TargetSelection](../../app/src/main/kotlin/dev/renkinProject/renkin/ui/project/TargetSelection.kt)
provide real search, stable component checkboxes, select all/recommended missing,
clear and counts. CREATE keeps manual/applied/fallback rows in the full launcher
inventory. EXTEND keeps source facts separate from local icons and locks. Selection
is UI preparation only, never input to quick batch or automatic generation. Primitive
selection/search/source context survives setup/editor/Home saved UI round trips;
there is no new project metadata store or durable selection guarantee.

[IconCoverageAnalyzer](../../app/src/main/kotlin/dev/renkinProject/renkin/apk/IconCoverageAnalyzer.kt)
uses the existing repository/parser. RESOLVABLE requires exact item mapping and a
successful nontransparent 96 px draw; declared missing/failed draw is BROKEN; a proven
index without an item is UNMAPPED. Invalid/missing XML is UNKNOWN, package change or
uninstall is UNAVAILABLE. Calendar/clock uncertainty and user identity remain UNKNOWN.
Fallback styling does not count as an exact mapped icon. The existing parser and
legacy last-valid duplicate winner remain unchanged, including ignoring null
attributes after a valid item; a sole missing drawable is still declared broken.

Local custom/manual and other-applied icons suppress recommendations; fallback can
still need replacement. Locks disable selection. Source-covered icons permit explicit
manual override. AI origin is never inferred from the existing DB. Room has no user
handle in its key: duplicate or non-current-user components are one unsupported,
disabled row with warning, not fictitious separate targets. A future user-key migration
requires its own decision and compatibility work.

Reads run off the main thread, sequentially, with cancellation between targets,
Boolean-only per-scan draw caching and one small preview at a time. Results carry
package/version/update/revision/theme plus profile/component/local snapshot. Reloads,
rapid source changes and leaving setup cancel prior jobs; resume and selected-package install/remove/update broadcasts revalidate package
identity. The receiver exists only while setup is composed; it never refreshes icons
or watches launcher apps in the background. Theme changes also invalidate reads. Explicit rescan rereads only the selected source or the existing pack catalog.
It does not change primary preferences, refresh icons, build, install or call AI.
A slow pathological drawable can delay cancellation during its own synchronous draw;
96 px validation cannot certify animation, monochrome or OEM behavior.

## Planned, not implemented

Codex 02 separate routes and read-only coverage exist in this branch; their fresh
Android/device acceptance is recorded separately in QA. Projects-specific style/asset persistence precedes automatic style recognition,
trial icons, batch staging and AI edits. Hosted backend authentication, credits,
automatic paid scheduling and Play publication remain future work. This Home
adds no new AI endpoint, schema or pack builder. Coverage is a bounded static draw, not dynamic launcher certification.
Follow [ROADMAP](../ROADMAP.md) and the next task in [HANDOFF](HANDOFF.md).
